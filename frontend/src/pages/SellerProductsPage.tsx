import { useEffect, useRef, useState, type FormEvent } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { isAxiosError } from "axios";
import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/layout/Footer";
import { useAuthStore } from "@/store/useAuthStore";
import { readAuthSession } from "@/lib/authSession";
import { formatVND } from "@/lib/formatters";
import { shopService, type ShopDto } from "@/services/shopService";
import { catalogService } from "@/services/catalogService";
import { sellerProductService as service, type SellerProduct, type SellerProductPage, type ProductPayload, type ProductStatus, type SkuPayload, type SellerSku } from "@/services/sellerProductService";
import type { Category } from "@/types";

const inputClass = "w-full rounded-lg border border-slate-300 px-3 py-2 bg-white disabled:bg-slate-100";
const buttonClass = "rounded-lg bg-sky-600 px-4 py-2 text-white font-semibold disabled:opacity-50 hover:bg-sky-700";
const states: Record<ProductStatus, string> = { DRAFT: "Bản nháp", ACTIVE: "Đang bán", INACTIVE: "Đã ẩn", OUT_OF_STOCK: "Ngừng bán - hết hàng" };
const errorText = (error: unknown) => isAxiosError(error) && typeof error.response?.data?.message === "string" ? error.response.data.message : "Chưa kết nối được máy chủ. Vui lòng thử lại.";

function useSessionGuard() {
  const token = useAuthStore(state => state.refreshToken);
  const mounted = useRef(true);
  useEffect(() => { mounted.current = true; return () => { mounted.current = false; }; }, []);
  return () => mounted.current && !!token && readAuthSession()?.refreshToken === token;
}

export function SellerProductsPage() {
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
    if (seller) shopService.mine().then(value => { if (active && readAuthSession()?.refreshToken === refreshToken) setShop(value); })
      .catch(err => { if (active && readAuthSession()?.refreshToken === refreshToken) setError(errorText(err)); })
      .finally(() => { if (active && readAuthSession()?.refreshToken === refreshToken) setLoading(false); });
    return () => { active = false; };
  }, [seller, refreshToken, user?.id, retry]);
  const selected = params.get("product");
  return <div className="min-h-screen bg-slate-50 flex flex-col"><Header />
    <main className="w-full max-w-6xl mx-auto px-4 py-8 flex-1 space-y-5">
      <nav className="flex flex-wrap gap-4 text-sky-700"><Link to="/seller/shop">Gian hàng của tôi</Link><Link to="/seller/products">Sản phẩm &amp; SKU</Link><Link to="/seller/orders">Đơn bán</Link></nav>
      <h1 className="text-2xl font-bold text-slate-900">Quản lý sản phẩm &amp; SKU</h1>
      {!seller ? <p role="alert">Chức năng dành cho tài khoản người bán.</p> : loading ? <p role="status">Đang tải gian hàng...</p>
        : error ? <div><p role="alert" className="text-red-700">{error}</p><button onClick={() => setRetry(v => v + 1)} className={buttonClass}>Thử lại</button></div>
          : !shop ? <p>Bạn cần <Link to="/seller/shop" className="text-sky-700 underline">mở gian hàng</Link> trước khi quản lý sản phẩm.</p>
            : shop.status !== "ACTIVE" ? <p role="alert">Gian hàng đang ngừng hoạt động hoặc bị khóa.</p>
              : selected ? <SellerProductEditor key={`${refreshToken}/${selected}`} selection={selected} /> : <ProductList key={refreshToken} />}
    </main><Footer /></div>;
}

function ProductList() {
  const current = useSessionGuard();
  const [params, setParams] = useSearchParams();
  const page = Number(params.get("page") || 1);
  const keyword = params.get("keyword") || "";
  const status = params.get("status") || "";
  const [search, setSearch] = useState(keyword);
  const [data, setData] = useState<SellerProductPage | null>(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const [retry, setRetry] = useState(0);
  useEffect(() => {
    let active = true;
    setLoading(true); setError(""); setData(null); setSearch(keyword);
    service.list({ page, keyword: keyword || undefined, status: status ? status as ProductStatus : undefined })
      .then(value => { if (active && current()) setData(value); })
      .catch(err => { if (active && current()) setError(errorText(err)); })
      .finally(() => { if (active && current()) setLoading(false); });
    return () => { active = false; };
    // Session changes remount this component; the filter values identify each read.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page, keyword, status, retry]);
  return <section className="space-y-5">
    <div className="flex flex-wrap justify-between gap-3">
      <form onSubmit={event => { event.preventDefault(); setParams({ keyword: search.trim(), status, page: "1" }); }} className="flex gap-2 flex-wrap">
        <input aria-label="Tìm sản phẩm của tôi" maxLength={255} placeholder="Tìm theo tên sản phẩm" value={search} onChange={e => setSearch(e.target.value)} className={inputClass + " sm:w-64"} />
        <select aria-label="Lọc trạng thái" value={status} onChange={e => setParams({ keyword, status: e.target.value, page: "1" })} className={inputClass + " sm:w-44"}>
          <option value="">Tất cả trạng thái</option>{Object.entries(states).map(([key, label]) => <option key={key} value={key}>{label}</option>)}
        </select><button className={buttonClass}>Tìm kiếm</button>
      </form><Link className={buttonClass} to="/seller/products?product=new">Thêm sản phẩm</Link>
    </div>
    {loading ? <p role="status">Đang tải sản phẩm...</p> : error ? <div><p role="alert" className="text-red-700">{error}</p><button onClick={() => setRetry(v => v + 1)} className="text-sky-700 underline">Tải lại danh sách</button></div>
      : data && <><p className="text-sm text-slate-600">{data.totalElements} sản phẩm</p>
        {!data.items.length ? <p>Chưa có sản phẩm phù hợp.</p> : <div className="overflow-x-auto rounded-xl border bg-white"><table className="w-full text-sm text-left">
          <thead className="bg-slate-100"><tr>{["Sản phẩm", "Danh mục", "Giá từ", "Tổng tồn kho", "Trạng thái", "Thao tác"].map(label => <th className="p-3" key={label}>{label}</th>)}</tr></thead>
          <tbody>{data.items.map(product => <tr key={product.id} className="border-t"><td className="p-3 font-medium">{product.name}</td><td className="p-3">{product.categoryName}</td>
            <td className="p-3">{product.price == null ? "Chưa có SKU" : formatVND(product.price)}</td><td className="p-3">{product.stockQuantity}</td><td className="p-3">{states[product.status]}</td>
            <td className="p-3"><Link className="text-sky-700 underline whitespace-nowrap" to={`/seller/products?product=${product.id}`}>Quản lý #{product.id}</Link></td></tr>)}</tbody>
        </table></div>}
        <div className="flex items-center gap-4"><button disabled={page <= 1} onClick={() => setParams({ keyword, status, page: String(page - 1) })} className="disabled:opacity-40 text-sky-700">Trang trước</button>
          <span>Trang {data.page}/{Math.max(1, data.totalPages)}</span><button disabled={page >= data.totalPages} onClick={() => setParams({ keyword, status, page: String(page + 1) })} className="disabled:opacity-40 text-sky-700">Trang sau</button></div>
      </>}
  </section>;
}

interface SkuDraft { code: string; price: string; original: string; stock: string; image: string; attributes: [string, string][]; version?: number }
const emptySku = (keys: string[] = []): SkuDraft => ({ code: "", price: "", original: "", stock: "0", image: "", attributes: [0, 1].map(i => [keys[i] || "", ""]) });
function skuDraft(sku: SellerSku): SkuDraft {
  let attributes: Record<string, string> = {};
  try { attributes = JSON.parse(sku.skuAttributes || "{}"); } catch { /* The seller can replace malformed attributes. */ }
  const entries = Object.entries(attributes || {});
  return { code: sku.skuCode, price: String(sku.price), original: sku.originalPrice == null ? "" : String(sku.originalPrice), stock: String(sku.stockQuantity), image: sku.skuImageUrl || "", version: sku.version,
    attributes: [entries[0] || ["", ""], entries[1] || ["", ""]] };
}
function payload(draft: SkuDraft): SkuPayload {
  const attributes: Record<string, string> = {};
  for (const [key, value] of draft.attributes) {
    if (!key.trim() && !value.trim()) continue;
    if (!key.trim() || !value.trim() || Object.prototype.hasOwnProperty.call(attributes, key.trim())) throw new Error("Điền đủ tên/giá trị và không trùng tên thuộc tính.");
    attributes[key.trim()] = value.trim();
  }
  return { skuCode: draft.code.trim(), price: Number(draft.price), originalPrice: draft.original ? Number(draft.original) : undefined, stockQuantity: Number(draft.stock), attributes, skuImageUrl: draft.image.trim() || undefined, version: draft.version };
}
function SkuFields({ value, change, disabled }: { value: SkuDraft; change: (value: SkuDraft) => void; disabled: boolean }) {
  const field = (key: "code" | "price" | "original" | "stock" | "image", label: string, type = "text", required = false) => <div><label className="block text-sm font-medium mb-1" htmlFor={`sku-${key}`}>{label}</label>
    <input id={`sku-${key}`} className={inputClass} type={type} required={required} disabled={disabled} value={value[key]} maxLength={key === "code" ? 100 : key === "image" ? 500 : undefined}
      min={type === "number" ? key === "stock" ? 0 : 0.01 : undefined} max={key === "stock" ? 1000000 : type === "number" ? 9999999999999.99 : undefined} step={key === "stock" ? 1 : type === "number" ? "0.01" : undefined}
      onChange={e => change({ ...value, [key]: e.target.value })} /></div>;
  return <fieldset disabled={disabled} className="grid sm:grid-cols-2 gap-4">
    {field("code", "Mã SKU", "text", true)}{field("price", "Giá bán (đ)", "number", true)}{field("original", "Giá gốc (đ)", "number")}{field("stock", "Tồn kho SKU", "number", true)}{field("image", "Ảnh biến thể (URL)")}
    <div className="sm:col-span-2"><p className="text-sm text-slate-600 mb-3">Thuộc tính tùy chọn, tối đa hai loại (ví dụ Màu sắc, Kích cỡ). Mọi SKU của sản phẩm dùng cùng tên thuộc tính và khác tổ hợp giá trị.</p>
      {value.attributes.map(([key, val], index) => <div key={index} className="grid grid-cols-2 gap-3 mb-3">
        <input aria-label={`Tên thuộc tính ${index + 1}`} placeholder={`Tên thuộc tính ${index + 1}`} maxLength={50} className={inputClass} value={key} onChange={e => change({ ...value, attributes: value.attributes.map((entry, i) => i === index ? [e.target.value, entry[1]] : entry) })} />
        <input aria-label={`Giá trị thuộc tính ${index + 1}`} placeholder={`Giá trị ${index + 1}`} maxLength={100} className={inputClass} value={val} onChange={e => change({ ...value, attributes: value.attributes.map((entry, i) => i === index ? [entry[0], e.target.value] : entry) })} />
      </div>)}
    </div>
  </fieldset>;
}

function flattenCategories(categories: Category[], prefix = ""): { id: number; label: string }[] {
  return categories.flatMap(category => [{ id: category.id, label: prefix + category.name }, ...flattenCategories(category.children || [], prefix + category.name + " / ")]);
}

function SellerProductEditor({ selection }: { selection: string }) {
  const creating = selection === "new";
  const id = Number(selection);
  const current = useSessionGuard();
  const [, setParams] = useSearchParams();
  const [product, setProduct] = useState<SellerProduct | null>(null);
  const [form, setForm] = useState<ProductPayload>({ name: "", categoryId: 0, description: "", mainImageUrl: "", status: "DRAFT" });
  const [categories, setCategories] = useState<Category[]>([]);
  const [sku, setSku] = useState<SkuDraft>(() => emptySku());
  const [editing, setEditing] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const inFlight = useRef(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [retry, setRetry] = useState(0);
  const [conflict, setConflict] = useState(false);
  useEffect(() => {
    let active = true;
    setLoading(true); setError(""); setConflict(false); setEditing(null); setSuccess("");
    if (!creating && (!Number.isSafeInteger(id) || id < 1)) { setError("Mã sản phẩm không hợp lệ"); setLoading(false); return; }
    Promise.all([catalogService.categories(), creating ? Promise.resolve(null) : service.detail(id)])
      .then(([tree, data]) => {
        if (!active || !current()) return;
        setCategories(tree); setProduct(data);
        if (data) setForm({ name: data.name, categoryId: data.categoryId, description: data.description || "", mainImageUrl: data.mainImageUrl || "", status: data.status });
        setSku(emptySku(data?.skus[0] ? skuDraft(data.skus[0]).attributes.map(([key]) => key) : []));
      }).catch(err => { if (active && current()) setError(errorText(err)); })
      .finally(() => { if (active && current()) setLoading(false); });
    return () => { active = false; };
    // The selection/session are fixed by this component's key.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [retry]);

  async function save(event: FormEvent, skuOnly = false) {
    event.preventDefault();
    if (inFlight.current) return;
    inFlight.current = true; setSaving(true); setError(""); setSuccess(""); setConflict(false);
    try {
      const saved = creating ? await service.create(form, [payload(sku)]) : skuOnly ? await service.saveSku(id, editing, payload(sku)) : await service.update(id, form);
      if (!current()) return;
      if (creating) { setParams({ product: String(saved.id) }); return; }
      setProduct(saved);
      if (skuOnly) { setEditing(null); setSku(emptySku(saved.skus[0] ? skuDraft(saved.skus[0]).attributes.map(([key]) => key) : [])); }
      setSuccess(skuOnly ? "Đã lưu SKU và cập nhật giá/tổng kho sản phẩm." : "Đã lưu thông tin sản phẩm.");
    } catch (err) {
      if (current()) {
        setError(isAxiosError(err) ? errorText(err) : err instanceof Error ? err.message : errorText(err));
        setConflict(isAxiosError(err) && err.response?.status === 409);
      }
    } finally { if (current()) { inFlight.current = false; setSaving(false); } }
  }

  return <section className="space-y-6">
    <Link to="/seller/products" className="text-sky-700 underline">← Danh sách sản phẩm</Link>
    {loading ? <p role="status">Đang tải dữ liệu sản phẩm...</p> : <>
      {error && <div role="alert" className="rounded-xl bg-red-50 p-4 text-red-700">{error}</div>}
      {success && <p role="status" className="rounded-xl bg-emerald-50 p-4 text-emerald-700">{success}</p>}
      {(conflict || (error && !categories.length)) && <button disabled={saving} onClick={() => setRetry(v => v + 1)} className="text-sky-700 underline">Tải lại dữ liệu</button>}
      {!categories.length && !error && <p>Chưa có danh mục sản phẩm. Cần có danh mục từ quản trị viên trước khi tạo sản phẩm.</p>}
      {!!categories.length && (creating || product) && <>
        <form onSubmit={event => save(event)} className="bg-white rounded-xl border p-5 space-y-4">
          <h2 className="font-bold text-xl">{creating ? "Thêm sản phẩm mới" : `Thông tin sản phẩm #${product!.id}`}</h2>
          <fieldset disabled={saving} className="grid sm:grid-cols-2 gap-4">
            <div><label htmlFor="product-name" className="block font-medium text-sm mb-1">Tên sản phẩm</label><input id="product-name" required maxLength={255} value={form.name} onChange={e => setForm({ ...form, name: e.target.value })} className={inputClass} /></div>
            <div><label htmlFor="product-category" className="block font-medium text-sm mb-1">Danh mục</label><select id="product-category" required value={form.categoryId || ""} onChange={e => setForm({ ...form, categoryId: Number(e.target.value) })} className={inputClass}>
              <option value="">Chọn danh mục</option>{flattenCategories(categories).map(category => <option key={category.id} value={category.id}>{category.label}</option>)}
            </select></div>
            <div><label htmlFor="product-status" className="block font-medium text-sm mb-1">Trạng thái sản phẩm</label><select id="product-status" value={form.status} onChange={e => setForm({ ...form, status: e.target.value as ProductStatus })} className={inputClass}>{Object.entries(states).map(([key, label]) => <option key={key} value={key}>{label}</option>)}</select></div>
            <div><label htmlFor="product-image" className="block font-medium text-sm mb-1">Ảnh chính (URL)</label><input id="product-image" maxLength={500} value={form.mainImageUrl} onChange={e => setForm({ ...form, mainImageUrl: e.target.value })} className={inputClass} /></div>
            <div className="sm:col-span-2"><label htmlFor="product-description" className="block font-medium text-sm mb-1">Mô tả sản phẩm</label><textarea id="product-description" rows={4} maxLength={10000} value={form.description} onChange={e => setForm({ ...form, description: e.target.value })} className={inputClass} /></div>
          </fieldset>
          {creating && <><h3 className="font-bold pt-4">SKU đầu tiên</h3><SkuFields value={sku} change={setSku} disabled={saving} /></>}
          <button disabled={saving} className={buttonClass}>{saving ? "Đang lưu..." : creating ? "Tạo sản phẩm" : "Lưu thông tin"}</button>
        </form>
        {product && <section className="bg-white rounded-xl border p-5 space-y-4">
          {product.status === "ACTIVE" && <Link to={`/product/${product.id}`} className="text-sky-700 underline">Xem trang sản phẩm</Link>}
          <h2 className="text-xl font-bold">Biến thể SKU</h2><p className="text-sm">Giá từ: {product.price == null ? "Chưa có SKU" : formatVND(product.price)} · Tổng tồn kho: <strong data-testid="seller-total-stock">{product.stockQuantity}</strong></p>
          <div className="overflow-x-auto"><table className="w-full text-sm text-left"><thead><tr>{["Mã SKU", "Thuộc tính", "Giá bán", "Tồn kho", "Thao tác"].map(label => <th className="p-2 border-b" key={label}>{label}</th>)}</tr></thead>
            <tbody>{product.skus.map(item => <tr key={item.id}><td className="p-2">{item.skuCode}</td><td className="p-2">{skuDraft(item).attributes.filter(([key]) => key).map(([key, value]) => `${key}: ${value}`).join(", ") || "Mặc định"}</td><td className="p-2">{formatVND(item.price)}</td><td className="p-2">{item.stockQuantity}</td><td className="p-2"><button type="button" disabled={saving} className="text-sky-700 underline" onClick={() => { setEditing(item.id); setSku(skuDraft(item)); setError(""); setConflict(false); setSuccess(""); }}>Sửa SKU {item.skuCode}</button></td></tr>)}</tbody>
          </table></div>
          <form onSubmit={event => save(event, true)} className="space-y-4 border-t pt-4">
            <h3 className="font-bold">{editing ? "Chỉnh sửa SKU" : "Thêm SKU"}</h3><SkuFields value={sku} change={setSku} disabled={saving} />
            <div className="flex gap-4"><button disabled={saving || (!editing && product.skus.length >= 100)} className={buttonClass}>{saving ? "Đang lưu..." : editing ? "Lưu SKU" : "Thêm SKU"}</button>
              {editing && <button type="button" disabled={saving} onClick={() => { setEditing(null); setSku(emptySku(product.skus[0] ? skuDraft(product.skus[0]).attributes.map(([key]) => key) : [])); }}>Hủy sửa SKU</button>}</div>
          </form>
          <p className="text-xs text-slate-500">Ẩn sản phẩm bằng trạng thái Đã ẩn. SKU được giữ để đơn hàng cũ tiếp tục tham chiếu đúng.</p>
        </section>}
      </>}
    </>}
  </section>;
}
