import { useEffect, useRef, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { isAxiosError } from "axios";
import { useAuthStore } from "@/store/useAuthStore";
import { readAuthSession, subscribeAuthSession } from "@/lib/authSession";
import { loadGoogleIdentity } from "@/lib/googleIdentity";
import { googleAuthService as service, type GoogleResult } from "@/services/googleAuthService";

type Pending = Extract<GoogleResult, { ticket: string }>;
const errorText = (err: unknown) => isAxiosError(err) ? err.response?.data?.message || "Chưa kết nối được đăng nhập Google. Vui lòng thử lại." : err instanceof Error ? err.message : "Đăng nhập Google chưa thành công.";
export function GoogleSignIn({ rememberMe = true, initialRole = "ROLE_CUSTOMER" }: { rememberMe?: boolean; initialRole?: "ROLE_CUSTOMER" | "ROLE_SELLER" }) {
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const host = useRef<HTMLDivElement>(null);
  const mounted = useRef(true);
  const interrupted = useRef(false);
  const inFlight = useRef(false);
  const generation = useRef(0);
  const originalSession = useRef(readAuthSession()?.refreshToken || null);
  const remember = useRef(rememberMe); remember.current = rememberMe;
  const destination = useRef(params.get("redirect")); destination.current = params.get("redirect");
  const [loading, setLoading] = useState(true), [ready, setReady] = useState(false), [busy, setBusy] = useState(false);
  const [error, setError] = useState(""), [retry, setRetry] = useState(0);
  const [pending, setPending] = useState<Pending | null>(null);
  const [role, setRole] = useState(initialRole), [password, setPassword] = useState("");
  useEffect(() => subscribeAuthSession(() => {
    interrupted.current = true; generation.current++;
    setBusy(false); setPending(null); setError("Phiên đăng nhập đã thay đổi. Vui lòng thử lại Google.");
  }), []);
  function restart() {
    generation.current++; interrupted.current = false; inFlight.current = false;
    originalSession.current = readAuthSession()?.refreshToken || null;
    setRetry(value => value + 1);
  }
  const current = () => mounted.current && !interrupted.current && (readAuthSession()?.refreshToken || null) === originalSession.current;

  function accept(result: GoogleResult) {
    if (!current()) return;
    if (result.status !== "AUTHENTICATED") { setPending(result); setPassword(""); return; }
    const auth = result.auth;
    if (!auth.accessToken || !auth.refreshToken || !auth.userId) throw new Error("Phản hồi đăng nhập không hợp lệ. Vui lòng thử lại.");
    useAuthStore.getState().setAuth({ accessToken: auth.accessToken, refreshToken: auth.refreshToken, rememberMe: remember.current,
      user: { id: auth.userId, email: auth.email, fullName: auth.fullName, role: auth.role, status: "ACTIVE" } });
    const redirect = destination.current;
    navigate(redirect?.startsWith("/") && !redirect.startsWith("//") ? redirect : auth.role === "ROLE_ADMIN" ? "/admin/users" : auth.role === "ROLE_SELLER" ? "/seller/shop" : "/");
  }

  useEffect(() => {
    mounted.current = true;
    let active = true;
    const abort = new AbortController();
    setLoading(true); setReady(false); setError(""); setPending(null); setPassword("");
    const attempt = generation.current;
    const valid = () => active && current() && generation.current === attempt;
    async function start() {
      try {
        const config = await service.config(abort.signal);
        if (!valid()) return;
        if (!config.enabled || !config.clientId) { setError("Đăng nhập Google tạm chưa khả dụng. Bạn có thể dùng cách đăng nhập khác."); return; }
        const [challenge, sdk] = await Promise.all([service.challenge(abort.signal), loadGoogleIdentity()]);
        if (!valid() || !host.current) return;
        sdk.initialize({ client_id: config.clientId, nonce: challenge.nonce, auto_select: false,
          callback: async ({ credential }) => {
            if (!valid() || inFlight.current) return;
            inFlight.current = true;
            setBusy(true); setError("");
            try { const result = await service.login(credential); if (valid()) accept(result); }
            catch (err) { if (valid()) setError(errorText(err)); }
            finally { if (valid()) { inFlight.current = false; setBusy(false); } }
          } });
        host.current.replaceChildren();
        sdk.renderButton(host.current, { type: "standard", theme: "outline", size: "large", text: "continue_with", width: Math.min(400, host.current.clientWidth || 300), locale: "vi" });
        setReady(true);
      } catch (err) { if (valid()) setError(errorText(err)); }
      finally { if (valid()) setLoading(false); }
    }
    void start();
    return () => { active = false; mounted.current = false; abort.abort(); };
    // Google SDK initializes once for each fresh server challenge; callbacks use the latest remember/redirect values.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [retry]);

  async function finish(event: React.FormEvent) {
    event.preventDefault();
    if (!pending || inFlight.current || !current()) return;
    inFlight.current = true;
    const attempt = generation.current;
    const valid = () => current() && generation.current === attempt;
    setBusy(true); setError("");
    try {
      const result = await service.complete(pending.ticket, pending.status === "NEW_ACCOUNT" ? { role } : { password });
      if (valid()) accept(result);
    } catch (err) { if (valid()) setError(errorText(err)); }
    finally { if (valid()) { inFlight.current = false; setBusy(false); } }
  }

  return <section className="rounded-xl border border-sky-100 bg-sky-50/40 p-4 space-y-3" aria-label="Đăng nhập bằng Google">
    <p className="font-semibold text-slate-800">Tiếp tục với Google</p>
    <p className="text-xs text-slate-600">Dùng Gmail của bạn, không cần tạo mật khẩu mới hoặc nhập OTP riêng.</p>
    <div ref={host} className={pending || busy ? "hidden" : "flex justify-center"} />
    {!ready && <button type="button" disabled className="w-full border rounded-lg bg-white px-4 py-2 text-slate-500">{loading ? "Đang chuẩn bị Google…" : "Tiếp tục với Google"}</button>}
    {busy && <p role="status" className="text-sm text-sky-700">Đang xác thực tài khoản…</p>}
    {error && <div className="space-y-2"><p role="alert" className="text-sm text-red-700">{error}</p>
      <button type="button" className="text-sm text-sky-700 underline" disabled={busy || loading} onClick={restart}>Thử lại Google</button></div>}
    {pending && <form onSubmit={finish} className="space-y-3">
      <p className="text-sm font-medium break-all">{pending.email}</p>
      {pending.status === "NEW_ACCOUNT" ? <>
        <p className="text-sm">Gmail đã được xác minh. Bạn muốn mua hàng hay mở gian hàng?</p>
        <label className="block text-sm font-medium" htmlFor="google-account-role">Vai trò tài khoản</label>
        <select id="google-account-role" className="w-full rounded-lg border bg-white p-2" value={role} disabled={busy} onChange={event => setRole(event.target.value as typeof role)}>
          <option value="ROLE_CUSTOMER">Người mua hàng</option><option value="ROLE_SELLER">Người bán hàng</option>
        </select>
      </> : <>
        <p className="text-sm">Gmail này đã có tài khoản HeliShop. Xác nhận mật khẩu hiện tại để liên kết Google và giữ nguyên dữ liệu.</p>
        <label className="block text-sm font-medium" htmlFor="google-link-password">Mật khẩu HeliShop hiện tại</label>
        <input id="google-link-password" type="password" autoComplete="current-password" required maxLength={72} value={password} disabled={busy}
          onChange={event => setPassword(event.target.value)} className="w-full rounded-lg border bg-white p-2" />
      </>}
      <button disabled={busy} className="w-full rounded-lg bg-sky-600 px-4 py-2 font-semibold text-white disabled:opacity-50">{pending.status === "NEW_ACCOUNT" ? "Hoàn tất đăng ký Google" : "Liên kết và đăng nhập"}</button>
      <button type="button" disabled={busy} onClick={restart} className="text-sm text-sky-700 underline">Chọn lại tài khoản Google</button>
    </form>}
  </section>;
}
