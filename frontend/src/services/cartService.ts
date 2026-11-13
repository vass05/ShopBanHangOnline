import { api, sessionRequestConfig } from "@/lib/api";
import type { CartItem } from "@/types";

export interface CartItemDto {
  skuId: number; productId: number; shopId: number; shopName: string;
  productTitle: string; skuCode?: string; skuVariant: string | null;
  imageUrl: string; price: number; quantity: number; stockAvailable: number;
}
export interface CartDto {
  userId: number;
  shops: { shopId: number; shopName: string; items: CartItemDto[]; shopTotal: number; shopItemCount: number }[];
  totalItems: number; totalQuantity: number; totalAmount: number;
}

export function mapCart(dto: CartDto): CartItem[] {
  return dto.shops.flatMap(shop => shop.items.map(item => {
    let attributes: Record<string, string> = {};
    try {
      const parsed = JSON.parse(item.skuVariant || "{}");
      if (parsed && typeof parsed === "object" && !Array.isArray(parsed)) attributes = parsed;
    } catch { /* Older carts can contain a plain SKU code instead of JSON attributes. */ }
    return {
      skuId: item.skuId, productId: item.productId, shopId: shop.shopId, shopName: shop.shopName,
      productName: item.productTitle, skuCode: item.skuCode || "", attributes,
      imageUrl: item.imageUrl, price: Number(item.price), quantity: item.quantity, stockQuantity: item.stockAvailable,
    };
  }));
}

export const cartService = {
  async get(): Promise<CartDto> { return (await api.get("/cart", sessionRequestConfig())).data.data; },
  async add(skuId: number, quantity: number): Promise<CartDto> { return (await api.post("/cart/items", { skuId, quantity }, sessionRequestConfig())).data.data; },
  async update(skuId: number, quantity: number): Promise<CartDto> { return (await api.put(`/cart/items/${skuId}`, { quantity }, sessionRequestConfig())).data.data; },
  async remove(skuIds: number[]): Promise<CartDto> { return (await api.delete("/cart/items", { ...sessionRequestConfig(), data: { skuIds } })).data.data; },
  async clear(): Promise<void> { await api.delete("/cart", sessionRequestConfig()); },
};
