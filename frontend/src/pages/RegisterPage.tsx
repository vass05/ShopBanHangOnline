import React, { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuthStore, User } from "@/store/useAuthStore";
import { api } from "@/lib/api";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import {
  Lock,
  Mail,
  User as UserIcon,
  Phone,
  Store,
  ShoppingBag,
  AlertCircle,
  CheckCircle2,
  Eye,
  EyeOff,
  Sparkles,
  X,
  Info,
} from "lucide-react";
import { cn } from "@/lib/utils";

type RoleType = "ROLE_CUSTOMER" | "ROLE_SELLER";

export const RegisterPage: React.FC = () => {
  const navigate = useNavigate();
  const { setAuth } = useAuthStore();

  const [role, setRole] = useState<RoleType>("ROLE_CUSTOMER");
  const [fullName, setFullName] = useState("");
  const [email, setEmail] = useState("");
  const [phone, setPhone] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);

  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const handleRegister = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);

    // Validation
    if (!fullName.trim()) {
      setErrorMessage("Vui lòng nhập họ và tên đầy đủ");
      return;
    }

    const hasEmail = Boolean(email.trim());
    const hasPhone = Boolean(phone.trim());

    if (!hasEmail && !hasPhone) {
      setErrorMessage("Vui lòng cung cấp ít nhất Địa chỉ Gmail hoặc Số điện thoại để đăng ký!");
      return;
    }

    if (hasEmail && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim())) {
      setErrorMessage("Địa chỉ Gmail không đúng định dạng (VD: name@gmail.com)!");
      return;
    }

    const cleanPhone = phone.trim().replace(/\s+/g, "");
    if (hasPhone && !/^(0|\+84)[0-9]{9}$/.test(cleanPhone)) {
      setErrorMessage("Số điện thoại không hợp lệ! Vui lòng nhập đúng 10 số (VD: 0988889999).");
      return;
    }

    if (password.length < 6) {
      setErrorMessage("Mật khẩu phải có tối thiểu 6 ký tự");
      return;
    }

    if (password !== confirmPassword) {
      setErrorMessage("Mật khẩu xác nhận không khớp với mật khẩu đã nhập");
      return;
    }

    setLoading(true);

    try {
      const response = await api.post("/auth/register", {
        fullName: fullName.trim(),
        email: hasEmail ? email.trim() : undefined,
        phone: hasPhone ? cleanPhone : undefined,
        password,
        role,
      });

      const data = response.data.data as any;
      const user: User = data.user || {
        id: data.userId || 1,
        fullName: data.fullName || fullName.trim(),
        email: data.email || (hasEmail ? email.trim() : `${cleanPhone}@phone.helishop.com`),
        phone: data.phone || (hasPhone ? cleanPhone : undefined),
        role: data.role || role,
        status: "ACTIVE",
      };

      setAuth({ user, accessToken: data.accessToken, refreshToken: data.refreshToken });

      // If seller, navigate to dashboard, else home
      if (user.role === "ROLE_SELLER") {
        navigate("/dashboard");
      } else {
        navigate("/");
      }
    } catch (err: any) {
      const msg =
        err.response?.data?.message ||
        "Đăng ký tài khoản thất bại. Vui lòng kiểm tra lại thông tin!";
      setErrorMessage(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-slate-100 via-sky-50/40 to-slate-100 p-4 py-10">
      <div className="w-full max-w-lg">
        {/* Brand Header */}
        <div className="text-center mb-6">
          <Link to="/" className="inline-block hover:scale-105 transition-transform">
            <div className="inline-flex items-center justify-center w-16 h-16 rounded-3xl bg-white shadow-xl shadow-sky-500/20 p-2 ring-2 ring-sky-100 mb-2">
              <img src="/images/logo.png" alt="HeliShop Logo" className="w-full h-full object-cover rounded-2xl" />
            </div>
          </Link>
          <h1 className="text-2xl font-extrabold tracking-tight text-slate-900">
            Tạo Tài Khoản Heli<span className="text-[#0284C7]">Shop</span>
          </h1>
          <p className="text-xs text-slate-500 mt-1 font-medium">
            Gia nhập cộng đồng mua sắm và kinh doanh trực tuyến hàng đầu
          </p>
        </div>

        {/* Error Alert */}
        {errorMessage && (
          <div className="mb-4 p-4 bg-red-50 border border-red-200 rounded-xl flex items-start gap-3 text-red-800 text-xs shadow-sm">
            <AlertCircle className="w-5 h-5 text-red-600 shrink-0 mt-0.5" />
            <div>
              <p className="font-semibold text-sm">Lỗi đăng ký</p>
              <p className="mt-0.5">{errorMessage}</p>
            </div>
          </div>
        )}

        <Card className="border-slate-200/80 shadow-xl bg-white/95 backdrop-blur-sm relative overflow-hidden">
          {/* Exit / Close button to go home */}
          <button
            type="button"
            onClick={() => navigate("/")}
            className="absolute top-4 right-4 p-2 text-slate-400 hover:text-slate-700 hover:bg-slate-100 rounded-full transition-all cursor-pointer z-10"
            title="Thoát về Trang Chủ"
          >
            <X className="w-5 h-5" />
          </button>

          <CardHeader className="space-y-1 pb-4 pr-12">
            <CardTitle className="text-xl font-bold">Đăng ký thành viên</CardTitle>
            <CardDescription className="text-xs">
              Chọn vai trò của bạn trên sàn và điền các thông tin dưới đây
            </CardDescription>
          </CardHeader>

          <form onSubmit={handleRegister}>
            <CardContent className="space-y-4">
              {/* Role Selection */}
              <div className="space-y-1.5">
                <label className="text-xs font-semibold text-slate-700 uppercase tracking-wider flex items-center justify-between">
                  <span>Bạn tham gia với vai trò gì?</span>
                  <span className="text-[11px] text-[#0284C7] font-normal lowercase">Bắt buộc</span>
                </label>
                <div className="grid grid-cols-2 gap-3 pt-1">
                  {/* Customer Card */}
                  <div
                    onClick={() => setRole("ROLE_CUSTOMER")}
                    className={cn(
                      "p-3.5 rounded-xl border-2 cursor-pointer transition-all flex flex-col justify-between relative",
                      role === "ROLE_CUSTOMER"
                        ? "border-[#0284C7] bg-sky-50/60 shadow-md shadow-sky-500/10 ring-1 ring-[#0284C7]/20"
                        : "border-slate-200 bg-white hover:border-slate-300 hover:bg-slate-50/50"
                    )}
                  >
                    {role === "ROLE_CUSTOMER" && (
                      <CheckCircle2 className="w-4 h-4 text-[#0284C7] absolute top-2.5 right-2.5" />
                    )}
                    <div className="p-2 w-9 h-9 rounded-lg bg-sky-100 text-[#0284C7] flex items-center justify-center mb-2">
                      <ShoppingBag className="w-5 h-5" />
                    </div>
                    <div>
                      <p className="text-xs font-bold text-slate-900">Người Mua Hàng</p>
                      <p className="text-[11px] text-slate-500 mt-0.5 leading-tight">
                        Khám phá triệu sản phẩm, voucher và deal Flash Sale
                      </p>
                    </div>
                  </div>

                  {/* Seller Card */}
                  <div
                    onClick={() => setRole("ROLE_SELLER")}
                    className={cn(
                      "p-3.5 rounded-xl border-2 cursor-pointer transition-all flex flex-col justify-between relative",
                      role === "ROLE_SELLER"
                        ? "border-[#0284C7] bg-sky-50/60 shadow-md shadow-sky-500/10 ring-1 ring-[#0284C7]/20"
                        : "border-slate-200 bg-white hover:border-slate-300 hover:bg-slate-50/50"
                    )}
                  >
                    {role === "ROLE_SELLER" && (
                      <CheckCircle2 className="w-4 h-4 text-[#0284C7] absolute top-2.5 right-2.5" />
                    )}
                    <div className="p-2 w-9 h-9 rounded-lg bg-amber-100 text-amber-600 flex items-center justify-center mb-2">
                      <Store className="w-5 h-5" />
                    </div>
                    <div>
                      <p className="text-xs font-bold text-slate-900">Người Bán Hàng</p>
                      <p className="text-[11px] text-slate-500 mt-0.5 leading-tight">
                        Mở gian hàng, đăng bán sản phẩm và quản lý đơn hàng
                      </p>
                    </div>
                  </div>
                </div>
              </div>

              {/* Full Name */}
              <div className="space-y-1.5">
                <label className="text-xs font-semibold text-slate-700 uppercase tracking-wider">
                  Họ và tên
                </label>
                <Input
                  type="text"
                  placeholder="Ví dụ: Nguyễn Văn A"
                  icon={<UserIcon className="w-4 h-4" />}
                  value={fullName}
                  onChange={(e) => setFullName(e.target.value)}
                  required
                />
                <p className="text-[11px] text-slate-500 flex items-center gap-1 pt-0.5">
                  <span className="text-sky-600 font-bold">*</span>
                  <span>Chú thích: Nhập đầy đủ họ và tên thật của bạn.</span>
                </p>
              </div>

              {/* Account Registration Method Notice */}
              <div className="p-3 bg-sky-50/70 border border-sky-200/80 rounded-xl flex items-start gap-2.5 text-xs text-sky-800">
                <Info className="w-4 h-4 text-[#0284C7] shrink-0 mt-0.5" />
                <span>
                  <strong>Phương thức đăng ký:</strong> Bạn có thể đăng ký bằng <strong>Gmail</strong> hoặc <strong>Số điện thoại</strong> (không bắt buộc cả hai, chỉ cần điền ít nhất 1 trong 2 thông tin).
                </span>
              </div>

              {/* Email */}
              <div className="space-y-1.5">
                <label className="text-xs font-semibold text-slate-700 uppercase tracking-wider flex items-center justify-between">
                  <span>Địa chỉ Gmail</span>
                  <span className="text-[11px] text-slate-400 font-normal lowercase">Tùy chọn nếu có SĐT</span>
                </label>
                <Input
                  type="email"
                  placeholder="name@gmail.com (hoặc để trống nếu dùng SĐT)"
                  icon={<Mail className="w-4 h-4" />}
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                />
                <p className="text-[11px] text-slate-500 flex items-center gap-1 pt-0.5">
                  <span className="text-sky-600 font-bold">*</span>
                  <span>Quy tắc: Nhập đúng định dạng Gmail (VD: user@gmail.com).</span>
                </p>
              </div>

              {/* Phone */}
              <div className="space-y-1.5">
                <label className="text-xs font-semibold text-slate-700 uppercase tracking-wider flex items-center justify-between">
                  <span>Số điện thoại</span>
                  <span className="text-[11px] text-slate-400 font-normal lowercase">Tùy chọn nếu có Gmail</span>
                </label>
                <Input
                  type="tel"
                  placeholder="0988889999 (hoặc để trống nếu dùng Gmail)"
                  icon={<Phone className="w-4 h-4" />}
                  value={phone}
                  onChange={(e) => setPhone(e.target.value)}
                />
                <p className="text-[11px] text-slate-500 flex items-center gap-1 pt-0.5">
                  <span className="text-sky-600 font-bold">*</span>
                  <span>Quy tắc: Số điện thoại di động gồm 10 chữ số (bắt đầu bằng 0 hoặc +84).</span>
                </p>
              </div>

              {/* Password */}
              <div className="space-y-1.5">
                <label className="text-xs font-semibold text-slate-700 uppercase tracking-wider">
                  Mật khẩu
                </label>
                <Input
                  type={showPassword ? "text" : "password"}
                  placeholder="Tối thiểu 6 ký tự..."
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
                  required
                />
                <p className="text-[11px] text-slate-500 flex items-center gap-1 pt-0.5">
                  <span className="text-sky-600 font-bold">*</span>
                  <span>Quy tắc: Mật khẩu bảo mật có độ dài tối thiểu từ 6 ký tự trở lên.</span>
                </p>
              </div>

              {/* Confirm Password */}
              <div className="space-y-1.5">
                <label className="text-xs font-semibold text-slate-700 uppercase tracking-wider">
                  Xác nhận mật khẩu
                </label>
                <Input
                  type={showConfirmPassword ? "text" : "password"}
                  placeholder="Nhập lại mật khẩu..."
                  icon={<Lock className="w-4 h-4" />}
                  rightElement={
                    <button
                      type="button"
                      onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                      className="p-1 text-slate-400 hover:text-slate-600 focus:outline-none transition-colors cursor-pointer"
                      title={showConfirmPassword ? "Ẩn mật khẩu" : "Hiện mật khẩu"}
                      tabIndex={-1}
                    >
                      {showConfirmPassword ? (
                        <EyeOff className="w-4 h-4 text-[#0284C7]" />
                      ) : (
                        <Eye className="w-4 h-4" />
                      )}
                    </button>
                  }
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  required
                />
                <p className="text-[11px] text-slate-500 flex items-center gap-1 pt-0.5">
                  <span className="text-sky-600 font-bold">*</span>
                  <span>Quy tắc: Nhập lại chính xác mật khẩu đã tạo ở trên.</span>
                </p>
              </div>
            </CardContent>

            <CardFooter className="flex flex-col gap-3 pt-2">
              <Button
                type="submit"
                variant="default"
                className="w-full text-sm py-5 bg-[#0284C7] hover:bg-[#0369A1] text-white shadow-md shadow-sky-600/20 cursor-pointer font-bold"
                isLoading={loading}
              >
                {role === "ROLE_SELLER" ? "Đăng Ký Mở Gian Hàng" : "Đăng Ký Tài Khoản"}
              </Button>

              <p className="text-xs text-center text-slate-500">
                Đã có tài khoản?{" "}
                <Link to="/login" className="text-[#0284C7] font-semibold hover:underline">
                  Đăng nhập ngay
                </Link>
              </p>
            </CardFooter>
          </form>
        </Card>
      </div>
    </div>
  );
};
