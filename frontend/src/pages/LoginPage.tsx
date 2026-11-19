import React, { useState, useEffect, useRef } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { useAuthStore, User } from "@/store/useAuthStore";
import { GoogleSignIn } from "@/components/GoogleSignIn";
import { api } from "@/lib/api";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { AuthDivider, AuthLayout } from "@/components/AuthLayout";
import {
  Lock,
  Mail,
  AlertCircle,
  CheckCircle2,
  Eye,
  EyeOff,
  X,
  KeyRound,
  Sparkles,
  ArrowLeft,
} from "lucide-react";

const REMEMBERED_ACCOUNT_KEY = "helishop_remembered_account";
const REMEMBER_ME_FLAG_KEY = "helishop_remember_me";

export const LoginPage: React.FC = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { setAuth, isAuthenticated, user } = useAuthStore();

  const emailInputRef = useRef<HTMLInputElement>(null);
  const passwordInputRef = useRef<HTMLInputElement>(null);

  // Đọc tài khoản đã ghi nhớ trên thiết bị này từ localStorage
  const [email, setEmail] = useState(() => {
    return localStorage.getItem(REMEMBERED_ACCOUNT_KEY) || "";
  });
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);

  // Trạng thái checkbox ghi nhớ (mặc định lấy theo cài đặt trước đó hoặc true)
  const [rememberMe, setRememberMe] = useState(() => {
    const saved = localStorage.getItem(REMEMBER_ME_FLAG_KEY);
    return saved !== null ? saved === "true" : true;
  });

  const [hasRememberedAccount, setHasRememberedAccount] = useState(() => {
    return !!localStorage.getItem(REMEMBERED_ACCOUNT_KEY);
  });

  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [dismissExpired, setDismissExpired] = useState(false);

  // Forgot Password Modal States
  const [showForgotModal, setShowForgotModal] = useState(false);
  const [forgotEmail, setForgotEmail] = useState("");
  const [forgotOtp, setForgotOtp] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [showNewPassword, setShowNewPassword] = useState(false);
  const [forgotStep, setForgotStep] = useState<1 | 2 | 3>(1);
  const [forgotLoading, setForgotLoading] = useState(false);
  const [forgotError, setForgotError] = useState<string | null>(null);
  const [forgotInfo, setForgotInfo] = useState<string | null>(null);

  const isExpired = searchParams.get("expired") === "true";

  useEffect(() => {
    if (isAuthenticated && !isExpired) {
      const redirect = searchParams.get("redirect");
      navigate(redirect?.startsWith("/") && !redirect.startsWith("//") ? redirect : user?.role === "ROLE_ADMIN" ? "/admin/users" : user?.role === "ROLE_SELLER" ? "/seller/shop" : "/");
    }
  }, [isAuthenticated, isExpired, navigate, searchParams, user?.role]);

  // Khi tải trang: nếu đã có tài khoản ghi nhớ thì tự động focus vào ô Mật khẩu
  useEffect(() => {
    if (email) {
      passwordInputRef.current?.focus();
    } else {
      emailInputRef.current?.focus();
    }
  }, []);

  // Xử lý bật/tắt ghi nhớ tài khoản
  const handleRememberMeToggle = (checked: boolean) => {
    setRememberMe(checked);
    localStorage.setItem(REMEMBER_ME_FLAG_KEY, String(checked));
    if (!checked) {
      localStorage.removeItem(REMEMBERED_ACCOUNT_KEY);
      setHasRememberedAccount(false);
    } else if (email.trim()) {
      localStorage.setItem(REMEMBERED_ACCOUNT_KEY, email.trim());
      setHasRememberedAccount(true);
    }
  };

  // Xóa nhanh tài khoản đã ghi nhớ trên thiết bị để đổi tài khoản khác
  const handleClearRememberedAccount = () => {
    setEmail("");
    localStorage.removeItem(REMEMBERED_ACCOUNT_KEY);
    setHasRememberedAccount(false);
    emailInputRef.current?.focus();
  };

  // Handle Login
  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);
    setSuccessMessage(null);
    setLoading(true);

    try {
      const response = await api.post("/auth/login", { email: email.trim(), password });
      const data = response.data.data as any;
      const accessToken = data.accessToken;
      const refreshToken = data.refreshToken;
      const user: User = data.user || {
        id: data.userId || 1,
        fullName: data.fullName || "Người dùng",
        email: data.email || email.trim(),
        phone: data.phone,
        role: data.role || "ROLE_CUSTOMER",
        status: "ACTIVE",
      };

      // Ghi nhớ tài khoản trên thiết bị này theo lựa chọn của người dùng
      if (rememberMe) {
        localStorage.setItem(REMEMBERED_ACCOUNT_KEY, email.trim());
        localStorage.setItem(REMEMBER_ME_FLAG_KEY, "true");
        setHasRememberedAccount(true);
      } else {
        localStorage.removeItem(REMEMBERED_ACCOUNT_KEY);
        localStorage.setItem(REMEMBER_ME_FLAG_KEY, "false");
        setHasRememberedAccount(false);
      }

      setAuth({ user, accessToken, refreshToken, rememberMe });
      const redirect = searchParams.get("redirect");
      navigate(redirect?.startsWith("/") && !redirect.startsWith("//") ? redirect : user.role === "ROLE_ADMIN" ? "/admin/users" : user.role === "ROLE_SELLER" ? "/seller/shop" : "/");
    } catch (err: any) {
      const msg = err.response?.data?.message || "Đăng nhập thất bại. Vui lòng kiểm tra lại thông tin!";
      setErrorMessage(msg);
    } finally {
      setLoading(false);
    }
  };

  // Forgot Password: Step 1 - Send OTP
  const handleSendOtp = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!forgotEmail.trim()) {
      setForgotError("Vui lòng nhập email");
      return;
    }

    setForgotError(null);
    setForgotInfo(null);
    setForgotLoading(true);

    try {
      const res = await api.post("/auth/forgot-password", { email: forgotEmail.trim() });
      const message = res.data.data || res.data.message || "Mã OTP xác thực đã được gửi!";
      setForgotInfo(message);
      setForgotStep(2);
    } catch (err: any) {
      const msg = err.response?.data?.message || "Không thể gửi mã xác thực. Vui lòng thử lại!";
      setForgotError(msg);
    } finally {
      setForgotLoading(false);
    }
  };

  // Forgot Password: Step 2 - Reset Password with OTP
  const handleResetPassword = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!forgotOtp.trim() || forgotOtp.trim().length !== 6) {
      setForgotError("Mã OTP phải có đúng 6 chữ số");
      return;
    }
    if (newPassword.length < 6) {
      setForgotError("Mật khẩu mới phải có tối thiểu 6 ký tự");
      return;
    }
    if (newPassword !== confirmPassword) {
      setForgotError("Mật khẩu xác nhận không khớp");
      return;
    }

    setForgotError(null);
    setForgotLoading(true);

    try {
      await api.post("/auth/reset-password", {
        email: forgotEmail.trim(),
        otp: forgotOtp.trim(),
        newPassword,
      });

      setForgotStep(3);
      setEmail(forgotEmail.trim());
      setSuccessMessage("Đặt lại mật khẩu thành công! Hãy đăng nhập bằng mật khẩu mới.");
    } catch (err: any) {
      const msg = err.response?.data?.message || "Đặt lại mật khẩu thất bại. Vui lòng kiểm tra lại mã OTP!";
      setForgotError(msg);
    } finally {
      setForgotLoading(false);
    }
  };

  // Close Forgot Modal and clean up
  const handleCloseForgotModal = () => {
    setShowForgotModal(false);
    setForgotStep(1);
    setForgotError(null);
    setForgotInfo(null);
    setForgotOtp("");
    setNewPassword("");
    setConfirmPassword("");
  };

  return (
    <>
      <AuthLayout title="Đăng nhập">
        {isExpired && !dismissExpired && (
          <div role="status" className="mb-4 flex items-center justify-between gap-2 rounded-lg bg-amber-50 p-3 text-sm text-amber-900">
            Phiên đã hết hạn. Vui lòng đăng nhập lại.
            <button type="button" aria-label="Đóng thông báo" onClick={() => setDismissExpired(true)}><X size={16} /></button>
          </div>
        )}
        {errorMessage && <p role="alert" className="mb-4 rounded-lg bg-red-50 p-3 text-sm text-red-800">{errorMessage}</p>}
        {successMessage && <p role="status" className="mb-4 rounded-lg bg-emerald-50 p-3 text-sm text-emerald-800">{successMessage}</p>}

        <form onSubmit={handleLogin} className="space-y-4">
          <div className="space-y-2">
            <div className="flex items-center justify-between gap-2">
              <label htmlFor="login-email" className="text-sm font-medium">Email</label>
              {hasRememberedAccount && email && (
                <button type="button" onClick={handleClearRememberedAccount} className="text-xs font-medium text-sky-700">
                  Đổi tài khoản
                </button>
              )}
            </div>
            <Input id="login-email" type="email" ref={emailInputRef} value={email}
              onChange={event => setEmail(event.target.value)} autoComplete="username"
              placeholder="ban@gmail.com" className="h-11" required />
          </div>
          <div className="space-y-2">
            <div className="flex items-center justify-between gap-2">
              <label htmlFor="login-password" className="text-sm font-medium">Mật khẩu</label>
              <button type="button" className="text-sm font-medium text-sky-700 hover:underline"
                onClick={() => { setForgotEmail(email); setShowForgotModal(true); }}>Quên mật khẩu?</button>
            </div>
            <Input id="login-password" ref={passwordInputRef} type={showPassword ? "text" : "password"}
              value={password} onChange={event => setPassword(event.target.value)}
              autoComplete="current-password" className="h-11" required
              rightElement={<button type="button" onClick={() => setShowPassword(!showPassword)}
                aria-label={showPassword ? "Ẩn mật khẩu" : "Hiện mật khẩu"}>
                {showPassword ? <EyeOff size={17} /> : <Eye size={17} />}
              </button>} />
          </div>
          <Button type="submit" className="h-11 w-full" isLoading={loading}>Đăng Nhập</Button>
          <label className="flex items-center gap-2 text-sm text-slate-700">
            <input type="checkbox" checked={rememberMe} onChange={event => handleRememberMeToggle(event.target.checked)}
              className="h-4 w-4 accent-sky-700" />
            Duy trì đăng nhập
          </label>
        </form>
        <AuthDivider />
        <GoogleSignIn rememberMe={rememberMe} />
        <p className="mt-6 text-center text-sm text-slate-600">
          Chưa có tài khoản? <Link to="/register" className="font-semibold text-sky-700 hover:underline">Đăng ký</Link>
        </p>
      </AuthLayout>


      {/* Forgot Password Modal */}
      {showForgotModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 backdrop-blur-sm p-4 animate-in fade-in duration-200">
          <div className="w-full max-w-md bg-white rounded-2xl shadow-2xl border border-slate-200/80 overflow-hidden">
            {/* Modal Header */}
            <div className="flex items-center justify-between px-6 py-4 border-b border-slate-100 bg-slate-50/50">
              <div className="flex items-center gap-2">
                <div className="p-2 rounded-xl bg-sky-100/80 text-[#0284C7]">
                  <KeyRound className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="text-base font-bold text-slate-900">Lấy lại mật khẩu</h3>
                  <p className="text-xs text-slate-500">
                    {forgotStep === 1 && "Bước 1: Nhập tài khoản để nhận mã xác thực OTP"}
                    {forgotStep === 2 && "Bước 2: Xác thực OTP và đặt mật khẩu mới"}
                    {forgotStep === 3 && "Hoàn tất khôi phục mật khẩu"}
                  </p>
                </div>
              </div>
              <button
                type="button"
                onClick={handleCloseForgotModal}
                className="p-1.5 rounded-lg text-slate-400 hover:text-slate-600 hover:bg-slate-100 transition-colors cursor-pointer"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Modal Content */}
            <div className="p-6">
              {/* Error in modal */}
              {forgotError && (
                <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded-xl flex items-start gap-2.5 text-red-800 text-xs">
                  <AlertCircle className="w-4 h-4 text-red-600 shrink-0 mt-0.5" />
                  <span>{forgotError}</span>
                </div>
              )}

              {/* Info in modal */}
              {forgotInfo && (
                <div className="mb-4 p-3 bg-sky-50 border border-sky-200 rounded-xl flex items-start gap-2.5 text-sky-900 text-xs">
                  <Sparkles className="w-4 h-4 text-[#0284C7] shrink-0 mt-0.5" />
                  <span className="font-medium">{forgotInfo}</span>
                </div>
              )}

              {/* Step 1: Input Email */}
              {forgotStep === 1 && (
                <form onSubmit={handleSendOtp} className="space-y-4">
                  <p className="text-xs text-slate-600 leading-relaxed">
                    Vui lòng nhập <strong>email</strong> đã đăng ký. Hệ thống sẽ cấp mã xác thực OTP có hiệu lực trong 10 phút.
                  </p>

                  <div className="space-y-1.5">
                    <label className="text-xs font-semibold text-slate-700 uppercase tracking-wider">
                      Email / Gmail
                    </label>
                    <Input
                      type="email"
                      placeholder="Nhập email đã đăng ký..."
                      icon={<Mail className="w-4 h-4" />}
                      value={forgotEmail}
                      onChange={(e) => setForgotEmail(e.target.value)}
                      required
                      autoFocus
                    />
                  </div>

                  <div className="flex gap-2 pt-2">
                    <Button
                      type="button"
                      variant="outline"
                      onClick={handleCloseForgotModal}
                      className="w-1/3 text-xs"
                    >
                      Hủy bỏ
                    </Button>
                    <Button
                      type="submit"
                      variant="default"
                      className="w-2/3 bg-[#0284C7] hover:bg-[#0369A1] text-white text-xs"
                      isLoading={forgotLoading}
                    >
                      Gửi mã xác thực OTP
                    </Button>
                  </div>
                </form>
              )}

              {/* Step 2: Input OTP and New Password */}
              {forgotStep === 2 && (
                <form onSubmit={handleResetPassword} className="space-y-4">
                  <div className="space-y-1.5">
                    <label className="text-xs font-semibold text-slate-700 uppercase tracking-wider">
                      Mã xác thực OTP (6 số)
                    </label>
                    <Input
                      type="text"
                      placeholder="Ví dụ: 123456"
                      icon={<KeyRound className="w-4 h-4" />}
                      value={forgotOtp}
                      onChange={(e) => setForgotOtp(e.target.value)}
                      maxLength={6}
                      required
                      autoFocus
                    />
                  </div>

                  <div className="space-y-1.5">
                    <label className="text-xs font-semibold text-slate-700 uppercase tracking-wider">
                      Mật khẩu mới
                    </label>
                    <Input
                      type={showNewPassword ? "text" : "password"}
                      placeholder="Tối thiểu 6 ký tự..."
                      icon={<Lock className="w-4 h-4" />}
                      rightElement={
                        <button
                          type="button"
                          onClick={() => setShowNewPassword(!showNewPassword)}
                          className="p-1 text-slate-400 hover:text-slate-600 focus:outline-none cursor-pointer"
                          tabIndex={-1}
                        >
                          {showNewPassword ? (
                            <EyeOff className="w-4 h-4 text-[#0284C7]" />
                          ) : (
                            <Eye className="w-4 h-4" />
                          )}
                        </button>
                      }
                      value={newPassword}
                      onChange={(e) => setNewPassword(e.target.value)}
                      required
                    />
                  </div>

                  <div className="space-y-1.5">
                    <label className="text-xs font-semibold text-slate-700 uppercase tracking-wider">
                      Xác nhận mật khẩu mới
                    </label>
                    <Input
                      type={showNewPassword ? "text" : "password"}
                      placeholder="Nhập lại mật khẩu mới..."
                      icon={<Lock className="w-4 h-4" />}
                      value={confirmPassword}
                      onChange={(e) => setConfirmPassword(e.target.value)}
                      required
                    />
                  </div>

                  <div className="flex items-center justify-between pt-1">
                    <button
                      type="button"
                      onClick={() => {
                        setForgotStep(1);
                        setForgotError(null);
                      }}
                      className="text-xs text-slate-500 hover:text-slate-800 flex items-center gap-1 cursor-pointer"
                    >
                      <ArrowLeft className="w-3 h-3" /> Đổi tài khoản khác
                    </button>
                    <button
                      type="button"
                      onClick={handleSendOtp}
                      disabled={forgotLoading}
                      className="text-xs text-[#0284C7] hover:underline font-medium cursor-pointer"
                    >
                      Gửi lại mã OTP
                    </button>
                  </div>

                  <div className="flex gap-2 pt-2">
                    <Button
                      type="button"
                      variant="outline"
                      onClick={handleCloseForgotModal}
                      className="w-1/3 text-xs"
                    >
                      Hủy bỏ
                    </Button>
                    <Button
                      type="submit"
                      variant="default"
                      className="w-2/3 bg-[#0284C7] hover:bg-[#0369A1] text-white text-xs"
                      isLoading={forgotLoading}
                    >
                      Xác nhận đặt lại
                    </Button>
                  </div>
                </form>
              )}

              {/* Step 3: Success Screen */}
              {forgotStep === 3 && (
                <div className="text-center py-4 space-y-4">
                  <div className="w-14 h-14 rounded-full bg-emerald-100 text-emerald-600 flex items-center justify-center mx-auto shadow-inner">
                    <CheckCircle2 className="w-8 h-8" />
                  </div>
                  <div>
                    <h4 className="text-base font-bold text-slate-900">Đặt lại mật khẩu thành công!</h4>
                    <p className="text-xs text-slate-500 mt-1 max-w-xs mx-auto">
                      Mật khẩu của tài khoản <strong>{forgotEmail}</strong> đã được cập nhật an toàn.
                    </p>
                  </div>
                  <Button
                    type="button"
                    onClick={handleCloseForgotModal}
                    className="w-full bg-[#0284C7] hover:bg-[#0369A1] text-white text-sm"
                  >
                    Đăng nhập ngay
                  </Button>
                </div>
              )}
            </div>
          </div>
        </div>
      )}
    </>
  );
};
