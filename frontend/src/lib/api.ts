import axios, { AxiosError, InternalAxiosRequestConfig } from "axios";

// Callback type for live debug logger in UI
export type InterceptorLogCallback = (log: {
  timestamp: string;
  type: "401_DETECTED" | "QUEUE_WAIT" | "REFRESHING" | "QUEUE_RESOLVED" | "REFRESH_SUCCESS" | "REFRESH_FAILED" | "REQUEST";
  message: string;
  details?: Record<string, unknown>;
}) => void;

let logListeners: InterceptorLogCallback[] = [];

export const registerInterceptorLogger = (callback: InterceptorLogCallback) => {
  logListeners.push(callback);
  return () => {
    logListeners = logListeners.filter(cb => cb !== callback);
  };
};

const notifyLog = (
  type: "401_DETECTED" | "QUEUE_WAIT" | "REFRESHING" | "QUEUE_RESOLVED" | "REFRESH_SUCCESS" | "REFRESH_FAILED" | "REQUEST",
  message: string,
  details?: Record<string, unknown>
) => {
  const timestamp = new Date().toLocaleTimeString('vi-VN', { hour12: false }) + '.' + String(new Date().getMilliseconds()).padStart(3, '0');
  logListeners.forEach(listener => listener({ timestamp, type, message, details }));
};

let isRefreshing = false;
let failedQueue: Array<{
  resolve: (value?: unknown) => void;
  reject: (reason?: unknown) => void;
}> = [];

const processQueue = (error: Error | null, token: string | null = null) => {
  const queueLength = failedQueue.length;
  if (queueLength > 0) {
    notifyLog("QUEUE_RESOLVED", `Giải phóng ${queueLength} request trong hàng đợi failedQueue`, {
      resolvedCount: queueLength,
      hasError: !!error,
    });
  }

  failedQueue.forEach((prom) => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve(token);
    }
  });
  failedQueue = [];
};

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api/v1",
  headers: { "Content-Type": "application/json" },
});

// Request Interceptor: Tự động đính kèm Bearer Token vào headers
api.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem("accessToken");
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    notifyLog("REQUEST", `Gửi request ${config.method?.toUpperCase()} ${config.url}`, {
      url: config.url,
      method: config.method,
      hasToken: !!token,
    });
    return config;
  },
  (error) => Promise.reject(error)
);

// Response Interceptor: Xử lý 401 và Silent Token Refresh với failedQueue
api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean };

    if (error.response?.status === 401 && !originalRequest._retry) {
      notifyLog("401_DETECTED", `Phát hiện lỗi 401 Unauthorized tại ${originalRequest.url}`, {
        url: originalRequest.url,
        isRefreshing,
      });

      if (isRefreshing) {
        notifyLog("QUEUE_WAIT", `Hệ thống đang refresh token. Đẩy request ${originalRequest.url} vào failedQueue (Queue size: ${failedQueue.length + 1})`);
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        })
          .then((token) => {
            originalRequest.headers.Authorization = `Bearer ${token}`;
            return api(originalRequest);
          })
          .catch((err) => Promise.reject(err));
      }

      originalRequest._retry = true;
      isRefreshing = true;
      notifyLog("REFRESHING", "Kích hoạt luồng làm mới token duy nhất tới /auth/refresh-token");

      try {
        const refreshToken = localStorage.getItem("refreshToken");
        const { data } = await axios.post(`${import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api/v1"}/auth/refresh-token`, {
          refreshToken,
        });

        const newAccessToken = data.data.accessToken;
        localStorage.setItem("accessToken", newAccessToken);

        api.defaults.headers.common.Authorization = `Bearer ${newAccessToken}`;
        originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;

        notifyLog("REFRESH_SUCCESS", "Làm mới Access Token thành công từ máy chủ!", {
          newTokenPrefix: newAccessToken.substring(0, 15) + "...",
        });

        processQueue(null, newAccessToken);
        return api(originalRequest);
      } catch (refreshError) {
        notifyLog("REFRESH_FAILED", "Refresh Token không hợp lệ hoặc đã hết hạn. Chuyển hướng về trang đăng nhập.");
        processQueue(refreshError as Error, null);
        localStorage.clear();
        window.location.href = "/login?expired=true";
        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }
    }
    return Promise.reject(error);
  }
);
