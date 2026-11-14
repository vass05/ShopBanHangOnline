import { useEffect, useRef, useState, type FormEvent } from "react";
import { Link } from "react-router-dom";
import { isAxiosError } from "axios";
import { Store, CheckCircle2, Loader2 } from "lucide-react";
import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/layout/Footer";
import { useAuthStore } from "@/store/useAuthStore";
import { readAuthSession } from "@/lib/authSession";
import { shopService, type ShopDto } from "@/services/shopService";

function message(error: unknown) {
  return isAxiosError(error) && typeof error.response?.data?.message === "string"
    ? error.response.data.message : "Không thể kết nối gian hàng. Vui lòng thử lại.";
}

export function SellerShopPage() {
  const { user, refreshToken } = useAuthStore();
  const seller = user?.role === "ROLE_SELLER";
  const [shop, setShop] = useState<ShopDto | null>(null);
  const [loading, setLoading] = useState(true);
  const [loaded, setLoaded] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [shopName, setShopName] = useState("");
  const [description, setDescription] = useState("");
  const [retry, setRetry] = useState(0);
  const generation = useRef(0);
  const inFlight = useRef(false);

  useEffect(() => {
    const version = ++generation.current;
    let active = true;
    setShop(null); setError(""); setShopName(""); setDescription(""); setLoaded(false);
    setSaving(false); inFlight.current = false; setLoading(seller);
    const current = () => active && version === generation.current
      && readAuthSession()?.refreshToken === refreshToken;
    if (seller) {
      shopService.mine().then(result => { if (current()) { setShop(result); setLoaded(true); } })
        .catch(err => { if (current()) setError(message(err)); })
        .finally(() => { if (current()) setLoading(false); });
    }
    return () => { active = false; ++generation.current; };
  }, [user?.id, seller, refreshToken, retry]);

  async function open(event: FormEvent) {
    event.preventDefault();
    if (inFlight.current) return;
    const name = shopName.trim();
    if (name.length < 3 || name.length > 150) {
      setError("Tên gian hàng cần từ 3 đến 150 ký tự."); return;
    }
    const version = generation.current;
    const current = () => version === generation.current
      && readAuthSession()?.refreshToken === refreshToken;
    inFlight.current = true; setSaving(true); setError("");
    try {
      const created = await shopService.open({ shopName: name, description: description.trim() || undefined });
      if (current()) setShop(created);
    } catch (err) {
      // A second tab may have opened this account's shop while this form was visible.
      if (current() && isAxiosError(err) && err.response?.status === 409) {
        try {
          const existing = await shopService.mine();
          if (current() && existing) { setShop(existing); return; }
        } catch { /* Keep the original conflict message when the reload also fails. */ }
      }
      if (current()) setError(message(err));
    } finally {
      if (current()) { inFlight.current = false; setSaving(false); }
    }
  }

  return <div className="min-h-screen bg-slate-50 flex flex-col">
    <Header />
    <main className="w-full max-w-3xl mx-auto px-4 py-10 flex-1">
      <div className="flex items-center gap-3 mb-6">
        <Store className="w-9 h-9 text-sky-600" />
        <div><h1 className="text-2xl font-bold text-slate-900">Gian hàng của tôi</h1>
          <p className="text-slate-500 text-sm">Kênh người bán HeliShop</p></div>
      </div>
      {!seller ? <section className="bg-white border rounded-2xl p-6 space-y-3">
        <h2 className="font-semibold">Chức năng dành cho tài khoản người bán</h2>
        <p className="text-slate-600">Đăng nhập bằng tài khoản người bán để mở hoặc xem gian hàng của bạn.</p>
        <Link to="/" className="text-sky-700 underline">Về trang chủ</Link>
      </section> : loading ? <p role="status" className="flex gap-2 text-slate-600"><Loader2 className="animate-spin w-5 h-5" />Đang tải gian hàng...</p>
        : !loaded ? <section className="bg-white border rounded-2xl p-6 space-y-4">
          <p role="alert" className="text-red-700">{error}</p>
          <button type="button" onClick={() => setRetry(value => value + 1)} className="text-sky-700 underline">Tải lại gian hàng</button>
        </section> : shop ? <section className="bg-white border border-slate-200 rounded-2xl p-6 space-y-5">
          <p role="status" className="flex items-center gap-2 text-emerald-700"><CheckCircle2 className="w-5 h-5" />Gian hàng đã được lưu</p>
          <h2 className="text-xl font-bold text-slate-900 break-words">{shop.shopName}</h2>
          <dl className="space-y-3 text-sm">
            <div><dt className="text-slate-500">Mã gian hàng</dt><dd>#{shop.id}</dd></div>
            <div><dt className="text-slate-500">Trạng thái</dt><dd>{({ ACTIVE: "Đang hoạt động", INACTIVE: "Tạm ngừng hoạt động", BANNED: "Đã bị khóa" })[shop.status]}</dd></div>
            <div><dt className="text-slate-500">Mô tả</dt><dd className="whitespace-pre-wrap break-words">{shop.description || "Chưa có mô tả"}</dd></div>
            <div><dt className="text-slate-500">Ngày mở</dt><dd>{new Date(shop.createdAt).toLocaleDateString("vi-VN")}</dd></div>
          </dl>
        </section> : <section className="bg-white border border-slate-200 rounded-2xl p-6">
          <h2 className="text-xl font-bold mb-2">Mở gian hàng</h2>
          <p className="text-sm text-slate-500 mb-6">Mỗi tài khoản người bán được mở một gian hàng. Chọn tên riêng cho gian hàng của bạn.</p>
          {error && <p role="alert" className="bg-red-50 text-red-700 rounded-lg p-3 mb-4">{error}</p>}
          <form onSubmit={open} className="space-y-5">
            <div><label htmlFor="shop-name" className="block text-sm font-semibold mb-2">Tên gian hàng</label>
              <input id="shop-name" required minLength={3} maxLength={150} value={shopName} onChange={event => setShopName(event.target.value)} disabled={saving}
                className="w-full border border-slate-300 rounded-xl px-3 py-2 focus:outline-sky-500" aria-describedby="shop-name-help" />
              <p id="shop-name-help" className="text-xs text-slate-500 mt-1">Từ 3 đến 150 ký tự. Tên chưa được sử dụng.</p></div>
            <div><label htmlFor="shop-description" className="block text-sm font-semibold mb-2">Mô tả gian hàng (không bắt buộc)</label>
              <textarea id="shop-description" rows={5} maxLength={2000} value={description} onChange={event => setDescription(event.target.value)} disabled={saving}
                className="w-full border border-slate-300 rounded-xl px-3 py-2 focus:outline-sky-500" />
              <p className="text-xs text-slate-500 text-right">{description.length}/2000</p></div>
            <button type="submit" disabled={saving} className="bg-sky-600 hover:bg-sky-700 text-white px-6 py-2.5 rounded-xl font-semibold disabled:opacity-50 inline-flex gap-2 items-center">
              {saving && <Loader2 className="animate-spin w-4 h-4" />}{saving ? "Đang mở gian hàng..." : "Mở gian hàng"}
            </button>
          </form>
        </section>}
    </main>
    <Footer />
  </div>;
}
