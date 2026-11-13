import axios from "axios";
import type { AxiosError, InternalAxiosRequestConfig } from "axios";
import { readAuthSession, writeAuthSession, updateSessionAccessToken } from "@/lib/authSession";

export type InterceptorLogCallback = (log: {
  timestamp: string;
  type: "401_DETECTED" | "QUEUE_WAIT" | "REFRESHING" | "QUEUE_RESOLVED" | "REFRESH_SUCCESS" | "REFRESH_FAILED" | "REQUEST";
  message: string;
  details?: Record<string, unknown>;
}) => void;

const listeners = new Set<InterceptorLogCallback>();
export function registerInterceptorLogger(callback: InterceptorLogCallback) {
  listeners.add(callback);
  return () => { listeners.delete(callback); };
}
const notifyLog = (type: Parameters<InterceptorLogCallback>[0]["type"], message: string) => {
  const timestamp = new Date().toLocaleTimeString("vi-VN", { hour12: false });
  listeners.forEach(listener => listener({ timestamp, type, message }));
};

const baseURL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api/v1";
export const api = axios.create({ baseURL, timeout: 15000 });
const refreshClient = axios.create({ baseURL, timeout: 15000 });
let refreshPromise: Promise<string> | null = null;
type SessionRequest = InternalAxiosRequestConfig & { _retry?: boolean; _sessionRefreshToken?: string | null };

export const getStoredAccessToken = () => readAuthSession()?.accessToken || null;
export const getStoredRefreshToken = () => readAuthSession()?.refreshToken || null;
export const setStoredAccessToken = updateSessionAccessToken;
export const clearStoredAuthTokens = () => writeAuthSession(null);

api.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const session = readAuthSession();
  const request = config as SessionRequest;
  if (!("_sessionRefreshToken" in request)) request._sessionRefreshToken = session?.refreshToken || null;
  const token = session?.accessToken;
  if (token) config.headers.Authorization = `Bearer ${token}`;
  else delete config.headers.Authorization;
  notifyLog("REQUEST", `${config.method?.toUpperCase()} ${config.url}`);
  return config;
});

api.interceptors.response.use(response => response, async (error: AxiosError) => {
  const request = error.config as SessionRequest | undefined;
  if (!request || error.response?.status !== 401 || request.url?.includes("/auth/")) return Promise.reject(error);
  if (request._retry) return Promise.reject(error);
  const session = readAuthSession();
  if (!session || request._sessionRefreshToken !== session.refreshToken) return Promise.reject(error);
  request._retry = true;
  // A late 401 can belong to a request sent before another request refreshed the token.
  if (request.headers.Authorization !== `Bearer ${session.accessToken}`) return api(request);
  notifyLog("401_DETECTED", `Phiên hết hạn tại ${request.url}`);

  if (!refreshPromise) {
    const refreshToken = session.refreshToken;
    notifyLog("REFRESHING", "Đang làm mới phiên đăng nhập");
    refreshPromise = refreshClient.post("/auth/refresh-token", { refreshToken })
      .then(({ data }) => {
        const token = data?.data?.accessToken;
        if (typeof token !== "string" || !token) throw new Error("Phản hồi refresh không hợp lệ");
        if (readAuthSession()?.refreshToken !== refreshToken) throw new Error("Phiên đăng nhập đã thay đổi");
        updateSessionAccessToken(token);
        notifyLog("REFRESH_SUCCESS", "Đã làm mới phiên đăng nhập");
        return token;
      })
      .catch((refreshError: unknown) => {
        if (readAuthSession()?.refreshToken === refreshToken) {
          writeAuthSession(null);
          notifyLog("REFRESH_FAILED", "Phiên đã hết hạn, vui lòng đăng nhập lại");
          window.location.href = "/login?expired=true";
        }
        throw refreshError;
      })
      .finally(() => { refreshPromise = null; });
  } else {
    notifyLog("QUEUE_WAIT", "Request đang chờ làm mới phiên");
  }

  await refreshPromise;
  if (readAuthSession()?.refreshToken !== session.refreshToken) return Promise.reject(error);
  notifyLog("QUEUE_RESOLVED", "Gửi lại request với phiên mới");
  return api(request);
});
