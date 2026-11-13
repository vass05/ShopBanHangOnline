import { api } from "@/lib/api";
import type { Category, Product } from "@/types";

export interface ProductDto {
  id: number; name: string; slug: string; description?: string; mainImageUrl?: string;
  price: number | null; rating: number | null; stockQuantity: number; status: string;
  shopId: number; shopName: string; categoryId: number; categoryName: string;
  skus?: { id: number; skuCode: string; price: number; originalPrice?: number; stockQuantity: number; skuAttributes?: string; skuImageUrl?: string }[];
  images?: { id: number; imageUrl: string; isThumbnail: boolean; displayOrder: number }[];
}
export interface CatalogPage { page: number; size: number; totalElements: number; totalPages: number; items: ProductDto[] }
export interface CatalogQuery { page: number; size: number; keyword?: string; categoryId?: number; shopId?: number; minPrice?: number; maxPrice?: number; minRating?: number; sortBy: string; sortDirection: "ASC" | "DESC" }

export function mapProduct(dto: ProductDto): Product {
  const productSkus = (dto.skus || []).filter(sku => sku.id != null && sku.price != null && Number.isFinite(Number(sku.price)) && Number(sku.price) >= 0).map(sku => {
    let attributes: Record<string, string> = {};
    try {
      const parsed = JSON.parse(sku.skuAttributes || "{}");
      if (parsed && typeof parsed === "object" && !Array.isArray(parsed)) attributes = parsed;
    } catch { /* An invalid attribute string does not create substitute SKUs. */ }
    return { id: sku.id, skuCode: sku.skuCode, price: Number(sku.price),
      originalPrice: sku.originalPrice != null ? Number(sku.originalPrice) : undefined,
      stockQuantity: sku.stockQuantity ?? 0, attributes, imageUrl: sku.skuImageUrl || dto.mainImageUrl };
  });
  const price = dto.price != null ? Number(dto.price) : productSkus.length ? Math.min(...productSkus.map(sku => sku.price)) : undefined;
  const representative = productSkus.find(sku => sku.price === price);
  return {
    id: dto.id, name: dto.name, slug: dto.slug, description: dto.description || "", price,
    originalPrice: representative?.originalPrice, rating: Number(dto.rating ?? 0), stockQuantity: dto.stockQuantity,
    category: { id: dto.categoryId, name: dto.categoryName, slug: "" },
    shop: { id: dto.shopId, shopName: dto.shopName },
    images: dto.images?.length ? dto.images : dto.mainImageUrl ? [{ id: dto.id, imageUrl: dto.mainImageUrl, isThumbnail: true, displayOrder: 0 }] : [],
    productSkus,
  };
}

export function mapCategory(dto: Category): Category {
  return { ...dto, children: dto.children?.map(mapCategory) || [] };
}

export const catalogService = {
  async categories(): Promise<Category[]> { return (await api.get("/categories/tree")).data.data.map(mapCategory); },
  async search(query: CatalogQuery): Promise<CatalogPage> { return (await api.get("/products", { params: { ...query, status: "ACTIVE" } })).data.data; },
  async byId(id: number): Promise<Product> { return mapProduct((await api.get(`/products/${id}`)).data.data); },
};
