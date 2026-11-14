import { api, sessionRequestConfig } from "@/lib/api";

export interface ShopDto {
  id: number;
  ownerId: number;
  shopName: string;
  description?: string;
  logoUrl?: string;
  bannerUrl?: string;
  status: "ACTIVE" | "INACTIVE" | "BANNED";
  createdAt: string;
}

export interface OpenShopPayload { shopName: string; description?: string }

export const shopService = {
  async mine(): Promise<ShopDto | null> {
    return (await api.get("/shops/me", sessionRequestConfig())).data.data ?? null;
  },
  async open({ shopName, description }: OpenShopPayload): Promise<ShopDto> {
    return (await api.post("/shops/me", { shopName, description }, sessionRequestConfig())).data.data;
  },
};
