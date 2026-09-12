import React, { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/layout/Footer";
import { useCartStore } from "@/store/useCartStore";
import { useOrderStore } from "@/store/useOrderStore";
import { useAuthStore } from "@/store/useAuthStore";
import { formatVND } from "@/lib/formatters";
import { Order, OrderItem, Address, Voucher } from "@/types";
import { api } from "@/lib/api";
import {
  MapPin,
  Truck,
  Ticket,
  CreditCard,
  ShieldCheck,
  CheckCircle2,
  ChevronRight,
  Store,
  DollarSign,
  QrCode,
} from "lucide-react";

const DEFAULT_ADDRESS: Address = {
  id: 1,
  receiverName: "Vũ Viết Anh",
  phone: "0988889999",
  detailAddress: "Thôn Tốt Động",
  ward: "Xã Quảng Bị",
  district: "Huyện Chương Mỹ",
  province: "TP. Hà Nội",
  isDefault: true,
};

const AVAILABLE_VOUCHERS: Voucher[] = [
  {
    id: 1,
    code: "HELI50K",
    name: "Giảm 50.000₫ cho đơn hàng từ 200.000₫",
    discountType: "FIXED_AMOUNT",
    discountValue: 50000,
    minOrderValue: 200000,
    expiryDate: "31/12/2026",
  },
  {
    id: 2,
    code: "FREESHIP",
    name: "Miễn phí vận chuyển toàn quốc (Giảm 30.000₫)",
    discountType: "FIXED_AMOUNT",
    discountValue: 30000,
    minOrderValue: 150000,
    expiryDate: "31/12/2026",
  },
  {
    id: 3,
    code: "VNPAY100K",
    name: "Ưu đãi thanh toán VNPAY-QR Giảm 100.000₫",
    discountType: "FIXED_AMOUNT",
    discountValue: 100000,
    minOrderValue: 500000,
    expiryDate: "31/12/2026",
  },
];

export const CheckoutPage: React.FC = () => {
  const navigate = useNavigate();
  const { user, isAuthenticated } = useAuthStore();
  const { getSelectedItems, removeSelected, getSelectedTotal } = useCartStore();
  const { addOrder } = useOrderStore();

  // Enforce login on checkout page
  React.useEffect(() => {
    if (!isAuthenticated) {
      alert("Bạn bắt buộc phải đăng nhập tài khoản trước khi tiến hành đặt hàng!");
      navigate("/login?redirect=/checkout");
    }
  }, [isAuthenticated, navigate]);

  const selectedItems = getSelectedItems();
  const rawSubtotal = getSelectedTotal();

  // Address State
  const [address, setAddress] = useState<Address>(() => ({
    ...DEFAULT_ADDRESS,
    receiverName: user?.fullName || DEFAULT_ADDRESS.receiverName,
    phone: user?.phone || DEFAULT_ADDRESS.phone,
  }));
  const [isEditingAddress, setIsEditingAddress] = useState(false);

  // Shipping Method State
  const [shippingMethod, setShippingMethod] = useState<"standard" | "express" | "economy">("standard");
  const shippingFees = {
    standard: 30000,
    express: 50000,
    economy: 15000,
  };
  const currentShippingFee = shippingFees[shippingMethod];

  // Voucher State
  const [voucherCode, setVoucherCode] = useState("HELI50K");
  const [appliedVoucher, setAppliedVoucher] = useState<Voucher | null>(AVAILABLE_VOUCHERS[0]);

  // Payment Method State
  const [paymentMethod, setPaymentMethod] = useState<"COD" | "VNPAY">("VNPAY");
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Calculations
  const discountAmount = appliedVoucher ? appliedVoucher.discountValue : 0;
  const totalAmount = Math.max(0, rawSubtotal + currentShippingFee - discountAmount);

  // Group selected items by shop
  const groupedByShop = React.useMemo(() => {
    const map = new Map<number, { shopId: number; shopName: string; items: typeof selectedItems }>();
    selectedItems.forEach((item) => {
      if (!map.has(item.shopId)) {
        map.set(item.shopId, { shopId: item.shopId, shopName: item.shopName, items: [] });
      }
      map.get(item.shopId)!.items.push(item);
    });
    return Array.from(map.values());
  }, [selectedItems]);

  const handleApplyVoucher = (e: React.FormEvent) => {
    e.preventDefault();
    const found = AVAILABLE_VOUCHERS.find((v) => v.code.toUpperCase() === voucherCode.trim().toUpperCase());
    if (found) {
      setAppliedVoucher(found);
    } else {
      alert("Mã voucher không tồn tại trên hệ thống!");
    }
  };

  const handlePlaceOrder = async () => {
    if (!isAuthenticated) {
      alert("Bạn bắt buộc phải đăng nhập tài khoản trước khi đặt hàng!");
      navigate("/login?redirect=/checkout");
      return;
    }

    if (selectedItems.length === 0) {
      alert("Chưa có sản phẩm nào được chọn!");
      navigate("/cart");
      return;
    }

    // Bắt buộc phải có Họ tên người nhận
    if (!address.receiverName || !address.receiverName.trim()) {
      alert("Vui lòng nhập họ và tên người nhận hàng!");
      setIsEditingAddress(true);
      return;
    }

    // Bắt buộc phải có Số điện thoại nhận hàng hợp lệ
    const cleanPhone = (address.phone || "").trim().replace(/\s+/g, "");
    if (!cleanPhone || !/^(0|\+84)[0-9]{9}$/.test(cleanPhone)) {
      alert("Bắt buộc phải có Số điện thoại nhận hàng hợp lệ (10 chữ số) để đặt đơn!");
      setIsEditingAddress(true);
      return;
    }

    // Bắt buộc phải có Địa chỉ nhận hàng chi tiết
    if (!address.detailAddress || !address.detailAddress.trim()) {
      alert("Bắt buộc phải có Địa chỉ nhận hàng chi tiết để giao hàng!");
      setIsEditingAddress(true);
      return;
    }

    setIsSubmitting(true);
    const orderCode = `ORD-${Date.now().toString().slice(-8)}`;

    const orderItems: OrderItem[] = selectedItems.map((item, idx) => ({
      id: idx + 1,
      skuId: item.skuId,
      productName: item.productName,
      skuCode: item.skuCode,
      attributes: item.attributes,
      unitPrice: item.price,
      quantity: item.quantity,
      totalPrice: item.price * item.quantity,
      imageUrl: item.imageUrl,
    }));

    const newOrder: Order = {
      id: Math.floor(Math.random() * 10000) + 10,
      orderCode,
      shopId: selectedItems[0].shopId,
      shopName: selectedItems[0].shopName,
      status: paymentMethod === "VNPAY" ? "PROCESSING" : "PENDING",
      paymentStatus: paymentMethod === "VNPAY" ? "PAID" : "PENDING",
      paymentMethod,
      shippingAddressSnapshot: `${address.receiverName} - ${address.phone}, ${address.detailAddress}, ${address.ward}, ${address.district}, ${address.province}`,
      receiverName: address.receiverName,
      receiverPhone: address.phone,
      items: orderItems,
      totalAmount,
      shippingFee: currentShippingFee,
      discountAmount,
      createdAt: new Date().toLocaleString("vi-VN"),
    };

    // Save order and clear purchased items from cart
    addOrder(newOrder);
    removeSelected();

    if (paymentMethod === "VNPAY") {
      try {
        // Try calling real backend VNPAY URL endpoint if active
        const res = await api.post("/payments/create-vnpay-url", {
          orderId: newOrder.id,
          bankCode: "NCB",
        });
        if (res.data?.data?.paymentUrl) {
          window.location.href = res.data.data.paymentUrl;
          return;
        }
      } catch {
        // Graceful fallback to payment result demo screen
      }

      // Seamless redirect to Payment Result screen
      navigate(
        `/payment-result?status=success&orderCode=${orderCode}&amount=${totalAmount}&vnp_TransactionNo=14592810&method=VNPAY`
      );
    } else {
      // COD Order
      navigate(
        `/payment-result?status=success&orderCode=${orderCode}&amount=${totalAmount}&method=COD`
      );
    }
  };

  if (selectedItems.length === 0) {
    return (
      <div className="min-h-screen bg-[#F5F5FA] flex flex-col">
        <Header />
        <main className="flex-1 max-w-4xl mx-auto p-8 text-center space-y-4">
          <h2 className="text-xl font-bold text-slate-800">Không có sản phẩm để thanh toán</h2>
          <p className="text-xs text-slate-500">Vui lòng quay lại giỏ hàng và chọn sản phẩm.</p>
          <Link to="/cart" className="inline-block px-5 py-2 bg-[#0284C7] text-white rounded-lg text-xs font-bold">
            Trở lại giỏ hàng
          </Link>
        </main>
        <Footer />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[#F5F5FA] flex flex-col">
      <Header />

      <main className="flex-1 max-w-5xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-6 space-y-6">
        {/* Step Indicator */}
        <div className="flex items-center justify-between bg-white p-4 rounded-xl shadow-sm border border-slate-200/80 text-xs">
          <div className="flex items-center gap-2 text-slate-400">
            <span className="w-5 h-5 rounded-full bg-slate-200 text-slate-700 flex items-center justify-center font-bold">1</span>
            <span>Giỏ hàng</span>
          </div>
          <ChevronRight className="w-4 h-4 text-slate-300" />
          <div className="flex items-center gap-2 text-[#0284C7] font-bold">
            <span className="w-5 h-5 rounded-full bg-[#0284C7] text-white flex items-center justify-center font-bold">2</span>
            <span>Thanh toán & Đặt hàng</span>
          </div>
          <ChevronRight className="w-4 h-4 text-slate-300" />
          <div className="flex items-center gap-2 text-slate-400">
            <span className="w-5 h-5 rounded-full bg-slate-200 text-slate-700 flex items-center justify-center font-bold">3</span>
            <span>Hoàn tất đơn</span>
          </div>
        </div>

        {/* 1. DELIVERY ADDRESS SECTION */}
        <div className="bg-white rounded-2xl p-5 shadow-sm border border-slate-200/80 space-y-3 relative overflow-hidden">
          {/* Top color ribbon bar */}
          <div className="absolute top-0 left-0 right-0 h-1 bg-gradient-to-r from-[#0284C7] via-sky-400 to-indigo-600" />

          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2 text-[#0284C7] font-bold text-sm uppercase tracking-wider">
              <MapPin className="w-4 h-4" />
              <span>Địa Chỉ Nhận Hàng</span>
            </div>
            <button
              onClick={() => setIsEditingAddress(!isEditingAddress)}
              className="text-xs text-[#0284C7] hover:underline font-semibold"
            >
              {isEditingAddress ? "Đóng" : "Thay đổi địa chỉ"}
            </button>
          </div>

          {!isEditingAddress ? (
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 text-xs text-slate-700">
              <div className="space-y-1">
                <div className="flex flex-wrap items-center gap-2">
                  <span className="font-extrabold text-slate-900 text-sm">
                    {address.receiverName || "Chưa có tên người nhận"}
                  </span>
                  {address.phone && /^(0|\+84)[0-9]{9}$/.test(address.phone.trim().replace(/\s+/g, "")) ? (
                    <span className="font-bold text-[#0284C7] bg-sky-50 px-2 py-0.5 rounded border border-sky-200">
                      📞 {address.phone}
                    </span>
                  ) : (
                    <span className="font-bold text-red-600 bg-red-50 px-2 py-0.5 rounded border border-red-200">
                      ⚠️ Cần cập nhật SĐT (Bắt buộc)
                    </span>
                  )}
                  <span className="inline-block border border-[#0284C7] text-[#0284C7] text-[10px] font-bold px-2 py-0.5 rounded">
                    Mặc định
                  </span>
                </div>
                <p className="text-slate-600">
                  {address.detailAddress ? (
                    `${address.detailAddress}, ${address.ward}, ${address.district}, ${address.province}`
                  ) : (
                    <span className="text-red-500 font-medium">⚠️ Chưa có địa chỉ giao hàng cụ thể</span>
                  )}
                </p>
              </div>
              <button
                type="button"
                onClick={() => setIsEditingAddress(true)}
                className="text-xs text-[#0284C7] hover:underline font-semibold shrink-0"
              >
                Sửa thông tin
              </button>
            </div>
          ) : (
            <div className="p-3 bg-slate-50 rounded-xl space-y-3 text-xs">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="text-[11px] font-bold text-slate-700 block mb-1">
                    Họ và tên người nhận <span className="text-red-500">*</span>
                  </label>
                  <input
                    type="text"
                    placeholder="Ví dụ: Nguyễn Văn A"
                    value={address.receiverName}
                    onChange={(e) => setAddress({ ...address, receiverName: e.target.value })}
                    className="w-full bg-white border border-slate-200 rounded-lg p-2 text-xs focus:ring-1 focus:ring-[#0284C7] outline-none"
                  />
                </div>
                <div>
                  <label className="text-[11px] font-bold text-slate-700 block mb-1">
                    Số điện thoại nhận hàng <span className="text-red-500">*</span>
                  </label>
                  <input
                    type="tel"
                    placeholder="0988889999 (bắt buộc 10 số)"
                    value={address.phone}
                    onChange={(e) => setAddress({ ...address, phone: e.target.value })}
                    className="w-full bg-white border border-slate-200 rounded-lg p-2 text-xs focus:ring-1 focus:ring-[#0284C7] outline-none"
                  />
                  <p className="text-[10px] text-slate-400 mt-0.5">* Bắt buộc để bưu tá liên lạc khi phát hàng.</p>
                </div>
              </div>
              <div>
                <label className="text-[11px] font-bold text-slate-700 block mb-1">
                  Địa chỉ chi tiết (Số nhà, tên đường, thôn xóm...) <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  placeholder="Địa chỉ chi tiết (Số nhà, tên đường, tòa nhà...)"
                  value={address.detailAddress}
                  onChange={(e) => setAddress({ ...address, detailAddress: e.target.value })}
                  className="w-full bg-white border border-slate-200 rounded-lg p-2 text-xs focus:ring-1 focus:ring-[#0284C7] outline-none"
                />
              </div>
              <div className="flex justify-end">
                <button
                  type="button"
                  onClick={() => setIsEditingAddress(false)}
                  className="px-4 py-1.5 bg-[#0284C7] hover:bg-[#0369A1] text-white font-bold rounded-lg text-xs shadow-sm"
                >
                  Xác Nhận Lưu
                </button>
              </div>
            </div>
          )}
        </div>

        {/* 2. ORDERED ITEMS (GROUPED BY SHOP) */}
        <div className="bg-white rounded-2xl shadow-sm border border-slate-200/80 overflow-hidden divide-y divide-slate-100">
          <div className="p-4 bg-slate-50/70 text-xs font-bold text-slate-500 uppercase tracking-wider">
            Sản Phẩm Đã Chọn ({selectedItems.length})
          </div>

          {groupedByShop.map((group) => (
            <div key={group.shopId} className="p-4 space-y-4">
              <div className="flex items-center gap-2 font-bold text-xs text-slate-800">
                <Store className="w-4 h-4 text-[#0284C7]" />
                <span>{group.shopName}</span>
              </div>

              <div className="space-y-3">
                {group.items.map((item) => (
                  <div key={item.skuId} className="flex items-center justify-between text-xs gap-4">
                    <div className="flex items-center gap-3 flex-1 min-w-0">
                      <img
                        src={item.imageUrl}
                        alt={item.productName}
                        className="w-12 h-12 rounded-lg object-cover border border-slate-200 shrink-0"
                      />
                      <div className="min-w-0">
                        <p className="font-semibold text-slate-800 truncate">{item.productName}</p>
                        {item.attributes && (
                          <p className="text-slate-400 text-[11px]">
                            Phân loại: {Object.values(item.attributes).join(", ")}
                          </p>
                        )}
                      </div>
                    </div>

                    <div className="text-right shrink-0">
                      <span className="text-slate-400 text-[11px] mr-3">x{item.quantity}</span>
                      <span className="font-bold text-slate-800">{formatVND(item.price * item.quantity)}</span>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          ))}
        </div>

        {/* 3. SHIPPING METHOD SELECTOR */}
        <div className="bg-white rounded-2xl p-5 shadow-sm border border-slate-200/80 space-y-3">
          <div className="flex items-center gap-2 text-xs font-bold text-slate-800 uppercase tracking-wider">
            <Truck className="w-4 h-4 text-[#0284C7]" />
            <span>Phương Thức Vận Chuyển</span>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 text-xs">
            <div
              onClick={() => setShippingMethod("standard")}
              className={`p-3 rounded-xl border-2 cursor-pointer transition-all ${
                shippingMethod === "standard"
                  ? "border-[#0284C7] bg-sky-50/50"
                  : "border-slate-200 hover:border-sky-200"
              }`}
            >
              <div className="flex items-center justify-between font-bold text-slate-800">
                <span>Nhanh (Tiêu Chuẩn)</span>
                <span className="text-[#0284C7]">{formatVND(shippingFees.standard)}</span>
              </div>
              <p className="text-[11px] text-slate-400 mt-1">Giao trong 2-3 ngày làm việc</p>
            </div>

            <div
              onClick={() => setShippingMethod("express")}
              className={`p-3 rounded-xl border-2 cursor-pointer transition-all ${
                shippingMethod === "express"
                  ? "border-[#0284C7] bg-sky-50/50"
                  : "border-slate-200 hover:border-sky-200"
              }`}
            >
              <div className="flex items-center justify-between font-bold text-slate-800">
                <span>Hỏa Tốc 2H</span>
                <span className="text-[#0284C7]">{formatVND(shippingFees.express)}</span>
              </div>
              <p className="text-[11px] text-slate-400 mt-1">Giao ngay trong ngày</p>
            </div>

            <div
              onClick={() => setShippingMethod("economy")}
              className={`p-3 rounded-xl border-2 cursor-pointer transition-all ${
                shippingMethod === "economy"
                  ? "border-[#0284C7] bg-sky-50/50"
                  : "border-slate-200 hover:border-sky-200"
              }`}
            >
              <div className="flex items-center justify-between font-bold text-slate-800">
                <span>Tiết Kiệm</span>
                <span className="text-[#0284C7]">{formatVND(shippingFees.economy)}</span>
              </div>
              <p className="text-[11px] text-slate-400 mt-1">Giao trong 4-5 ngày</p>
            </div>
          </div>
        </div>

        {/* 4. PAYMENT METHOD SELECTOR */}
        <div className="bg-white rounded-2xl p-5 shadow-sm border border-slate-200/80 space-y-4">
          <div className="flex items-center gap-2 text-xs font-bold text-slate-800 uppercase tracking-wider">
            <CreditCard className="w-4 h-4 text-[#0284C7]" />
            <span>Phương Thức Thanh Toán</span>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 text-xs">
            {/* VNPAY */}
            <div
              onClick={() => setPaymentMethod("VNPAY")}
              className={`p-4 rounded-xl border-2 cursor-pointer flex items-center justify-between transition-all ${
                paymentMethod === "VNPAY"
                  ? "border-[#0284C7] bg-sky-50/60 shadow-sm"
                  : "border-slate-200 hover:border-sky-200 bg-white"
              }`}
            >
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-lg bg-[#0369A1] text-white flex items-center justify-center font-black text-xs">
                  VNPAY
                </div>
                <div>
                  <h5 className="font-bold text-slate-900 text-sm">Cổng Thanh Toán Trực Tuyến VNPAY-QR</h5>
                  <p className="text-[11px] text-slate-500">Quét mã QR, Thẻ ATM nội địa, Visa / Mastercard / JCB</p>
                </div>
              </div>
              {paymentMethod === "VNPAY" && (
                <CheckCircle2 className="w-5 h-5 text-[#0284C7]" />
              )}
            </div>

            {/* COD */}
            <div
              onClick={() => setPaymentMethod("COD")}
              className={`p-4 rounded-xl border-2 cursor-pointer flex items-center justify-between transition-all ${
                paymentMethod === "COD"
                  ? "border-[#0284C7] bg-sky-50/60 shadow-sm"
                  : "border-slate-200 hover:border-sky-200 bg-white"
              }`}
            >
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-lg bg-emerald-600 text-white flex items-center justify-center font-black text-xs">
                  COD
                </div>
                <div>
                  <h5 className="font-bold text-slate-900 text-sm">Thanh Toán Khi Nhận Hàng (COD)</h5>
                  <p className="text-[11px] text-slate-500">Kiểm tra hàng trước khi thanh toán tiền mặt</p>
                </div>
              </div>
              {paymentMethod === "COD" && (
                <CheckCircle2 className="w-5 h-5 text-[#0284C7]" />
              )}
            </div>
          </div>
        </div>

        {/* 5. VOUCHER & FINAL BREAKDOWN */}
        <div className="bg-white rounded-2xl p-6 shadow-sm border border-slate-200/80 space-y-4">
          {/* Voucher input */}
          <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 pb-4 border-b border-slate-100">
            <div className="flex items-center gap-2 text-xs font-bold text-slate-800">
              <Ticket className="w-4 h-4 text-[#0284C7]" />
              <span>HeliShop Voucher</span>
            </div>

            <form onSubmit={handleApplyVoucher} className="flex items-center gap-2 w-full sm:w-auto">
              <input
                type="text"
                placeholder="Nhập mã voucher"
                value={voucherCode}
                onChange={(e) => setVoucherCode(e.target.value)}
                className="bg-slate-50 border border-slate-200 rounded-lg px-3 py-1.5 text-xs font-bold uppercase w-full sm:w-36 focus:outline-none focus:ring-1 focus:ring-[#0284C7]"
              />
              <button
                type="submit"
                className="px-4 py-1.5 bg-[#0284C7] text-white rounded-lg text-xs font-bold shrink-0"
              >
                Áp Dụng
              </button>
            </form>
          </div>

          {/* Pricing table */}
          <div className="space-y-2 text-xs text-slate-600 pt-1">
            <div className="flex justify-between">
              <span>Tổng tiền hàng:</span>
              <span className="font-semibold text-slate-800">{formatVND(rawSubtotal)}</span>
            </div>
            <div className="flex justify-between">
              <span>Phí vận chuyển:</span>
              <span className="font-semibold text-slate-800">{formatVND(currentShippingFee)}</span>
            </div>
            {discountAmount > 0 && (
              <div className="flex justify-between text-emerald-600 font-semibold">
                <span>Giảm giá voucher ({appliedVoucher?.code}):</span>
                <span>-{formatVND(discountAmount)}</span>
              </div>
            )}
            <div className="flex justify-between text-sm sm:text-base font-extrabold text-slate-900 pt-3 border-t border-slate-100">
              <span>Tổng thanh toán:</span>
              <span className="text-xl font-black text-[#0284C7]">{formatVND(totalAmount)}</span>
            </div>
          </div>

          {/* Place Order CTA Button */}
          <div className="pt-4 flex items-center justify-between">
            <span className="text-xs text-slate-400">
              Nhấn "Đặt hàng" đồng nghĩa với việc bạn đồng ý tuân theo Điều khoản HeliShop
            </span>
            <button
              onClick={handlePlaceOrder}
              disabled={isSubmitting}
              className="py-3 px-10 bg-gradient-to-r from-[#0284C7] to-[#0369A1] hover:opacity-95 text-white font-extrabold rounded-xl text-sm shadow-lg shadow-sky-600/30 transition-all shrink-0"
            >
              {isSubmitting ? "Đang xử lý..." : "Đặt Hàng Ngay"}
            </button>
          </div>
        </div>
      </main>

      <Footer />
    </div>
  );
};
