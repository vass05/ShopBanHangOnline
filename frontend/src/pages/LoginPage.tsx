import React, { useState, useEffect } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { useAuthStore, User } from "@/store/useAuthStore";
import { api } from "@/lib/api";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { Lock, Mail, ShoppingBag, AlertCircle, CheckCircle2, ShieldCheck } from "lucide-react";

export const LoginPage: React.FC = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { setAuth, isAuthenticated } = useAuthStore();

  const [email, setEmail] = useState("customer@helishop.com");
  const [password, setPassword] = useState("Password123!");
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const isExpired = searchParams.get("expired") === "true";

  useEffect(() => {
    if (isAuthenticated && !isExpired) {
      navigate("/dashboard");
    }
  }, [isAuthenticated, isExpired, navigate]);

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);
    setLoading(true);

    try {
      const response = await api.post("/auth/login", { email, password });
      const { accessToken, refreshToken, user } = response.data.data as {
        accessToken: string;
        refreshToken: string;
        user: User;
      };

      setAuth({ user, accessToken, refreshToken });
      navigate("/dashboard");
    } catch (err: any) {
      const msg = err.response?.data?.message || "Đăng nhập thất bại. Vui lòng kiểm tra lại thông tin!";
      setErrorMessage(msg);
    } finally {
      setLoading(false);
    }
  };

  const handleAutoFill = (fillEmail: string) => {
    setEmail(fillEmail);
    setPassword("Password123!");
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
          <div className="mb-4 p-4 bg-amber-50 border border-amber-200 rounded-xl flex items-start gap-3 text-amber-800 text-sm shadow-sm animate-bounce-short">
            <AlertCircle className="w-5 h-5 text-amber-600 shrink-0 mt-0.5" />
            <div>
              <p className="font-semibold">Phiên làm việc đã hết hạn!</p>
              <p className="text-xs text-amber-700 mt-0.5">
                Refresh token đã hết hạn hoặc phiên đăng xuất đã được thu hồi. Vui lòng đăng nhập lại.
              </p>
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
              Nhập thông tin tài khoản hoặc chọn tài khoản mẫu bên dưới
            </CardDescription>
          </CardHeader>
          <form onSubmit={handleLogin}>
            <CardContent className="space-y-4">
              <div className="space-y-1.5">
                <label className="text-xs font-semibold text-slate-700 uppercase tracking-wider">
                  Địa chỉ Email
                </label>
                <Input
                  type="email"
                  placeholder="name@example.com"
                  icon={<Mail className="w-4 h-4" />}
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  required
                />
              </div>

              <div className="space-y-1.5">
                <div className="flex items-center justify-between">
                  <label className="text-xs font-semibold text-slate-700 uppercase tracking-wider">
                    Mật khẩu
                  </label>
                  <span className="text-xs text-[#0284C7] hover:underline cursor-pointer">
                    Quên mật khẩu?
                  </span>
                </div>
                <Input
                  type="password"
                  placeholder="••••••••"
                  icon={<Lock className="w-4 h-4" />}
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  required
                />
              </div>

              {/* Quick autofill sample accounts */}
              <div className="pt-2 border-t border-slate-100">
                <p className="text-xs text-slate-500 mb-2 font-medium">Tài khoản kiểm thử nhanh (Demo):</p>
                <div className="grid grid-cols-2 gap-2">
                  <button
                    type="button"
                    onClick={() => handleAutoFill("customer@helishop.com")}
                    className="text-xs py-1.5 px-2.5 rounded-lg border border-slate-200 hover:border-sky-400 hover:bg-sky-50 text-slate-700 hover:text-[#0284C7] transition-colors text-left flex items-center gap-1.5"
                  >
                    <CheckCircle2 className="w-3.5 h-3.5 text-emerald-500" />
                    <span>Customer</span>
                  </button>
                  <button
                    type="button"
                    onClick={() => handleAutoFill("seller@helishop.com")}
                    className="text-xs py-1.5 px-2.5 rounded-lg border border-slate-200 hover:border-sky-400 hover:bg-sky-50 text-slate-700 hover:text-[#0284C7] transition-colors text-left flex items-center gap-1.5"
                  >
                    <ShieldCheck className="w-3.5 h-3.5 text-sky-600" />
                    <span>Seller</span>
                  </button>
                </div>
              </div>
            </CardContent>

            <CardFooter className="flex flex-col gap-3 pt-2">
              <Button
                type="submit"
                variant="default"
                className="w-full text-base py-5 bg-[#0284C7] hover:bg-[#0369A1] text-white shadow-md shadow-sky-600/20"
                isLoading={loading}
              >
                Đăng Nhập
              </Button>
              <p className="text-xs text-center text-slate-500">
                Chưa có tài khoản?{" "}
                <span className="text-[#0284C7] font-semibold cursor-pointer hover:underline">
                  Đăng ký ngay
                </span>
              </p>
            </CardFooter>
          </form>
        </Card>
      </div>
    </div>
  );
};
