import React, { useEffect, useMemo, useState } from "react";
import { useSearchParams, Link } from "react-router-dom";
import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/layout/Footer";
import { useAuthStore } from "@/store/useAuthStore";
import { orderService } from "@/services/orderService";
import { paymentService, paymentView } from "@/services/paymentService";
import type { VerifiedReturn, PaymentView } from "@/services/paymentService";
import type { OrderDto } from "@/services/checkoutService";
import { formatVND } from "@/lib/formatters";

const messages: Record<PaymentView, [string, string]> = {
  paid: ["Thanh toán thành công", "Máy chủ đã xác nhận thanh toán của đơn hàng."],
  placed: ["Đã tạo đơn COD", "Bạn thanh toán khi nhận hàng. Theo dõi tiến độ tại đơn mua."],
  pending: ["Đang chờ xác nhận thanh toán", "Đơn hàng vẫn được giữ. Kết quả sẽ cập nhật khi máy chủ nhận xác nhận thanh toán."],
  failed: ["Thanh toán chưa thành công", "Bạn có thể mở đơn mua và thử thanh toán lại."],
  invalid: ["Chưa xác minh được kết quả", "Thông tin trở về không hợp lệ. Vui lòng kiểm tra trạng thái trong đơn mua."],
  cancelled: ["Đơn hàng đã đóng", "Đơn đã hủy hoặc trả hàng và không được kích hoạt lại từ trang thanh toán."],
  "closed-paid": ["Thanh toán cần được đối soát", "Máy chủ ghi nhận thanh toán cho đơn đã đóng. Vui lòng liên hệ hỗ trợ để xử lý hoàn tiền."],
  refunded: ["Đã ghi nhận hoàn tiền", "Kiểm tra chi tiết trong đơn mua của bạn."],
};

export const PaymentResultPage: React.FC = () => {
  const [params] = useSearchParams();
  const { isAuthenticated, user } = useAuthStore();
  const orderCode = params.get("vnp_TxnRef") || params.get("orderCode");
  const callback = useMemo(() => Object.fromEntries([...params.entries()].filter(([key]) => key.startsWith("vnp_"))), [params]);
  const [order, setOrder] = useState<OrderDto | null>(null);
  const [verified, setVerified] = useState<VerifiedReturn | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [retry, setRetry] = useState(0);

  useEffect(() => {
    let active = true;
    let timer: ReturnType<typeof setTimeout> | undefined;
    setOrder(null); setVerified(null); setError("");
    if (!orderCode || !isAuthenticated) return;
    setLoading(true);
    let result: VerifiedReturn | null = null;
    const load = async (attempt: number) => {
      try {
        if (attempt === 0 && callback.vnp_SecureHash) {
          result = await paymentService.verifyReturn(callback);
          if (!active) return;
          setVerified(result);
        }
        const data = await orderService.byCode(orderCode);
        if (!active) return;
        setOrder(data);
        if (paymentView(data, result) === "pending" && attempt < 15) timer = setTimeout(() => void load(attempt + 1), 2000);
      } catch (error: unknown) {
        if (active) setError((error as { response?: { data?: { message?: string } } }).response?.data?.message || "Chưa tải được kết quả. Vui lòng kiểm tra lại đơn mua.");
      } finally { if (active) setLoading(false); }
    };
    void load(0);
    return () => { active = false; clearTimeout(timer); };
  }, [orderCode, callback, isAuthenticated, user?.id, retry]);

  const view = order ? paymentView(order, verified) : null;
  const redirect = `/payment-result?${params.toString()}`;
  return <div className="min-h-screen bg-[#F5F5FA] flex flex-col">
    <Header />
    <main className="flex-1 max-w-3xl w-full mx-auto px-4 py-10">
      <section className="bg-white border rounded-3xl p-8 space-y-5 text-center">
        <h1 className="text-2xl font-bold">{view ? messages[view][0] : "Kết quả thanh toán"}</h1>
        {!orderCode && <p role="alert">Không có mã đơn hàng để tra cứu.</p>}
        {orderCode && !isAuthenticated && <p>Vui lòng <Link className="text-sky-700 underline" to={`/login?redirect=${encodeURIComponent(redirect)}`}>đăng nhập</Link> để xem trạng thái đơn hàng của bạn.</p>}
        {loading && <p role="status">Đang kiểm tra với máy chủ…</p>}
        {error && <p role="alert" className="text-red-600">{error}</p>}
        {view && <p className="text-slate-600">{messages[view][1]}</p>}
        {order && <dl className="bg-slate-50 border rounded-xl p-5 text-left text-sm space-y-3">
          <div className="flex justify-between gap-3"><dt>Mã đơn</dt><dd className="font-mono break-all">{order.orderCode}</dd></div>
          <div className="flex justify-between gap-3"><dt>Số tiền đơn hàng</dt><dd className="font-bold text-sky-700">{formatVND(Number(order.finalAmount))}</dd></div>
          <div className="flex justify-between gap-3"><dt>Phương thức</dt><dd>{order.paymentMethod}</dd></div>
          {verified?.verified && verified.transactionCode && <div className="flex justify-between gap-3"><dt>Mã giao dịch</dt><dd>{verified.transactionCode}</dd></div>}
        </dl>}
        <div className="flex flex-wrap justify-center gap-4 text-sm">
          {isAuthenticated && orderCode && <button disabled={loading} className="border rounded-lg px-4 py-2" onClick={() => setRetry(value => value + 1)}>Kiểm tra lại</button>}
          <Link to="/orders" className="bg-sky-600 text-white rounded-lg px-4 py-2">Xem đơn mua</Link>
          <Link to="/" className="border rounded-lg px-4 py-2">Tiếp tục mua sắm</Link>
        </div>
      </section>
    </main>
    <Footer />
  </div>;
};
