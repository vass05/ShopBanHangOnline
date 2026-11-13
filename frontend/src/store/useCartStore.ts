import { create } from "zustand";
import type { CartItem, CartShopGroup } from "@/types";
import { cartService, mapCart } from "@/services/cartService";
import { readAuthSession, subscribeAuthSession } from "@/lib/authSession";

interface CartState {
  items: CartItem[];
  selectedSkuIds: number[];
  loading: boolean;
  error: string | null;
  fetchCart: () => Promise<boolean>;
  addSku: (skuId: number, quantity: number) => Promise<boolean>;
  addToCart: (item: Omit<CartItem, "quantity">, quantity: number) => Promise<boolean>;
  updateQuantity: (skuId: number, quantity: number) => Promise<boolean>;
  removeFromCart: (skuId: number) => Promise<boolean>;
  removeSelected: () => Promise<boolean>;
  clearCart: () => Promise<boolean>;
  toggleSelect: (skuId: number) => void;
  toggleSelectShop: (shopId: number) => void;
  toggleSelectAll: () => void;
  getShopGroups: () => CartShopGroup[];
  getSelectedItems: () => CartItem[];
  getSelectedTotal: () => number;
  getSelectedCount: () => number;
  getTotalBadgeCount: () => number;
  isAllSelected: () => boolean;
  isShopSelected: (shopId: number) => boolean;
}

const GUEST_KEY = "helishop-guest-cart-v3";
function guestItems(): CartItem[] {
  try {
    const items = JSON.parse(localStorage.getItem(GUEST_KEY) || "[]");
    return Array.isArray(items) ? items.filter(item => Number.isInteger(item.skuId) && item.quantity > 0) : [];
  } catch { return []; }
}
const identity = () => {
  const session = readAuthSession();
  return session ? `${session.user.id}:${session.refreshToken}` : "guest";
};
let owner = identity();
let epoch = 0;
let queue: Promise<unknown> = Promise.resolve();

// Do not import the old unscoped cart, which could belong to another signed-in user.
localStorage.removeItem("helishop-cart-storage");
localStorage.removeItem("helishop-cart-storage-v2");

export const useCartStore = create<CartState>()((set, get) => {
  const applyItems = (items: CartItem[], selectSku?: number, initial = false) => {
    const selected = initial ? items.map(item => item.skuId) : get().selectedSkuIds;
    set({ items, selectedSkuIds: items.filter(item => selected.includes(item.skuId) || item.skuId === selectSku).map(item => item.skuId) });
    if (!readAuthSession()) localStorage.setItem(GUEST_KEY, JSON.stringify(items));
  };
  const run = (action: (isCurrent: () => boolean) => Promise<void>) => {
    const requestEpoch = epoch;
    set({ loading: true, error: null });
    const result = queue.then(async () => {
      if (requestEpoch !== epoch) return false;
      try {
        await action(() => requestEpoch === epoch);
        return requestEpoch === epoch;
      } catch (error: unknown) {
        if (requestEpoch === epoch) {
          const failure = error as { response?: { data?: { message?: string } }; message?: string };
          set({ error: failure.response?.data?.message || failure.message || "Không thể cập nhật giỏ hàng. Vui lòng thử lại." });
        }
        return false;
      } finally {
        if (requestEpoch === epoch) set({ loading: false });
      }
    });
    queue = result;
    return result;
  };
  const remove = (skuIds: number[]) => run(async isCurrent => {
    if (!skuIds.length) return;
    if (readAuthSession()) {
      const dto = await cartService.remove(skuIds);
      if (isCurrent()) applyItems(mapCart(dto));
    } else {
      applyItems(get().items.filter(item => !skuIds.includes(item.skuId)));
    }
  });

  return {
    items: owner === "guest" ? guestItems() : [],
    selectedSkuIds: owner === "guest" ? guestItems().map(item => item.skuId) : [],
    loading: false, error: null,
    addSku: (skuId, quantity) => run(async isCurrent => {
      if (!readAuthSession()) throw new Error("Vui lòng đăng nhập để mua lại");
      if (!Number.isInteger(quantity) || quantity < 1) throw new Error("Số lượng không hợp lệ");
      const dto = await cartService.add(skuId, quantity);
      if (isCurrent()) applyItems(mapCart(dto), skuId);
    }),
    fetchCart: () => run(async isCurrent => {
      if (!readAuthSession()) { applyItems(guestItems(), undefined, true); return; }
      let dto = await cartService.get();
      if (!isCurrent()) return;
      applyItems(mapCart(dto), undefined, get().items.length === 0);
      // Merge by maximum quantity rather than addition: retry after an ambiguous response cannot double the quantity.
      for (const guest of guestItems()) {
        if (!isCurrent()) return;
        const existing = mapCart(dto).find(item => item.skuId === guest.skuId);
        dto = existing
          ? await cartService.update(guest.skuId, Math.max(existing.quantity, guest.quantity))
          : await cartService.add(guest.skuId, guest.quantity);
        if (!isCurrent()) return;
        localStorage.setItem(GUEST_KEY, JSON.stringify(guestItems().filter(item => item.skuId !== guest.skuId)));
      }
      if (isCurrent()) applyItems(mapCart(dto), undefined, get().items.length === 0);
    }),
    addToCart: (item, quantity) => run(async isCurrent => {
      if (!Number.isInteger(quantity) || quantity < 1) throw new Error("Số lượng phải là số nguyên dương");
      if (readAuthSession()) {
        const dto = await cartService.add(item.skuId, quantity);
        if (isCurrent()) applyItems(mapCart(dto), item.skuId);
      } else {
        const old = get().items.find(entry => entry.skuId === item.skuId);
        const nextQuantity = (old?.quantity || 0) + quantity;
        if (nextQuantity > item.stockQuantity) throw new Error("Số lượng vượt quá tồn kho");
        const next = { ...item, quantity: nextQuantity };
        applyItems([...get().items.filter(entry => entry.skuId !== item.skuId), next], item.skuId);
      }
    }),
    updateQuantity: (skuId, quantity) => run(async isCurrent => {
      if (!Number.isInteger(quantity) || quantity < 0) throw new Error("Số lượng không hợp lệ");
      if (readAuthSession()) {
        const dto = await cartService.update(skuId, quantity);
        if (isCurrent()) applyItems(mapCart(dto));
      } else {
        const item = get().items.find(entry => entry.skuId === skuId);
        if (item && quantity > item.stockQuantity) throw new Error("Số lượng vượt quá tồn kho");
        applyItems(get().items.flatMap(entry => entry.skuId !== skuId ? [entry] : quantity ? [{ ...entry, quantity }] : []));
      }
    }),
    removeFromCart: skuId => remove([skuId]),
    removeSelected: () => remove([...get().selectedSkuIds]),
    clearCart: () => run(async isCurrent => {
      if (readAuthSession()) await cartService.clear();
      if (isCurrent()) applyItems([]);
    }),
    toggleSelect: skuId => set({ selectedSkuIds: get().selectedSkuIds.includes(skuId) ? get().selectedSkuIds.filter(id => id !== skuId) : [...get().selectedSkuIds, skuId] }),
    toggleSelectShop: shopId => {
      const ids = get().items.filter(item => item.shopId === shopId).map(item => item.skuId);
      set({ selectedSkuIds: get().isShopSelected(shopId) ? get().selectedSkuIds.filter(id => !ids.includes(id)) : [...new Set([...get().selectedSkuIds, ...ids])] });
    },
    toggleSelectAll: () => set({ selectedSkuIds: get().isAllSelected() ? [] : get().items.map(item => item.skuId) }),
    getShopGroups: () => {
      const groups = new Map<number, CartShopGroup>();
      for (const item of get().items) {
        const group = groups.get(item.shopId) || { shopId: item.shopId, shopName: item.shopName, items: [], subtotal: 0 };
        group.items.push(item);
        group.subtotal += item.price * item.quantity;
        groups.set(item.shopId, group);
      }
      return [...groups.values()];
    },
    getSelectedItems: () => get().items.filter(item => get().selectedSkuIds.includes(item.skuId)),
    getSelectedTotal: () => get().getSelectedItems().reduce((sum, item) => sum + item.price * item.quantity, 0),
    getSelectedCount: () => get().getSelectedItems().reduce((sum, item) => sum + item.quantity, 0),
    getTotalBadgeCount: () => get().items.reduce((sum, item) => sum + item.quantity, 0),
    isAllSelected: () => get().items.length > 0 && get().items.every(item => get().selectedSkuIds.includes(item.skuId)),
    isShopSelected: shopId => {
      const items = get().items.filter(item => item.shopId === shopId);
      return items.length > 0 && items.every(item => get().selectedSkuIds.includes(item.skuId));
    },
  };
});

subscribeAuthSession(() => {
  const nextOwner = identity();
  if (owner === nextOwner) return;
  owner = nextOwner;
  epoch++;
  useCartStore.setState({ items: [], selectedSkuIds: [], error: null, loading: false });
  void useCartStore.getState().fetchCart();
});
if (readAuthSession()) void useCartStore.getState().fetchCart();
