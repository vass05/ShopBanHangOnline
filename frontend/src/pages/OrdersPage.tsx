import React, { useEffect, useMemo, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/layout/Footer";
import { useOrderStore } from "@/store/useOrderStore";
import { useAuthStore } from "@/store/useAuthStore";
import { useCartStore } from "@/store/useCartStore";
import { api, sessionRequestConfig } from "@/lib/api";
import { readAuthSession } from "@/lib/authSession";
import type { OrderDto } from "@/services/checkoutService";
import { formatVND } from "@/lib/formatters";

const statusLabels: Record<string, string> = { PENDING: "Chờ xác nhận", CONFIRMED: "Đã xác nhận", PROCESSING: "Đang xử lý", SHIPPING: "Đang giao", DELIVERED: "Đã giao", CANCELLED: "Đã hủy", RETURNED: "Đã trả hàng" };
const paymentLabels: Record<string, string> = { UNPAID: "Chưa thanh toán", PENDING: "Chờ thanh toán", PAID: "Đã thanh toán", FAILED: "Thanh toán thất bại", REFUNDED: "Đã hoàn tiền" };

export const OrdersPage: React.FC = () => {
  const navigate = useNavigate();
  const { user } = useAuthStore();
  const { orders, loading, error, fetchOrders, cancelOrder } = useOrderStore();
  const { addSku, error: cartError } = useCartStore();
  const [tab, setTab] = useState("ALL");
  const [busy, setBusy] = useState(false);
  const [actionError, setActionError] = useState("");
  useEffect(() => { void fetchOrders(); }, [user?.id, fetchOrders]);
  const visible = useMemo(() => orders.filter(order => tab === "ALL" || order.orderStatus === tab)
    .sort((a, b) => b.id - a.id), [orders, tab]);

  const cancel = async (id: number) => {
    if (!window.confirm("Hủy đơn hàng này?")) return;
    await cancelOrder(id);
  };
  const rebuy = async (order: OrderDto) => {
    setBusy(true); setActionError("");
    for (const item of order.items) {
      if (!item.skuId || !await addSku(item.skuId, item.quantity)) {
        setActionError("Một số sản phẩm chưa thêm được. Kiểm tra giỏ hàng và thông báo tồn kho trước khi thử lại.");
        setBusy(false); return;
      }
    }
    setBusy(false); navigate("/cart");
  };
  const pay = async (orderCode: string) => {
    setBusy(true); setActionError("");
    const token = readAuthSession()?.refreshToken;
    try {
      const result = await api.post("/payments/create-vnpay-url", { orderCode }, sessionRequestConfig());
      if (readAuthSession()?.refreshToken !== token) return;
      if (!result.data?.data?.paymentUrl) throw new Error("Không nhận được URL thanh toán");
      window.location.assign(result.data.data.paymentUrl);
    } catch (error: unknown) {
      setActionError((error as { response?: { data?: { message?: string } } }).response?.data?.message || "Chưa tạo được yêu cầu thanh toán. Đơn hàng vẫn được giữ để thử lại.");
    } finally { setBusy(false); }
  };

  return <div className="min-h-screen bg-[#F5F5FA] flex flex-col">
    <Header />
    <main className="max-w-5xl w-full mx-auto px-4 py-8 flex-1 space-y-5">
      <div className="flex justify-between items-center"><h1 className="text-2xl font-bold">Đơn mua của tôi</h1><button onClick={() => void fetchOrders()} disabled={loading || busy} className="text-sky-700 underline">Làm mới</button></div>
      {(error || actionError || cartError) && <p role="alert" className="bg-red-50 text-red-700 rounded-xl p-4">{error || actionError || cartError}</p>}
      <nav className="bg-white rounded-xl border flex overflow-x-auto" aria-label="Trạng thái đơn hàng">
        {[["ALL", "Tất cả"], ...Object.entries(statusLabels)].map(([value, label]) => <button key={value} onClick={() => setTab(value)} className={`p-3 text-sm whitespace-nowrap ${tab === value ? "text-sky-700 border-b-2 border-sky-600 font-bold" : "text-slate-500"}`}>{label}</button>)}
      </nav>
      {loading && <p role="status">Đang cập nhật đơn hàng…</p>}
      {!loading && !visible.length && <section className="bg-white border rounded-2xl p-10 text-center space-y-3"><p>Chưa có đơn hàng trong mục này.</p><Link to="/" className="text-sky-700 underline">Khám phá sản phẩm</Link></section>}
      {visible.map(order => <article key={order.id} className="bg-white border rounded-2xl p-5 space-y-4">
        <div className="flex flex-wrap justify-between gap-2"><div><h2 className="font-bold">{order.shopName || "Đơn hàng"}</h2><p className="text-xs font-mono text-slate-500">{order.orderCode}</p></div><div className="text-right text-sm"><p className="font-bold text-sky-700">{statusLabels[order.orderStatus] || order.orderStatus}</p><p>{paymentLabels[order.paymentStatus] || order.paymentStatus} · {order.paymentMethod}</p></div></div>
        {order.items.map(item => <div key={item.id} className="border-t pt-3 flex gap-3 items-center">
          {item.imageUrl && <img src={item.imageUrl} alt="" className="w-16 h-16 object-cover rounded-lg" />}
          <div className="flex-1 text-sm"><p className="font-semibold">{item.productNameSnapshot}</p><p className="text-slate-500">{item.skuCode} · Số lượng: {item.quantity}</p></div>
          <span className="text-sm">{formatVND(Number(item.subtotal))}</span>
        </div>)}
        <p className="text-sm text-slate-500">Giao đến: {order.shippingAddressSnapshot}</p>
        <div className="text-right text-sm space-y-1"><p>Tiền hàng {formatVND(Number(order.totalAmount))} + vận chuyển {formatVND(Number(order.shippingFee || 0))} − giảm giá {formatVND(Number(order.discountAmount || 0))}</p><p className="font-bold text-lg text-sky-700">Tổng: {formatVND(Number(order.finalAmount))}</p></div>
        <div className="flex flex-wrap justify-end gap-3 text-sm">
          {["PENDING", "CONFIRMED"].includes(order.orderStatus) && order.paymentStatus !== "PAID" && <button disabled={loading || busy} onClick={() => void cancel(order.id)} className="border rounded-lg px-4 py-2 disabled:opacity-50">Hủy đơn</button>}
          {order.paymentMethod === "VNPAY" && ["UNPAID", "PENDING", "FAILED"].includes(order.paymentStatus) && !["CANCELLED", "RETURNED", "DELIVERED"].includes(order.orderStatus) && <button disabled={loading || busy} onClick={() => void pay(order.orderCode)} className="bg-sky-600 text-white rounded-lg px-4 py-2 disabled:opacity-50">Thanh toán</button>}
          <button disabled={loading || busy} onClick={() => void rebuy(order)} className="border border-sky-600 text-sky-700 rounded-lg px-4 py-2 disabled:opacity-50">Mua lại</button>
        </div>
      </article>)}
    </main>
    <Footer />
  </div>;
};
