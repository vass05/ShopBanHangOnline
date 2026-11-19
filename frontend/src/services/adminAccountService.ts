import { api, sessionRequestConfig } from "@/lib/api";

export type AccountStatus = "ACTIVE" | "BLOCKED" | "INACTIVE";
export interface AdminAccount { id: number; email: string; fullName: string; role: "ROLE_CUSTOMER" | "ROLE_SELLER"; status: AccountStatus; createdAt?: string }
export interface AccountPage { items: AdminAccount[]; page: number; size: number; totalElements: number; totalPages: number }
export interface AccountQuery { page: number; keyword?: string; role?: string; status?: string }
export const adminAccountService = {
  async list({ page, keyword, role, status }: AccountQuery): Promise<AccountPage> {
    return (await api.get("/admin/users", { ...sessionRequestConfig(), params: { page, size: 20, keyword, role, status } })).data.data;
  },
  async update(id: number, expectedStatus: AccountStatus, status: "ACTIVE" | "BLOCKED"): Promise<AdminAccount> {
    return (await api.patch(`/admin/users/${id}/status`, { expectedStatus, status }, sessionRequestConfig())).data.data;
  },
};
