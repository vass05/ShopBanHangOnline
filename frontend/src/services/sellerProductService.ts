import { api, sessionRequestConfig } from "@/lib/api";
import type { ProductDto } from "@/services/catalogService";

export type ProductStatus = "DRAFT" | "ACTIVE" | "INACTIVE" | "OUT_OF_STOCK";
export interface SellerSku {
  id: number; skuCode: string; price: number; originalPrice?: number | null; stockQuantity: number;
  skuAttributes?: string; skuImageUrl?: string | null; version: number;
}
export interface SellerProduct extends Omit<ProductDto, "skus" | "status"> { status: ProductStatus; skus: SellerSku[] }
export interface SellerProductPage { page: number; size: number; totalPages: number; totalElements: number; items: SellerProduct[] }
export interface ProductPayload { name: string; categoryId: number; description?: string; mainImageUrl?: string; status: ProductStatus }
export interface SkuPayload {
  skuCode: string; price: number; originalPrice?: number; stockQuantity: number;
  attributes: Record<string, string>; skuImageUrl?: string; version?: number;
}
const metadata = ({ name, categoryId, description, mainImageUrl, status }: ProductPayload) => ({ name, categoryId, description, mainImageUrl, status });
const skuFields = ({ skuCode, price, originalPrice, stockQuantity, attributes, skuImageUrl, version }: SkuPayload) => ({ skuCode, price, originalPrice, stockQuantity, attributes, skuImageUrl, version });
export const sellerProductService = {
  async list(query: { page: number; keyword?: string; status?: ProductStatus }): Promise<SellerProductPage> {
    return (await api.get("/seller/products", { ...sessionRequestConfig(), params: { page: query.page, size: 20, keyword: query.keyword, status: query.status } })).data.data;
  },
  async detail(id: number): Promise<SellerProduct> { return (await api.get(`/seller/products/${id}`, sessionRequestConfig())).data.data; },
  async create(product: ProductPayload, skus: SkuPayload[]): Promise<SellerProduct> {
    return (await api.post("/seller/products", { product: metadata(product), skus: skus.map(skuFields) }, sessionRequestConfig())).data.data;
  },
  async update(id: number, product: ProductPayload): Promise<SellerProduct> {
    return (await api.put(`/seller/products/${id}`, metadata(product), sessionRequestConfig())).data.data;
  },
  async saveSku(id: number, skuId: number | null, sku: SkuPayload): Promise<SellerProduct> {
    const path = `/seller/products/${id}/skus${skuId == null ? "" : `/${skuId}`}`;
    const config = sessionRequestConfig();
    return (await (skuId == null ? api.post(path, skuFields(sku), config) : api.put(path, skuFields(sku), config))).data.data;
  },
};
