import { api, sessionRequestConfig } from "@/lib/api";
import type { OrderDto } from "@/services/checkoutService";

export type FulfillmentStatus = "PENDING" | "CONFIRMED" | "PROCESSING" | "SHIPPING" | "DELIVERED" | "CANCELLED" | "RETURNED";
export interface SellerOrderDetail { order: OrderDto; allowedStatuses: FulfillmentStatus[]; actionNote?: string; updatedAt?: string }
export interface SellerOrderPage { page: number; size: number; totalElements: number; totalPages: number; items: OrderDto[] }
export interface SellerOrderQuery { page: number; keyword?: string; status?: string; paymentStatus?: string }
export const sellerOrderService = {
  async list({ page, keyword, status, paymentStatus }: SellerOrderQuery): Promise<SellerOrderPage> {
    return (await api.get("/seller/orders", { ...sessionRequestConfig(), params: { page, size: 20, keyword, status, paymentStatus } })).data.data;
  },
  async detail(id: number): Promise<SellerOrderDetail> { return (await api.get(`/seller/orders/${id}`, sessionRequestConfig())).data.data; },
  async transition(id: number, expectedStatus: string, status: FulfillmentStatus): Promise<SellerOrderDetail> {
    return (await api.patch(`/seller/orders/${id}/status`, { expectedStatus, status }, sessionRequestConfig())).data.data;
  },
};
