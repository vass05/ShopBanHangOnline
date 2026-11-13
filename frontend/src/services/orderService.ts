import { api, sessionRequestConfig } from "@/lib/api";
import type { OrderDto } from "@/services/checkoutService";

export const orderService = {
  async mine(): Promise<OrderDto[]> { return (await api.get("/orders/me", sessionRequestConfig())).data.data; },
  async byCode(code: string): Promise<OrderDto> { return (await api.get(`/orders/code/${encodeURIComponent(code)}`, sessionRequestConfig())).data.data; },
  async cancel(id: number): Promise<OrderDto> { return (await api.patch(`/orders/${id}/status`, null, { ...sessionRequestConfig(), params: { status: "CANCELLED" } })).data.data; },
};
