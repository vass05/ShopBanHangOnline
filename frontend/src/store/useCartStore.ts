import { create } from "zustand";
import { persist } from "zustand/middleware";
import { CartItem, CartShopGroup } from "@/types";
import { MOCK_PRODUCTS } from "@/data/mockData";

interface CartState {
  items: CartItem[];
  selectedSkuIds: number[];

  // Actions
  addToCart: (item: Omit<CartItem, "quantity">, quantity: number) => void;
  updateQuantity: (skuId: number, quantity: number) => void;
  removeFromCart: (skuId: number) => void;
  removeSelected: () => void;
  clearCart: () => void;

  // Selection
  toggleSelect: (skuId: number) => void;
  toggleSelectShop: (shopId: number) => void;
  toggleSelectAll: () => void;

  // Computed Getters
  getShopGroups: () => CartShopGroup[];
  getSelectedItems: () => CartItem[];
  getSelectedTotal: () => number;
  getSelectedCount: () => number;
  getTotalBadgeCount: () => number;
  isAllSelected: () => boolean;
  isShopSelected: (shopId: number) => boolean;
}

// Initial sample cart items for immediate rich visual display
const INITIAL_CART_ITEMS: CartItem[] = [
  {
    skuId: 1001,
    productId: 1,
    productName: MOCK_PRODUCTS[0].name,
    skuCode: MOCK_PRODUCTS[0].productSkus[0].skuCode,
    attributes: MOCK_PRODUCTS[0].productSkus[0].attributes,
    price: MOCK_PRODUCTS[0].productSkus[0].price,
    originalPrice: MOCK_PRODUCTS[0].productSkus[0].originalPrice,
    quantity: 1,
    stockQuantity: MOCK_PRODUCTS[0].productSkus[0].stockQuantity,
    imageUrl: MOCK_PRODUCTS[0].images[0].imageUrl,
    shopId: MOCK_PRODUCTS[0].shop.id,
    shopName: MOCK_PRODUCTS[0].shop.shopName,
  },
  {
    skuId: 4001,
    productId: 4,
    productName: MOCK_PRODUCTS[3].name,
    skuCode: MOCK_PRODUCTS[3].productSkus[0].skuCode,
    attributes: MOCK_PRODUCTS[3].productSkus[0].attributes,
    price: MOCK_PRODUCTS[3].productSkus[0].price,
    originalPrice: MOCK_PRODUCTS[3].productSkus[0].originalPrice,
    quantity: 2,
    stockQuantity: MOCK_PRODUCTS[3].productSkus[0].stockQuantity,
    imageUrl: MOCK_PRODUCTS[3].images[0].imageUrl,
    shopId: MOCK_PRODUCTS[3].shop.id,
    shopName: MOCK_PRODUCTS[3].shop.shopName,
  },
];

export const useCartStore = create<CartState>()(
  persist(
    (set, get) => ({
      items: INITIAL_CART_ITEMS,
      selectedSkuIds: [1001, 4001],

      addToCart: (itemData, quantity) => {
        const { items, selectedSkuIds } = get();
        const existing = items.find((i) => i.skuId === itemData.skuId);

        if (existing) {
          const newQty = Math.min(existing.quantity + quantity, itemData.stockQuantity);
          set({
            items: items.map((i) =>
              i.skuId === itemData.skuId ? { ...i, quantity: newQty } : i
            ),
          });
        } else {
          const newItem: CartItem = { ...itemData, quantity };
          set({
            items: [...items, newItem],
            selectedSkuIds: [...selectedSkuIds, newItem.skuId],
          });
        }
      },

      updateQuantity: (skuId, quantity) => {
        if (quantity <= 0) {
          get().removeFromCart(skuId);
          return;
        }
        const { items } = get();
        set({
          items: items.map((i) => {
            if (i.skuId === skuId) {
              const clamped = Math.min(quantity, i.stockQuantity);
              return { ...i, quantity: clamped };
            }
            return i;
          }),
        });
      },

      removeFromCart: (skuId) => {
        const { items, selectedSkuIds } = get();
        set({
          items: items.filter((i) => i.skuId !== skuId),
          selectedSkuIds: selectedSkuIds.filter((id) => id !== skuId),
        });
      },

      removeSelected: () => {
        const { items, selectedSkuIds } = get();
        set({
          items: items.filter((i) => !selectedSkuIds.includes(i.skuId)),
          selectedSkuIds: [],
        });
      },

      clearCart: () => set({ items: [], selectedSkuIds: [] }),

      toggleSelect: (skuId) => {
        const { selectedSkuIds } = get();
        if (selectedSkuIds.includes(skuId)) {
          set({ selectedSkuIds: selectedSkuIds.filter((id) => id !== skuId) });
        } else {
          set({ selectedSkuIds: [...selectedSkuIds, skuId] });
        }
      },

      toggleSelectShop: (shopId) => {
        const { items, selectedSkuIds } = get();
        const shopItems = items.filter((i) => i.shopId === shopId);
        const shopSkuIds = shopItems.map((i) => i.skuId);
        const allShopSelected = shopSkuIds.every((id) => selectedSkuIds.includes(id));

        if (allShopSelected) {
          set({ selectedSkuIds: selectedSkuIds.filter((id) => !shopSkuIds.includes(id)) });
        } else {
          const union = Array.from(new Set([...selectedSkuIds, ...shopSkuIds]));
          set({ selectedSkuIds: union });
        }
      },

      toggleSelectAll: () => {
        const { items, isAllSelected } = get();
        if (isAllSelected()) {
          set({ selectedSkuIds: [] });
        } else {
          set({ selectedSkuIds: items.map((i) => i.skuId) });
        }
      },

      getShopGroups: () => {
        const { items } = get();
        const groupsMap = new Map<number, CartShopGroup>();

        items.forEach((item) => {
          if (!groupsMap.has(item.shopId)) {
            groupsMap.set(item.shopId, {
              shopId: item.shopId,
              shopName: item.shopName,
              items: [],
              subtotal: 0,
            });
          }
          const group = groupsMap.get(item.shopId)!;
          group.items.push(item);
          group.subtotal += item.price * item.quantity;
        });

        return Array.from(groupsMap.values());
      },

      getSelectedItems: () => {
        const { items, selectedSkuIds } = get();
        return items.filter((i) => selectedSkuIds.includes(i.skuId));
      },

      getSelectedTotal: () => {
        const { items, selectedSkuIds } = get();
        return items
          .filter((i) => selectedSkuIds.includes(i.skuId))
          .reduce((sum, i) => sum + i.price * i.quantity, 0);
      },

      getSelectedCount: () => {
        const { items, selectedSkuIds } = get();
        return items
          .filter((i) => selectedSkuIds.includes(i.skuId))
          .reduce((sum, i) => sum + i.quantity, 0);
      },

      getTotalBadgeCount: () => {
        const { items } = get();
        return items.reduce((sum, i) => sum + i.quantity, 0);
      },

      isAllSelected: () => {
        const { items, selectedSkuIds } = get();
        if (items.length === 0) return false;
        return items.every((i) => selectedSkuIds.includes(i.skuId));
      },

      isShopSelected: (shopId) => {
        const { items, selectedSkuIds } = get();
        const shopItems = items.filter((i) => i.shopId === shopId);
        if (shopItems.length === 0) return false;
        return shopItems.every((i) => selectedSkuIds.includes(i.skuId));
      },
    }),
    {
      name: "helishop-cart-storage",
      partialize: (state) => ({
        items: state.items,
        selectedSkuIds: state.selectedSkuIds,
      }),
    }
  )
);
