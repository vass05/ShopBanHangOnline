import React from "react";
import { ShieldCheck, Truck, RotateCcw, Award, PhoneCall } from "lucide-react";

export const Footer: React.FC = () => {
  return (
    <footer className="bg-white border-t border-slate-200 text-slate-600 text-xs mt-16">
      {/* 1. Value Proposition Banner */}
      <div className="bg-sky-50/70 border-b border-sky-100 py-6 px-4">
        <div className="max-w-7xl mx-auto grid grid-cols-2 md:grid-cols-4 gap-6">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-full bg-sky-100 text-[#0284C7] flex items-center justify-center shrink-0">
              <Truck className="w-5 h-5" />
            </div>
            <div>
              <h4 className="font-bold text-slate-800 text-sm">Giao Hàng Miễn Phí</h4>
              <p className="text-slate-500 text-xs">Đơn hàng từ 0Đ toàn quốc</p>
            </div>
          </div>

          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-full bg-sky-100 text-[#0284C7] flex items-center justify-center shrink-0">
              <ShieldCheck className="w-5 h-5" />
            </div>
            <div>
              <h4 className="font-bold text-slate-800 text-sm">100% Chính Hãng</h4>
              <p className="text-slate-500 text-xs">Đền gấp 2 nếu hàng giả</p>
            </div>
          </div>

          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-full bg-sky-100 text-[#0284C7] flex items-center justify-center shrink-0">
              <RotateCcw className="w-5 h-5" />
            </div>
            <div>
              <h4 className="font-bold text-slate-800 text-sm">7 Ngày Đổi Trả</h4>
              <p className="text-slate-500 text-xs">Miễn phí hoàn trả hàng</p>
            </div>
          </div>

          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-full bg-sky-100 text-[#0284C7] flex items-center justify-center shrink-0">
              <PhoneCall className="w-5 h-5" />
            </div>
            <div>
              <h4 className="font-bold text-slate-800 text-sm">Hỗ Trợ 24/7</h4>
              <p className="text-slate-500 text-xs">Hotline: 1900 8888</p>
            </div>
          </div>
        </div>
      </div>

      {/* 2. Directory Links */}
      <div className="max-w-7xl mx-auto py-10 px-4 sm:px-8 grid grid-cols-2 md:grid-cols-4 gap-8">
        <div>
          <h5 className="font-bold text-slate-800 uppercase tracking-wider mb-3">Chăm Sóc Khách Hàng</h5>
          <ul className="space-y-2 text-slate-500">
            <li className="hover:text-[#0284C7] cursor-pointer">Trung Tâm Trợ Giúp</li>
            <li className="hover:text-[#0284C7] cursor-pointer">HeliShop Blog</li>
            <li className="hover:text-[#0284C7] cursor-pointer">Hướng Dẫn Mua Hàng</li>
            <li className="hover:text-[#0284C7] cursor-pointer">Thanh Toán VNPAY & COD</li>
            <li className="hover:text-[#0284C7] cursor-pointer">Chính Sách Vận Chuyển</li>
            <li className="hover:text-[#0284C7] cursor-pointer">Trả Hàng & Hoàn Tiền</li>
          </ul>
        </div>

        <div>
          <h5 className="font-bold text-slate-800 uppercase tracking-wider mb-3">Về HeliShop Core</h5>
          <ul className="space-y-2 text-slate-500">
            <li className="hover:text-[#0284C7] cursor-pointer">Giới Thiệu HeliShop VN</li>
            <li className="hover:text-[#0284C7] cursor-pointer">Tuyển Dụng Nhân Tài</li>
            <li className="hover:text-[#0284C7] cursor-pointer">Điều Khoản HeliShop</li>
            <li className="hover:text-[#0284C7] cursor-pointer">Chính Sách Bảo Mật</li>
            <li className="hover:text-[#0284C7] cursor-pointer">Chính Hãng HeliMall</li>
            <li className="hover:text-[#0284C7] cursor-pointer">Kênh Người Bán</li>
          </ul>
        </div>

        <div>
          <h5 className="font-bold text-slate-800 uppercase tracking-wider mb-3">Thanh Toán & Vận Chuyển</h5>
          <div className="grid grid-cols-3 gap-2">
            <div className="border border-slate-200 rounded p-1 text-center font-bold text-[#0369A1] bg-sky-50/50">
              VNPAY
            </div>
            <div className="border border-slate-200 rounded p-1 text-center font-bold text-slate-700 bg-slate-50">
              VISA
            </div>
            <div className="border border-slate-200 rounded p-1 text-center font-bold text-slate-700 bg-slate-50">
              MasterCard
            </div>
            <div className="border border-slate-200 rounded p-1 text-center font-bold text-emerald-600 bg-emerald-50">
              COD
            </div>
            <div className="border border-slate-200 rounded p-1 text-center font-bold text-orange-600 bg-orange-50">
              GHN
            </div>
            <div className="border border-slate-200 rounded p-1 text-center font-bold text-red-600 bg-red-50">
              SPX
            </div>
          </div>
        </div>

        <div>
          <h5 className="font-bold text-slate-800 uppercase tracking-wider mb-3">Tải Ứng Dụng HeliShop</h5>
          <p className="text-slate-500 mb-2">Tải ngay ứng dụng để nhận voucher 100.000₫ cho đơn hàng đầu tiên!</p>
          <div className="flex gap-2">
            <div className="w-20 h-20 bg-slate-100 border border-slate-200 rounded-lg flex items-center justify-center font-mono text-[10px] text-slate-400">
              [QR CODE]
            </div>
            <div className="flex flex-col justify-around">
              <button className="px-3 py-1.5 border border-slate-200 rounded bg-slate-50 font-semibold hover:border-[#0284C7] hover:text-[#0284C7]">
                App Store
              </button>
              <button className="px-3 py-1.5 border border-slate-200 rounded bg-slate-50 font-semibold hover:border-[#0284C7] hover:text-[#0284C7]">
                Google Play
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* 3. Bottom Legal Disclaimer */}
      <div className="bg-slate-100 py-6 border-t border-slate-200 text-center text-slate-500 space-y-1">
        <p className="font-semibold text-slate-700">© 2026 - Bản quyền thuộc về HeliShop Core E-Commerce Platform</p>
        <p>Địa chỉ: Tòa nhà Landmark 81, 720A Điện Biên Phủ, Phường 22, Bình Thạnh, TP. Hồ Chí Minh</p>
        <p>Chịu trách nhiệm nội dung: Ban Giám Đốc Công Nghệ HeliShop</p>
      </div>
    </footer>
  );
};
