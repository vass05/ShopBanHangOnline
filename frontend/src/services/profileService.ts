import { api } from "@/lib/api";

export interface UserProfile {
  id: number;
  email: string;
  fullName: string;
  phone?: string;
  avatarUrl?: string;
  role: string;
  status: string;
  createdAt: string;
  updatedAt: string;
}

export interface UserAddress {
  id: number;
  userId: number;
  recipientName: string;
  phone: string;
  provinceCity: string;
  district: string;
  ward: string;
  streetAddress: string;
  isDefault: boolean;
  fullAddress?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface UpdateProfileData {
  fullName: string;
  phone?: string;
  avatarUrl?: string;
}

export interface ChangePasswordData {
  oldPassword: string;
  newPassword: string;
  confirmPassword: string;
}

export interface AddressData {
  recipientName: string;
  phone: string;
  provinceCity: string;
  district: string;
  ward: string;
  streetAddress: string;
  isDefault?: boolean;
}

export const profileService = {
  async getMyProfile(): Promise<UserProfile> {
    const res = await api.get("/users/me");
    return res.data.data;
  },

  async updateProfile(data: UpdateProfileData): Promise<UserProfile> {
    const res = await api.put("/users/me", data);
    return res.data.data;
  },

  async uploadAvatar(file: File): Promise<UserProfile> {
    const formData = new FormData();
    formData.append("file", file);
    const res = await api.post("/users/me/avatar", formData, {
      headers: {
        "Content-Type": "multipart/form-data",
      },
    });
    return res.data.data;
  },

  async changePassword(data: ChangePasswordData): Promise<string> {
    const res = await api.put("/users/me/password", data);
    return res.data.message || "Đổi mật khẩu thành công";
  },

  async getUserAddresses(): Promise<UserAddress[]> {
    const res = await api.get("/users/me/addresses");
    return res.data.data;
  },

  async addAddress(data: AddressData): Promise<UserAddress> {
    const res = await api.post("/users/me/addresses", data);
    return res.data.data;
  },

  async updateAddress(id: number, data: AddressData): Promise<UserAddress> {
    const res = await api.put(`/users/me/addresses/${id}`, data);
    return res.data.data;
  },

  async deleteAddress(id: number): Promise<void> {
    await api.delete(`/users/me/addresses/${id}`);
  },

  async setDefaultAddress(id: number): Promise<UserAddress> {
    const res = await api.put(`/users/me/addresses/${id}/default`);
    return res.data.data;
  },
};
