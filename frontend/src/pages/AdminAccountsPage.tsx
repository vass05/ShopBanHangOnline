import { useEffect, useRef, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { isAxiosError } from "axios";
import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/layout/Footer";
import { useAuthStore } from "@/store/useAuthStore";
import { readAuthSession } from "@/lib/authSession";
import { adminAccountService as service, type AccountPage, type AdminAccount } from "@/services/adminAccountService";

const input = "rounded-lg border border-slate-300 bg-white px-3 py-2";
const button = "rounded-lg bg-sky-600 hover:bg-sky-700 text-white px-4 py-2 font-semibold disabled:opacity-50";
const statuses = { ACTIVE: "Hoạt động", BLOCKED: "Đã khóa", INACTIVE: "Ngừng hoạt động" };
const errorText = (err: unknown) => isAxiosError(err) && typeof err.response?.data?.message === "string" ? err.response.data.message : "Chưa tải được tài khoản. Vui lòng thử lại.";

export function AdminAccountsPage() {
  const { user, refreshToken } = useAuthStore();
  return <div className="min-h-screen flex flex-col bg-slate-50"><Header />
    <main className="w-full max-w-6xl mx-auto px-4 py-8 flex-1 space-y-5">
      <Link to="/" className="text-sky-700">Trang chủ</Link><h1 className="text-2xl font-bold text-slate-900">Quản lý tài khoản</h1>
      {user?.role !== "ROLE_ADMIN" ? <p role="alert">Chức năng dành cho quản trị viên.</p> : <AccountList key={refreshToken} />}
    </main><Footer /></div>;
}

function AccountList() {
  const token = useAuthStore(state => state.refreshToken);
  const mounted = useRef(true);
  const [params, setParams] = useSearchParams();
  const page = Number(params.get("page") || 1), keyword = params.get("keyword") || "", role = params.get("role") || "", status = params.get("status") || "";
  const [search, setSearch] = useState(keyword);
  const [data, setData] = useState<AccountPage | null>(null);
  const [loading, setLoading] = useState(true), [saving, setSaving] = useState(false);
  const [error, setError] = useState(""), [success, setSuccess] = useState("");
  const [retry, setRetry] = useState(0);
  const current = () => mounted.current && !!token && readAuthSession()?.refreshToken === token;
  useEffect(() => { mounted.current = true; return () => { mounted.current = false; }; }, []);
  useEffect(() => {
    let active = true;
    setData(null); setError(""); setLoading(true); setSearch(keyword);
    service.list({ page, keyword: keyword || undefined, role: role || undefined, status: status || undefined })
      .then(value => { if (active && current()) setData(value); })
      .catch(err => { if (active && current()) setError(errorText(err)); })
      .finally(() => { if (active && current()) setLoading(false); });
    return () => { active = false; };
    // A new session remounts this component.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page, keyword, role, status, retry]);
  async function update(account: AdminAccount) {
    if (!current() || saving) return;
    const target = account.status === "ACTIVE" ? "BLOCKED" : "ACTIVE";
    const verb = target === "BLOCKED" ? "Khóa" : "Mở khóa";
    if (!window.confirm(`${verb} tài khoản ${account.email}? Người dùng sẽ cần đăng nhập lại.`) || !current()) return;
    setSaving(true); setError(""); setSuccess("");
    try {
      await service.update(account.id, account.status, target);
      if (current()) { setSuccess(`${verb} tài khoản thành công.`); setRetry(value => value + 1); }
    } catch (err) { if (current()) { setData(null); setError(errorText(err)); } }
    finally { if (current()) setSaving(false); }
  }
  return <section className="space-y-4">
    <p className="text-sm text-slate-600">Quản lý người mua và người bán. Khóa tài khoản thu hồi phiên đăng nhập; dữ liệu đơn hàng và gian hàng được giữ lại.</p>
    <form className="flex flex-wrap gap-3" onSubmit={event => { event.preventDefault(); setParams({ keyword: search.trim(), role, status, page: "1" }); }}>
      <input aria-label="Tìm email hoặc họ tên" placeholder="Email hoặc họ tên" maxLength={100} className={input} value={search} onChange={event => setSearch(event.target.value)} />
      <select aria-label="Lọc vai trò tài khoản" className={input} value={role} onChange={event => setParams({ keyword, role: event.target.value, status, page: "1" })}>
        <option value="">Người mua và người bán</option><option value="ROLE_CUSTOMER">Người mua</option><option value="ROLE_SELLER">Người bán</option>
      </select>
      <select aria-label="Lọc trạng thái tài khoản" className={input} value={status} onChange={event => setParams({ keyword, role, status: event.target.value, page: "1" })}>
        <option value="">Tất cả trạng thái</option>{Object.entries(statuses).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
      </select><button className={button} disabled={saving}>Tìm tài khoản</button>
      <button type="button" className="text-sky-700 underline disabled:opacity-50" disabled={loading || saving} onClick={() => setRetry(value => value + 1)}>Tải lại danh sách</button>
    </form>
    {success && <p role="status" className="text-emerald-700">{success}</p>}
    {loading ? <p role="status">Đang tải tài khoản...</p> : error ? <div><p role="alert" className="text-red-700">{error}</p><button className={button} onClick={() => setRetry(value => value + 1)}>Thử lại</button></div>
      : data && <><p className="text-sm text-slate-600">{data.totalElements} tài khoản</p>
        {!data.items.length ? <p>Không có tài khoản phù hợp.</p> : <div className="rounded-xl border bg-white overflow-x-auto"><table className="w-full text-sm text-left">
          <thead className="bg-slate-100"><tr>{["Tài khoản", "Họ tên", "Vai trò", "Trạng thái", "Ngày tạo", "Thao tác"].map(label => <th key={label} className="p-3">{label}</th>)}</tr></thead>
          <tbody>{data.items.map(account => <tr key={account.id} className="border-t"><td className="p-3">{account.email}<span className="block text-xs text-slate-500">#{account.id}</span></td>
            <td className="p-3">{account.fullName}</td><td className="p-3">{account.role === "ROLE_SELLER" ? "Người bán" : "Người mua"}</td><td className="p-3">{statuses[account.status]}</td>
            <td className="p-3">{account.createdAt ? new Date(account.createdAt).toLocaleDateString("vi-VN") : "—"}</td><td className="p-3"><button disabled={saving} className={button} onClick={() => update(account)}>{account.status === "ACTIVE" ? "Khóa tài khoản" : "Mở khóa tài khoản"}</button></td>
          </tr>)}</tbody></table></div>}
        <div className="flex items-center gap-4"><button className={button} disabled={saving || page <= 1} onClick={() => setParams({ keyword, role, status, page: String(page - 1) })}>Trang trước</button>
          <span>Trang {data.page}/{Math.max(1, data.totalPages)}</span><button className={button} disabled={saving || page >= data.totalPages} onClick={() => setParams({ keyword, role, status, page: String(page + 1) })}>Trang sau</button></div>
      </>}
  </section>;
}
