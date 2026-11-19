import { useState } from "react";
import type { FormEvent } from "react";
import { isAxiosError } from "axios";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { AuthDivider, AuthLayout } from "@/components/AuthLayout";
import { GoogleSignIn } from "@/components/GoogleSignIn";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { api } from "@/lib/api";
import { useAuthStore } from "@/store/useAuthStore";
import type { User } from "@/store/useAuthStore";

type Role = "ROLE_CUSTOMER" | "ROLE_SELLER";

interface RegistrationSession {
  userId: number;
  email: string;
  fullName: string;
  role: Role;
  accessToken: string;
  refreshToken: string;
}

function registrationError(error: unknown, fallback: string) {
  if (isAxiosError(error)) {
    if (error.response?.status === 503) {
      return "Tạm thời chưa gửi được mã qua email. Bạn có thể thử lại sau hoặc tiếp tục với Google.";
    }
    return error.response?.data?.message || fallback;
  }
  return fallback;
}

export function RegisterPage() {
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const setAuth = useAuthStore(state => state.setAuth);
  const initialRole = params.get("role") === "seller" ? "ROLE_SELLER" : "ROLE_CUSTOMER";
  const [role, setRole] = useState<Role>(initialRole);
  const [rememberMe, setRememberMe] = useState(true);
  const [email, setEmail] = useState("");
  const [emailWithCode, setEmailWithCode] = useState("");
  const [otp, setOtp] = useState("");
  const [password, setPassword] = useState("");
  const [confirmation, setConfirmation] = useState("");
  const [notice, setNotice] = useState("");
  const [error, setError] = useState("");
  const [sending, setSending] = useState(false);
  const [registering, setRegistering] = useState(false);
  const busy = sending || registering;

  async function sendCode(address: string) {
    if (busy) return;
    setError("");
    setNotice("");
    if (!/^[^\s@]+@gmail\.com$/.test(address)) {
      setError("Vui lòng nhập địa chỉ @gmail.com hợp lệ.");
      return;
    }

    setSending(true);
    try {
      await api.post("/auth/register/otp", { email: address });
      setEmailWithCode(address);
      setOtp("");
      setNotice("Mã xác nhận đã được gửi. Kiểm tra hộp thư và thư rác.");
    } catch (error) {
      setError(registrationError(error, "Chưa gửi được mã xác nhận. Vui lòng thử lại."));
    } finally {
      setSending(false);
    }
  }

  function changeEmail() {
    setEmailWithCode("");
    setOtp("");
    setPassword("");
    setConfirmation("");
    setNotice("");
    setError("");
  }

  async function register(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (busy || !emailWithCode) return;
    setError("");

    if (password !== confirmation) {
      setError("Mật khẩu xác nhận không khớp.");
      return;
    }

    setRegistering(true);
    try {
      const response = await api.post<{ data: RegistrationSession }>("/auth/register", {
        email: emailWithCode, otp, password, role,
      });
      const { userId, fullName, email, role: accountRole, accessToken, refreshToken } = response.data.data;
      const user: User = { id: userId, fullName, email, role: accountRole, status: "ACTIVE" };
      setAuth({ user, accessToken, refreshToken, rememberMe });
      navigate(accountRole === "ROLE_SELLER" ? "/seller/shop" : "/");
    } catch (error) {
      setError(registrationError(error, "Chưa đăng ký được tài khoản. Vui lòng thử lại."));
    } finally {
      setRegistering(false);
    }
  }

  return (
    <AuthLayout title="Đăng ký">
      {error && <p role="alert" className="mb-4 rounded-lg bg-red-50 p-3 text-sm text-red-800">{error}</p>}
      {notice && <p role="status" className="mb-4 rounded-lg bg-emerald-50 p-3 text-sm text-emerald-800">{notice}</p>}

      {emailWithCode ? (
        <form onSubmit={register} className="space-y-4">
          <div className="flex flex-wrap items-center justify-between gap-2 text-sm">
            <span className="min-w-0 break-all font-medium">{emailWithCode}</span>
            <button type="button" disabled={busy} onClick={changeEmail} className="font-medium text-sky-700 hover:underline">
              Đổi Gmail
            </button>
          </div>
          <div className="space-y-2">
            <div className="flex items-center justify-between gap-2">
              <label htmlFor="register-otp" className="text-sm font-medium">Mã OTP đăng ký (6 số)</label>
              <button type="button" disabled={busy} onClick={() => void sendCode(emailWithCode)} className="text-sm font-medium text-sky-700 hover:underline">
                Gửi lại mã
              </button>
            </div>
            <Input id="register-otp" value={otp} onChange={event => setOtp(event.target.value.replace(/\D/g, ""))}
              inputMode="numeric" autoComplete="one-time-code" minLength={6} maxLength={6} pattern="[0-9]{6}" required />
          </div>
          <div className="space-y-2">
            <label htmlFor="register-password" className="text-sm font-medium">Mật khẩu</label>
            <Input id="register-password" type="password" autoComplete="new-password" value={password}
              onChange={event => setPassword(event.target.value)} minLength={6} required />
          </div>
          <div className="space-y-2">
            <label htmlFor="register-confirmation" className="text-sm font-medium">Nhập lại mật khẩu</label>
            <Input id="register-confirmation" type="password" autoComplete="new-password" value={confirmation}
              onChange={event => setConfirmation(event.target.value)} minLength={6} required />
          </div>
          <div className="space-y-2">
            <label htmlFor="register-role" className="text-sm font-medium">Vai trò tài khoản</label>
            <select id="register-role" value={role} onChange={event => setRole(event.target.value as Role)}
              className="h-10 w-full rounded-lg border border-slate-200 bg-white px-3 text-sm">
              <option value="ROLE_CUSTOMER">Người mua hàng</option>
              <option value="ROLE_SELLER">Người bán hàng</option>
            </select>
          </div>
          <Button type="submit" className="h-11 w-full" isLoading={registering} disabled={sending}>Tạo tài khoản</Button>
        </form>
      ) : (
        <form onSubmit={event => { event.preventDefault(); void sendCode(email.trim().toLowerCase()); }} className="space-y-4">
          <div className="space-y-2">
            <label htmlFor="register-email" className="text-sm font-medium">Địa chỉ Gmail</label>
            <Input id="register-email" type="email" autoComplete="email" placeholder="ban@gmail.com"
              value={email} disabled={sending} onChange={event => setEmail(event.target.value)} className="h-11" required />
          </div>
          <Button type="submit" className="h-11 w-full" isLoading={sending}>Tiếp tục</Button>
        </form>
      )}
      <label className="mt-4 flex items-center gap-2 text-sm text-slate-700">
        <input type="checkbox" checked={rememberMe} onChange={event => setRememberMe(event.target.checked)}
          className="h-4 w-4 accent-sky-700" />
        Duy trì đăng nhập
      </label>
      <AuthDivider />
      <GoogleSignIn initialRole={initialRole} rememberMe={rememberMe} />
      <p className="mt-6 text-center text-sm text-slate-600">
        Đã có tài khoản? <Link to="/login" className="font-semibold text-sky-700 hover:underline">Đăng nhập</Link>
      </p>
    </AuthLayout>
  );
}
