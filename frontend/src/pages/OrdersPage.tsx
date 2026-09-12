import React, { useState, useMemo } from "react";
import { Link, useNavigate } from "react-router-dom";
import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/layout/Footer";
import { useOrderStore } from "@/store/useOrderStore";
import { useCartStore } from "@/store/useCartStore";
import { formatVND } from "@/lib/formatters";
import { OrderStatus } from "@/types";
import {
  Package,
  Store,
  Truck,
  RotateCcw,
  MessageSquare,
  Clock,
  CheckCircle,
  XCircle,
  AlertCircle,
  ShoppingBag,
  ArrowLeft,
  ChevronRight,
  User as UserIcon,
} from "lucide-react";

export const OrdersPage: React.FC = () => {
  const navigate = useNavigate();
  const { orders, updateOrderStatus } = useOrderStore();
  const { addToCart } = useCartStore();

  const [activeTab, setActiveTab] = useState<string>("ALL");

  const TABS = [
    { id: "ALL", label: "Tất cả" },
    { id: "PENDING", label: "Chờ thanh toán" },
    { id: "PROCESSING", label: "Đang xử lý" },
    { id: "SHIPPING", label: "Đang giao" },
    { id: "DELIVERED", label: "Đã giao" },
    { id: "CANCELLED", label: "Đã hủy" },
  ];

  const filteredOrders = useMemo(() => {
    if (activeTab === "ALL") return orders;
    return orders.filter((o) => o.status === activeTab);
  }, [orders, activeTab]);

  const handleCancelOrder = (orderId: number) => {
    if (window.confirm("Bạn có chắc chắn muốn hủy đơn hàng này không?")) {
      updateOrderStatus(orderId, "CANCELLED");
    }
  };

  const handleGoBack = () => {
    if (window.history.length > 1) {
      navigate(-1);
    } else {
      navigate("/profile");
    }
  };

  const handleRebuy = (order: typeof orders[0]) => {
    order.items.forEach((item) => {
      addToCart(
        {
          skuId: item.skuId,
          productId: 1,
          productName: item.productName,
          skuCode: item.skuCode,
          attributes: item.attributes,
          price: item.unitPrice,
          stockQuantity: 50,
          imageUrl: item.imageUrl || "",
          shopId: order.shopId,
          shopName: order.shopName,
        },
        item.quantity
      );
    });
    navigate("/cart");
  };

  const getStatusBadge = (status: OrderStatus) => {
    switch (status) {
      case "PROCESSING":
        return (
          <span className="flex items-center gap-1 text-[#0284C7] bg-sky-50 px-2.5 py-1 rounded-full text-xs font-bold border border-sky-200">
            <Clock className="w-3.5 h-3.5" />
            <span>ĐANG XỬ LÝ</span>
          </span>
        );
      case "SHIPPING":
        return (
          <span className="flex items-center gap-1 text-indigo-600 bg-indigo-50 px-2.5 py-1 rounded-full text-xs font-bold border border-indigo-200">
            <Truck className="w-3.5 h-3.5" />
            <span>ĐANG VẬN CHUYỂN</span>
          </span>
        );
      case "DELIVERED":
        return (
          <span className="flex items-center gap-1 text-emerald-600 bg-emerald-50 px-2.5 py-1 rounded-full text-xs font-bold border border-emerald-200">
            <CheckCircle className="w-3.5 h-3.5" />
            <span>GIAO THÀNH CÔNG</span>
          </span>
        );
      case "CANCELLED":
        return (
          <span className="flex items-center gap-1 text-red-600 bg-red-50 px-2.5 py-1 rounded-full text-xs font-bold border border-red-200">
            <XCircle className="w-3.5 h-3.5" />
            <span>ĐÃ HỦY ĐƠN</span>
          </span>
        );
      default:
        return (
          <span className="flex items-center gap-1 text-amber-600 bg-amber-50 px-2.5 py-1 rounded-full text-xs font-bold border border-amber-200">
            <AlertCircle className="w-3.5 h-3.5" />
            <span>CHỜ THANH TOÁN</span>
          </span>
        );
    }
  };

  return (
    <div className="min-h-screen bg-[#F5F5FA] flex flex-col">
      <Header />

      <main className="flex-1 max-w-5xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-6 space-y-5">
        {/* Top Navigation Bar with Back Button & Breadcrumbs */}
        <div className="flex flex-wrap items-center justify-between gap-3 text-xs text-slate-500">
          <div className="flex items-center gap-2 flex-wrap">
            <button
              onClick={handleGoBack}
              className="inline-flex items-center gap-1.5 px-3 py-1.5 bg-white hover:bg-slate-100 text-slate-700 font-bold rounded-xl border border-slate-200 transition-all shadow-xs cursor-pointer group"
            >
              <ArrowLeft className="w-4 h-4 group-hover:-translate-x-1 transition-transform text-[#0284C7]" />
              <span>Quay lại</span>
            </button>
            <span className="text-slate-300">|</span>
            <Link to="/" className="hover:text-[#0284C7] transition-colors">
              Trang chủ
            </Link>
            <ChevronRight className="w-3.5 h-3.5 text-slate-400" />
            <Link to="/profile" className="hover:text-[#0284C7] transition-colors">
              Hồ sơ cá nhân
            </Link>
            <ChevronRight className="w-3.5 h-3.5 text-slate-400" />
            <span className="text-[#0284C7] font-semibold">Đơn mua của tôi</span>
          </div>

          <div className="hidden sm:flex items-center gap-3">
            <Link
              to="/profile"
              className="inline-flex items-center gap-1 text-slate-600 hover:text-[#0284C7] transition-colors font-medium"
            >
              <UserIcon className="w-3.5 h-3.5" />
              <span>Về hồ sơ</span>
            </Link>
            <span className="text-slate-300">•</span>
            <Link
              to="/"
              className="inline-flex items-center gap-1 text-[#0284C7] hover:underline font-semibold"
            >
              <ShoppingBag className="w-3.5 h-3.5" />
              <span>Tiếp tục mua sắm</span>
            </Link>
          </div>
        </div>

        {/* Title */}
        <div className="bg-white rounded-2xl p-5 shadow-sm border border-slate-200/80 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <button
              onClick={handleGoBack}
              title="Quay lại"
              className="w-10 h-10 rounded-xl bg-slate-100 hover:bg-sky-50 hover:text-[#0284C7] text-slate-700 flex items-center justify-center transition-all cursor-pointer group shrink-0"
            >
              <ArrowLeft className="w-5 h-5 group-hover:-translate-x-0.5 transition-transform" />
            </button>
            <div className="w-10 h-10 rounded-xl bg-sky-50 text-[#0284C7] flex items-center justify-center shrink-0">
              <Package className="w-5 h-5" />
            </div>
            <div>
              <h1 className="text-lg font-bold text-slate-800">Quản Lý Đơn Mua Của Tôi</h1>
              <p className="text-xs text-slate-400">Theo dõi tiến độ đơn hàng và lịch sử thanh toán</p>
            </div>
          </div>
          <span className="text-xs font-bold text-slate-500">
            Tổng cộng: <strong className="text-[#0284C7]">{orders.length}</strong> đơn hàng
          </span>
        </div>

        {/* Shopee-style Status Filter Tabs */}
        <div className="bg-white rounded-xl shadow-sm border border-slate-200/80 overflow-x-auto no-scrollbar flex items-center">
          {TABS.map((tab) => {
            const isActive = activeTab === tab.id;
            return (
              <button
                key={tab.id}
                onClick={() => setActiveTab(tab.id)}
                className={`px-5 py-3.5 text-xs sm:text-sm font-bold whitespace-nowrap transition-colors border-b-2 flex-1 text-center ${
                  isActive
                    ? "border-[#0284C7] text-[#0284C7] bg-sky-50/40"
                    : "border-transparent text-slate-600 hover:text-slate-900 hover:bg-slate-50"
                }`}
              >
                {tab.label}
              </button>
            );
          })}
        </div>

        {/* Orders List */}
        {filteredOrders.length === 0 ? (
          <div className="bg-white rounded-2xl p-16 text-center shadow-sm border border-slate-200/80 space-y-4">
            <div className="w-16 h-16 rounded-full bg-slate-100 text-slate-400 flex items-center justify-center mx-auto">
              <Package className="w-8 h-8 opacity-40" />
            </div>
            <h3 className="text-base font-bold text-slate-800">Chưa có đơn hàng nào trong mục này</h3>
            <p className="text-xs text-slate-500 max-w-sm mx-auto">
              Khi bạn mua sản phẩm từ các Shop, trạng thái đơn hàng sẽ được cập nhật liên tục tại đây.
            </p>
            <div className="flex items-center justify-center gap-3 pt-2">
              <button
                onClick={handleGoBack}
                className="inline-flex items-center gap-1.5 px-4 py-2 bg-slate-100 hover:bg-slate-200 text-slate-700 font-bold rounded-xl text-xs transition-colors cursor-pointer"
              >
                <ArrowLeft className="w-3.5 h-3.5" />
                <span>Quay lại</span>
              </button>
              <Link
                to="/"
                className="inline-flex items-center gap-2 px-5 py-2 bg-[#0284C7] text-white font-bold rounded-xl text-xs hover:bg-[#0369A1] transition-colors"
              >
                <ShoppingBag className="w-4 h-4" />
                <span>Khám phá sản phẩm ngay</span>
              </Link>
            </div>
          </div>
        ) : (
          <div className="space-y-4">
            {filteredOrders.map((order) => (
              <div
                key={order.id}
                className="bg-white rounded-2xl shadow-sm border border-slate-200/80 overflow-hidden"
              >
                {/* Order Shop Header */}
                <div className="p-4 bg-slate-50/70 border-b border-slate-100 flex flex-wrap items-center justify-between gap-3">
                  <div className="flex items-center gap-2 font-bold text-xs sm:text-sm text-slate-800">
                    <Store className="w-4 h-4 text-[#0284C7]" />
                    <span>{order.shopName}</span>
                    <button className="text-[11px] text-[#0284C7] border border-sky-200 bg-sky-50 px-2 py-0.5 rounded font-semibold ml-2">
                      Chat Shop
                    </button>
                  </div>

                  <div className="flex items-center gap-3">
                    <span className="text-xs font-mono text-slate-400">
                      Mã đơn: #{order.orderCode}
                    </span>
                    {getStatusBadge(order.status)}
                  </div>
                </div>

                {/* Items in Order */}
                <div className="p-4 divide-y divide-slate-100">
                  {order.items.map((item) => (
                    <div key={item.id} className="py-3 first:pt-0 last:pb-0 flex items-center justify-between gap-4 text-xs">
                      <div className="flex items-center gap-3 flex-1 min-w-0">
                        <img
                          src={item.imageUrl || "https://images.unsplash.com/photo-1695048133142-1a20484d2569?w=200&auto=format&fit=crop&q=80"}
                          alt={item.productName}
                          className="w-16 h-16 rounded-lg object-cover border border-slate-200 shrink-0"
                        />
                        <div className="space-y-1 min-w-0 flex-1">
                          <p className="font-bold text-slate-800 truncate">{item.productName}</p>
                          {item.attributes && (
                            <p className="text-slate-400 text-[11px]">
                              Phân loại: {Object.values(item.attributes).join(", ")}
                            </p>
                          )}
                          <p className="text-slate-400 text-[11px]">Số lượng: x{item.quantity}</p>
                        </div>
                      </div>

                      <div className="text-right shrink-0">
                        <span className="text-sm font-extrabold text-[#0284C7]">
                          {formatVND(item.totalPrice)}
                        </span>
                      </div>
                    </div>
                  ))}
                </div>

                {/* Order Footer & Actions */}
                <div className="p-4 bg-slate-50/50 border-t border-slate-100 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
                  <div className="text-xs text-slate-500 space-y-0.5">
                    <p>
                      Phương thức:{" "}
                      <strong className="text-slate-700">
                        {order.paymentMethod === "VNPAY" ? "Thanh toán VNPAY" : "Tiền mặt khi nhận hàng (COD)"}
                      </strong>
                    </p>
                    <p>Thời gian đặt: {order.createdAt}</p>
                  </div>

                  <div className="flex items-center justify-between w-full sm:w-auto gap-4">
                    <div className="text-right">
                      <span className="text-xs text-slate-500">Thành tiền: </span>
                      <strong className="text-base sm:text-lg font-black text-[#0284C7]">
                        {formatVND(order.totalAmount)}
                      </strong>
                    </div>

                    <div className="flex items-center gap-2">
                      {order.status === "PENDING" || order.status === "PROCESSING" ? (
                        <button
                          onClick={() => handleCancelOrder(order.id)}
                          className="px-4 py-2 border border-red-200 text-red-600 hover:bg-red-50 rounded-xl text-xs font-bold transition-colors"
                        >
                          Hủy Đơn Hàng
                        </button>
                      ) : null}

                      <button
                        onClick={() => handleRebuy(order)}
                        className="px-5 py-2 bg-[#0284C7] hover:bg-[#0369A1] text-white font-bold rounded-xl text-xs shadow-sm shadow-sky-600/20 transition-all"
                      >
                        Mua Lại
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </main>

      <Footer />
    </div>
  );
};
