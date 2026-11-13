import React from "react";
import { Link } from "react-router-dom";
import { Product } from "@/types";
import { formatVND, formatCompact } from "@/lib/formatters";
import { Star, Truck, Heart } from "lucide-react";

interface ProductCardProps {
  product: Product;
}

export const ProductCard: React.FC<ProductCardProps> = ({ product }) => {
  const discountPercent =
    product.originalPrice && product.price !== undefined && product.originalPrice > product.price
      ? Math.round(((product.originalPrice - product.price) / product.originalPrice) * 100)
      : null;

  const thumbnail =
    product.images && product.images.length > 0
      ? product.images[0].imageUrl
      : "/images/product-placeholder.svg";

  return (
    <Link
      to={`/product/${product.id}`}
      className="group bg-white rounded-xl overflow-hidden border border-slate-200/80 hover:border-sky-300 hover:shadow-shopee-hover transition-all duration-200 flex flex-col hover:-translate-y-1"
    >
      {/* 1. Square Image Container (1:1 Ratio) */}
      <div className="relative w-full pt-[100%] bg-slate-100 overflow-hidden">
        <img
          src={thumbnail}
          alt={product.name}
          loading="lazy"
          className="absolute inset-0 w-full h-full object-cover object-center group-hover:scale-105 transition-transform duration-300"
        />

        {/* Badges Overlay */}
        <div className="absolute top-2 left-2 flex flex-col gap-1 z-10">
          {product.isMall && (
            <span className="bg-[#D0011B] text-white font-black text-[10px] px-1.5 py-0.5 rounded shadow-sm">
              MALL
            </span>
          )}
          {product.isFavorite && (
            <span className="bg-[#0284C7] text-white font-bold text-[10px] px-1.5 py-0.5 rounded shadow-sm">
              Yêu thích
            </span>
          )}
        </div>

        {/* Discount Badge Ribbon */}
        {discountPercent && (
          <div className="absolute top-0 right-0 bg-[#EF4444] text-white text-[11px] font-black px-2 py-1 rounded-bl-lg shadow-sm flex flex-col items-center leading-none">
            <span>-{discountPercent}%</span>
            <span className="text-[8px] font-semibold uppercase mt-0.5">Giảm</span>
          </div>
        )}

        {product.stockQuantity === 0 && <span className="absolute bottom-2 left-2 rounded bg-slate-700 text-white px-2 py-1 text-xs">Hết hàng</span>}
      </div>

      {/* 2. Product Information */}
      <div className="p-3 flex-1 flex flex-col justify-between space-y-2">
        {/* Title */}
        <h3
          className="text-xs sm:text-sm font-medium text-slate-800 line-clamp-2 leading-snug group-hover:text-[#0284C7] transition-colors"
          title={product.name}
        >
          {product.name}
        </h3>

        {/* Price & Sold Section */}
        <div className="space-y-1.5 pt-1">
          <div className="flex items-baseline gap-2 flex-wrap">
            <span className="text-sm sm:text-base font-extrabold text-[#0284C7]">
              {product.price !== undefined ? formatVND(product.price) : "Chưa có giá"}
            </span>
            {product.originalPrice && product.price !== undefined && product.originalPrice > product.price && (
              <span className="text-[11px] text-slate-400 line-through">
                {formatVND(product.originalPrice)}
              </span>
            )}
          </div>

          {/* Rating and Sold count */}
          <div className="flex items-center justify-between text-[11px] text-slate-500 pt-1 border-t border-slate-50">
            <div className="flex items-center gap-1">
              <Star className="w-3 h-3 fill-amber-400 text-amber-400" />
              <span className="font-semibold text-slate-700">{product.rating > 0 ? product.rating : "Chưa có đánh giá"}</span>
            </div>
            {product.soldCount !== undefined && <span>Đã bán {formatCompact(product.soldCount)}</span>}
          </div>
        </div>
      </div>
    </Link>
  );
};
