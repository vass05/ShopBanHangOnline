import { api, sessionRequestConfig } from "@/lib/api";

export interface CheckoutPayload {
  addressId: number; shippingMethod: "standard" | "express" | "economy";
  paymentMethod: "COD" | "VNPAY"; voucherCode?: string; note?: string;
  items: { skuId: number; quantity: number }[];
}
export interface OrderDto {
  id: number; orderCode: string; customerId: number; shopId: number; shopName: string;
  totalAmount: number; shippingFee: number; discountAmount: number; finalAmount: number;
  orderStatus: string; paymentStatus: string; paymentMethod: "COD" | "VNPAY";
  paymentExpiresAt?: string;
  shippingAddressSnapshot: string; note?: string; createdAt: string;
  items: { id: number; skuId: number; productId: number; skuCode: string; imageUrl: string;
    productNameSnapshot: string; skuVariantSnapshot: string; quantity: number; priceAtPurchase: number; subtotal: number }[];
}
export interface CheckoutQuote {
  totalAmount: number; shippingFee: number; discountAmount: number; finalAmount: number;
  shops: { shopId: number; shopName: string; totalAmount: number; shippingFee: number; discountAmount: number; finalAmount: number;
    items: { skuId: number; productId: number; productName: string; imageUrl: string; quantity: number; unitPrice: number; subtotal: number }[] }[];
}

export function checkoutAttemptKey(userId: number, payload: CheckoutPayload): string {
  const storageKey = `helishop-checkout-attempt:${userId}`;
  const fingerprint = JSON.stringify(payload);
  try {
    const previous = JSON.parse(sessionStorage.getItem(storageKey) || "null");
    if (previous?.fingerprint === fingerprint && typeof previous.key === "string") return previous.key;
  } catch { /* Replace invalid local metadata; backend still validates the request. */ }
  const key = Array.from(crypto.getRandomValues(new Uint8Array(16)), byte => byte.toString(16).padStart(2, "0")).join("");
  sessionStorage.setItem(storageKey, JSON.stringify({ fingerprint, key }));
  return key;
}

export const checkoutService = {
  async quote(payload: CheckoutPayload): Promise<CheckoutQuote> {
    return (await api.post("/orders/quote", payload, sessionRequestConfig())).data.data;
  },
  async create(payload: CheckoutPayload, key: string): Promise<OrderDto[]> {
    return (await api.post("/orders/checkout-batch", payload, { ...sessionRequestConfig(), headers: { "Idempotency-Key": key } })).data.data;
  },
};
