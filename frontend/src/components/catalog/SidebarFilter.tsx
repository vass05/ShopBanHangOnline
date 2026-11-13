import React, { useState, useEffect } from "react";
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

function containsCategory(category: Category, id: number): boolean {
  return category.id === id || (category.children || []).some(child => containsCategory(child, id));
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
  const [expandedCategories, setExpandedCategories] = useState<Record<number, boolean>>({ 1: true, 2: true, 3: true, 4: true, 5: true });

  // Sync price inputs when priceRange is reset externally
  useEffect(() => {
    setMinPrice(priceRange.min);
    setMaxPrice(priceRange.max);
  }, [priceRange.min, priceRange.max]);

  useEffect(() => {
    if (!selectedCategoryId) return;
    const expanded: Record<number, boolean> = {};
    const walk = (list: Category[]) => list.forEach(category => {
      if (containsCategory(category, selectedCategoryId)) {
        expanded[category.id] = true;
        walk(category.children || []);
      }
    });
    walk(categories);
    setExpandedCategories(previous => ({ ...previous, ...expanded }));
  }, [selectedCategoryId, categories]);

  const toggleCategoryExpand = (id: number) => {
    setExpandedCategories((prev) => ({ ...prev, [id]: !prev[id] }));
  };

  const handleApplyPrice = (e: React.FormEvent) => {
    e.preventDefault();
    onPriceChange(minPrice, maxPrice);
  };

  const renderCategory = (category: Category): React.ReactNode => (
    <div key={category.id} className="space-y-1">
      <div className={`flex items-center rounded-lg text-xs ${selectedCategoryId === category.id ? "bg-sky-50 text-sky-700 font-bold" : "text-slate-700 hover:bg-slate-50"}`}>
        <button type="button" onClick={() => onSelectCategory(selectedCategoryId === category.id ? null : category.id)} className="flex-1 text-left px-2 py-2">{category.name}</button>
        {!!category.children?.length && <button type="button" aria-label={`Mở danh mục ${category.name}`} onClick={() => toggleCategoryExpand(category.id)} className="p-2">{expandedCategories[category.id] ? <ChevronDown className="w-3 h-3" /> : <ChevronRight className="w-3 h-3" />}</button>}
      </div>
      {!!category.children?.length && expandedCategories[category.id] && <div className="pl-3 border-l ml-2">{category.children.map(renderCategory)}</div>}
    </div>
  );

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

          {categories.map(renderCategory)}
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

    </aside>
  );
};
