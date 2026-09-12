import React, { useState, useEffect } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { useAuthStore, User } from "@/store/useAuthStore";
import { api } from "@/lib/api";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import {
  Lock,
  Mail,
  AlertCircle,
  CheckCircle2,
  Eye,
  EyeOff,
  UserCheck,
  X,
  KeyRound,
  ArrowLeft,
  Clock,
  Sparkles,
} from "lucide-react";
import { cn } from "@/lib/utils";

interface RememberedAccount {
  email: string;
  fullName?: string;
  role?: string;
  lastLogin: number;
}

const STORAGE_KEY = "helishop_remembered_accounts";

export const LoginPage: React.FC = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { setAuth, isAuthenticated } = useAuthStore();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [rememberMe, setRememberMe] = useState(true);
  const [rememberedAccounts, setRememberedAccounts] = useState<RememberedAccount[]>([]);
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

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

  // Load remembered accounts from localStorage
  useEffect(() => {
    try {
      const stored = localStorage.getItem(STORAGE_KEY);
      if (stored) {
        const parsed: RememberedAccount[] = JSON.parse(stored);
        if (Array.isArray(parsed) && parsed.length > 0) {
          setRememberedAccounts(parsed);
        }
      }
    } catch {
      // Ignore parse error
    }
  }, []);

  useEffect(() => {
    if (isAuthenticated && !isExpired) {
      navigate("/");
    }
  }, [isAuthenticated, isExpired, navigate]);

  // Handle remove remembered account
  const removeRememberedAccount = (accEmail: string) => {
    const updated = rememberedAccounts.filter((a) => a.email !== accEmail);
    setRememberedAccounts(updated);
    localStorage.setItem(STORAGE_KEY, JSON.stringify(updated));
    if (email === accEmail) {
      setEmail("");
    }
  };

  // Clear all remembered accounts
  const clearAllRemembered = () => {
    setRememberedAccounts([]);
    localStorage.removeItem(STORAGE_KEY);
  };

  // Handle Login
  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);
    setSuccessMessage(null);
    setLoading(true);

    try {
      const response = await api.post("/auth/login", { email: email.trim(), password });
      const { accessToken, refreshToken, user } = response.data.data as {
        accessToken: string;
        refreshToken: string;
        user: User;
      };

      // Save or update remembered accounts
      if (rememberMe) {
        const newEntry: RememberedAccount = {
          email: user.email || email.trim(),
          fullName: user.fullName,
          role: user.role,
          lastLogin: Date.now(),
        };
        const filtered = rememberedAccounts.filter((a) => a.email !== newEntry.email);
        const updated = [newEntry, ...filtered].slice(0, 5); // Keep up to 5
        setRememberedAccounts(updated);
        localStorage.setItem(STORAGE_KEY, JSON.stringify(updated));
      } else {
        const updated = rememberedAccounts.filter((a) => a.email !== email.trim());
        setRememberedAccounts(updated);
        localStorage.setItem(STORAGE_KEY, JSON.stringify(updated));
      }

      setAuth({ user, accessToken, refreshToken });
      navigate("/");
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
      setForgotError("Vui lòng nhập email hoặc số điện thoại");
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
        newPassword: newPassword.trim(),
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
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-slate-100 via-sky-50/40 to-slate-100 p-4">
      <div className="w-full max-w-md">
        {/* HeliShop Brand Header */}
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-20 h-20 rounded-3xl bg-white shadow-xl shadow-sky-500/20 p-2 ring-2 ring-sky-100 mb-3 hover:scale-105 transition-transform">
            <img src="/images/logo.png" alt="HeliShop Logo" className="w-full h-full object-cover rounded-2xl" />
          </div>
          <h1 className="text-3xl font-extrabold tracking-tight text-slate-900">
            Heli<span className="text-[#0284C7]">Shop</span> Core
          </h1>
          <p className="text-sm text-slate-500 mt-1 font-medium">
            Sàn Thương Mại Điện Tử HeliShop - Trải Nghiệm Mua Sắm Số 1
          </p>
        </div>

        {/* Expired Notification Alert */}
        {isExpired && (
          <div className="mb-4 p-4 bg-amber-50 border border-amber-200 rounded-xl flex items-start gap-3 text-amber-800 text-sm shadow-sm">
            <AlertCircle className="w-5 h-5 text-amber-600 shrink-0 mt-0.5" />
            <div>
              <p className="font-semibold">Phiên làm việc đã hết hạn!</p>
              <p className="text-xs text-amber-700 mt-0.5">
                Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại để tiếp tục.
              </p>
            </div>
          </div>
        )}

        {/* Success Alert */}
        {successMessage && (
          <div className="mb-4 p-4 bg-emerald-50 border border-emerald-200 rounded-xl flex items-start gap-3 text-emerald-800 text-sm shadow-sm">
            <CheckCircle2 className="w-5 h-5 text-emerald-600 shrink-0 mt-0.5" />
            <div>
              <p className="font-semibold">Thành công</p>
              <p className="text-xs text-emerald-700 mt-0.5">{successMessage}</p>
            </div>
          </div>
        )}

        {/* Error Alert */}
        {errorMessage && (
          <div className="mb-4 p-4 bg-red-50 border border-red-200 rounded-xl flex items-start gap-3 text-red-800 text-sm shadow-sm">
            <AlertCircle className="w-5 h-5 text-red-600 shrink-0 mt-0.5" />
            <div>
              <p className="font-semibold">Lỗi xác thực</p>
              <p className="text-xs text-red-700 mt-0.5">{errorMessage}</p>
            </div>
          </div>
        )}

        <Card className="border-slate-200/80 shadow-xl bg-white/95 backdrop-blur-sm">
          <CardHeader className="space-y-1 pb-4">
            <CardTitle className="text-2xl font-bold">Đăng nhập</CardTitle>
            <CardDescription>
              Nhập Gmail hoặc Số điện thoại để truy cập tài khoản mua sắm
            </CardDescription>
          </CardHeader>

          <form onSubmit={handleLogin}>
            <CardContent className="space-y-4">
              {/* Remembered Accounts Chips */}
              {rememberedAccounts.length > 0 && (
                <div className="p-3 bg-slate-50/80 rounded-xl border border-slate-200/70 space-y-2">
                  <div className="flex items-center justify-between text-xs text-slate-500 font-medium">
                    <span className="flex items-center gap-1.5 text-slate-700 font-semibold">
                      <Clock className="w-3.5 h-3.5 text-[#0284C7]" /> Tài khoản đã lưu:
                    </span>
                    <button
                      type="button"
                      onClick={clearAllRemembered}
                      className="text-[11px] text-slate-400 hover:text-red-500 transition-colors cursor-pointer"
                    >
                      Xóa tất cả
                    </button>
                  </div>
                  <div className="flex flex-wrap gap-1.5">
                    {rememberedAccounts.map((acc) => (
                      <div
                        key={acc.email}
                        onClick={() => setEmail(acc.email)}
                        className={cn(
                          "group flex items-center gap-1 px-2.5 py-1 rounded-lg text-xs font-medium border cursor-pointer transition-all",
                          email === acc.email
                            ? "bg-sky-50 border-[#0284C7] text-[#0284C7] shadow-sm ring-1 ring-[#0284C7]/20"
                            : "bg-white border-slate-200 text-slate-700 hover:bg-sky-50/50 hover:border-sky-300"
                        )}
                        title={`Bấm để chọn: ${acc.email} ${acc.fullName ? `(${acc.fullName})` : ""}`}
                      >
                        <UserCheck className="w-3 h-3 text-[#0284C7] shrink-0" />
                        <span className="max-w-[150px] truncate">{acc.email}</span>
                        <button
                          type="button"
                          onClick={(e) => {
                            e.stopPropagation();
                            removeRememberedAccount(acc.email);
                          }}
                          className="p-0.5 rounded-full hover:bg-slate-200 text-slate-400 hover:text-slate-600 transition-colors ml-0.5"
                          title="Xóa tài khoản này khỏi danh sách lưu"
                        >
                          <X className="w-2.5 h-2.5" />
                        </button>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* Email Input */}
              <div className="space-y-1.5">
                <label className="text-xs font-semibold text-slate-700 uppercase tracking-wider">
                  Gmail hoặc Số điện thoại
                </label>
                <Input
                  type="text"
                  placeholder="Nhập Gmail hoặc số điện thoại..."
                  icon={<Mail className="w-4 h-4" />}
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  autoComplete="username"
                  required
                />
              </div>

              {/* Password Input with Show/Hide Toggle */}
              <div className="space-y-1.5">
                <div className="flex items-center justify-between">
                  <label className="text-xs font-semibold text-slate-700 uppercase tracking-wider">
                    Mật khẩu
                  </label>
                  <button
                    type="button"
                    onClick={() => {
                      setForgotEmail(email);
                      setShowForgotModal(true);
                    }}
                    className="text-xs text-[#0284C7] hover:underline font-medium cursor-pointer"
                  >
                    Quên mật khẩu?
                  </button>
                </div>
                <Input
                  type={showPassword ? "text" : "password"}
                  placeholder="••••••••"
                  icon={<Lock className="w-4 h-4" />}
                  rightElement={
                    <button
                      type="button"
                      onClick={() => setShowPassword(!showPassword)}
                      className="p-1 text-slate-400 hover:text-slate-600 focus:outline-none transition-colors cursor-pointer"
                      title={showPassword ? "Ẩn mật khẩu" : "Hiện mật khẩu"}
                      tabIndex={-1}
                    >
                      {showPassword ? (
                        <EyeOff className="w-4 h-4 text-[#0284C7]" />
                      ) : (
                        <Eye className="w-4 h-4" />
                      )}
                    </button>
                  }
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  autoComplete="current-password"
                  required
                />
              </div>

              {/* Remember Me Checkbox */}
              <div className="flex items-center justify-between pt-1">
                <label className="flex items-center gap-2 cursor-pointer text-xs text-slate-600 select-none hover:text-slate-900">
                  <input
                    type="checkbox"
                    checked={rememberMe}
                    onChange={(e) => setRememberMe(e.target.checked)}
                    className="w-4 h-4 rounded border-slate-300 text-[#0284C7] focus:ring-[#0284C7] accent-[#0284C7]"
                  />
                  <span>Ghi nhớ tài khoản trên thiết bị này</span>
                </label>
              </div>
            </CardContent>

            <CardFooter className="flex flex-col gap-3 pt-2">
              <Button
                type="submit"
                variant="default"
                className="w-full text-base py-5 bg-[#0284C7] hover:bg-[#0369A1] text-white shadow-md shadow-sky-600/20 cursor-pointer"
                isLoading={loading}
              >
                Đăng Nhập
              </Button>
              <p className="text-xs text-center text-slate-500">
                Chưa có tài khoản?{" "}
                <Link to="/register" className="text-[#0284C7] font-semibold hover:underline">
                  Đăng ký ngay
                </Link>
              </p>
            </CardFooter>
          </form>
        </Card>
      </div>

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
                    Vui lòng nhập <strong>Gmail</strong> hoặc <strong>Số điện thoại</strong> đã đăng ký. Hệ thống sẽ cấp mã xác thực OTP có hiệu lực trong 10 phút.
                  </p>

                  <div className="space-y-1.5">
                    <label className="text-xs font-semibold text-slate-700 uppercase tracking-wider">
                      Gmail hoặc Số điện thoại
                    </label>
                    <Input
                      type="text"
                      placeholder="Nhập Gmail hoặc số điện thoại..."
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
    </div>
  );
};
