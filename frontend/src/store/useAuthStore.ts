import { create } from "zustand";
import { persist } from "zustand/middleware";
import { api } from "@/lib/api";

export interface User {
  id: number;
  fullName: string;
  email: string;
  phone?: string;
  role: "ROLE_CUSTOMER" | "ROLE_SELLER" | "ROLE_ADMIN";
  status: "ACTIVE" | "INACTIVE" | "BLOCKED";
  avatarUrl?: string;
}

interface AuthState {
  user: User | null;
  accessToken: string | null;
  refreshToken: string | null;
  isAuthenticated: boolean;

  setAuth: (payload: { user: User; accessToken: string; refreshToken: string }) => void;
  updateAccessToken: (newAccessToken: string) => void;
  corruptAccessToken: () => void;
  logout: () => Promise<void>;
  clearAuth: () => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      user: null,
      accessToken: localStorage.getItem("accessToken"),
      refreshToken: localStorage.getItem("refreshToken"),
      isAuthenticated: !!localStorage.getItem("accessToken"),

      setAuth: ({ user, accessToken, refreshToken }) => {
        localStorage.setItem("accessToken", accessToken);
        localStorage.setItem("refreshToken", refreshToken);
        api.defaults.headers.common.Authorization = `Bearer ${accessToken}`;

        set({
          user,
          accessToken,
          refreshToken,
          isAuthenticated: true,
        });
      },

      updateAccessToken: (newAccessToken: string) => {
        localStorage.setItem("accessToken", newAccessToken);
        api.defaults.headers.common.Authorization = `Bearer ${newAccessToken}`;

        set({
          accessToken: newAccessToken,
          isAuthenticated: true,
        });
      },

      /**
       * Phương thức phục vụ kiểm thử: Cố tình làm hỏng Access Token để kích hoạt lỗi 401 khi gọi API
       */
      corruptAccessToken: () => {
        const corrupted = "corrupted_token_test_401_simulation";
        localStorage.setItem("accessToken", corrupted);
        api.defaults.headers.common.Authorization = `Bearer ${corrupted}`;
        set({ accessToken: corrupted });
      },

      logout: async () => {
        const { refreshToken } = get();
        try {
          if (refreshToken) {
            await api.post("/auth/logout", { refreshToken });
          }
        } catch {
          // Ignore network logout error
        } finally {
          localStorage.removeItem("accessToken");
          localStorage.removeItem("refreshToken");
          delete api.defaults.headers.common.Authorization;

          set({
            user: null,
            accessToken: null,
            refreshToken: null,
            isAuthenticated: false,
          });
        }
      },

      clearAuth: () => {
        localStorage.removeItem("accessToken");
        localStorage.removeItem("refreshToken");
        delete api.defaults.headers.common.Authorization;

        set({
          user: null,
          accessToken: null,
          refreshToken: null,
          isAuthenticated: false,
        });
      },
    }),
    {
      name: "helishop-auth-storage",
      partialize: (state) => ({
        user: state.user,
        accessToken: state.accessToken,
        refreshToken: state.refreshToken,
        isAuthenticated: state.isAuthenticated,
      }),
    }
  )
);
