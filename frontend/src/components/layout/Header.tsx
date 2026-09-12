import React, { useState, useRef, useEffect } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuthStore } from "@/store/useAuthStore";
import { useCartStore } from "@/store/useCartStore";
import { formatVND } from "@/lib/formatters";
import { HeliShopLogo } from "./HeliShopLogo";
import {
  ShoppingBag,
  Search,
  ShoppingCart,
  User as UserIcon,
  Bell,
  HelpCircle,
  Globe,
  LogOut,
  Package,
  Sliders,
  ChevronDown,
  Sparkles,
} from "lucide-react";

export const Header: React.FC = () => {
  const navigate = useNavigate();
  const { user, isAuthenticated, logout } = useAuthStore();
  const { items, getTotalBadgeCount } = useCartStore();

  const [searchQuery, setSearchQuery] = useState("");
  const [showAutocomplete, setShowAutocomplete] = useState(false);
  const [showUserMenu, setShowUserMenu] = useState(false);
  const [showCartPreview, setShowCartPreview] = useState(false);
  const [avatarError, setAvatarError] = useState(false);

  useEffect(() => {
    setAvatarError(false);
  }, [user?.avatarUrl]);

  const searchRef = useRef<HTMLDivElement>(null);
  const userMenuRef = useRef<HTMLDivElement>(null);
  const cartPreviewRef = useRef<HTMLDivElement>(null);

  const cartCount = getTotalBadgeCount();

  const SUGGESTIONS = [
    "iPhone 16 Pro Max 256GB Titan",
    "Tai nghe Sony WH-1000XM5",
    "Bàn phím cơ NuPhy Air75 V2",
    "MacBook Pro 14 M3 Pro",
    "Áo Polo Coolmate ExCool",
    "Chuột Logitech G Pro X Superlight 2",
    "Củ sạc Anker Prime 67W GaN",
    "Màn hình Dell UltraSharp 2K",
  ];

  // Close dropdowns on outside click
  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (searchRef.current && !searchRef.current.contains(e.target as Node)) {
        setShowAutocomplete(false);
      }
      if (userMenuRef.current && !userMenuRef.current.contains(e.target as Node)) {
        setShowUserMenu(false);
      }
      if (cartPreviewRef.current && !cartPreviewRef.current.contains(e.target as Node)) {
        setShowCartPreview(false);
      }
    };
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setShowAutocomplete(false);
    if (searchQuery.trim()) {
      navigate(`/?q=${encodeURIComponent(searchQuery.trim())}`);
    }
  };

  const handleSelectSuggestion = (term: string) => {
    setSearchQuery(term);
    setShowAutocomplete(false);
    navigate(`/?q=${encodeURIComponent(term)}`);
  };

  return (
    <header className="sticky top-0 z-50 shadow-md">
      {/* 1. TOP SUB-HEADER BAR */}
      <div className="bg-[#0369A1] text-sky-100 text-xs py-1.5 px-4 sm:px-8 border-b border-sky-800/40">
        <div className="max-w-7xl mx-auto flex items-center justify-between">
          <div className="flex items-center gap-4">
            <span className="hover:text-white cursor-pointer transition-colors">Kênh Người Bán</span>
            <span className="hidden md:inline hover:text-white cursor-pointer transition-colors">Tải ứng dụng</span>
            <div className="hidden sm:flex items-center gap-1.5">
              <span>Kết nối:</span>
              <span className="w-2 h-2 rounded-full bg-emerald-400"></span>
              <span className="font-semibold text-white">HeliShop Official</span>
            </div>
          </div>

          <div className="flex items-center gap-4">
            <div className="flex items-center gap-1 hover:text-white cursor-pointer transition-colors">
              <Bell className="w-3.5 h-3.5" />
              <span>Thông Báo</span>
            </div>
            <div className="hidden sm:flex items-center gap-1 hover:text-white cursor-pointer transition-colors">
              <HelpCircle className="w-3.5 h-3.5" />
              <span>Hỗ Trợ</span>
            </div>
            <div className="hidden md:flex items-center gap-1 hover:text-white cursor-pointer transition-colors">
              <Globe className="w-3.5 h-3.5" />
              <span>Tiếng Việt</span>
              <ChevronDown className="w-3 h-3" />
            </div>

            {/* Authentication quick link */}
            {isAuthenticated ? (
              <div className="relative" ref={userMenuRef}>
                <button
                  onClick={() => setShowUserMenu(!showUserMenu)}
                  className="flex items-center gap-1.5 hover:text-white font-medium"
                >
                  {user?.avatarUrl && !avatarError ? (
                    <img
                      src={user.avatarUrl.startsWith("/uploads/") ? `http://localhost:8080${user.avatarUrl}` : user.avatarUrl}
                      alt={user.fullName || "Avatar"}
                      onError={() => setAvatarError(true)}
                      className="w-5 h-5 rounded-full object-cover ring-1 ring-white/60"
                    />
                  ) : (
                    <div className="w-5 h-5 rounded-full bg-[#EFEFEF] flex items-center justify-center overflow-hidden shrink-0 ring-1 ring-white/60">
                      <svg viewBox="0 0 15 15" fill="none" className="w-3.5 h-3.5">
                        <circle cx="7.5" cy="4.5" r="2.5" stroke="#C6C6C6" strokeWidth="1.2" />
                        <path d="M1.5 14.2C1.5 10.9 4.2 8.2 7.5 8.2C10.8 8.2 13.5 10.9 13.5 14.2" stroke="#C6C6C6" strokeWidth="1.2" strokeLinecap="round" />
                      </svg>
                    </div>
                  )}
                  <span>{user?.fullName || user?.email}</span>
                  <ChevronDown className="w-3 h-3" />
                </button>

                {showUserMenu && (
                  <div className="absolute right-0 mt-2 w-48 bg-white rounded-xl shadow-xl border border-slate-100 text-slate-700 py-1 z-50 text-sm animate-in fade-in zoom-in-95">
                    <div className="px-3 py-2 border-b border-slate-100">
                      <p className="font-bold text-slate-800 truncate">{user?.fullName}</p>
                      <p className="text-xs text-slate-400 truncate">{user?.email}</p>
                    </div>
                    <Link
                      to="/profile"
                      onClick={() => setShowUserMenu(false)}
                      className="flex items-center gap-2 px-3 py-2 hover:bg-sky-50 hover:text-[#0284C7] transition-colors"
                    >
                      <UserIcon className="w-4 h-4 text-sky-600" />
                      <span>Hồ sơ của tôi</span>
                    </Link>
                    <Link
                      to="/orders"
                      onClick={() => setShowUserMenu(false)}
                      className="flex items-center gap-2 px-3 py-2 hover:bg-sky-50 hover:text-[#0284C7] transition-colors"
                    >
                      <Package className="w-4 h-4 text-sky-600" />
                      <span>Đơn mua của tôi</span>
                    </Link>
                    <button
                      onClick={() => {
                        setShowUserMenu(false);
                        logout();
                        navigate("/login");
                      }}
                      className="flex items-center gap-2 w-full text-left px-3 py-2 hover:bg-red-50 text-red-600 transition-colors border-t border-slate-100"
                    >
                      <LogOut className="w-4 h-4" />
                      <span>Đăng xuất</span>
                    </button>
                  </div>
                )}
              </div>
            ) : (
              <div className="flex items-center gap-2 font-medium">
                <Link to="/login" className="hover:text-white transition-colors">
                  Đăng Nhập
                </Link>
                <span>|</span>
                <Link to="/register" className="hover:text-white transition-colors">
                  Đăng Ký
                </Link>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* 2. MAIN OCEAN BLUE HEADER BAR */}
      <div className="ocean-gradient text-white py-3.5 px-4 sm:px-8">
        <div className="max-w-7xl mx-auto flex items-center justify-between gap-4 lg:gap-8">
          {/* Logo Brand */}
          <HeliShopLogo size="md" variant="on-dark" showBadge badgeText="Mall" />

          {/* Large Central Search with Autocomplete */}
          <div className="flex-1 max-w-2xl relative" ref={searchRef}>
            <form onSubmit={handleSearchSubmit} className="flex items-center bg-white rounded-xl shadow-inner p-1">
              <div className="pl-3 pr-2 text-slate-400">
                <Search className="w-5 h-5" />
              </div>
              <input
                type="text"
                placeholder="HeliShop bao ship 0Đ - Tìm kiếm thương hiệu, sản phẩm công nghệ..."
                value={searchQuery}
                onChange={(e) => {
                  setSearchQuery(e.target.value);
                  setShowAutocomplete(true);
                }}
                onFocus={() => setShowAutocomplete(true)}
                className="w-full bg-transparent text-slate-800 text-sm placeholder:text-slate-400 focus:outline-none py-1.5"
              />
              <button
                type="submit"
                className="bg-[#0284C7] hover:bg-[#0369A1] text-white px-5 py-2 rounded-lg font-semibold text-sm transition-colors shrink-0 flex items-center gap-1 shadow-sm"
              >
                <span>Tìm kiếm</span>
              </button>
            </form>

            {/* Autocomplete Dropdown */}
            {showAutocomplete && (
              <div className="absolute left-0 right-0 top-full mt-1 bg-white rounded-xl shadow-2xl border border-slate-100 text-slate-700 py-2 z-50 overflow-hidden animate-in fade-in">
                <div className="px-3 py-1.5 text-[11px] font-bold text-slate-400 uppercase tracking-wider flex items-center gap-1.5">
                  <Sparkles className="w-3.5 h-3.5 text-sky-500" />
                  Gợi ý tìm kiếm phổ biến
                </div>
                {SUGGESTIONS.map((term, idx) => (
                  <div
                    key={idx}
                    onClick={() => handleSelectSuggestion(term)}
                    className="px-4 py-2 hover:bg-sky-50 hover:text-[#0284C7] cursor-pointer text-sm flex items-center justify-between transition-colors"
                  >
                    <span>{term}</span>
                    <span className="text-xs text-sky-500 font-medium">Tìm</span>
                  </div>
                ))}
              </div>
            )}

            {/* Quick hot search keywords underneath */}
            <div className="hidden sm:flex items-center gap-3 text-xs text-sky-100 mt-1.5 pl-1 overflow-x-auto no-scrollbar whitespace-nowrap">
              <span onClick={() => handleSelectSuggestion("iPhone 16 Pro Max")} className="hover:text-white cursor-pointer">iPhone 16</span>
              <span onClick={() => handleSelectSuggestion("Sony WH-1000XM5")} className="hover:text-white cursor-pointer">Tai nghe Sony</span>
              <span onClick={() => handleSelectSuggestion("NuPhy Air75 V2")} className="hover:text-white cursor-pointer">Bàn phím NuPhy</span>
              <span onClick={() => handleSelectSuggestion("MacBook Pro")} className="hover:text-white cursor-pointer">MacBook Pro</span>
              <span onClick={() => handleSelectSuggestion("Logitech G Pro")} className="hover:text-white cursor-pointer">Chuột Logitech</span>
              <span onClick={() => handleSelectSuggestion("Coolmate ExCool")} className="hover:text-white cursor-pointer">Áo Polo Coolmate</span>
            </div>
          </div>

          {/* Cart Icon & Live Counter Badge */}
          <div className="relative shrink-0" ref={cartPreviewRef}>
            <Link
              to="/cart"
              onMouseEnter={() => setShowCartPreview(true)}
              className="relative p-2.5 bg-white/10 hover:bg-white/20 rounded-xl flex items-center justify-center transition-colors group"
            >
              <ShoppingCart className="w-6 h-6 text-white group-hover:scale-110 transition-transform" />
              {cartCount > 0 && (
                <span className="absolute -top-1 -right-1 bg-[#EF4444] text-white text-xs font-black min-w-5 h-5 px-1.5 rounded-full flex items-center justify-center shadow-md border-2 border-[#0369A1] animate-bounce-gentle">
                  {cartCount > 99 ? "99+" : cartCount}
                </span>
              )}
            </Link>

            {/* Cart Preview Hover Popover */}
            {showCartPreview && items.length > 0 && (
              <div
                onMouseLeave={() => setShowCartPreview(false)}
                className="absolute right-0 top-full mt-2 w-80 bg-white rounded-xl shadow-2xl border border-slate-100 text-slate-800 p-3 z-50 text-xs hidden lg:block animate-in fade-in zoom-in-95"
              >
                <p className="text-slate-400 font-semibold mb-2 uppercase text-[10px]">Sản phẩm mới thêm vào giỏ</p>
                <div className="space-y-2 max-h-60 overflow-y-auto">
                  {items.slice(0, 3).map((item) => (
                    <div key={item.skuId} className="flex items-center gap-2.5 p-1.5 hover:bg-slate-50 rounded-lg">
                      <img src={item.imageUrl} alt={item.productName} className="w-10 h-10 object-cover rounded border border-slate-100" />
                      <div className="flex-1 min-w-0">
                        <p className="font-semibold text-slate-800 truncate">{item.productName}</p>
                        <p className="text-slate-400 text-[11px]">SL: {item.quantity}</p>
                      </div>
                      <span className="text-[#0284C7] font-bold shrink-0">{formatVND(item.price)}</span>
                    </div>
                  ))}
                </div>
                <div className="mt-3 pt-2 border-t border-slate-100 flex items-center justify-between">
                  <span className="text-slate-500 font-medium">Tổng {cartCount} sản phẩm</span>
                  <Link
                    to="/cart"
                    onClick={() => setShowCartPreview(false)}
                    className="bg-[#0284C7] text-white px-3 py-1.5 rounded-lg font-semibold hover:bg-[#0369A1] transition-colors"
                  >
                    Xem Giỏ Hàng
                  </Link>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </header>
  );
};
