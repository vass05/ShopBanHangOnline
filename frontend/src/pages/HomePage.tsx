import React, { useState, useMemo, useEffect } from "react";
import { useSearchParams } from "react-router-dom";
import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/layout/Footer";
import { ProductCard } from "@/components/catalog/ProductCard";
import { SidebarFilter } from "@/components/catalog/SidebarFilter";
import { Product, Category } from "@/types";
import { catalogService, mapProduct } from "@/services/catalogService";
import {
  Sparkles,
  ChevronLeft,
  ChevronRight,
  Flame,
  Smartphone,
  Laptop,
  Shirt,
  Tv,
  Watch,
  Headphones,
  Gamepad2,
  RefreshCw,
  ServerOff,
} from "lucide-react";

export const HomePage: React.FC = () => {
  const [searchParams, setSearchParams] = useSearchParams();
  const searchKeyword = searchParams.get("q") || "";

  // Live Backend State
  const [products, setProducts] = useState<Product[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [isBackendConnected, setIsBackendConnected] = useState<boolean>(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // Filter States
  const [selectedCategoryId, setSelectedCategoryId] = useState<number | null>(null);
  const [priceRange, setPriceRange] = useState({ min: "", max: "" });
  const [selectedRating, setSelectedRating] = useState<number | null>(null);
  const [sortBy, setSortBy] = useState<"popular" | "latest" | "price-asc" | "price-desc">("popular");

  // Hero Carousel State
  const [activeBanner, setActiveBanner] = useState(0);

  const [page, setPage] = useState(1);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [reload, setReload] = useState(0);
  const [categoryError, setCategoryError] = useState("");
  const shopId = Number(searchParams.get("shop")) || undefined;
  const fetchCatalogData = () => setReload(value => value + 1);

  useEffect(() => {
    let active = true;
    catalogService.categories().then(data => { if (active) { setCategories(data); setCategoryError(""); } })
      .catch(() => { if (active) setCategoryError("Chưa tải được danh mục. Bạn vẫn có thể tìm sản phẩm và thử lại."); });
    return () => { active = false; };
  }, [reload]);

  useEffect(() => {
    const value = Number(searchParams.get("category"));
    setSelectedCategoryId(Number.isInteger(value) && value > 0 ? value : null);
  }, [searchParams]);

  useEffect(() => { setPage(1); }, [searchKeyword, selectedCategoryId, shopId, priceRange.min, priceRange.max, selectedRating, sortBy]);

  useEffect(() => {
    let active = true;
    setIsLoading(true); setErrorMessage(null); setProducts([]);
    const timer = setTimeout(() => {
      catalogService.search({ page, size: 20, keyword: searchKeyword || undefined,
        categoryId: selectedCategoryId || undefined, shopId,
        minPrice: priceRange.min !== "" ? Number(priceRange.min) : undefined,
        maxPrice: priceRange.max !== "" ? Number(priceRange.max) : undefined,
        minRating: selectedRating || undefined,
        sortBy: sortBy.startsWith("price") ? "price" : sortBy === "latest" ? "createdAt" : "rating",
        sortDirection: sortBy === "price-asc" ? "ASC" : "DESC",
      }).then(data => {
        if (!active) return;
        setProducts(data.items.map(mapProduct)); setTotalElements(data.totalElements); setTotalPages(data.totalPages);
        setIsBackendConnected(true);
      }).catch(error => {
        if (!active) return;
        setIsBackendConnected(false);
        setErrorMessage(error.response?.data?.message || "Chưa tải được sản phẩm. Vui lòng thử lại sau.");
      }).finally(() => { if (active) setIsLoading(false); });
    }, 150);
    return () => { active = false; clearTimeout(timer); };
  }, [page, searchKeyword, selectedCategoryId, shopId, priceRange.min, priceRange.max, selectedRating, sortBy, reload]);

  const handleSelectCategory = (catId: number | null) => {
    setSelectedCategoryId(catId);
    const newParams = new URLSearchParams(searchParams);
    if (catId) {
      newParams.set("category", String(catId));
    } else {
      newParams.delete("category");
    }
    setSearchParams(newParams, { replace: true });
  };

  const BANNERS = [
    {
      id: 1,
      title: "HeliShop Super Brand Day",
      subtitle: "Khám phá thiết bị và phụ kiện từ các cửa hàng trên HeliShop",
      badge: "KHÁM PHÁ HELISHOP",
      bgColor: "from-[#0284C7] via-[#0369A1] to-[#0C4A6E]",
      tag: "Khám phá sản phẩm",
    },
    {
      id: 2,
      title: "Lễ Hội Thanh Toán VNPAY",
      subtitle: "Thanh toán COD hoặc VNPAY theo từng đơn hàng",
      badge: "THANH TOÁN THEO ĐƠN",
      bgColor: "from-[#0369A1] via-[#0284C7] to-[#0EA5E9]",
      tag: "Lựa chọn thanh toán",
    },
    {
      id: 3,
      title: "Chọn cách giao hàng phù hợp",
      subtitle: "Xem phí giao theo từng cửa hàng ở bước thanh toán",
      badge: "LỰA CHỌN GIAO HÀNG",
      bgColor: "from-[#075985] via-[#0284C7] to-[#38BDF8]",
      tag: "Giao hàng theo đơn",
    },
  ];

  // Find active category name for visual feedback
  const activeCategoryName = useMemo(() => {
    if (!selectedCategoryId) return null;
    const findInTree = (list: Category[]): string | null => {
      for (const cat of list) {
        if (cat.id === selectedCategoryId) return cat.name;
        if (cat.children) {
          const found = findInTree(cat.children);
          if (found) return found;
        }
      }
      return null;
    };
    return findInTree(categories);
  }, [selectedCategoryId, categories]);

  const filteredProducts = products;

  const handleResetFilters = () => {
    setSelectedCategoryId(null);
    setPriceRange({ min: "", max: "" });
    setSelectedRating(null);
    setSortBy("popular");
    setSearchParams({});
  };

  const getCategoryIcon = (slug: string) => {
    switch (slug) {
      case "dien-thoai-phu-kien":
      case "dien-thoai-thong-minh":
        return <Smartphone className="w-5 h-5 text-[#0284C7]" />;
      case "may-tinh-laptop":
      case "laptop-gaming-do-hoa":
        return <Laptop className="w-5 h-5 text-indigo-600" />;
      case "thiet-bi-am-thanh":
      case "tai-nghe-chong-on-hi-res":
        return <Headphones className="w-5 h-5 text-purple-600" />;
      case "ban-phim-chuot-gaming":
      case "ban-phim-co-custom":
        return <Gamepad2 className="w-5 h-5 text-amber-600" />;
      case "thoi-trang-nam":
      case "ao-polo-ao-thun-cong-nghe":
        return <Shirt className="w-5 h-5 text-rose-500" />;
      case "dien-gia-dung":
        return <Tv className="w-5 h-5 text-amber-500" />;
      default:
        return <Watch className="w-5 h-5 text-emerald-500" />;
    }
  };

  return (
    <div className="min-h-screen bg-[#F5F5FA] flex flex-col">
      <Header />

      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-6 space-y-6">
        {/* 1. HERO PROMOTION CAROUSEL BANNER */}
        <div className="relative rounded-2xl overflow-hidden shadow-md text-white">
          <div
            className={`p-8 sm:p-10 bg-gradient-to-r ${BANNERS[activeBanner].bgColor} transition-all duration-500 flex flex-col md:flex-row items-start md:items-center justify-between gap-6 min-h-[200px]`}
          >
            <div className="space-y-2 max-w-xl">
              <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-white/20 backdrop-blur-sm text-xs font-black tracking-wider uppercase border border-white/30">
                <Sparkles className="w-3.5 h-3.5 text-sky-200" />
                <span>{BANNERS[activeBanner].badge}</span>
              </div>
              <h2 className="text-2xl sm:text-3xl font-extrabold tracking-tight">
                {BANNERS[activeBanner].title}
              </h2>
              <p className="text-sky-100 text-xs sm:text-sm font-medium">
                {BANNERS[activeBanner].subtitle}
              </p>
            </div>

            <div className="shrink-0 flex items-center gap-3">
              <div className="p-3 bg-white/10 backdrop-blur-md rounded-xl border border-white/20 text-center">
                <span className="block text-[11px] font-bold text-sky-200 uppercase">Ưu đãi hôm nay</span>
                <span className="text-lg font-black text-white">{BANNERS[activeBanner].tag}</span>
              </div>
            </div>
          </div>

          {/* Carousel navigation arrows & dots */}
          <button
            onClick={() => setActiveBanner((prev) => (prev === 0 ? BANNERS.length - 1 : prev - 1))}
            className="absolute left-3 top-1/2 -translate-y-1/2 p-2 rounded-full bg-black/20 hover:bg-black/40 text-white backdrop-blur-sm transition-colors"
          >
            <ChevronLeft className="w-5 h-5" />
          </button>
          <button
            onClick={() => setActiveBanner((prev) => (prev === BANNERS.length - 1 ? 0 : prev + 1))}
            className="absolute right-3 top-1/2 -translate-y-1/2 p-2 rounded-full bg-black/20 hover:bg-black/40 text-white backdrop-blur-sm transition-colors"
          >
            <ChevronRight className="w-5 h-5" />
          </button>

          <div className="absolute bottom-3 left-1/2 -translate-x-1/2 flex items-center gap-1.5">
            {BANNERS.map((_, idx) => (
              <button
                key={idx}
                onClick={() => setActiveBanner(idx)}
                className={`h-2 rounded-full transition-all ${
                  activeBanner === idx ? "w-6 bg-white" : "w-2 bg-white/40"
                }`}
              />
            ))}
          </div>
        </div>

        {/* 2. TOP CATEGORIES STRIP */}
        {categories.length > 0 && (
          <div className="bg-white rounded-xl p-4 shadow-sm border border-slate-200/80">
            <div className="flex items-center justify-between mb-3 pb-2 border-b border-slate-100">
              <div className="flex items-center gap-2">
                <Flame className="w-4 h-4 text-[#0284C7]" />
                <h3 className="font-bold text-xs uppercase tracking-wider text-slate-800">
                  Danh Mục Nổi Bật
                </h3>
              </div>
              {selectedCategoryId && (
                <button
                  onClick={() => handleSelectCategory(null)}
                  className="text-xs text-[#0284C7] hover:underline flex items-center gap-1 font-semibold transition-colors"
                >
                  <RefreshCw className="w-3 h-3" />
                  <span>Xem tất cả sản phẩm</span>
                </button>
              )}
            </div>
            <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-5 gap-3">
              {categories.map((cat) => {
                const isSelected =
                  selectedCategoryId === cat.id ||
                  cat.children?.some((ch) => ch.id === selectedCategoryId);
                return (
                  <div
                    key={cat.id}
                    onClick={() =>
                      handleSelectCategory(selectedCategoryId === cat.id ? null : cat.id)
                    }
                    className={`flex items-center gap-3 p-3 rounded-xl border transition-all cursor-pointer ${
                      isSelected
                        ? "border-[#0284C7] bg-sky-50 shadow-sm ring-2 ring-sky-200"
                        : "border-slate-100 hover:border-sky-200 hover:bg-slate-50"
                    }`}
                  >
                    <div
                      className={`w-10 h-10 rounded-lg flex items-center justify-center shrink-0 border transition-colors ${
                        isSelected
                          ? "bg-[#0284C7]/15 border-[#0284C7]/30 text-[#0284C7]"
                          : "bg-white shadow-xs border-slate-100 text-slate-600"
                      }`}
                    >
                      {getCategoryIcon(cat.slug)}
                    </div>
                    <div className="flex flex-col min-w-0">
                      <span
                        className={`text-xs font-semibold leading-tight line-clamp-2 ${
                          isSelected ? "text-[#0284C7] font-bold" : "text-slate-700"
                        }`}
                      >
                        {cat.name}
                      </span>
                      {isSelected && (
                        <span className="text-[10px] text-[#0284C7] font-semibold mt-0.5">
                          ✓ Đang chọn
                        </span>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        )}

        {/* 3. MAIN CATALOG SECTION (2 COLUMNS: SIDEBAR FILTER & PRODUCT GRID) */}
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
          {/* Left Sidebar Filter (3 cols) */}
          <div className="lg:col-span-3">
            {categoryError && <p role="alert" className="text-sm text-amber-700 mb-3">{categoryError}</p>}
            <SidebarFilter
              categories={categories}
              selectedCategoryId={selectedCategoryId}
              onSelectCategory={handleSelectCategory}
              priceRange={priceRange}
              onPriceChange={(min, max) => setPriceRange({ min, max })}
              selectedRating={selectedRating}
              onRatingChange={setSelectedRating}
              onResetFilters={handleResetFilters}
            />
          </div>

          {/* Right Product Grid & Sorting Toolbar (9 cols) */}
          <div className="lg:col-span-9 space-y-4">
            {/* Sorting Toolbar (Shopee-style) */}
            <div className="bg-slate-100/90 rounded-xl p-3 flex flex-wrap items-center justify-between gap-3 border border-slate-200/80">
              <div className="flex items-center gap-2 flex-wrap">
                <span className="text-xs font-bold text-slate-600 mr-1">Sắp xếp theo:</span>

                <button
                  onClick={() => setSortBy("popular")}
                  className={`px-4 py-1.5 rounded-lg text-xs font-semibold transition-colors shadow-sm ${
                    sortBy === "popular"
                      ? "bg-[#0284C7] text-white"
                      : "bg-white text-slate-700 hover:bg-slate-50"
                  }`}
                >
                  Phổ Biến
                </button>

                <button
                  onClick={() => setSortBy("latest")}
                  className={`px-4 py-1.5 rounded-lg text-xs font-semibold transition-colors shadow-sm ${
                    sortBy === "latest"
                      ? "bg-[#0284C7] text-white"
                      : "bg-white text-slate-700 hover:bg-slate-50"
                  }`}
                >
                  Mới Nhất
                </button>

{/* Price Sort Dropdown */}
                <div className="relative inline-block">
                  <select
                    value={sortBy === "price-asc" || sortBy === "price-desc" ? sortBy : ""}
                    onChange={(e) => setSortBy(e.target.value as any)}
                    className="bg-white text-slate-700 border-0 rounded-lg px-3 py-1.5 text-xs font-semibold shadow-sm focus:outline-none focus:ring-1 focus:ring-[#0284C7] cursor-pointer"
                  >
                    <option value="" disabled>Giá</option>
                    <option value="price-asc">Giá: Thấp đến Cao</option>
                    <option value="price-desc">Giá: Cao đến Thấp</option>
                  </select>
                </div>
              </div>

              {/* Product count stats */}
              <div className="text-xs text-slate-500 font-medium">
                Tổng <strong className="text-slate-800">{totalElements}</strong> sản phẩm · Trang {page}/{Math.max(1, totalPages)}
              </div>
            </div>

            {/* Active Category Filter Indicator */}
            {selectedCategoryId && activeCategoryName && (
              <div className="text-xs text-slate-700 bg-sky-50/90 border border-sky-200 px-3.5 py-2.5 rounded-xl flex items-center justify-between shadow-xs">
                <div className="flex items-center gap-2">
                  <span className="text-slate-500 font-medium">Đang lọc theo danh mục:</span>
                  <span className="font-bold text-[#0284C7] bg-white px-2.5 py-0.5 rounded-md border border-sky-200 shadow-xs">
                    {activeCategoryName}
                  </span>
                  <span className="text-slate-400 font-medium">({totalElements} sản phẩm)</span>
                </div>
                <button
                  onClick={() => handleSelectCategory(null)}
                  className="inline-flex items-center gap-1 text-slate-500 hover:text-red-600 font-semibold transition-colors text-xs"
                >
                  <span>✕ Bỏ chọn danh mục</span>
                </button>
              </div>
            )}

            {/* Keyword Search Indicator */}
            {searchKeyword && (
              <div className="text-xs text-slate-600 bg-white p-2.5 rounded-lg border border-slate-200 flex items-center justify-between">
                <span>
                  Kết quả tìm kiếm cho từ khóa: <strong className="text-[#0284C7]">"{searchKeyword}"</strong>
                </span>
                <button
                  onClick={() => (window.location.href = "/")}
                  className="text-slate-400 hover:text-slate-600 font-bold"
                >
                  ✕ Xóa
                </button>
              </div>
            )}

            {/* Loading Skeleton */}
            {isLoading && (
              <div className="grid grid-cols-2 sm:grid-cols-3 xl:grid-cols-4 gap-3.5 sm:gap-4">
                {[1, 2, 3, 4, 5, 6, 7, 8].map((n) => (
                  <div key={n} className="bg-white rounded-xl p-3 border border-slate-200/80 animate-pulse space-y-3">
                    <div className="w-full aspect-square bg-slate-200 rounded-lg" />
                    <div className="h-4 bg-slate-200 rounded w-3/4" />
                    <div className="h-4 bg-slate-200 rounded w-1/2" />
                    <div className="h-6 bg-slate-200 rounded w-2/3" />
                  </div>
                ))}
              </div>
            )}

            {/* Backend Offline / Connection Error Banner */}
            {!isLoading && !isBackendConnected && (
              <div className="bg-amber-50 border border-amber-200 rounded-2xl p-8 text-center space-y-4">
                <div className="w-14 h-14 bg-amber-100 text-amber-600 rounded-full flex items-center justify-center mx-auto">
                  <ServerOff className="w-7 h-7" />
                </div>
                <div className="space-y-1 max-w-md mx-auto" role="alert">
                  <h4 className="font-bold text-slate-900 text-base">Chưa tải được sản phẩm</h4>
                  <p className="text-sm text-slate-600">{errorMessage || "Vui lòng thử lại sau."}</p>
                </div>
                <button
                  onClick={fetchCatalogData}
                  className="inline-flex items-center gap-2 px-5 py-2.5 bg-[#0284C7] hover:bg-[#0369A1] text-white rounded-xl text-xs font-bold transition-colors shadow-sm"
                >
                  <RefreshCw className="w-4 h-4" />
                  <span>Thử kết nối lại</span>
                </button>
              </div>
            )}

            {/* Empty Products List */}
            {!isLoading && isBackendConnected && filteredProducts.length === 0 && (
              <div className="bg-white rounded-xl p-12 text-center border border-slate-200/80 space-y-3">
                <div className="w-16 h-16 rounded-full bg-sky-50 text-[#0284C7] flex items-center justify-center mx-auto">
                  <Sparkles className="w-8 h-8 opacity-40" />
                </div>
                <h4 className="font-bold text-slate-800 text-base">
                  {selectedCategoryId
                    ? `Không tìm thấy sản phẩm nào trong danh mục "${activeCategoryName || ""}"`
                    : "Không tìm thấy sản phẩm phù hợp"}
                </h4>
                <p className="text-xs text-slate-500 max-w-sm mx-auto">
                  Hãy thử thay đổi từ khóa tìm kiếm hoặc chọn danh mục khác để xem thêm nhiều sản phẩm.
                </p>
                <button
                  onClick={handleResetFilters}
                  className="px-4 py-2 bg-[#0284C7] text-white rounded-lg text-xs font-semibold shadow-sm hover:bg-[#0369A1] transition-colors"
                >
                  Xem tất cả sản phẩm
                </button>
              </div>
            )}

            {/* Products Grid */}
            {!isLoading && filteredProducts.length > 0 && (
              <div className="grid grid-cols-2 sm:grid-cols-3 xl:grid-cols-4 gap-3.5 sm:gap-4">
                {filteredProducts.map((product) => (
                  <ProductCard key={product.id} product={product} />
                ))}
              </div>
            )}
            {!isLoading && isBackendConnected && totalPages > 1 && <nav aria-label="Phân trang sản phẩm" className="flex justify-center items-center gap-4 pt-4">
              <button disabled={page <= 1} onClick={() => setPage(value => value - 1)} className="border rounded-lg px-4 py-2 disabled:opacity-40">Trang trước</button>
              <span className="text-sm">{page} / {totalPages}</span>
              <button disabled={page >= totalPages} onClick={() => setPage(value => value + 1)} className="border rounded-lg px-4 py-2 disabled:opacity-40">Trang sau</button>
            </nav>}
          </div>
        </div>
      </main>

      <Footer />
    </div>
  );
};
