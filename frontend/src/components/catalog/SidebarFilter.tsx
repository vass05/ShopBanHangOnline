import React, { useState } from "react";
import { Category } from "@/types";
import { Filter, Star, Check, ChevronDown, ChevronRight, RotateCcw } from "lucide-react";

interface SidebarFilterProps {
  categories: Category[];
  selectedCategoryId: number | null;
  onSelectCategory: (categoryId: number | null) => void;
  priceRange: { min: string; max: string };
  onPriceChange: (min: string, max: string) => void;
  selectedRating: number | null;
  onRatingChange: (rating: number | null) => void;
  onResetFilters: () => void;
}

export const SidebarFilter: React.FC<SidebarFilterProps> = ({
  categories,
  selectedCategoryId,
  onSelectCategory,
  priceRange,
  onPriceChange,
  selectedRating,
  onRatingChange,
  onResetFilters,
}) => {
  const [minPrice, setMinPrice] = useState(priceRange.min);
  const [maxPrice, setMaxPrice] = useState(priceRange.max);
  const [expandedCategories, setExpandedCategories] = useState<Record<number, boolean>>({ 1: true, 2: true });

  const toggleCategoryExpand = (id: number) => {
    setExpandedCategories((prev) => ({ ...prev, [id]: !prev[id] }));
  };

  const handleApplyPrice = (e: React.FormEvent) => {
    e.preventDefault();
    onPriceChange(minPrice, maxPrice);
  };

  return (
    <aside className="bg-white rounded-xl border border-slate-200/80 p-4 space-y-6 text-sm text-slate-700 shadow-sm">
      {/* Filter Header */}
      <div className="flex items-center justify-between pb-3 border-b border-slate-100">
        <div className="flex items-center gap-2 font-bold text-slate-900 uppercase text-xs tracking-wider">
          <Filter className="w-4 h-4 text-[#0284C7]" />
          <span>Bộ Lọc Tìm Kiếm</span>
        </div>
        <button
          onClick={onResetFilters}
          className="text-xs text-[#0284C7] hover:underline flex items-center gap-1 font-medium"
          title="Đặt lại bộ lọc"
        >
          <RotateCcw className="w-3 h-3" />
          <span>Xóa hết</span>
        </button>
      </div>

      {/* 1. Category Tree Filter */}
      <div className="space-y-2">
        <h4 className="font-bold text-slate-900 text-xs uppercase tracking-wider">Tất Cả Danh Mục</h4>
        <div className="space-y-1">
          <div
            onClick={() => onSelectCategory(null)}
            className={`px-2 py-1.5 rounded-lg cursor-pointer flex items-center justify-between text-xs font-semibold transition-colors ${
              selectedCategoryId === null
                ? "bg-sky-50 text-[#0284C7] font-bold"
                : "hover:bg-slate-50 text-slate-700"
            }`}
          >
            <span>Toàn bộ sản phẩm</span>
            {selectedCategoryId === null && <Check className="w-3.5 h-3.5" />}
          </div>

          {categories.map((cat) => {
            const hasChildren = cat.children && cat.children.length > 0;
            const isSelected = selectedCategoryId === cat.id;
            const isExpanded = expandedCategories[cat.id];

            return (
              <div key={cat.id} className="space-y-1">
                <div
                  className={`px-2 py-1.5 rounded-lg cursor-pointer flex items-center justify-between text-xs transition-colors ${
                    isSelected
                      ? "bg-sky-50 text-[#0284C7] font-bold"
                      : "hover:bg-slate-50 text-slate-700"
                  }`}
                >
                  <span onClick={() => onSelectCategory(cat.id)} className="flex-1">
                    {cat.name}
                  </span>
                  {hasChildren && (
                    <button
                      type="button"
                      onClick={(e) => {
                        e.stopPropagation();
                        toggleCategoryExpand(cat.id);
                      }}
                      className="p-1 text-slate-400 hover:text-slate-600"
                    >
                      {isExpanded ? <ChevronDown className="w-3 h-3" /> : <ChevronRight className="w-3 h-3" />}
                    </button>
                  )}
                </div>

                {/* Subcategories */}
                {hasChildren && isExpanded && (
                  <div className="pl-4 space-y-1 border-l-2 border-slate-100 ml-2">
                    {cat.children!.map((sub) => {
                      const isSubSelected = selectedCategoryId === sub.id;
                      return (
                        <div
                          key={sub.id}
                          onClick={() => onSelectCategory(sub.id)}
                          className={`px-2 py-1 rounded-lg cursor-pointer text-xs transition-colors flex items-center justify-between ${
                            isSubSelected
                              ? "bg-sky-50 text-[#0284C7] font-bold"
                              : "hover:bg-slate-50 text-slate-600"
                          }`}
                        >
                          <span>{sub.name}</span>
                          {isSubSelected && <Check className="w-3 h-3" />}
                        </div>
                      );
                    })}
                  </div>
                )}
              </div>
            );
          })}
        </div>
      </div>

      {/* 2. Price Range Filter */}
      <div className="space-y-3 pt-3 border-t border-slate-100">
        <h4 className="font-bold text-slate-900 text-xs uppercase tracking-wider">Khoảng Giá (₫)</h4>
        <form onSubmit={handleApplyPrice} className="space-y-2.5">
          <div className="flex items-center gap-2">
            <input
              type="number"
              placeholder="₫ TỪ"
              value={minPrice}
              onChange={(e) => setMinPrice(e.target.value)}
              className="w-full bg-slate-50 border border-slate-200 rounded-lg px-2.5 py-1.5 text-xs text-slate-800 placeholder:text-slate-400 focus:outline-none focus:ring-1 focus:ring-[#0284C7]"
            />
            <span className="text-slate-400 font-bold">-</span>
            <input
              type="number"
              placeholder="₫ ĐẾN"
              value={maxPrice}
              onChange={(e) => setMaxPrice(e.target.value)}
              className="w-full bg-slate-50 border border-slate-200 rounded-lg px-2.5 py-1.5 text-xs text-slate-800 placeholder:text-slate-400 focus:outline-none focus:ring-1 focus:ring-[#0284C7]"
            />
          </div>
          <button
            type="submit"
            className="w-full py-1.5 bg-[#0284C7] hover:bg-[#0369A1] text-white rounded-lg text-xs font-semibold shadow-sm transition-colors uppercase"
          >
            Áp Dụng
          </button>
        </form>
      </div>

      {/* 3. Star Rating Filter */}
      <div className="space-y-2 pt-3 border-t border-slate-100">
        <h4 className="font-bold text-slate-900 text-xs uppercase tracking-wider">Đánh Giá</h4>
        <div className="space-y-1">
          {[5, 4, 3].map((starCount) => {
            const isSelected = selectedRating === starCount;
            return (
              <div
                key={starCount}
                onClick={() => onRatingChange(isSelected ? null : starCount)}
                className={`px-2 py-1.5 rounded-lg cursor-pointer flex items-center justify-between text-xs transition-colors ${
                  isSelected ? "bg-sky-50 text-[#0284C7] font-bold" : "hover:bg-slate-50"
                }`}
              >
                <div className="flex items-center gap-1.5">
                  <div className="flex items-center">
                    {Array.from({ length: 5 }).map((_, idx) => (
                      <Star
                        key={idx}
                        className={`w-3.5 h-3.5 ${
                          idx < starCount
                            ? "fill-amber-400 text-amber-400"
                            : "fill-slate-200 text-slate-200"
                        }`}
                      />
                    ))}
                  </div>
                  {starCount < 5 && <span className="text-[11px] text-slate-500">trở lên</span>}
                </div>
                {isSelected && <Check className="w-3.5 h-3.5" />}
              </div>
            );
          })}
        </div>
      </div>

      {/* 4. Service & Promotion Tags */}
      <div className="space-y-2 pt-3 border-t border-slate-100">
        <h4 className="font-bold text-slate-900 text-xs uppercase tracking-wider">Dịch Vụ & Khuyến Mãi</h4>
        <div className="space-y-1.5 text-xs text-slate-600">
          <label className="flex items-center gap-2 cursor-pointer hover:text-[#0284C7]">
            <input type="checkbox" className="rounded border-slate-300 text-[#0284C7] focus:ring-[#0284C7]" />
            <span>Freeship Xtra</span>
          </label>
          <label className="flex items-center gap-2 cursor-pointer hover:text-[#0284C7]">
            <input type="checkbox" className="rounded border-slate-300 text-[#0284C7] focus:ring-[#0284C7]" />
            <span>Chính hãng HeliMall</span>
          </label>
          <label className="flex items-center gap-2 cursor-pointer hover:text-[#0284C7]">
            <input type="checkbox" className="rounded border-slate-300 text-[#0284C7] focus:ring-[#0284C7]" />
            <span>Đang giảm giá Flash Sale</span>
          </label>
        </div>
      </div>
    </aside>
  );
};
