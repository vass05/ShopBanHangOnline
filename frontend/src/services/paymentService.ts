import { api, sessionRequestConfig } from "@/lib/api";
import type { OrderDto } from "@/services/checkoutService";

export interface VerifiedReturn {
  verified: boolean; orderCode?: string; transactionCode?: string; amount: number;
  status: string; responseCode?: string; message: string;
}
export type PaymentView = "paid" | "placed" | "pending" | "failed" | "invalid" | "cancelled" | "closed-paid" | "refunded";

export function paymentView(order: OrderDto, result?: VerifiedReturn | null): PaymentView {
  const closed = ["CANCELLED", "RETURNED"].includes(order.orderStatus);
  if (order.paymentStatus === "REFUNDED") return "refunded";
  if (order.paymentStatus === "PAID") return closed ? "closed-paid" : "paid";
  if (closed) return "cancelled";
  if (result && !result.verified) return "invalid";
  if (order.paymentMethod === "COD") return "placed";
  if (order.paymentStatus === "FAILED" || result?.status === "FAILED") return "failed";
  return "pending";
}

export const paymentService = {
  async createUrl(orderCode: string): Promise<string> {
    const result = await api.post("/payments/create-vnpay-url", { orderCode }, sessionRequestConfig());
    const value = result.data?.data?.paymentUrl;
    if (typeof value !== "string" || !/^https?:\/\//i.test(value)) throw new Error("Không nhận được URL thanh toán hợp lệ");
    return value;
  },
  async verifyReturn(params: Record<string, string>): Promise<VerifiedReturn> {
    return (await api.get("/payments/vnpay-return", { ...sessionRequestConfig(), params })).data.data;
  },
};
