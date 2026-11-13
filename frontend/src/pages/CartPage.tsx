import React, { useEffect } from "react";
import { Link, useNavigate } from "react-router-dom";
import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/layout/Footer";
import { useCartStore } from "@/store/useCartStore";
import { useAuthStore } from "@/store/useAuthStore";
import { formatVND } from "@/lib/formatters";
import {
  ShoppingBag,
  Trash2,
  Plus,
  Minus,
  Store,
  Ticket,
  ShieldCheck,
  ArrowRight,
  Sparkles,
} from "lucide-react";

export const CartPage: React.FC = () => {
  const navigate = useNavigate();
  const {
    items,
    selectedSkuIds,
    getShopGroups,
    getSelectedTotal,
    getSelectedCount,
    updateQuantity,
    removeFromCart,
    removeSelected,
    toggleSelect,
    toggleSelectShop,
    toggleSelectAll,
    isAllSelected,
    isShopSelected,
    loading,
    error,
    fetchCart,
  } = useCartStore();

  useEffect(() => { void fetchCart(); }, [fetchCart]);

  const shopGroups = getShopGroups();
  const selectedCount = getSelectedCount();
  const rawSubtotal = getSelectedTotal();
  const finalTotal = rawSubtotal;

  // Debounced quantity updates
  const handleQuantityChange = (skuId: number, currentQty: number, delta: number, stock: number) => {
    const newQty = Math.min(Math.max(1, currentQty + delta), stock);
    if (!loading) void updateQuantity(skuId, newQty);
  };

  const { isAuthenticated } = useAuthStore();

  const handleProceedCheckout = () => {
    if (loading || error) return;
    if (selectedCount === 0) {
      alert("Vui lòng chọn ít nhất 1 sản phẩm để tiến hành đặt hàng!");
      return;
    }
    if (!isAuthenticated) {
      alert("Bạn bắt buộc phải đăng nhập tài khoản trước khi tiến hành đặt hàng!");
      navigate("/login?redirect=/checkout");
      return;
    }
    navigate("/checkout");
  };

  return (
    <div className="min-h-screen bg-[#F5F5FA] flex flex-col">
      <Header />

      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-6 space-y-4 pb-28">
        {/* Cart Title Banner */}
        <div className="bg-white rounded-xl p-4 shadow-sm border border-slate-200/80 flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-lg bg-sky-50 text-[#0284C7] flex items-center justify-center font-bold">
              <ShoppingBag className="w-5 h-5" />
            </div>
            <h1 className="text-base sm:text-lg font-bold text-slate-800">
              Giỏ Hàng HeliShop ({items.length} mặt hàng)
            </h1>
          </div>
          <span className="text-xs text-slate-500 font-medium hidden sm:inline">
            Tất cả sản phẩm đều được bảo vệ bởi HeliShop Care
          </span>
        </div>

        {error && <div role="alert" className="bg-red-50 text-red-700 rounded-xl p-4 text-sm">{error} <button onClick={() => void fetchCart()} className="underline ml-2">Thử lại</button></div>}
        {loading && <p role="status" className="text-sm text-slate-500">Đang đồng bộ giỏ hàng…</p>}
        {!loading && items.length === 0 ? (
          <div className="bg-white rounded-2xl p-16 text-center shadow-sm border border-slate-200/80 space-y-4">
            <div className="w-20 h-20 rounded-full bg-sky-50 text-[#0284C7] flex items-center justify-center mx-auto">
              <ShoppingBag className="w-10 h-10 opacity-30" />
            </div>
            <h3 className="text-lg font-bold text-slate-800">Giỏ hàng của bạn còn trống</h3>
            <p className="text-xs text-slate-500 max-w-sm mx-auto">
              Hãy khám phá hàng ngàn sản phẩm công nghệ, phụ kiện và thời trang chất lượng cao trên HeliShop.
            </p>
            <Link
              to="/"
              className="inline-flex items-center gap-2 px-6 py-2.5 bg-[#0284C7] text-white font-bold rounded-xl text-xs hover:bg-[#0369A1] transition-colors shadow-md shadow-sky-600/20"
            >
              <span>Mua sắm ngay</span>
              <ArrowRight className="w-4 h-4" />
            </Link>
          </div>
        ) : (
          <div className="space-y-4">
            {/* Table Header Row (Desktop) */}
            <div className="hidden md:grid grid-cols-12 gap-4 bg-white rounded-xl p-3.5 shadow-sm border border-slate-200/80 text-xs font-bold text-slate-500 uppercase tracking-wider items-center">
              <div className="col-span-5 flex items-center gap-3">
                <input
                  type="checkbox"
                  checked={isAllSelected()}
                  onChange={toggleSelectAll}
                  className="w-4 h-4 rounded border-slate-300 text-[#0284C7] focus:ring-[#0284C7]"
                />
                <span>Sản Phẩm ({items.length})</span>
              </div>
              <div className="col-span-2 text-center">Đơn Giá</div>
              <div className="col-span-2 text-center">Số Lượng</div>
              <div className="col-span-2 text-center">Số Tiền</div>
              <div className="col-span-1 text-center">Thao Tác</div>
            </div>

            {/* SHOP GROUPS (Shopee-Style Group by Shop) */}
            {shopGroups.map((group) => {
              const isGroupChecked = isShopSelected(group.shopId);

              return (
                <div
                  key={group.shopId}
                  className="bg-white rounded-2xl shadow-sm border border-slate-200/80 overflow-hidden"
                >
                  {/* Shop Header Bar */}
                  <div className="p-3.5 bg-slate-50/80 border-b border-slate-100 flex items-center justify-between">
                    <div className="flex items-center gap-3">
                      <input
                        type="checkbox"
                        checked={isGroupChecked}
                        onChange={() => toggleSelectShop(group.shopId)}
                        className="w-4 h-4 rounded border-slate-300 text-[#0284C7] focus:ring-[#0284C7]"
                      />
                      <div className="flex items-center gap-1.5 font-bold text-xs sm:text-sm text-slate-800">
                        <Store className="w-4 h-4 text-[#0284C7]" />
                        <span>{group.shopName}</span>
                        <span className="bg-[#0284C7] text-white text-[9px] font-black px-1.5 py-0.2 rounded ml-1">
                          Official
                        </span>
                      </div>
                    </div>

                    <button className="text-xs text-[#0284C7] font-semibold hover:underline">
                      Chat ngay
                    </button>
                  </div>

                  {/* Items in this shop */}
                  <div className="divide-y divide-slate-100">
                    {group.items.map((item) => {
                      const isItemChecked = selectedSkuIds.includes(item.skuId);
                      const displayQty = item.quantity;
                      const itemSubtotal = item.price * displayQty;

                      return (
                        <div
                          key={item.skuId}
                          className={`p-4 grid grid-cols-1 md:grid-cols-12 gap-4 items-center transition-colors ${
                            isItemChecked ? "bg-sky-50/20" : ""
                          }`}
                        >
                          {/* Col 1-5: Checkbox, Image, Info */}
                          <div className="col-span-5 flex items-start gap-3">
                            <input
                              type="checkbox"
                              checked={isItemChecked}
                              onChange={() => toggleSelect(item.skuId)}
                              className="w-4 h-4 rounded border-slate-300 text-[#0284C7] focus:ring-[#0284C7] mt-1 shrink-0"
                            />
                            <Link to={`/product/${item.productId}`} className="shrink-0">
                              <img
                                src={item.imageUrl}
                                alt={item.productName}
                                className="w-16 h-16 sm:w-20 sm:h-20 object-cover rounded-lg border border-slate-200"
                              />
                            </Link>
                            <div className="space-y-1 min-w-0 flex-1">
                              <Link
                                to={`/product/${item.productId}`}
                                className="text-xs font-semibold text-slate-800 hover:text-[#0284C7] line-clamp-2 leading-snug"
                              >
                                {item.productName}
                              </Link>
                              {item.attributes && (
                                <div className="inline-block bg-slate-100 text-slate-600 text-[11px] px-2 py-0.5 rounded">
                                  Phân loại: {Object.values(item.attributes).join(", ")}
                                </div>
                              )}
                              <p className="text-[11px] text-slate-400 font-mono">
                                SKU: {item.skuCode}
                              </p>
                            </div>
                          </div>

                          {/* Col 6-7: Unit Price */}
                          <div className="col-span-2 text-left md:text-center">
                            <div className="text-xs sm:text-sm font-bold text-slate-800">
                              {formatVND(item.price)}
                            </div>
                            {item.originalPrice && item.originalPrice > item.price && (
                              <div className="text-[11px] text-slate-400 line-through">
                                {formatVND(item.originalPrice)}
                              </div>
                            )}
                          </div>

                          {/* Col 8-9: Quantity Stepper with Debounce */}
                          <div className="col-span-2 flex items-center justify-start md:justify-center">
                            <div className="flex items-center border border-slate-200 rounded-lg overflow-hidden bg-white shadow-sm">
                              <button
                                onClick={() =>
                                  handleQuantityChange(item.skuId, item.quantity, -1, item.stockQuantity)
                                }
                                disabled={loading || displayQty <= 1}
                                className="p-1.5 hover:bg-slate-100 text-slate-600 disabled:opacity-30 transition-colors"
                              >
                                <Minus className="w-3.5 h-3.5" />
                              </button>
                              <span className="w-10 text-center text-xs font-bold text-slate-800">
                                {displayQty}
                              </span>
                              <button
                                onClick={() =>
                                  handleQuantityChange(item.skuId, item.quantity, 1, item.stockQuantity)
                                }
                                disabled={loading || displayQty >= item.stockQuantity}
                                className="p-1.5 hover:bg-slate-100 text-slate-600 disabled:opacity-30 transition-colors"
                              >
                                <Plus className="w-3.5 h-3.5" />
                              </button>
                            </div>
                          </div>

                          {/* Col 10-11: Subtotal */}
                          <div className="col-span-2 text-left md:text-center text-xs sm:text-sm font-extrabold text-[#0284C7]">
                            {formatVND(itemSubtotal)}
                          </div>

                          {/* Col 12: Delete Button */}
                          <div className="col-span-1 flex justify-end md:justify-center">
                            <button
                              onClick={() => removeFromCart(item.skuId)}
                              className="p-1.5 text-slate-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors"
                              title="Xóa sản phẩm"
                            >
                              <Trash2 className="w-4 h-4" />
                            </button>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                </div>
              );
            })}

            {/* Voucher Section */}
            <div className="bg-white rounded-2xl p-4 shadow-sm border border-slate-200/80 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
              <div className="flex items-center gap-2 text-xs font-bold text-slate-800">
                <Ticket className="w-4 h-4 text-[#0284C7]" />
                <span>HeliShop Voucher Khuyến Mãi</span>
              </div>

              <p className="text-sm text-slate-500">Áp dụng voucher và xem phí vận chuyển tại bước thanh toán.</p>
            </div>
          </div>
        )}
      </main>

      {/* 4. STICKY BOTTOM BAR (Chuẩn Shopee) */}
      {items.length > 0 && (
        <div className="fixed bottom-0 left-0 right-0 z-40 bg-white border-t border-slate-200 shadow-sticky-bar py-3 px-4 sm:px-8">
          <div className="max-w-7xl mx-auto flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
            <div className="flex items-center gap-4 text-xs text-slate-700">
              <label className="flex items-center gap-2 cursor-pointer font-semibold">
                <input
                  type="checkbox"
                  checked={isAllSelected()}
                  onChange={toggleSelectAll}
                  className="w-4 h-4 rounded border-slate-300 text-[#0284C7] focus:ring-[#0284C7]"
                />
                <span>Chọn tất cả ({items.length})</span>
              </label>

              <button
                onClick={removeSelected}
                disabled={loading || selectedCount === 0}
                className="text-slate-500 hover:text-red-600 font-medium disabled:opacity-40"
              >
                Xóa các mục đã chọn
              </button>
            </div>

            <div className="flex items-center justify-between w-full sm:w-auto gap-4">
              <div className="text-right">
                <div className="text-xs text-slate-500">
                  Tổng thanh toán ({selectedCount} sản phẩm):
                </div>
                <div className="text-lg sm:text-xl font-black text-[#0284C7] leading-tight">
                  {formatVND(finalTotal)}
                </div>
              </div>

              <button
                onClick={handleProceedCheckout}
                disabled={loading || !!error || selectedCount === 0}
                className="py-3 px-8 bg-gradient-to-r from-[#0284C7] to-[#0369A1] hover:opacity-95 text-white font-extrabold rounded-xl text-sm shadow-md shadow-sky-600/25 disabled:opacity-50 transition-all shrink-0"
              >
                Mua Hàng ({selectedCount})
              </button>
            </div>
          </div>
        </div>
      )}

      <Footer />
    </div>
  );
};
