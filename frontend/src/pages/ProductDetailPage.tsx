import React, { useState, useMemo } from "react";
import { useParams, useNavigate, Link } from "react-router-dom";
import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/layout/Footer";
import { useCartStore } from "@/store/useCartStore";
import { MOCK_PRODUCTS } from "@/data/mockData";
import { formatVND, formatCompact } from "@/lib/formatters";
import {
  Star,
  Truck,
  ShieldCheck,
  RotateCcw,
  ShoppingCart,
  Zap,
  Check,
  ChevronRight,
  Store,
  MessageSquare,
  Sparkles,
  Plus,
  Minus,
  Heart,
  Share2,
} from "lucide-react";

export const ProductDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { addToCart } = useCartStore();

  const product = useMemo(() => {
    const found = MOCK_PRODUCTS.find((p) => p.id === Number(id));
    return found || MOCK_PRODUCTS[0];
  }, [id]);

  // Gallery image state
  const [selectedImageIndex, setSelectedImageIndex] = useState(0);

  // Extract variant attribute types (e.g. ["Màu sắc", "Dung lượng"] or ["Màu sắc", "Size"])
  const attributeKeys = useMemo(() => {
    const keysSet = new Set<string>();
    product.productSkus.forEach((sku) => {
      Object.keys(sku.attributes).forEach((k) => keysSet.add(k));
    });
    return Array.from(keysSet);
  }, [product]);

  // Selected attribute values state
  const [selectedAttributes, setSelectedAttributes] = useState<Record<string, string>>(() => {
    const initial: Record<string, string> = {};
    if (product.productSkus.length > 0) {
      attributeKeys.forEach((key) => {
        initial[key] = product.productSkus[0].attributes[key] || "";
      });
    }
    return initial;
  });

  // Quantity state
  const [quantity, setQuantity] = useState(1);
  const [isAddedToast, setIsAddedToast] = useState(false);

  // Find active SKU matching all selected attributes
  const currentSku = useMemo(() => {
    return (
      product.productSkus.find((sku) => {
        return attributeKeys.every((key) => sku.attributes[key] === selectedAttributes[key]);
      }) || product.productSkus[0]
    );
  }, [product, attributeKeys, selectedAttributes]);

  const activePrice = currentSku ? currentSku.price : product.price;
  const activeOriginalPrice = currentSku?.originalPrice || product.originalPrice;
  const stockAvailable = currentSku ? currentSku.stockQuantity : 10;

  const discountPercent =
    activeOriginalPrice && activeOriginalPrice > activePrice
      ? Math.round(((activeOriginalPrice - activePrice) / activeOriginalPrice) * 100)
      : null;

  // Change attribute selection
  const handleSelectAttribute = (key: string, val: string) => {
    setSelectedAttributes((prev) => ({ ...prev, [key]: val }));
  };

  const handleAddToCart = () => {
    addToCart(
      {
        skuId: currentSku.id,
        productId: product.id,
        productName: product.name,
        skuCode: currentSku.skuCode,
        attributes: currentSku.attributes,
        price: currentSku.price,
        originalPrice: currentSku.originalPrice,
        stockQuantity: currentSku.stockQuantity,
        imageUrl: currentSku.imageUrl || product.images[0].imageUrl,
        shopId: product.shop.id,
        shopName: product.shop.shopName,
      },
      quantity
    );

    setIsAddedToast(true);
    setTimeout(() => setIsAddedToast(false), 2500);
  };

  const handleBuyNow = () => {
    handleAddToCart();
    navigate("/checkout");
  };

  return (
    <div className="min-h-screen bg-[#F5F5FA] flex flex-col">
      <Header />

      {/* Floating Add to Cart Success Toast Notification */}
      {isAddedToast && (
        <div className="fixed top-20 right-4 sm:right-8 z-50 bg-[#0369A1] text-white p-4 rounded-xl shadow-2xl flex items-center gap-3 border border-sky-400 animate-in slide-in-from-top-4">
          <div className="w-8 h-8 rounded-full bg-emerald-500 text-white flex items-center justify-center shrink-0">
            <Check className="w-5 h-5 stroke-[3]" />
          </div>
          <div>
            <p className="font-bold text-sm">Đã thêm vào giỏ hàng thành công!</p>
            <p className="text-xs text-sky-100">
              {product.name} ({Object.values(currentSku.attributes).join(", ")}) x{quantity}
            </p>
          </div>
        </div>
      )}

      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-6 space-y-6">
        {/* Breadcrumb Bar */}
        <nav className="flex items-center gap-1.5 text-xs text-slate-500">
          <Link to="/" className="hover:text-[#0284C7] font-medium">HeliShop</Link>
          <ChevronRight className="w-3 h-3 text-slate-400" />
          <Link to={`/?category=${product.category.id}`} className="hover:text-[#0284C7] font-medium">
            {product.category.name}
          </Link>
          <ChevronRight className="w-3 h-3 text-slate-400" />
          <span className="text-slate-800 font-semibold truncate max-w-md">{product.name}</span>
        </nav>

        {/* Product Detail Main Card */}
        <div className="bg-white rounded-2xl p-6 shadow-sm border border-slate-200/80 grid grid-cols-1 lg:grid-cols-12 gap-8">
          {/* Left Gallery (5 cols) */}
          <div className="lg:col-span-5 space-y-4">
            {/* Main Preview Image with Hover Zoom Effect */}
            <div className="relative w-full pt-[100%] rounded-xl overflow-hidden bg-slate-100 border border-slate-100 group cursor-crosshair">
              <img
                src={product.images[selectedImageIndex]?.imageUrl || product.images[0].imageUrl}
                alt={product.name}
                className="absolute inset-0 w-full h-full object-cover object-center group-hover:scale-125 transition-transform duration-300 ease-out"
              />
              <div className="absolute top-3 left-3 bg-[#0284C7] text-white text-xs font-bold px-2 py-1 rounded shadow">
                HeliMall Chính Hãng
              </div>
            </div>

            {/* Thumbnail Strip */}
            <div className="flex items-center gap-3 overflow-x-auto no-scrollbar pb-1">
              {product.images.map((img, idx) => (
                <button
                  key={img.id}
                  onClick={() => setSelectedImageIndex(idx)}
                  className={`relative w-16 h-16 rounded-lg overflow-hidden border-2 shrink-0 transition-all ${
                    selectedImageIndex === idx
                      ? "border-[#0284C7] shadow-sm scale-105"
                      : "border-slate-200 hover:border-sky-300 opacity-70 hover:opacity-100"
                  }`}
                >
                  <img src={img.imageUrl} alt="thumb" className="w-full h-full object-cover" />
                </button>
              ))}
            </div>

            {/* Social Share & Favorite */}
            <div className="flex items-center justify-between pt-3 border-t border-slate-100 text-xs text-slate-500">
              <div className="flex items-center gap-3">
                <span className="font-medium text-slate-700">Chia sẻ:</span>
                <span className="w-6 h-6 rounded-full bg-blue-600 text-white flex items-center justify-center font-bold text-xs cursor-pointer">f</span>
                <span className="w-6 h-6 rounded-full bg-sky-500 text-white flex items-center justify-center font-bold text-xs cursor-pointer">t</span>
              </div>
              <button className="flex items-center gap-1.5 text-rose-500 hover:text-rose-600 font-semibold">
                <Heart className="w-4 h-4 fill-rose-500" />
                <span>Đã thích ({formatCompact(product.soldCount + 120)})</span>
              </button>
            </div>
          </div>

          {/* Right Product Buy Information (7 cols) */}
          <div className="lg:col-span-7 space-y-5">
            {/* Title & Badge */}
            <div className="space-y-2">
              <div className="flex items-center gap-2">
                <span className="bg-[#D0011B] text-white text-[11px] font-black px-2 py-0.5 rounded">
                  MALL
                </span>
                <span className="text-xs font-semibold text-[#0284C7]">
                  Gian Hàng Chính Hãng Được Bảo Hộ
                </span>
              </div>
              <h1 className="text-lg sm:text-xl font-bold text-slate-900 leading-snug">
                {product.name}
              </h1>
            </div>

            {/* Rating & Sold Section */}
            <div className="flex items-center gap-4 text-xs text-slate-500 py-1 border-y border-slate-100">
              <div className="flex items-center gap-1 text-[#0284C7] font-bold">
                <span className="text-sm underline">{product.rating}</span>
                <div className="flex items-center">
                  {Array.from({ length: 5 }).map((_, i) => (
                    <Star key={i} className="w-3.5 h-3.5 fill-amber-400 text-amber-400" />
                  ))}
                </div>
              </div>
              <div className="h-3.5 w-px bg-slate-200" />
              <div>
                <span className="font-bold text-slate-800 mr-1">{formatCompact(product.reviewCount)}</span>
                <span>Đánh Giá</span>
              </div>
              <div className="h-3.5 w-px bg-slate-200" />
              <div>
                <span className="font-bold text-slate-800 mr-1">{formatCompact(product.soldCount)}</span>
                <span>Đã Bán</span>
              </div>
            </div>

            {/* Price Box with Ocean Blue Gradient */}
            <div className="bg-gradient-to-r from-sky-50 via-sky-50/60 to-white p-4 rounded-xl border border-sky-100 space-y-2">
              <div className="flex items-baseline gap-3 flex-wrap">
                {activeOriginalPrice && (
                  <span className="text-sm text-slate-400 line-through">
                    {formatVND(activeOriginalPrice)}
                  </span>
                )}
                <span className="text-2xl sm:text-3xl font-black text-[#0284C7]">
                  {formatVND(activePrice)}
                </span>
                {discountPercent && (
                  <span className="bg-[#EF4444] text-white text-xs font-extrabold px-2 py-0.5 rounded-md">
                    -{discountPercent}% GIẢM
                  </span>
                )}
              </div>

              <div className="flex items-center gap-2 text-xs text-[#0369A1] font-semibold pt-1">
                <Zap className="w-4 h-4 text-amber-500 fill-amber-500" />
                <span>Gì cũng rẻ - Giá tốt nhất hôm nay trên HeliShop</span>
              </div>
            </div>

            {/* Shipping Info */}
            <div className="space-y-2 text-xs text-slate-600 pt-1">
              <div className="flex items-start gap-3">
                <span className="w-24 text-slate-400 font-medium shrink-0">Vận Chuyển</span>
                <div className="space-y-1">
                  <div className="flex items-center gap-1.5 font-bold text-slate-800">
                    <Truck className="w-4 h-4 text-emerald-600" />
                    <span>Miễn phí vận chuyển</span>
                  </div>
                  <p className="text-slate-500">
                    Giao hàng nhanh 2-3 ngày toàn quốc. Nhận voucher giảm 30.000₫ phí ship.
                  </p>
                </div>
              </div>
            </div>

            {/* 2-LEVEL VARIANT SELECTOR (Colors x Size/Capacity) */}
            <div className="space-y-4 pt-3 border-t border-slate-100">
              {attributeKeys.map((attrKey) => {
                // Get all distinct values for this attribute
                const values = Array.from(
                  new Set(product.productSkus.map((sku) => sku.attributes[attrKey]).filter(Boolean))
                );

                return (
                  <div key={attrKey} className="flex items-start gap-3 text-xs">
                    <span className="w-24 text-slate-400 font-medium shrink-0 pt-2">{attrKey}</span>
                    <div className="flex flex-wrap gap-2 flex-1">
                      {values.map((val) => {
                        const isSelected = selectedAttributes[attrKey] === val;
                        return (
                          <button
                            key={val}
                            onClick={() => handleSelectAttribute(attrKey, val)}
                            className={`px-3.5 py-2 rounded-lg border font-semibold transition-all flex items-center gap-1.5 ${
                              isSelected
                                ? "border-[#0284C7] bg-sky-50 text-[#0284C7] shadow-sm"
                                : "border-slate-200 hover:border-sky-300 text-slate-700 bg-white"
                            }`}
                          >
                            <span>{val}</span>
                            {isSelected && <Check className="w-3.5 h-3.5" />}
                          </button>
                        );
                      })}
                    </div>
                  </div>
                );
              })}

              {/* Quantity Selector & Stock Display */}
              <div className="flex items-center gap-3 text-xs">
                <span className="w-24 text-slate-400 font-medium shrink-0">Số Lượng</span>
                <div className="flex items-center gap-3">
                  <div className="flex items-center border border-slate-200 rounded-lg overflow-hidden bg-white shadow-sm">
                    <button
                      onClick={() => setQuantity((q) => Math.max(1, q - 1))}
                      disabled={quantity <= 1}
                      className="p-2 hover:bg-slate-100 text-slate-600 disabled:opacity-30"
                    >
                      <Minus className="w-3.5 h-3.5" />
                    </button>
                    <input
                      type="number"
                      value={quantity}
                      onChange={(e) => {
                        const val = parseInt(e.target.value) || 1;
                        setQuantity(Math.min(Math.max(1, val), stockAvailable));
                      }}
                      className="w-12 text-center text-xs font-bold text-slate-800 border-x border-slate-200 py-1.5 focus:outline-none"
                    />
                    <button
                      onClick={() => setQuantity((q) => Math.min(stockAvailable, q + 1))}
                      disabled={quantity >= stockAvailable}
                      className="p-2 hover:bg-slate-100 text-slate-600 disabled:opacity-30"
                    >
                      <Plus className="w-3.5 h-3.5" />
                    </button>
                  </div>
                  <span className="text-slate-500">
                    <strong className="text-slate-800">{stockAvailable}</strong> sản phẩm có sẵn
                  </span>
                  <span className="text-[11px] font-mono text-slate-400">
                    (Mã SKU: {currentSku.skuCode})
                  </span>
                </div>
              </div>
            </div>

            {/* Action Buttons: Add to Cart & Buy Now */}
            <div className="flex items-center gap-3 pt-4 border-t border-slate-100">
              <button
                onClick={handleAddToCart}
                className="flex-1 py-3 px-4 bg-sky-50 text-[#0284C7] border-2 border-[#0284C7] hover:bg-sky-100 rounded-xl font-bold text-sm flex items-center justify-center gap-2 transition-all shadow-sm"
              >
                <ShoppingCart className="w-4 h-4" />
                <span>Thêm Vào Giỏ Hàng</span>
              </button>

              <button
                onClick={handleBuyNow}
                className="flex-1 py-3 px-4 bg-gradient-to-r from-[#0284C7] to-[#0369A1] hover:opacity-95 text-white rounded-xl font-bold text-sm flex items-center justify-center gap-2 transition-all shadow-md shadow-sky-600/25"
              >
                <Zap className="w-4 h-4" />
                <span>Mua Ngay</span>
              </button>
            </div>
          </div>
        </div>

        {/* Shop Profile Banner */}
        <div className="bg-white rounded-2xl p-5 shadow-sm border border-slate-200/80 flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
          <div className="flex items-center gap-3.5">
            <img
              src={product.shop.avatarUrl}
              alt={product.shop.shopName}
              className="w-14 h-14 rounded-full border border-slate-200 object-cover"
            />
            <div>
              <div className="flex items-center gap-2">
                <h4 className="font-bold text-slate-900 text-sm">{product.shop.shopName}</h4>
                <span className="bg-[#0284C7] text-white text-[9px] font-black px-1.5 py-0.5 rounded">
                  Chính Hãng
                </span>
              </div>
              <p className="text-xs text-slate-400 mt-0.5">Online 5 phút trước</p>
              <div className="flex items-center gap-2 mt-2">
                <button className="px-3 py-1 bg-sky-50 border border-sky-200 text-[#0284C7] rounded-lg text-xs font-semibold flex items-center gap-1">
                  <MessageSquare className="w-3 h-3" />
                  <span>Chat Ngay</span>
                </button>
                <button className="px-3 py-1 bg-slate-50 border border-slate-200 text-slate-700 rounded-lg text-xs font-semibold flex items-center gap-1">
                  <Store className="w-3 h-3" />
                  <span>Xem Shop</span>
                </button>
              </div>
            </div>
          </div>

          <div className="grid grid-cols-3 gap-6 text-xs text-slate-600 border-t md:border-t-0 md:border-l border-slate-100 pt-3 md:pt-0 md:pl-6">
            <div>
              <span className="text-slate-400 block">Đánh Giá</span>
              <strong className="text-[#0284C7] text-sm">{product.shop.rating} / 5.0</strong>
            </div>
            <div>
              <span className="text-slate-400 block">Sản Phẩm</span>
              <strong className="text-slate-800 text-sm">{product.shop.productsCount}</strong>
            </div>
            <div>
              <span className="text-slate-400 block">Tỉ Lệ Phản Hồi</span>
              <strong className="text-[#0284C7] text-sm">{product.shop.responseRate}</strong>
            </div>
          </div>
        </div>

        {/* Product Specs & Description */}
        <div className="bg-white rounded-2xl p-6 shadow-sm border border-slate-200/80 space-y-6">
          <div className="space-y-3">
            <h3 className="text-sm font-bold text-slate-900 uppercase tracking-wider bg-slate-50 p-2.5 rounded-lg">
              Chi Tiết Sản Phẩm
            </h3>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-y-2.5 text-xs">
              <div className="flex">
                <span className="w-32 text-slate-400 font-medium">Danh Mục:</span>
                <span className="text-[#0284C7] font-semibold">{product.category.name}</span>
              </div>
              <div className="flex">
                <span className="w-32 text-slate-400 font-medium">Thương Hiệu:</span>
                <span className="text-slate-800 font-semibold">{product.shop.shopName}</span>
              </div>
              <div className="flex">
                <span className="w-32 text-slate-400 font-medium">Tình Trạng:</span>
                <span className="text-slate-800">Mới 100% Nguyên Seal</span>
              </div>
              <div className="flex">
                <span className="w-32 text-slate-400 font-medium">Xuất Xứ:</span>
                <span className="text-slate-800">Chính hãng phân phối tại Việt Nam</span>
              </div>
              {product.specs &&
                Object.entries(product.specs).map(([key, val]) => (
                  <div key={key} className="flex">
                    <span className="w-32 text-slate-400 font-medium">{key}:</span>
                    <span className="text-slate-800">{val}</span>
                  </div>
                ))}
            </div>
          </div>

          <div className="space-y-3 pt-4 border-t border-slate-100">
            <h3 className="text-sm font-bold text-slate-900 uppercase tracking-wider bg-slate-50 p-2.5 rounded-lg">
              Mô Tả Sản Phẩm
            </h3>
            <div className="text-xs sm:text-sm text-slate-600 leading-relaxed space-y-3 whitespace-pre-line">
              <p>{product.description}</p>
              <p>
                🛡️ <strong>Cam kết chất lượng từ HeliShop:</strong>
                <br />- 100% sản phẩm chính hãng có hóa đơn điện tử VAT đầy đủ.
                <br />- Bảo hành chính hãng 12 tháng tại các trung tâm ủy quyền toàn quốc.
                <br />- Đổi mới trong vòng 30 ngày nếu phát sinh lỗi phần cứng do nhà sản xuất.
              </p>
            </div>
          </div>
        </div>
      </main>

      <Footer />
    </div>
  );
};
