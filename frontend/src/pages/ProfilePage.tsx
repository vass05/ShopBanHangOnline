import React, { useState, useEffect, useRef } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/layout/Footer";
import { useAuthStore } from "@/store/useAuthStore";
import {
  profileService,
  UserProfile,
  UserAddress,
  AddressData,
} from "@/services/profileService";
import {
  User as UserIcon,
  Mail,
  Phone,
  MapPin,
  Lock,
  Camera,
  Check,
  CheckCircle2,
  AlertCircle,
  Plus,
  Edit3,
  Trash2,
  Eye,
  EyeOff,
  Package,
  LogOut,
  ChevronRight,
  ShieldCheck,
  Sparkles,
  Loader2,
  Upload,
  X,
} from "lucide-react";

export const ShopeeDefaultAvatar: React.FC<{ className?: string; iconSize?: string }> = ({
  className = "w-28 h-28 sm:w-32 sm:h-32",
  iconSize = "w-16 h-16 sm:w-18 sm:h-18",
}) => (
  <div
    className={`rounded-full bg-[#EFEFEF] flex items-center justify-center overflow-hidden shrink-0 select-none ${className}`}
  >
    <svg
      viewBox="0 0 15 15"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      className={iconSize}
    >
      <circle
        cx="7.5"
        cy="4.5"
        r="2.5"
        stroke="#C6C6C6"
        strokeWidth="1.2"
      />
      <path
        d="M1.5 14.2C1.5 10.9 4.2 8.2 7.5 8.2C10.8 8.2 13.5 10.9 13.5 14.2"
        stroke="#C6C6C6"
        strokeWidth="1.2"
        strokeLinecap="round"
      />
    </svg>
  </div>
);

export const ProfilePage: React.FC = () => {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const currentTab = searchParams.get("tab") || "profile";

  const { user: authUser, updateUser, logout } = useAuthStore();

  // Active Tab: "profile" | "addresses" | "password"
  const [activeTab, setActiveTab] = useState<string>(currentTab);

  // Profile Form State
  const [fullName, setFullName] = useState(authUser?.fullName || "");
  const [phone, setPhone] = useState(authUser?.phone || "");
  const [avatarUrl, setAvatarUrl] = useState(authUser?.avatarUrl || "");
  const [avatarError, setAvatarError] = useState(false);
  const [sidebarAvatarError, setSidebarAvatarError] = useState(false);
  const [isUpdatingProfile, setIsUpdatingProfile] = useState(false);
  const [isUploadingAvatar, setIsUploadingAvatar] = useState(false);
  const [profileSuccessMsg, setProfileSuccessMsg] = useState("");
  const [profileErrorMsg, setProfileErrorMsg] = useState("");
  const fileInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    setAvatarError(false);
    setSidebarAvatarError(false);
  }, [avatarUrl]);

  const resolveAvatar = (url?: string) => {
    if (!url) return "";
    if (url.startsWith("/uploads/")) {
      return `http://localhost:8080${url}`;
    }
    return url;
  };

  // Addresses State
  const [addresses, setAddresses] = useState<UserAddress[]>([]);
  const [isLoadingAddresses, setIsLoadingAddresses] = useState(false);
  const [isAddressModalOpen, setIsAddressModalOpen] = useState(false);
  const [editingAddress, setEditingAddress] = useState<UserAddress | null>(null);
  const [addressForm, setAddressForm] = useState<AddressData>({
    recipientName: "",
    phone: "",
    provinceCity: "",
    district: "",
    ward: "",
    streetAddress: "",
    isDefault: false,
  });
  const [isSavingAddress, setIsSavingAddress] = useState(false);
  const [addressErrorMsg, setAddressErrorMsg] = useState("");
  const [addressSuccessMsg, setAddressSuccessMsg] = useState("");

  // Password Form State
  const [oldPassword, setOldPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [showOldPassword, setShowOldPassword] = useState(false);
  const [showNewPassword, setShowNewPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [isChangingPassword, setIsChangingPassword] = useState(false);
  const [passwordSuccessMsg, setPasswordSuccessMsg] = useState("");
  const [passwordErrorMsg, setPasswordErrorMsg] = useState("");

  // Load user profile on mount
  useEffect(() => {
    const fetchProfile = async () => {
      try {
        const profile = await profileService.getMyProfile();
        setFullName(profile.fullName || "");
        setPhone(profile.phone || "");
        setAvatarUrl(profile.avatarUrl || "");
        updateUser({
          fullName: profile.fullName,
          phone: profile.phone,
          avatarUrl: profile.avatarUrl,
        });
      } catch (err) {
        console.error("Lỗi tải hồ sơ:", err);
      }
    };

    fetchProfile();
  }, [updateUser]);

  // Load addresses when switching to addresses tab
  const loadAddresses = async () => {
    setIsLoadingAddresses(true);
    try {
      const data = await profileService.getUserAddresses();
      setAddresses(data);
    } catch (err) {
      console.error("Lỗi lấy danh sách địa chỉ:", err);
    } finally {
      setIsLoadingAddresses(false);
    }
  };

  useEffect(() => {
    if (activeTab === "addresses") {
      loadAddresses();
    }
  }, [activeTab]);

  const handleTabChange = (tab: string) => {
    setActiveTab(tab);
    setSearchParams({ tab });
    setProfileSuccessMsg("");
    setProfileErrorMsg("");
    setPasswordSuccessMsg("");
    setPasswordErrorMsg("");
    setAddressSuccessMsg("");
    setAddressErrorMsg("");
  };

  // Handle Avatar Selection & Upload
  const handleAvatarClick = () => {
    fileInputRef.current?.click();
  };

  const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    if (file.size > 1 * 1024 * 1024) {
      setProfileErrorMsg("Dụng lượng file tối đa 1 MB. Vui lòng chọn ảnh nhỏ hơn!");
      return;
    }

    const allowed = ["image/jpeg", "image/png", "image/webp"];
    if (!allowed.includes(file.type)) {
      setProfileErrorMsg("Định dạng không được hỗ trợ. Vui lòng chọn tệp .JPEG, .PNG hoặc .WEBP!");
      return;
    }

    // Show immediate local preview
    const localPreviewUrl = URL.createObjectURL(file);
    setAvatarUrl(localPreviewUrl);
    setProfileErrorMsg("");
    setProfileSuccessMsg("");
    setIsUploadingAvatar(true);

    try {
      const updatedProfile = await profileService.uploadAvatar(file);
      const newAvatarUrl = updatedProfile.avatarUrl || localPreviewUrl;
      setAvatarUrl(newAvatarUrl);
      setAvatarError(false);
      setSidebarAvatarError(false);
      updateUser({
        avatarUrl: newAvatarUrl,
      });
      setProfileSuccessMsg("Tải lên và cập nhật ảnh đại diện thành công!");
      setTimeout(() => setProfileSuccessMsg(""), 4000);
    } catch (err: any) {
      setProfileErrorMsg(
        err.response?.data?.message || "Không thể tải ảnh lên máy chủ. Vui lòng thử lại!"
      );
    } finally {
      setIsUploadingAvatar(false);
      if (fileInputRef.current) {
        fileInputRef.current.value = "";
      }
    }
  };

  // Handle Remove Avatar to restore Shopee default
  const handleRemoveAvatar = async () => {
    setAvatarUrl("");
    setAvatarError(false);
    setSidebarAvatarError(false);
    updateUser({ avatarUrl: undefined });
    try {
      await profileService.updateProfile({
        fullName,
        phone,
        avatarUrl: "",
      });
      setProfileSuccessMsg("Đã đặt lại ảnh đại diện về biểu tượng mặc định!");
      setTimeout(() => setProfileSuccessMsg(""), 3000);
    } catch {
      // ignore
    }
  };

  // Handle Profile Update
  const handleSaveProfile = async (e: React.FormEvent) => {
    e.preventDefault();
    setProfileSuccessMsg("");
    setProfileErrorMsg("");

    if (!fullName.trim()) {
      setProfileErrorMsg("Họ và tên không được để trống");
      return;
    }

    setIsUpdatingProfile(true);
    try {
      const updated = await profileService.updateProfile({
        fullName: fullName.trim(),
        phone: phone.trim() || undefined,
        avatarUrl: avatarUrl.trim() || undefined,
      });

      updateUser({
        fullName: updated.fullName,
        phone: updated.phone,
        avatarUrl: updated.avatarUrl,
      });

      setProfileSuccessMsg("Cập nhật thông tin hồ sơ thành công!");
      setTimeout(() => setProfileSuccessMsg(""), 4000);
    } catch (err: any) {
      setProfileErrorMsg(
        err.response?.data?.message || "Không thể cập nhật thông tin. Vui lòng kiểm tra lại!"
      );
    } finally {
      setIsUpdatingProfile(false);
    }
  };

  // Handle Address Modal Open/Close
  const openAddAddressModal = () => {
    setEditingAddress(null);
    setAddressForm({
      recipientName: authUser?.fullName || "",
      phone: authUser?.phone || "",
      provinceCity: "",
      district: "",
      ward: "",
      streetAddress: "",
      isDefault: addresses.length === 0,
    });
    setAddressErrorMsg("");
    setIsAddressModalOpen(true);
  };

  const openEditAddressModal = (addr: UserAddress) => {
    setEditingAddress(addr);
    setAddressForm({
      recipientName: addr.recipientName,
      phone: addr.phone,
      provinceCity: addr.provinceCity,
      district: addr.district,
      ward: addr.ward,
      streetAddress: addr.streetAddress,
      isDefault: addr.isDefault,
    });
    setAddressErrorMsg("");
    setIsAddressModalOpen(true);
  };

  const handleSaveAddress = async (e: React.FormEvent) => {
    e.preventDefault();
    setAddressErrorMsg("");

    if (
      !addressForm.recipientName.trim() ||
      !addressForm.phone.trim() ||
      !addressForm.provinceCity.trim() ||
      !addressForm.district.trim() ||
      !addressForm.ward.trim() ||
      !addressForm.streetAddress.trim()
    ) {
      setAddressErrorMsg("Vui lòng điền đầy đủ tất cả các trường thông tin địa chỉ");
      return;
    }

    setIsSavingAddress(true);
    try {
      if (editingAddress) {
        await profileService.updateAddress(editingAddress.id, addressForm);
        setAddressSuccessMsg("Cập nhật địa chỉ nhận hàng thành công!");
      } else {
        await profileService.addAddress(addressForm);
        setAddressSuccessMsg("Thêm mới địa chỉ nhận hàng thành công!");
      }

      setIsAddressModalOpen(false);
      await loadAddresses();
      setTimeout(() => setAddressSuccessMsg(""), 4000);
    } catch (err: any) {
      setAddressErrorMsg(
        err.response?.data?.message || "Không thể lưu địa chỉ. Vui lòng thử lại!"
      );
    } finally {
      setIsSavingAddress(false);
    }
  };

  const handleDeleteAddress = async (id: number) => {
    if (!window.confirm("Bạn có chắc chắn muốn xóa địa chỉ nhận hàng này?")) {
      return;
    }
    try {
      await profileService.deleteAddress(id);
      setAddressSuccessMsg("Đã xóa địa chỉ thành công!");
      await loadAddresses();
      setTimeout(() => setAddressSuccessMsg(""), 4000);
    } catch (err: any) {
      setAddressErrorMsg(
        err.response?.data?.message || "Không thể xóa địa chỉ này. Vui lòng thử lại!"
      );
    }
  };

  const handleSetDefaultAddress = async (id: number) => {
    try {
      await profileService.setDefaultAddress(id);
      setAddressSuccessMsg("Đã thiết lập địa chỉ mặc định mới!");
      await loadAddresses();
      setTimeout(() => setAddressSuccessMsg(""), 4000);
    } catch (err: any) {
      setAddressErrorMsg(
        err.response?.data?.message || "Không thể đặt làm mặc định. Vui lòng thử lại!"
      );
    }
  };

  // Handle Password Change
  const handleChangePassword = async (e: React.FormEvent) => {
    e.preventDefault();
    setPasswordSuccessMsg("");
    setPasswordErrorMsg("");

    if (!oldPassword) {
      setPasswordErrorMsg("Vui lòng nhập mật khẩu hiện tại");
      return;
    }
    if (newPassword.length < 6) {
      setPasswordErrorMsg("Mật khẩu mới phải có tối thiểu 6 ký tự");
      return;
    }
    if (newPassword !== confirmPassword) {
      setPasswordErrorMsg("Xác nhận mật khẩu mới không khớp");
      return;
    }

    setIsChangingPassword(true);
    try {
      const msg = await profileService.changePassword({
        oldPassword,
        newPassword,
        confirmPassword,
      });

      setPasswordSuccessMsg(msg);
      setOldPassword("");
      setNewPassword("");
      setConfirmPassword("");
      setTimeout(() => setPasswordSuccessMsg(""), 5000);
    } catch (err: any) {
      setPasswordErrorMsg(
        err.response?.data?.message || "Đổi mật khẩu thất bại. Vui lòng kiểm tra lại mật khẩu cũ!"
      );
    } finally {
      setIsChangingPassword(false);
    }
  };

  return (
    <div className="min-h-screen flex flex-col bg-slate-50">
      <Header />

      {/* Main Container */}
      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-6">
        {/* Breadcrumbs */}
        <nav className="flex items-center gap-2 text-xs text-slate-500 mb-6">
          <Link to="/" className="hover:text-[#0284C7] transition-colors">
            Trang chủ
          </Link>
          <ChevronRight className="w-3.5 h-3.5" />
          <span className="text-slate-800 font-medium">Tài khoản của tôi</span>
          <ChevronRight className="w-3.5 h-3.5" />
          <span className="text-[#0284C7] font-semibold">
            {activeTab === "profile"
              ? "Hồ sơ cá nhân"
              : activeTab === "addresses"
              ? "Địa chỉ nhận hàng"
              : "Đổi mật khẩu"}
          </span>
        </nav>

        {/* 2-Column Dashboard Layout */}
        <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
          {/* LEFT SIDEBAR */}
          <aside className="lg:col-span-1 space-y-4">
            {/* User Profile Card */}
            <div className="bg-white rounded-2xl p-5 border border-slate-200/80 shadow-sm flex items-center gap-3.5">
              <div className="relative">
                {avatarUrl && !sidebarAvatarError ? (
                  <img
                    src={resolveAvatar(avatarUrl)}
                    alt={fullName || "User Avatar"}
                    onError={() => setSidebarAvatarError(true)}
                    className="w-14 h-14 rounded-full object-cover ring-2 ring-slate-200"
                  />
                ) : (
                  <ShopeeDefaultAvatar className="w-14 h-14" iconSize="w-8 h-8" />
                )}
                <span className="absolute bottom-0 right-0 w-4 h-4 rounded-full bg-emerald-500 border-2 border-white ring-1 ring-emerald-300"></span>
              </div>
              <div className="flex-1 min-w-0">
                <h2 className="font-bold text-slate-800 text-base truncate">
                  {fullName || "Người dùng HeliShop"}
                </h2>
                <div className="flex items-center gap-1 text-xs text-slate-500 mt-0.5">
                  <ShieldCheck className="w-3.5 h-3.5 text-sky-600 shrink-0" />
                  <span className="truncate">Thành viên HeliShop</span>
                </div>
              </div>
            </div>

            {/* Navigation Tabs */}
            <div className="bg-white rounded-2xl border border-slate-200/80 shadow-sm p-2 space-y-1">
              <button
                onClick={() => handleTabChange("profile")}
                className={`w-full flex items-center gap-3 px-4 py-3 rounded-xl text-sm font-medium transition-all ${
                  activeTab === "profile"
                    ? "bg-sky-50 text-[#0284C7] font-semibold shadow-xs"
                    : "text-slate-600 hover:bg-slate-50 hover:text-slate-900"
                }`}
              >
                <UserIcon className="w-4 h-4 text-sky-600 shrink-0" />
                <span>Hồ sơ của tôi</span>
              </button>

              <button
                onClick={() => handleTabChange("addresses")}
                className={`w-full flex items-center justify-between px-4 py-3 rounded-xl text-sm font-medium transition-all ${
                  activeTab === "addresses"
                    ? "bg-sky-50 text-[#0284C7] font-semibold shadow-xs"
                    : "text-slate-600 hover:bg-slate-50 hover:text-slate-900"
                }`}
              >
                <div className="flex items-center gap-3">
                  <MapPin className="w-4 h-4 text-sky-600 shrink-0" />
                  <span>Địa chỉ nhận hàng</span>
                </div>
                {addresses.length > 0 && (
                  <span className="bg-sky-100 text-[#0369A1] text-xs font-bold px-2 py-0.5 rounded-full">
                    {addresses.length}
                  </span>
                )}
              </button>

              <button
                onClick={() => handleTabChange("password")}
                className={`w-full flex items-center gap-3 px-4 py-3 rounded-xl text-sm font-medium transition-all ${
                  activeTab === "password"
                    ? "bg-sky-50 text-[#0284C7] font-semibold shadow-xs"
                    : "text-slate-600 hover:bg-slate-50 hover:text-slate-900"
                }`}
              >
                <Lock className="w-4 h-4 text-sky-600 shrink-0" />
                <span>Đổi mật khẩu</span>
              </button>

              <Link
                to="/orders"
                className="w-full flex items-center gap-3 px-4 py-3 rounded-xl text-sm font-medium text-slate-600 hover:bg-slate-50 hover:text-slate-900 transition-all"
              >
                <Package className="w-4 h-4 text-sky-600 shrink-0" />
                <span>Đơn mua của tôi</span>
              </Link>

              <div className="pt-2 border-t border-slate-100">
                <button
                  onClick={() => {
                    logout();
                    navigate("/login");
                  }}
                  className="w-full flex items-center gap-3 px-4 py-2.5 rounded-xl text-sm font-medium text-red-600 hover:bg-red-50 transition-all"
                >
                  <LogOut className="w-4 h-4 shrink-0" />
                  <span>Đăng xuất</span>
                </button>
              </div>
            </div>
          </aside>

          {/* RIGHT MAIN CONTENT AREA */}
          <section className="lg:col-span-3">
            {/* TAB 1: HỒ SƠ CỦA TÔI */}
            {activeTab === "profile" && (
              <div className="bg-white rounded-2xl border border-slate-200/80 shadow-sm p-6 sm:p-8">
                <div className="border-b border-slate-100 pb-4 mb-6">
                  <h1 className="text-xl font-bold text-slate-900">Hồ Sơ Của Tôi</h1>
                  <p className="text-xs text-slate-500 mt-1">
                    Quản lý thông tin hồ sơ để bảo mật và tối ưu trải nghiệm mua sắm trên HeliShop
                  </p>
                </div>

                {/* Notifications */}
                {profileSuccessMsg && (
                  <div className="mb-6 p-4 bg-emerald-50 border border-emerald-200 rounded-xl flex items-center gap-2.5 text-emerald-800 text-sm animate-in fade-in">
                    <CheckCircle2 className="w-5 h-5 text-emerald-600 shrink-0" />
                    <span>{profileSuccessMsg}</span>
                  </div>
                )}
                {profileErrorMsg && (
                  <div className="mb-6 p-4 bg-red-50 border border-red-200 rounded-xl flex items-center gap-2.5 text-red-800 text-sm animate-in fade-in">
                    <AlertCircle className="w-5 h-5 text-red-600 shrink-0" />
                    <span>{profileErrorMsg}</span>
                  </div>
                )}

                <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
                  {/* Left: Input Form */}
                  <form onSubmit={handleSaveProfile} className="md:col-span-2 space-y-5">
                    {/* Tên đăng nhập / Email */}
                    <div>
                      <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1.5">
                        Địa chỉ Email / Tên đăng nhập
                      </label>
                      <div className="relative">
                        <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-400">
                          <Mail className="w-4 h-4" />
                        </div>
                        <input
                          type="text"
                          value={authUser?.email || ""}
                          disabled
                          className="w-full pl-10 pr-24 py-2.5 bg-slate-100/80 border border-slate-200 rounded-xl text-slate-600 text-sm cursor-not-allowed font-medium"
                        />
                        <div className="absolute inset-y-0 right-0 pr-3 flex items-center">
                          <span className="inline-flex items-center gap-1 text-[11px] font-bold text-emerald-700 bg-emerald-100 px-2 py-0.5 rounded-full">
                            <Check className="w-3 h-3" /> Đã liên kết
                          </span>
                        </div>
                      </div>
                      <p className="text-[11px] text-slate-400 mt-1">
                        Email dùng để đăng nhập và nhận thông báo xác nhận đơn hàng từ HeliShop.
                      </p>
                    </div>

                    {/* Họ và tên */}
                    <div>
                      <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1.5">
                        Họ và tên <span className="text-red-500">*</span>
                      </label>
                      <div className="relative">
                        <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-400">
                          <UserIcon className="w-4 h-4" />
                        </div>
                        <input
                          type="text"
                          value={fullName}
                          onChange={(e) => setFullName(e.target.value)}
                          placeholder="Nhập họ và tên đầy đủ của bạn..."
                          className="w-full pl-10 pr-4 py-2.5 bg-white border border-slate-200 rounded-xl text-slate-800 text-sm focus:outline-none focus:ring-2 focus:ring-[#0284C7]/20 focus:border-[#0284C7] transition-all"
                        />
                      </div>
                    </div>

                    {/* Số điện thoại */}
                    <div>
                      <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1.5">
                        Số điện thoại (Dùng để đăng nhập & nhận hàng)
                      </label>
                      <div className="relative">
                        <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-400">
                          <Phone className="w-4 h-4" />
                        </div>
                        <input
                          type="tel"
                          value={phone}
                          onChange={(e) => setPhone(e.target.value)}
                          placeholder="Ví dụ: 0988889999"
                          className="w-full pl-10 pr-4 py-2.5 bg-white border border-slate-200 rounded-xl text-slate-800 text-sm focus:outline-none focus:ring-2 focus:ring-[#0284C7]/20 focus:border-[#0284C7] transition-all"
                        />
                      </div>
                      <p className="text-[11px] text-slate-400 mt-1">
                        Số điện thoại có thể thay đổi bất kỳ lúc nào. Sau khi đổi, bạn có thể dùng số mới này để đăng nhập và nhận hàng từ shipper.
                      </p>
                    </div>



                    {/* Submit Button */}
                    <div className="pt-3">
                      <button
                        type="submit"
                        disabled={isUpdatingProfile}
                        className="inline-flex items-center gap-2 bg-[#0284C7] hover:bg-[#0369A1] text-white font-semibold px-8 py-2.5 rounded-xl shadow-sm transition-colors disabled:opacity-50 text-sm"
                      >
                        {isUpdatingProfile ? (
                          <>
                            <Loader2 className="w-4 h-4 animate-spin" />
                            <span>Đang lưu...</span>
                          </>
                        ) : (
                          <span>Lưu Thay Đổi</span>
                        )}
                      </button>
                    </div>
                  </form>

                  {/* Right: Avatar File Picker & Live Preview */}
                  <div className="flex flex-col items-center justify-start border-t md:border-t-0 md:border-l border-slate-100 pt-6 md:pt-0 md:pl-8">
                    {/* Hidden Native File Input */}
                    <input
                      type="file"
                      ref={fileInputRef}
                      onChange={handleFileChange}
                      accept="image/jpeg,image/png,image/webp"
                      className="hidden"
                    />

                    {/* Avatar Circle - Pure image or exact Shopee neutral placeholder */}
                    <div
                      onClick={handleAvatarClick}
                      className="relative cursor-pointer"
                      title="Bấm để chọn ảnh từ máy tính"
                    >
                      {avatarUrl && !avatarError ? (
                        <img
                          src={resolveAvatar(avatarUrl)}
                          alt={fullName || "User Avatar"}
                          onError={() => setAvatarError(true)}
                          className="w-28 h-28 sm:w-32 sm:h-32 rounded-full object-cover shadow-xs"
                        />
                      ) : (
                        <ShopeeDefaultAvatar />
                      )}

                      {/* Uploading Spinner Overlay */}
                      {isUploadingAvatar && (
                        <div className="absolute inset-0 bg-slate-900/60 rounded-full flex flex-col items-center justify-center text-white backdrop-blur-xs">
                          <Loader2 className="w-8 h-8 animate-spin text-sky-400 mb-1" />
                          <span className="text-[10px] font-bold">Đang tải...</span>
                        </div>
                      )}
                    </div>

                    {/* Choose Image Button - Clean square-ish white button */}
                    <button
                      type="button"
                      onClick={handleAvatarClick}
                      disabled={isUploadingAvatar}
                      className="mt-4 px-5 py-2 bg-white border border-[#e0e0e0] text-[#555555] text-sm hover:bg-slate-50 transition-colors shadow-2xs cursor-pointer rounded-[2px]"
                    >
                      Chọn Ảnh
                    </button>

                    {/* Remove Avatar Button if user has custom avatar */}
                    {avatarUrl && !avatarError && (
                      <button
                        type="button"
                        onClick={handleRemoveAvatar}
                        className="mt-2 text-xs text-slate-500 hover:text-red-500 hover:underline cursor-pointer"
                      >
                        Xóa ảnh đại diện
                      </button>
                    )}

                    {/* Exact format notice from Shopee */}
                    <div className="text-[13px] text-[#888888] text-center mt-3.5 space-y-1 leading-snug">
                      <p>Dụng lượng file tối đa 1 MB</p>
                      <p>Định dạng:.JPEG, .PNG</p>
                    </div>
                  </div>
                </div>
              </div>
            )}

            {/* TAB 2: ĐỊA CHỈ NHẬN HÀNG (SỔ ĐỊA CHỈ) */}
            {activeTab === "addresses" && (
              <div className="bg-white rounded-2xl border border-slate-200/80 shadow-sm p-6 sm:p-8">
                <div className="flex flex-col sm:flex-row sm:items-center justify-between border-b border-slate-100 pb-4 mb-6 gap-4">
                  <div>
                    <h1 className="text-xl font-bold text-slate-900">Địa Chỉ Của Tôi</h1>
                    <p className="text-xs text-slate-500 mt-1">
                      Địa chỉ nhận hàng sẽ được tự động điền khi bạn tiến hành thanh toán đơn hàng
                    </p>
                  </div>
                  <button
                    onClick={openAddAddressModal}
                    className="inline-flex items-center gap-1.5 bg-[#0284C7] hover:bg-[#0369A1] text-white px-4 py-2.5 rounded-xl font-semibold text-sm shadow-sm transition-colors shrink-0"
                  >
                    <Plus className="w-4 h-4" />
                    <span>Thêm Địa Chỉ Mới</span>
                  </button>
                </div>

                {/* Notifications */}
                {addressSuccessMsg && (
                  <div className="mb-6 p-4 bg-emerald-50 border border-emerald-200 rounded-xl flex items-center gap-2.5 text-emerald-800 text-sm animate-in fade-in">
                    <CheckCircle2 className="w-5 h-5 text-emerald-600 shrink-0" />
                    <span>{addressSuccessMsg}</span>
                  </div>
                )}
                {addressErrorMsg && (
                  <div className="mb-6 p-4 bg-red-50 border border-red-200 rounded-xl flex items-center gap-2.5 text-red-800 text-sm animate-in fade-in">
                    <AlertCircle className="w-5 h-5 text-red-600 shrink-0" />
                    <span>{addressErrorMsg}</span>
                  </div>
                )}

                {/* Address List */}
                {isLoadingAddresses ? (
                  <div className="py-16 text-center text-slate-400 flex flex-col items-center justify-center">
                    <Loader2 className="w-8 h-8 animate-spin text-sky-600 mb-2" />
                    <p className="text-sm">Đang tải danh sách địa chỉ...</p>
                  </div>
                ) : addresses.length === 0 ? (
                  <div className="py-16 text-center text-slate-500 bg-slate-50/60 rounded-2xl border border-dashed border-slate-200">
                    <MapPin className="w-12 h-12 text-slate-300 mx-auto mb-3" />
                    <p className="font-semibold text-slate-700">Bạn chưa có địa chỉ nhận hàng nào</p>
                    <p className="text-xs text-slate-400 mt-1 mb-4">
                      Hãy thêm địa chỉ giao hàng để tiến hành đặt hàng nhanh chóng hơn!
                    </p>
                    <button
                      onClick={openAddAddressModal}
                      className="inline-flex items-center gap-1.5 bg-[#0284C7] text-white px-5 py-2 rounded-xl text-sm font-semibold hover:bg-[#0369A1] transition-colors"
                    >
                      <Plus className="w-4 h-4" />
                      <span>Thêm địa chỉ ngay</span>
                    </button>
                  </div>
                ) : (
                  <div className="space-y-4">
                    {addresses.map((addr) => (
                      <div
                        key={addr.id}
                        className={`p-5 rounded-2xl border transition-all ${
                          addr.isDefault
                            ? "border-sky-300 bg-sky-50/30 shadow-xs"
                            : "border-slate-200 bg-white hover:border-slate-300"
                        }`}
                      >
                        <div className="flex flex-col sm:flex-row sm:items-start justify-between gap-4">
                          <div className="space-y-1.5 flex-1">
                            <div className="flex items-center gap-3 flex-wrap">
                              <span className="font-bold text-slate-900 text-base">
                                {addr.recipientName}
                              </span>
                              <span className="text-slate-300">|</span>
                              <span className="text-slate-600 text-sm font-medium">
                                {addr.phone}
                              </span>
                              {addr.isDefault && (
                                <span className="inline-flex items-center gap-1 bg-emerald-100 text-emerald-800 text-[11px] font-bold px-2 py-0.5 rounded-md border border-emerald-200">
                                  <Check className="w-3 h-3" /> Mặc định
                                </span>
                              )}
                            </div>
                            <p className="text-sm text-slate-700 font-medium">
                              {addr.streetAddress}
                            </p>
                            <p className="text-xs text-slate-500">
                              {addr.ward}, {addr.district}, {addr.provinceCity}
                            </p>
                          </div>

                          {/* Action Buttons */}
                          <div className="flex sm:flex-col items-end gap-2 shrink-0">
                            <div className="flex items-center gap-3 text-sm">
                              <button
                                onClick={() => openEditAddressModal(addr)}
                                className="text-[#0284C7] hover:text-[#0369A1] font-semibold flex items-center gap-1 transition-colors"
                              >
                                <Edit3 className="w-3.5 h-3.5" />
                                <span>Cập nhật</span>
                              </button>
                              <span className="text-slate-200">|</span>
                              <button
                                onClick={() => handleDeleteAddress(addr.id)}
                                className="text-red-500 hover:text-red-700 font-medium flex items-center gap-1 transition-colors"
                              >
                                <Trash2 className="w-3.5 h-3.5" />
                                <span>Xóa</span>
                              </button>
                            </div>
                            {!addr.isDefault && (
                              <button
                                onClick={() => handleSetDefaultAddress(addr.id)}
                                className="mt-1 text-xs font-semibold px-3 py-1.5 rounded-lg border border-slate-200 text-slate-700 hover:bg-slate-100 transition-colors"
                              >
                                Thiết lập mặc định
                              </button>
                            )}
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            )}

            {/* TAB 3: ĐỔI MẬT KHẨU */}
            {activeTab === "password" && (
              <div className="bg-white rounded-2xl border border-slate-200/80 shadow-sm p-6 sm:p-8 max-w-2xl">
                <div className="border-b border-slate-100 pb-4 mb-6">
                  <h1 className="text-xl font-bold text-slate-900">Đổi Mật Khẩu</h1>
                  <p className="text-xs text-slate-500 mt-1">
                    Để bảo vệ tài khoản, vui lòng không chia sẻ mật khẩu của bạn cho bất kỳ ai
                  </p>
                </div>

                {/* Notifications */}
                {passwordSuccessMsg && (
                  <div className="mb-6 p-4 bg-emerald-50 border border-emerald-200 rounded-xl flex items-center gap-2.5 text-emerald-800 text-sm animate-in fade-in">
                    <CheckCircle2 className="w-5 h-5 text-emerald-600 shrink-0" />
                    <span>{passwordSuccessMsg}</span>
                  </div>
                )}
                {passwordErrorMsg && (
                  <div className="mb-6 p-4 bg-red-50 border border-red-200 rounded-xl flex items-center gap-2.5 text-red-800 text-sm animate-in fade-in">
                    <AlertCircle className="w-5 h-5 text-red-600 shrink-0" />
                    <span>{passwordErrorMsg}</span>
                  </div>
                )}

                <form onSubmit={handleChangePassword} className="space-y-5">
                  {/* Mật khẩu hiện tại */}
                  <div>
                    <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1.5">
                      Mật khẩu hiện tại <span className="text-red-500">*</span>
                    </label>
                    <div className="relative">
                      <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-400">
                        <Lock className="w-4 h-4" />
                      </div>
                      <input
                        type={showOldPassword ? "text" : "password"}
                        value={oldPassword}
                        onChange={(e) => setOldPassword(e.target.value)}
                        placeholder="Nhập mật khẩu bạn đang sử dụng..."
                        className="w-full pl-10 pr-10 py-2.5 bg-white border border-slate-200 rounded-xl text-slate-800 text-sm focus:outline-none focus:ring-2 focus:ring-[#0284C7]/20 focus:border-[#0284C7] transition-all"
                      />
                      <button
                        type="button"
                        onClick={() => setShowOldPassword(!showOldPassword)}
                        className="absolute inset-y-0 right-0 pr-3.5 flex items-center text-slate-400 hover:text-slate-600"
                      >
                        {showOldPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                      </button>
                    </div>
                  </div>

                  {/* Mật khẩu mới */}
                  <div>
                    <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1.5">
                      Mật khẩu mới <span className="text-red-500">*</span>
                    </label>
                    <div className="relative">
                      <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-400">
                        <Lock className="w-4 h-4" />
                      </div>
                      <input
                        type={showNewPassword ? "text" : "password"}
                        value={newPassword}
                        onChange={(e) => setNewPassword(e.target.value)}
                        placeholder="Tối thiểu 6 ký tự..."
                        className="w-full pl-10 pr-10 py-2.5 bg-white border border-slate-200 rounded-xl text-slate-800 text-sm focus:outline-none focus:ring-2 focus:ring-[#0284C7]/20 focus:border-[#0284C7] transition-all"
                      />
                      <button
                        type="button"
                        onClick={() => setShowNewPassword(!showNewPassword)}
                        className="absolute inset-y-0 right-0 pr-3.5 flex items-center text-slate-400 hover:text-slate-600"
                      >
                        {showNewPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                      </button>
                    </div>
                  </div>

                  {/* Xác nhận mật khẩu mới */}
                  <div>
                    <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1.5">
                      Xác nhận mật khẩu mới <span className="text-red-500">*</span>
                    </label>
                    <div className="relative">
                      <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-400">
                        <Lock className="w-4 h-4" />
                      </div>
                      <input
                        type={showConfirmPassword ? "text" : "password"}
                        value={confirmPassword}
                        onChange={(e) => setConfirmPassword(e.target.value)}
                        placeholder="Nhập lại mật khẩu mới..."
                        className="w-full pl-10 pr-10 py-2.5 bg-white border border-slate-200 rounded-xl text-slate-800 text-sm focus:outline-none focus:ring-2 focus:ring-[#0284C7]/20 focus:border-[#0284C7] transition-all"
                      />
                      <button
                        type="button"
                        onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                        className="absolute inset-y-0 right-0 pr-3.5 flex items-center text-slate-400 hover:text-slate-600"
                      >
                        {showConfirmPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                      </button>
                    </div>
                  </div>

                  {/* Submit Button */}
                  <div className="pt-3">
                    <button
                      type="submit"
                      disabled={isChangingPassword}
                      className="inline-flex items-center gap-2 bg-[#0284C7] hover:bg-[#0369A1] text-white font-semibold px-8 py-2.5 rounded-xl shadow-sm transition-colors disabled:opacity-50 text-sm"
                    >
                      {isChangingPassword ? (
                        <>
                          <Loader2 className="w-4 h-4 animate-spin" />
                          <span>Đang xác nhận...</span>
                        </>
                      ) : (
                        <span>Xác Nhận Đổi Mật Khẩu</span>
                      )}
                    </button>
                  </div>
                </form>
              </div>
            )}
          </section>
        </div>
      </main>

      {/* MODAL THÊM / CẬP NHẬT ĐỊA CHỈ */}
      {isAddressModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-xs animate-in fade-in">
          <div className="bg-white rounded-3xl shadow-2xl max-w-lg w-full overflow-hidden border border-slate-100 animate-in zoom-in-95">
            {/* Modal Header */}
            <div className="px-6 py-4 border-b border-slate-100 flex items-center justify-between">
              <h3 className="font-bold text-slate-900 text-base flex items-center gap-2">
                <MapPin className="w-5 h-5 text-sky-600" />
                <span>{editingAddress ? "Cập Nhật Địa Chỉ" : "Địa Chỉ Nhận Hàng Mới"}</span>
              </h3>
              <button
                onClick={() => setIsAddressModalOpen(false)}
                className="w-8 h-8 rounded-full hover:bg-slate-100 flex items-center justify-center text-slate-400 hover:text-slate-600 transition-colors"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Modal Body */}
            <form onSubmit={handleSaveAddress} className="p-6 space-y-4">
              {addressErrorMsg && (
                <div className="p-3 bg-red-50 border border-red-200 rounded-xl text-xs text-red-700 flex items-center gap-2">
                  <AlertCircle className="w-4 h-4 shrink-0" />
                  <span>{addressErrorMsg}</span>
                </div>
              )}

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">
                    Họ và tên người nhận <span className="text-red-500">*</span>
                  </label>
                  <input
                    type="text"
                    required
                    value={addressForm.recipientName}
                    onChange={(e) =>
                      setAddressForm({ ...addressForm, recipientName: e.target.value })
                    }
                    placeholder="Ví dụ: Nguyễn Văn A"
                    className="w-full px-3.5 py-2 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-[#0284C7]/20 focus:border-[#0284C7]"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">
                    Số điện thoại <span className="text-red-500">*</span>
                  </label>
                  <input
                    type="tel"
                    required
                    value={addressForm.phone}
                    onChange={(e) =>
                      setAddressForm({ ...addressForm, phone: e.target.value })
                    }
                    placeholder="Ví dụ: 0988889999"
                    className="w-full px-3.5 py-2 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-[#0284C7]/20 focus:border-[#0284C7]"
                  />
                </div>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">
                    Tỉnh / Thành phố <span className="text-red-500">*</span>
                  </label>
                  <input
                    type="text"
                    required
                    value={addressForm.provinceCity}
                    onChange={(e) =>
                      setAddressForm({ ...addressForm, provinceCity: e.target.value })
                    }
                    placeholder="Ví dụ: Hà Nội"
                    className="w-full px-3.5 py-2 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-[#0284C7]/20 focus:border-[#0284C7]"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">
                    Quận / Huyện <span className="text-red-500">*</span>
                  </label>
                  <input
                    type="text"
                    required
                    value={addressForm.district}
                    onChange={(e) =>
                      setAddressForm({ ...addressForm, district: e.target.value })
                    }
                    placeholder="Ví dụ: Cầu Giấy"
                    className="w-full px-3.5 py-2 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-[#0284C7]/20 focus:border-[#0284C7]"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  Phường / Xã <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  required
                  value={addressForm.ward}
                  onChange={(e) =>
                    setAddressForm({ ...addressForm, ward: e.target.value })
                  }
                  placeholder="Ví dụ: Phường Quan Hoa"
                  className="w-full px-3.5 py-2 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-[#0284C7]/20 focus:border-[#0284C7]"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  Địa chỉ chi tiết (Số nhà, tên toà nhà, ngõ...) <span className="text-red-500">*</span>
                </label>
                <textarea
                  rows={2}
                  required
                  value={addressForm.streetAddress}
                  onChange={(e) =>
                    setAddressForm({ ...addressForm, streetAddress: e.target.value })
                  }
                  placeholder="Ví dụ: Số 25 ngách 68/12 đường Cầu Giấy"
                  className="w-full px-3.5 py-2 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-[#0284C7]/20 focus:border-[#0284C7] resize-none"
                />
              </div>

              <div className="flex items-center gap-2 pt-2">
                <input
                  type="checkbox"
                  id="modalIsDefault"
                  checked={addressForm.isDefault}
                  onChange={(e) =>
                    setAddressForm({ ...addressForm, isDefault: e.target.checked })
                  }
                  className="w-4 h-4 text-sky-600 rounded border-slate-300 focus:ring-sky-500"
                />
                <label htmlFor="modalIsDefault" className="text-xs font-medium text-slate-700 cursor-pointer">
                  Đặt làm địa chỉ giao hàng mặc định
                </label>
              </div>

              {/* Modal Footer Buttons */}
              <div className="flex items-center justify-end gap-3 pt-4 border-t border-slate-100">
                <button
                  type="button"
                  onClick={() => setIsAddressModalOpen(false)}
                  className="px-5 py-2 rounded-xl text-sm font-semibold text-slate-600 hover:bg-slate-100 transition-colors"
                >
                  Trở Lại
                </button>
                <button
                  type="submit"
                  disabled={isSavingAddress}
                  className="inline-flex items-center gap-2 bg-[#0284C7] hover:bg-[#0369A1] text-white px-6 py-2 rounded-xl text-sm font-semibold shadow-sm transition-colors disabled:opacity-50"
                >
                  {isSavingAddress ? (
                    <>
                      <Loader2 className="w-4 h-4 animate-spin" />
                      <span>Đang lưu...</span>
                    </>
                  ) : (
                    <span>Hoàn Thành</span>
                  )}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      <Footer />
    </div>
  );
};

export default ProfilePage;
