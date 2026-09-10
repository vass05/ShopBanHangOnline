import React from "react";
import { useSearchParams, Link } from "react-router-dom";
import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/layout/Footer";
import { formatVND } from "@/lib/formatters";
import {
  CheckCircle,
  XCircle,
  ShoppingBag,
  Package,
  ArrowRight,
  ShieldCheck,
  Calendar,
  CreditCard,
} from "lucide-react";

export const PaymentResultPage: React.FC = () => {
  const [searchParams] = useSearchParams();

  const responseCode = searchParams.get("vnp_ResponseCode");
  const statusParam = searchParams.get("status");
  const orderCode = searchParams.get("orderCode") || "ORD-20260910-8F3A";
  const amountParam = searchParams.get("amount") || searchParams.get("vnp_Amount") || "34970000";
  const transactionNo = searchParams.get("vnp_TransactionNo") || "14592810";
  const method = searchParams.get("method") || "VNPAY";

  // VNPAY responseCode "00" is success
  const isSuccess = statusParam === "success" || responseCode === "00";
  const amount = Number(amountParam);

  return (
    <div className="min-h-screen bg-[#F5F5FA] flex flex-col">
      <Header />

      <main className="flex-1 max-w-3xl w-full mx-auto px-4 sm:px-6 py-10">
        <div className="bg-white rounded-3xl p-8 sm:p-10 shadow-sm border border-slate-200/80 text-center space-y-6">
          {/* Status Icon */}
          <div className="flex justify-center">
            {isSuccess ? (
              <div className="w-20 h-20 rounded-full bg-emerald-50 text-emerald-600 flex items-center justify-center border-4 border-emerald-100 shadow-lg animate-bounce-gentle">
                <CheckCircle className="w-10 h-10 stroke-[2.5]" />
              </div>
            ) : (
              <div className="w-20 h-20 rounded-full bg-red-50 text-red-600 flex items-center justify-center border-4 border-red-100 shadow-lg">
                <XCircle className="w-10 h-10 stroke-[2.5]" />
              </div>
            )}
          </div>

          {/* Heading */}
          <div className="space-y-2">
            <h1 className="text-2xl sm:text-3xl font-black text-slate-900">
              {isSuccess
                ? method === "VNPAY"
                  ? "Thanh Toán Thành Công!"
                  : "Đặt Hàng Thành Công!"
                : "Giao Dịch Không Thành Công"}
            </h1>
            <p className="text-xs sm:text-sm text-slate-500 max-w-md mx-auto leading-relaxed">
              {isSuccess
                ? "Cảm ơn bạn đã tin tưởng mua sắm tại HeliShop. Đơn hàng của bạn đã được xác nhận và người bán đang đóng gói sản phẩm."
                : "Giao dịch thanh toán chưa hoàn tất do khách hàng hủy hoặc lỗi xác thực từ ngân hàng. Bạn có thể thử lại bất cứ lúc nào."}
            </p>
          </div>

          {/* Transaction Summary Card */}
          <div className="bg-slate-50 rounded-2xl p-5 border border-slate-200/80 text-left text-xs space-y-3 max-w-lg mx-auto">
            <div className="flex justify-between pb-2 border-b border-slate-200/60">
              <span className="text-slate-500">Mã đơn hàng:</span>
              <strong className="font-mono text-slate-900 text-sm">{orderCode}</strong>
            </div>

            <div className="flex justify-between pb-2 border-b border-slate-200/60">
              <span className="text-slate-500">Số tiền thanh toán:</span>
              <strong className="text-base font-black text-[#0284C7]">{formatVND(amount)}</strong>
            </div>

            <div className="flex justify-between pb-2 border-b border-slate-200/60">
              <span className="text-slate-500">Phương thức:</span>
              <span className="font-bold text-slate-800">
                {method === "VNPAY" ? "Cổng thanh toán VNPAY Sandbox" : "Thanh toán khi nhận hàng (COD)"}
              </span>
            </div>

            {method === "VNPAY" && (
              <div className="flex justify-between pb-2 border-b border-slate-200/60">
                <span className="text-slate-500">Mã giao dịch VNPAY:</span>
                <span className="font-mono font-bold text-slate-800">{transactionNo}</span>
              </div>
            )}

            <div className="flex justify-between">
              <span className="text-slate-500">Thời gian giao dịch:</span>
              <span className="font-medium text-slate-800">{new Date().toLocaleString("vi-VN")}</span>
            </div>
          </div>

          {/* Security Badge */}
          <div className="inline-flex items-center gap-2 text-xs text-slate-500 bg-sky-50 px-3 py-1.5 rounded-full border border-sky-100">
            <ShieldCheck className="w-4 h-4 text-[#0284C7]" />
            <span>Giao dịch được bảo vệ và mã hóa bởi HeliShop Core</span>
          </div>

          {/* Action Buttons */}
          <div className="flex flex-col sm:flex-row items-center justify-center gap-3 pt-2">
            <Link
              to="/orders"
              className="w-full sm:w-auto px-6 py-3 bg-[#0284C7] hover:bg-[#0369A1] text-white font-bold rounded-xl text-xs flex items-center justify-center gap-2 shadow-md shadow-sky-600/25 transition-all"
            >
              <Package className="w-4 h-4" />
              <span>Xem Đơn Hàng Của Tôi</span>
            </Link>

            <Link
              to="/"
              className="w-full sm:w-auto px-6 py-3 bg-white border border-slate-200 hover:bg-slate-50 text-slate-700 font-bold rounded-xl text-xs flex items-center justify-center gap-2 transition-colors shadow-sm"
            >
              <ShoppingBag className="w-4 h-4" />
              <span>Tiếp Tục Mua Sắm</span>
            </Link>
          </div>
        </div>
      </main>

      <Footer />
    </div>
  );
};
