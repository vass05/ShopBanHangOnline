import React, { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/layout/Footer";
import { useCartStore } from "@/store/useCartStore";
import { useAuthStore } from "@/store/useAuthStore";
import { profileService } from "@/services/profileService";
import type { UserAddress } from "@/services/profileService";
import { checkoutService, checkoutAttemptKey } from "@/services/checkoutService";
import type { CheckoutPayload, CheckoutQuote, OrderDto } from "@/services/checkoutService";
import { paymentService } from "@/services/paymentService";
import { readAuthSession } from "@/lib/authSession";
import { formatVND } from "@/lib/formatters";

const failureMessage = (error: unknown) => (error as { response?: { data?: { message?: string } } }).response?.data?.message || "Không thể xử lý yêu cầu. Vui lòng thử lại.";
const panel = "bg-white rounded-2xl border border-slate-200 p-5 space-y-4";
const field = "border border-slate-300 rounded-lg p-2.5 w-full text-sm";

export const CheckoutPage: React.FC = () => {
  const { user } = useAuthStore();
  const { items, selectedSkuIds, loading: cartLoading, fetchCart } = useCartStore();
  const selected = useMemo(() => items.filter(item => selectedSkuIds.includes(item.skuId)), [items, selectedSkuIds]);
  const [addresses, setAddresses] = useState<UserAddress[]>([]);
  const [addressId, setAddressId] = useState(0);
  const [shippingMethod, setShippingMethod] = useState<CheckoutPayload["shippingMethod"]>("standard");
  const [paymentMethod, setPaymentMethod] = useState<"COD" | "VNPAY">("COD");
  const [voucherInput, setVoucherInput] = useState("");
  const [voucherCode, setVoucherCode] = useState("");
  const [note, setNote] = useState("");
  const [quote, setQuote] = useState<CheckoutQuote | null>(null);
  const [quoting, setQuoting] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [loadingAddresses, setLoadingAddresses] = useState(true);
  const [error, setError] = useState("");
  const [created, setCreated] = useState<OrderDto[]>([]);
  const [retry, setRetry] = useState(0);
  const payload = useMemo<CheckoutPayload>(() => ({ addressId, shippingMethod, paymentMethod,
    voucherCode: voucherCode || undefined, note: note || undefined,
    items: selected.map(item => ({ skuId: item.skuId, quantity: item.quantity })).sort((a, b) => a.skuId - b.skuId),
  }), [addressId, shippingMethod, paymentMethod, voucherCode, note, selected]);

  useEffect(() => {
    let active = true;
    setAddresses([]); setAddressId(0); setCreated([]); setLoadingAddresses(true);
    profileService.getUserAddresses().then(data => {
      if (!active) return;
      setAddresses(data); setAddressId((data.find(address => address.isDefault) || data[0])?.id || 0);
    }).catch(error => { if (active) setError(failureMessage(error)); })
      .finally(() => { if (active) setLoadingAddresses(false); });
    return () => { active = false; };
  }, [user?.id, retry]);

  useEffect(() => {
    let active = true;
    setQuote(null);
    if (!payload.addressId || !payload.items.length || created.length) { setQuoting(false); return; }
    setQuoting(true); setError("");
    const timer = setTimeout(() => {
      checkoutService.quote(payload).then(data => { if (active) setQuote(data); })
        .catch(error => { if (active) setError(failureMessage(error)); })
        .finally(() => { if (active) setQuoting(false); });
    }, 200);
    return () => { active = false; clearTimeout(timer); };
  }, [payload, created.length, retry]);

  const placeOrder = async () => {
    if (!user || !quote || quoting || cartLoading || submitting || created.length) return;
    const session = readAuthSession()?.refreshToken;
    setSubmitting(true); setError("");
    try {
      const orders = await checkoutService.create(payload, checkoutAttemptKey(user.id, payload));
      if (readAuthSession()?.refreshToken !== session) return;
      setCreated(orders);
      sessionStorage.removeItem(`helishop-checkout-attempt:${user.id}`);
      if (!await fetchCart()) setError("Đơn đã được lưu, nhưng giỏ hàng chưa đồng bộ. Vui lòng kiểm tra đơn mua trước khi đặt lại.");
    } catch (error) {
      if (readAuthSession()?.refreshToken === session) setError(failureMessage(error));
    } finally { setSubmitting(false); }
  };

  const pay = async (orderCode: string) => {
    const session = readAuthSession()?.refreshToken;
    setSubmitting(true); setError("");
    try {
      const url = await paymentService.createUrl(orderCode);
      if (readAuthSession()?.refreshToken !== session) return;
      window.location.assign(url);
    } catch (error) { setError(failureMessage(error)); }
    finally { setSubmitting(false); }
  };

  return <div className="min-h-screen bg-[#F5F5FA] flex flex-col">
    <Header />
    <main className="flex-1 max-w-6xl w-full mx-auto px-4 py-8 space-y-5">
      <h1 className="text-2xl font-bold text-slate-800">Thanh toán</h1>
      {error && <div role="alert" className="rounded-xl bg-red-50 text-red-700 p-4">{error} <button className="underline ml-2" onClick={() => { if (created.length) setError(""); else setRetry(value => value + 1); }}>Thử lại</button></div>}
      {created.length ? <section className={panel}>
        <h2 className="text-xl font-bold text-emerald-700">Đã tạo {created.length} đơn hàng</h2>
        <p>Đơn hàng đã được lưu. Đơn VNPAY chỉ được ghi nhận đã thanh toán sau khi máy chủ xác nhận.</p>
        {created.map(order => <div key={order.id} className="border rounded-xl p-4 flex flex-wrap justify-between gap-3">
          <div><strong>{order.shopName}</strong><p className="font-mono text-sm">{order.orderCode}</p><p>{formatVND(Number(order.finalAmount))} · Chưa thanh toán</p></div>
          {order.paymentMethod === "VNPAY" && <button disabled={submitting} onClick={() => void pay(order.orderCode)} className="bg-sky-600 text-white rounded-lg px-4 py-2 disabled:opacity-50">Thanh toán đơn này</button>}
        </div>)}
        <Link to="/orders" className="text-sky-700 underline">Xem đơn mua của tôi</Link>
      </section> : user?.role !== "ROLE_CUSTOMER" ? <p>Vui lòng sử dụng tài khoản khách hàng để đặt mua.</p>
        : !selected.length ? <section className={panel}><p>Chưa chọn sản phẩm để đặt hàng.</p><Link to="/cart" className="text-sky-700 underline">Trở lại giỏ hàng</Link></section>
        : <div className="grid lg:grid-cols-3 gap-5">
          <div className="lg:col-span-2 space-y-5">
            <section className={panel}>
              <h2 className="font-bold">Địa chỉ nhận hàng</h2>
              {loadingAddresses ? <p role="status">Đang tải địa chỉ…</p> : addresses.length ? <select aria-label="Địa chỉ nhận hàng" className={field} value={addressId} onChange={event => setAddressId(Number(event.target.value))} disabled={submitting}>
                {addresses.map(address => <option key={address.id} value={address.id}>{address.recipientName} · {address.phone} · {address.fullAddress || `${address.streetAddress}, ${address.ward}, ${address.district}, ${address.provinceCity}`}</option>)}
              </select> : <p>Bạn cần thêm địa chỉ trước khi đặt hàng.</p>}
              <Link to="/profile?tab=addresses" className="text-sky-700 underline text-sm">Quản lý địa chỉ</Link>
            </section>
            <section className={panel}>
              <h2 className="font-bold">Sản phẩm theo cửa hàng</h2>
              {quote ? quote.shops.map(shop => <div key={shop.shopId} className="border rounded-xl p-4 space-y-3">
                <h3 className="font-semibold">{shop.shopName}</h3>
                {shop.items.map(item => <div key={item.skuId} className="flex justify-between gap-3 text-sm"><span>{item.productName} × {item.quantity}</span><span>{formatVND(Number(item.subtotal))}</span></div>)}
                <p className="text-right text-sm">Tổng đơn: <strong>{formatVND(Number(shop.finalAmount))}</strong></p>
              </div>) : selected.map(item => <p key={item.skuId} className="text-sm">{item.productName} × {item.quantity}</p>)}
            </section>
            <section className={panel}>
              <label className="block font-bold">Giao hàng <select className={field} value={shippingMethod} onChange={event => setShippingMethod(event.target.value as CheckoutPayload["shippingMethod"])} disabled={submitting}>
                <option value="standard">Tiêu chuẩn — 30.000₫ / shop</option><option value="express">Nhanh — 50.000₫ / shop</option><option value="economy">Tiết kiệm — 15.000₫ / shop</option>
              </select></label>
              <label className="block font-bold">Phương thức thanh toán <select className={field} value={paymentMethod} onChange={event => setPaymentMethod(event.target.value as "COD" | "VNPAY")} disabled={submitting}>
                <option value="COD">Thanh toán khi nhận hàng (COD)</option><option value="VNPAY">VNPAY — thanh toán từng đơn theo shop</option>
              </select></label>
              <label className="block text-sm">Ghi chú<textarea className={field} maxLength={500} value={note} onChange={event => setNote(event.target.value)} disabled={submitting} /></label>
            </section>
          </div>
          <aside className={`${panel} h-fit lg:sticky lg:top-28`}>
            <h2 className="font-bold">Chi tiết thanh toán</h2>
            <form className="flex gap-2" onSubmit={event => { event.preventDefault(); setVoucherCode(voucherInput.trim().toUpperCase()); }}>
              <input aria-label="Mã voucher" className={field} placeholder="Nhập mã voucher" value={voucherInput} onChange={event => setVoucherInput(event.target.value)} disabled={submitting} />
              <button disabled={submitting} className="text-sky-700 font-semibold">Áp dụng</button>
            </form>
            {voucherCode && <button className="text-sm underline" onClick={() => { setVoucherCode(""); setVoucherInput(""); }}>Bỏ mã {voucherCode}</button>}
            {quoting && <p role="status">Đang cập nhật báo giá…</p>}
            {quote && <dl className="space-y-3 text-sm">
              <div className="flex justify-between"><dt>Tiền hàng</dt><dd>{formatVND(Number(quote.totalAmount))}</dd></div>
              <div className="flex justify-between"><dt>Phí vận chuyển</dt><dd>{formatVND(Number(quote.shippingFee))}</dd></div>
              <div className="flex justify-between"><dt>Giảm giá</dt><dd>−{formatVND(Number(quote.discountAmount))}</dd></div>
              <div className="flex justify-between border-t pt-3 font-bold text-lg"><dt>Tổng thanh toán</dt><dd>{formatVND(Number(quote.finalAmount))}</dd></div>
            </dl>}
            <p className="text-xs text-slate-500">Giá, tồn kho và ưu đãi được kiểm tra lại khi đặt hàng. Mỗi cửa hàng tạo một đơn riêng.</p>
            <button onClick={() => void placeOrder()} disabled={!quote || quoting || submitting || cartLoading} className="bg-sky-600 text-white font-bold rounded-xl p-3 w-full disabled:opacity-50">{submitting ? "Đang đặt hàng…" : "Đặt hàng"}</button>
          </aside>
        </div>}
    </main>
    <Footer />
  </div>;
};
