import React, { useState, useMemo, useEffect } from "react";
import { useParams, useNavigate, Link } from "react-router-dom";
import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/layout/Footer";
import { useCartStore } from "@/store/useCartStore";
import { formatVND, formatCompact } from "@/lib/formatters";
import { Product } from "@/types";
import { catalogService } from "@/services/catalogService";
import {
  Star,
  Truck,
  ShoppingCart,
  Zap,
  Check,
  ChevronRight,
  Store,
  MessageSquare,
  Plus,
  Minus,
  Heart,
  AlertCircle,
  ArrowLeft,
} from "lucide-react";

export const ProductDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { addToCart, loading: cartLoading, error: cartError } = useCartStore();

  const [product, setProduct] = useState<Product | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  // Gallery image state
  const [selectedImageIndex, setSelectedImageIndex] = useState(0);

  // Quantity state
  const [quantity, setQuantity] = useState(1);
  const [isAddedToast, setIsAddedToast] = useState(false);

  useEffect(() => {
    let active = true;
    setIsLoading(true); setProduct(null); setError(null); setSelectedImageIndex(0); setQuantity(1);
    if (!id || !Number.isInteger(Number(id)) || Number(id) < 1) {
      setError("Mã sản phẩm không hợp lệ"); setIsLoading(false); return;
    }
    catalogService.byId(Number(id)).then(data => { if (active) setProduct(data); })
      .catch(error => { if (active) setError(error.response?.data?.message || "Chưa tải được sản phẩm. Vui lòng thử lại."); })
      .finally(() => { if (active) setIsLoading(false); });
    return () => { active = false; };
  }, [id]);

  // Extract variant attribute types (e.g. ["Màu sắc", "Dung lượng"] or ["Màu sắc", "Size"])
  const attributeKeys = useMemo(() => {
    if (!product) return [];
    const keysSet = new Set<string>();
    product.productSkus.forEach((sku) => {
      Object.keys(sku.attributes).forEach((k) => keysSet.add(k));
    });
    return Array.from(keysSet);
  }, [product]);

  // Selected attribute values state
  const [selectedAttributes, setSelectedAttributes] = useState<Record<string, string>>({});

  useEffect(() => {
    if (product && product.productSkus.length > 0) {
      const initial: Record<string, string> = {};
      attributeKeys.forEach((key) => {
        initial[key] = product.productSkus[0].attributes[key] || "";
      });
      setSelectedAttributes(initial);
    }
  }, [product, attributeKeys]);

  // Find active SKU matching all selected attributes
  const currentSku = useMemo(() => {
    if (!product || product.productSkus.length === 0) return null;
    return (
      product.productSkus.find((sku) => {
        return attributeKeys.every((key) => sku.attributes[key] === selectedAttributes[key]);
      }) || null
    );
  }, [product, attributeKeys, selectedAttributes]);

  const activePrice = currentSku ? currentSku.price : product?.price;
  const activeOriginalPrice = currentSku?.originalPrice || product?.originalPrice;
  const stockAvailable = currentSku ? currentSku.stockQuantity : 0;

  const discountPercent =
    activeOriginalPrice && activePrice !== undefined && activeOriginalPrice > activePrice
      ? Math.round(((activeOriginalPrice - activePrice) / activeOriginalPrice) * 100)
      : null;

  // Change attribute selection
  const handleSelectAttribute = (key: string, val: string) => {
    setSelectedAttributes((prev) => ({ ...prev, [key]: val }));
  };

  const handleAddToCart = async () => {
    if (!product || !currentSku || cartLoading || stockAvailable < 1) return false;

    const added = await addToCart(
      {
        skuId: currentSku.id,
        productId: product.id,
        productName: product.name,
        skuCode: currentSku.skuCode,
        attributes: currentSku.attributes,
        price: currentSku.price,
        originalPrice: currentSku.originalPrice,
        stockQuantity: currentSku.stockQuantity,
        imageUrl: currentSku.imageUrl || product.images[0]?.imageUrl || "",
        shopId: product.shop.id,
        shopName: product.shop.shopName,
      },
      quantity
    );

    if (!added) return false;
    setIsAddedToast(true);
    setTimeout(() => setIsAddedToast(false), 2500);
    return true;
  };

  const handleBuyNow = async () => {
    if (await handleAddToCart()) navigate("/checkout");
  };

  if (isLoading) {
    return (
      <div className="min-h-screen bg-[#F5F5FA] flex flex-col">
        <Header />
        <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-6 space-y-6">
          <div className="bg-white rounded-2xl p-8 border border-slate-200/80 animate-pulse grid grid-cols-1 lg:grid-cols-12 gap-8">
            <div className="lg:col-span-5 space-y-4">
              <div className="w-full aspect-square bg-slate-200 rounded-xl" />
              <div className="flex gap-2">
                {[1, 2, 3].map((n) => (
                  <div key={n} className="w-16 h-16 bg-slate-200 rounded-lg" />
                ))}
              </div>
            </div>
            <div className="lg:col-span-7 space-y-4">
              <div className="h-6 bg-slate-200 rounded w-1/4" />
              <div className="h-8 bg-slate-200 rounded w-3/4" />
              <div className="h-16 bg-slate-200 rounded-xl w-full" />
              <div className="h-10 bg-slate-200 rounded w-1/2" />
            </div>
          </div>
        </main>
        <Footer />
      </div>
    );
  }

  if (error || !product) {
    return (
      <div className="min-h-screen bg-[#F5F5FA] flex flex-col">
        <Header />
        <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-12">
          <div className="bg-white rounded-2xl p-12 border border-slate-200/80 text-center max-w-md mx-auto space-y-4 shadow-sm">
            <div className="w-14 h-14 bg-red-50 text-red-500 rounded-full flex items-center justify-center mx-auto">
              <AlertCircle className="w-7 h-7" />
            </div>
            <h3 className="font-bold text-slate-800 text-lg">Không tìm thấy sản phẩm</h3>
            <p className="text-xs text-slate-500 leading-relaxed">
              {error || "Sản phẩm không tồn tại trong cơ sở dữ liệu MySQL hoặc đã bị gỡ khỏi hệ thống."}
            </p>
            <button
              onClick={() => navigate("/")}
              className="inline-flex items-center gap-2 px-5 py-2.5 bg-[#0284C7] hover:bg-[#0369A1] text-white rounded-xl text-xs font-bold transition-colors shadow-sm"
            >
              <ArrowLeft className="w-4 h-4" />
              <span>Quay lại trang chủ</span>
            </button>
          </div>
        </main>
        <Footer />
      </div>
    );
  }

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
              {product.name} ({Object.values(currentSku?.attributes || {}).join(", ")}) x{quantity}
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
                src={product.images[selectedImageIndex]?.imageUrl || product.images[0]?.imageUrl || "/images/product-placeholder.svg"}
                alt={product.name}
                className="absolute inset-0 w-full h-full object-cover object-center group-hover:scale-125 transition-transform duration-300 ease-out"
              />
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

          </div>

          {/* Right Product Buy Information (7 cols) */}
          <div className="lg:col-span-7 space-y-5">
            {/* Title & Badge */}
            <div className="space-y-2">
              <p className="text-xs text-sky-700">{product.shop.shopName}</p>
              <h1 className="text-lg sm:text-xl font-bold text-slate-900 leading-snug">
                {product.name}
              </h1>
            </div>

            <div className="flex items-center gap-4 text-xs text-slate-500 py-2 border-y border-slate-100">
              {product.rating > 0 ? <span className="font-semibold text-amber-600">★ {product.rating} / 5</span> : <span>Chưa có đánh giá</span>}
              {product.reviewCount !== undefined && <span>{formatCompact(product.reviewCount)} đánh giá</span>}
              {product.soldCount !== undefined && <span>Đã bán {formatCompact(product.soldCount)}</span>}
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
                  {activePrice !== undefined ? formatVND(activePrice) : "Chưa có giá"}
                </span>
                {discountPercent && (
                  <span className="bg-[#EF4444] text-white text-xs font-extrabold px-2 py-0.5 rounded-md">
                    -{discountPercent}% GIẢM
                  </span>
                )}
              </div>

              <div className="flex items-center gap-2 text-xs text-[#0369A1] font-semibold pt-1">
                <Zap className="w-4 h-4 text-amber-500 fill-amber-500" />
                <span>Giá theo phiên bản sản phẩm đã chọn.</span>
              </div>
            </div>

            {/* Shipping Info */}
            <div className="space-y-2 text-xs text-slate-600 pt-1">
              <div className="flex items-start gap-3">
                <span className="w-24 text-slate-400 font-medium shrink-0">Vận Chuyển</span>
                <div className="space-y-1">
                  <div className="flex items-center gap-1.5 font-bold text-slate-800">
                    <Truck className="w-4 h-4 text-emerald-600" />
                    <span>Chọn giao hàng tại bước thanh toán</span>
                  </div>
                  <p className="text-slate-500">
                    Phí giao và điều kiện voucher được tính theo địa chỉ và cửa hàng.
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

                if (values.length === 0) return null;

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
                      aria-label="Số lượng sản phẩm"
                      disabled={!currentSku || stockAvailable < 1}
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
                    (Mã SKU: {currentSku?.skuCode || "Chưa chọn biến thể"})
                  </span>
                </div>
              </div>
            </div>

            {/* Action Buttons: Add to Cart & Buy Now */}
            {cartError && <p role="alert" className="text-sm text-red-600">{cartError}</p>}
            <div className="flex items-center gap-3 pt-4 border-t border-slate-100">
              <button
                onClick={handleAddToCart}
                disabled={cartLoading || !currentSku || stockAvailable < 1}
                className="flex-1 py-3 px-4 bg-sky-50 text-[#0284C7] border-2 border-[#0284C7] hover:bg-sky-100 rounded-xl font-bold text-sm flex items-center justify-center gap-2 transition-all shadow-sm"
              >
                <ShoppingCart className="w-4 h-4" />
                <span>Thêm Vào Giỏ Hàng</span>
              </button>

              <button
                onClick={handleBuyNow}
                disabled={cartLoading || !currentSku || stockAvailable < 1}
                className="flex-1 py-3 px-4 bg-gradient-to-r from-[#0284C7] to-[#0369A1] hover:opacity-95 text-white rounded-xl font-bold text-sm flex items-center justify-center gap-2 transition-all shadow-md shadow-sky-600/25"
              >
                <Zap className="w-4 h-4" />
                <span>Mua Ngay</span>
              </button>
            </div>
          </div>
        </div>

        <section className="bg-white rounded-2xl p-5 border flex items-center justify-between gap-4">
          <div className="flex items-center gap-3"><Store className="w-8 h-8 text-sky-600" /><h2 className="font-bold">{product.shop.shopName}</h2></div>
          <Link to={`/?shop=${product.shop.id}`} className="text-sm text-sky-700 border border-sky-200 rounded-lg px-4 py-2">Xem sản phẩm của shop</Link>
        </section>
        {/* Product Description */}
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
                <span className="w-32 text-slate-400 font-medium">Gian hàng:</span>
                <span className="text-slate-800 font-semibold">{product.shop.shopName}</span>
              </div>

            </div>
          </div>

          <div className="space-y-3 pt-4 border-t border-slate-100">
            <h3 className="text-sm font-bold text-slate-900 uppercase tracking-wider bg-slate-50 p-2.5 rounded-lg">
              Mô Tả Sản Phẩm
            </h3>
            <div className="text-xs sm:text-sm text-slate-600 leading-relaxed space-y-3 whitespace-pre-line">
              <p>{product.description}</p>

            </div>
          </div>
        </div>
      </main>

      <Footer />
    </div>
  );
};
