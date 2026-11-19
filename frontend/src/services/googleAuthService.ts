import { api, sessionRequestConfig } from "@/lib/api";
import type { User } from "@/store/useAuthStore";

export interface GoogleAuth {
  accessToken: string; refreshToken: string; userId: number; email: string; fullName: string; role: User["role"];
}
export type GoogleResult = { status: "AUTHENTICATED"; auth: GoogleAuth }
  | { status: "NEW_ACCOUNT" | "LINK_REQUIRED"; ticket: string; email: string; fullName: string };
export const googleAuthService = {
  async config(signal?: AbortSignal): Promise<{ enabled: boolean; clientId?: string }> {
    return (await api.get("/auth/google/config", { ...sessionRequestConfig(), signal })).data.data;
  },
  async challenge(signal?: AbortSignal): Promise<{ nonce: string }> {
    return (await api.post("/auth/google/challenge", {}, { ...sessionRequestConfig(), signal })).data.data;
  },
  async login(credential: string): Promise<GoogleResult> {
    return (await api.post("/auth/google", { credential }, sessionRequestConfig())).data.data;
  },
  async complete(ticket: string, options: { role?: "ROLE_CUSTOMER" | "ROLE_SELLER"; password?: string }): Promise<GoogleResult> {
    return (await api.post("/auth/google/complete", { ticket, role: options.role, password: options.password }, sessionRequestConfig())).data.data;
  },
};
