import { useEffect, useRef, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { isAxiosError } from "axios";
import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/layout/Footer";
import { useAuthStore } from "@/store/useAuthStore";
import { readAuthSession } from "@/lib/authSession";
import { formatVND } from "@/lib/formatters";
import { shopService, type ShopDto } from "@/services/shopService";
import { sellerOrderService as service, type SellerOrderDetail, type SellerOrderPage, type FulfillmentStatus } from "@/services/sellerOrderService";

const states: Record<string, string> = { PENDING: "Chờ xác nhận", CONFIRMED: "Đã xác nhận", PROCESSING: "Đang đóng gói", SHIPPING: "Đang giao", DELIVERED: "Đã giao", CANCELLED: "Đã hủy", RETURNED: "Đã trả hàng" };
const payments: Record<string, string> = { UNPAID: "Chưa thanh toán", PENDING: "Chờ thanh toán", PAID: "Đã thanh toán", FAILED: "Thanh toán thất bại", REFUNDED: "Đã hoàn tiền" };
const actions: Partial<Record<FulfillmentStatus, string>> = { CONFIRMED: "Xác nhận đơn", PROCESSING: "Bắt đầu đóng gói", SHIPPING: "Bàn giao vận chuyển", DELIVERED: "Đánh dấu đã giao", CANCELLED: "Hủy đơn" };
const inputClass = "rounded-lg border border-slate-300 bg-white px-3 py-2 w-full sm:w-auto";
const buttonClass = "rounded-lg bg-sky-600 hover:bg-sky-700 text-white px-4 py-2 font-semibold disabled:opacity-50";
const errorText = (error: unknown) => isAxiosError(error) && typeof error.response?.data?.message === "string" ? error.response.data.message : "Chưa kết nối được đơn bán. Vui lòng thử lại.";
function useSessionGuard() {
  const token = useAuthStore(state => state.refreshToken);
  const mounted = useRef(true);
  useEffect(() => { mounted.current = true; return () => { mounted.current = false; }; }, []);
  return () => mounted.current && !!token && readAuthSession()?.refreshToken === token;
}
function variant(value: string) {
  try {
    const parsed = JSON.parse(value || "{}");
    if (parsed && typeof parsed === "object" && !Array.isArray(parsed)) return Object.entries(parsed).map(([key, val]) => `${key}: ${String(val)}`).join(", ") || "Mặc định";
  } catch { /* Preserve the stored snapshot as plain text when it is not JSON. */ }
  return value || "Mặc định";
}

export function SellerOrdersPage() {
  const { user, refreshToken } = useAuthStore();
  const [params] = useSearchParams();
  const [shop, setShop] = useState<ShopDto | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [retry, setRetry] = useState(0);
  const seller = user?.role === "ROLE_SELLER";
  useEffect(() => {
    let active = true;
    setShop(null); setError(""); setLoading(seller);
    const current = () => active && readAuthSession()?.refreshToken === refreshToken;
    if (seller) shopService.mine().then(value => { if (current()) setShop(value); })
      .catch(err => { if (current()) setError(errorText(err)); })
      .finally(() => { if (current()) setLoading(false); });
    return () => { active = false; };
  }, [seller, user?.id, refreshToken, retry]);
  const selected = params.get("order");
  return <div className="min-h-screen bg-slate-50 flex flex-col"><Header />
    <main className="w-full max-w-6xl mx-auto px-4 py-8 flex-1 space-y-5">
      <nav className="flex flex-wrap gap-4 text-sky-700"><Link to="/seller/shop">Gian hàng của tôi</Link><Link to="/seller/products">Sản phẩm &amp; SKU</Link><Link to="/seller/orders">Đơn bán</Link></nav>
      <h1 className="text-2xl font-bold text-slate-900">Xử lý đơn bán</h1>
      {!seller ? <p role="alert">Chức năng dành cho tài khoản người bán.</p> : loading ? <p role="status">Đang tải gian hàng...</p>
        : error ? <div className="space-y-3"><p role="alert" className="text-red-700">{error}</p><button className={buttonClass} onClick={() => setRetry(value => value + 1)}>Thử lại</button></div>
          : !shop ? <p>Bạn cần <Link to="/seller/shop" className="text-sky-700 underline">mở gian hàng</Link> trước khi xử lý đơn bán.</p>
            : shop.status !== "ACTIVE" ? <p role="alert">Gian hàng đang ngừng hoạt động hoặc bị khóa.</p>
              : selected ? <OrderDetail key={`${refreshToken}/${selected}`} selection={selected} /> : <OrderList key={refreshToken} />}
    </main><Footer /></div>;
}

function OrderList() {
  const current = useSessionGuard();
  const [params, setParams] = useSearchParams();
  const page = Number(params.get("page") || 1);
  const keyword = params.get("keyword") || "";
  const status = params.get("status") || "";
  const paymentStatus = params.get("paymentStatus") || "";
  const [search, setSearch] = useState(keyword);
  const [data, setData] = useState<SellerOrderPage | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [retry, setRetry] = useState(0);
  useEffect(() => {
    let active = true;
    setData(null); setError(""); setLoading(true); setSearch(keyword);
    service.list({ page, keyword: keyword || undefined, status: status || undefined, paymentStatus: paymentStatus || undefined })
      .then(value => { if (active && current()) setData(value); })
      .catch(err => { if (active && current()) setError(errorText(err)); })
      .finally(() => { if (active && current()) setLoading(false); });
    return () => { active = false; };
    // A session change remounts this component; filters identify each request.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page, keyword, status, paymentStatus, retry]);
  return <section className="space-y-4">
    <form className="flex flex-wrap gap-3" onSubmit={event => { event.preventDefault(); setParams({ keyword: search.trim(), status, paymentStatus, page: "1" }); }}>
      <input aria-label="Tìm mã đơn bán" placeholder="Tìm theo mã đơn" maxLength={64} className={inputClass} value={search} onChange={event => setSearch(event.target.value)} />
      <select aria-label="Lọc trạng thái đơn bán" className={inputClass} value={status} onChange={event => setParams({ keyword, status: event.target.value, paymentStatus, page: "1" })}>
        <option value="">Tất cả trạng thái</option>{Object.entries(states).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
      </select>
      <select aria-label="Lọc thanh toán đơn bán" className={inputClass} value={paymentStatus} onChange={event => setParams({ keyword, status, paymentStatus: event.target.value, page: "1" })}>
        <option value="">Tất cả thanh toán</option>{Object.entries(payments).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
      </select>
      <button className={buttonClass}>Tìm đơn</button>
      <button type="button" className="text-sky-700 underline disabled:opacity-50" disabled={loading} onClick={() => setRetry(value => value + 1)}>Làm mới danh sách</button>
    </form>
    {loading ? <p role="status">Đang tải đơn bán...</p> : error ? <div><p role="alert" className="text-red-700">{error}</p><button className="text-sky-700 underline" onClick={() => setRetry(value => value + 1)}>Tải lại danh sách</button></div>
      : data && <><p className="text-sm text-slate-600">{data.totalElements} đơn hàng</p>
        {!data.items.length ? <p>Chưa có đơn bán phù hợp.</p> : <div className="rounded-xl border bg-white overflow-x-auto"><table className="w-full text-sm text-left">
          <thead className="bg-slate-100"><tr>{["Mã đơn", "Khách hàng", "Trạng thái", "Thanh toán", "Tổng tiền", "Thao tác"].map(label => <th key={label} className="p-3">{label}</th>)}</tr></thead>
          <tbody>{data.items.map(order => <tr key={order.id} className="border-t"><td className="p-3 font-mono">{order.orderCode}</td><td className="p-3">{order.customerName || `Khách #${order.customerId}`}</td>
            <td className="p-3">{states[order.orderStatus] || order.orderStatus}</td><td className="p-3">{payments[order.paymentStatus] || order.paymentStatus} · {order.paymentMethod}</td><td className="p-3 whitespace-nowrap">{formatVND(Number(order.finalAmount))}</td>
            <td className="p-3"><Link className="text-sky-700 underline whitespace-nowrap" to={`/seller/orders?order=${order.id}`}>Xử lý #{order.id}</Link></td></tr>)}</tbody>
        </table></div>}
        <div className="flex items-center gap-4"><button disabled={page <= 1} onClick={() => setParams({ keyword, status, paymentStatus, page: String(page - 1) })} className="text-sky-700 disabled:opacity-40">Trang trước</button>
          <span>Trang {data.page}/{Math.max(1, data.totalPages)}</span><button disabled={page >= data.totalPages} onClick={() => setParams({ keyword, status, paymentStatus, page: String(page + 1) })} className="text-sky-700 disabled:opacity-40">Trang sau</button></div>
      </>}
  </section>;
}

function OrderDetail({ selection }: { selection: string }) {
  const current = useSessionGuard();
  const id = Number(selection);
  const [data, setData] = useState<SellerOrderDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [retry, setRetry] = useState(0);
  const inFlight = useRef(false);
  useEffect(() => {
    let active = true;
    setData(null); setError(""); setSuccess(""); setLoading(true);
    if (!Number.isSafeInteger(id) || id < 1) { setError("Mã đơn không hợp lệ"); setLoading(false); return; }
    service.detail(id).then(value => { if (active && current()) setData(value); })
      .catch(err => { if (active && current()) setError(errorText(err)); })
      .finally(() => { if (active && current()) setLoading(false); });
    return () => { active = false; };
    // The selected order and session are fixed by this component's key.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [retry]);

  async function transition(target: FulfillmentStatus) {
    if (!current() || !data || inFlight.current) return;
    const question = target === "CANCELLED" ? "Hủy đơn này và hoàn lại tồn kho?" : `${actions[target]} cho đơn ${data.order.orderCode}?`;
    if (!window.confirm(question) || !current()) return;
    inFlight.current = true; setBusy(true); setError(""); setSuccess("");
    try {
      const result = await service.transition(id, data.order.orderStatus, target);
      if (current()) { setData(result); setSuccess(`Đã cập nhật: ${states[result.order.orderStatus] || result.order.orderStatus}`); }
    } catch (err) { if (current()) setError(errorText(err)); }
    finally { if (current()) { inFlight.current = false; setBusy(false); } }
  }
  const order = data?.order;
  return <section className="space-y-5">
    <div className="flex flex-wrap justify-between gap-4"><Link to="/seller/orders" className="text-sky-700 underline">← Danh sách đơn bán</Link>
      <button disabled={loading || busy} onClick={() => setRetry(value => value + 1)} className="text-sky-700 underline disabled:opacity-50">Tải lại dữ liệu</button></div>
    {error && <p role="alert" className="bg-red-50 text-red-700 rounded-xl p-4">{error}</p>}
    {success && <p role="status" className="bg-emerald-50 text-emerald-700 rounded-xl p-4">{success}</p>}
    {loading ? <p role="status">Đang tải chi tiết đơn bán...</p> : order && data && <article className="bg-white border rounded-2xl p-5 space-y-5">
      <div className="flex flex-wrap justify-between gap-3"><div><h2 className="text-lg font-bold break-all">{order.orderCode}</h2><p className="text-sm text-slate-500">Tạo lúc {new Date(order.createdAt).toLocaleString("vi-VN")}</p></div>
        <div className="text-sm"><p className="font-bold text-sky-700" data-testid="seller-order-status">{states[order.orderStatus] || order.orderStatus}</p><p data-testid="seller-payment-status">{payments[order.paymentStatus] || order.paymentStatus} · {order.paymentMethod}</p></div></div>
      <dl className="space-y-3 text-sm"><div><dt className="text-slate-500">Khách đặt hàng</dt><dd>{order.customerName || `Khách #${order.customerId}`}</dd></div>
        <div><dt className="text-slate-500">Người nhận / địa chỉ giao hàng</dt><dd className="whitespace-pre-wrap">{order.shippingAddressSnapshot}</dd></div>
        <div><dt className="text-slate-500">Ghi chú của khách</dt><dd className="whitespace-pre-wrap">{order.note || "Không có ghi chú"}</dd></div></dl>
      <div className="space-y-3">{order.items.map(item => <div className="border-t pt-3 flex gap-3" key={item.id}>
        {item.imageUrl && <img src={item.imageUrl} alt="" className="w-16 h-16 object-cover rounded-lg" />}
        <div className="flex-1 text-sm"><p className="font-semibold">{item.productNameSnapshot}</p><p className="text-slate-500">{variant(item.skuVariantSnapshot)}</p><p>{item.skuCode} · Số lượng: {item.quantity} · Đơn giá: {formatVND(Number(item.priceAtPurchase))}</p></div>
        <p className="text-sm font-semibold">{formatVND(Number(item.subtotal))}</p>
      </div>)}</div>
      <div className="text-right text-sm space-y-1"><p>Tiền hàng: {formatVND(Number(order.totalAmount))}</p><p>Phí vận chuyển: {formatVND(Number(order.shippingFee || 0))}</p><p>Giảm giá: {formatVND(Number(order.discountAmount || 0))}</p><p className="text-lg font-bold text-sky-700">Tổng: {formatVND(Number(order.finalAmount))}</p></div>
      {data.actionNote && <p className="rounded-xl bg-amber-50 text-amber-900 p-4 text-sm">{data.actionNote}</p>}
      <div className="flex flex-wrap gap-3">{data.allowedStatuses.filter(status => actions[status]).map(status => <button key={status} disabled={busy} onClick={() => void transition(status)}
        className={status === "CANCELLED" ? "rounded-lg border border-red-300 px-4 py-2 text-red-700 disabled:opacity-50" : buttonClass}>{busy ? "Đang cập nhật..." : actions[status]}</button>)}</div>
      {!data.allowedStatuses.length && <p className="text-sm text-slate-500">Hiện không có thao tác chuyển trạng thái khả dụng.</p>}
      <p className="text-xs text-slate-500">Thao tác ghi nhận xử lý nội bộ, chưa tạo vận đơn hoặc tự xác nhận thu tiền COD.</p>
    </article>}
  </section>;
}
