import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { useAuthStore } from "@/store/useAuthStore";
import { api, registerInterceptorLogger } from "@/lib/api";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import {
  ShoppingBag,
  LogOut,
  RefreshCw,
  Zap,
  ShieldAlert,
  CheckCircle,
  Clock,
  Database,
  Terminal,
  Layers,
  Copy,
  Check
} from "lucide-react";

interface LogEntry {
  id: string;
  timestamp: string;
  type: string;
  message: string;
  details?: Record<string, unknown>;
}

export const DashboardPage: React.FC = () => {
  const navigate = useNavigate();
  const { user, accessToken, refreshToken, corruptAccessToken, logout } = useAuthStore();

  const [logs, setLogs] = useState<LogEntry[]>([]);
  const [testingParallel, setTestingParallel] = useState(false);
  const [copiedToken, setCopiedToken] = useState<string | null>(null);

  // Subscribe to live Axios Interceptor events
  useEffect(() => {
    const unsubscribe = registerInterceptorLogger((log) => {
      setLogs((prev) => [
        {
          id: Math.random().toString(36).substring(2, 9),
          ...log,
        },
        ...prev.slice(0, 49), // Keep last 50 logs
      ]);
    });

    return unsubscribe;
  }, []);

  // TanStack Query: Fetch Categories Tree
  const {
    data: categoriesData,
    isLoading: isLoadingCategories,
    refetch: refetchCategories,
  } = useQuery({
    queryKey: ["categories"],
    queryFn: async () => {
      const res = await api.get("/categories/tree");
      return res.data?.data || [];
    },
  });

  const handleLogout = async () => {
    await logout();
    navigate("/login");
  };

  const handleCopy = (text: string, type: string) => {
    navigator.clipboard.writeText(text);
    setCopiedToken(type);
    setTimeout(() => setCopiedToken(null), 2000);
  };

  const handleCorruptToken = () => {
    corruptAccessToken();
    setLogs((prev) => [
      {
        id: Math.random().toString(36).substring(2, 9),
        timestamp: new Date().toLocaleTimeString("vi-VN"),
        type: "CORRUPT",
        message: "⚠️ Đã cố tình làm hỏng Access Token để giả lập trạng thái hết hạn / 401 Unauthorized!",
      },
      ...prev,
    ]);
  };

  const handleTestParallelRequests = async () => {
    setTestingParallel(true);
    setLogs((prev) => [
      {
        id: Math.random().toString(36).substring(2, 9),
        timestamp: new Date().toLocaleTimeString("vi-VN"),
        type: "BATCH_START",
        message: "🚀 Bắt đầu bắn đồng thời 5 request API song song để kiểm tra failedQueue...",
      },
      ...prev,
    ]);

    try {
      // 5 concurrent requests sent at the exact same millisecond
      const results = await Promise.allSettled([
        api.get("/categories/tree"),
        api.get("/products?page=0&size=2"),
        api.get("/categories"),
        api.get("/cart"),
        api.get("/categories/tree"),
      ]);

      const successCount = results.filter((r) => r.status === "fulfilled").length;
      setLogs((prev) => [
        {
          id: Math.random().toString(36).substring(2, 9),
          timestamp: new Date().toLocaleTimeString("vi-VN"),
          type: "BATCH_DONE",
          message: `✅ Hoàn thành 5 request song song: ${successCount}/5 request thành công sau khi failedQueue giải phóng!`,
        },
        ...prev,
      ]);
    } catch (err: any) {
      console.error("Parallel error:", err);
    } finally {
      setTestingParallel(false);
    }
  };

  const isCorrupted = accessToken?.includes("corrupted");

  return (
    <div className="min-h-screen bg-slate-50">
      {/* Top Header Navbar */}
      <header className="sticky top-0 z-40 bg-white border-b border-slate-200 shadow-sm">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="flex items-center justify-center w-10 h-10 rounded-xl bg-gradient-to-tr from-[#EE4D2D] to-[#FF7337] text-white shadow-md shadow-orange-500/20">
              <ShoppingBag className="w-5 h-5" />
            </div>
            <div>
              <span className="text-xl font-black tracking-tight text-slate-900">
                Heli<span className="text-[#EE4D2D]">Shop</span>
              </span>
              <span className="ml-2 text-xs py-0.5 px-2 rounded bg-orange-100 text-[#EE4D2D] font-bold">
                Sprint 4 Core
              </span>
            </div>
          </div>

          <div className="flex items-center gap-4">
            <div className="flex items-center gap-3 border-r border-slate-200 pr-4">
              <div className="w-9 h-9 rounded-full bg-slate-100 border border-slate-200 flex items-center justify-center text-slate-700 font-bold text-sm">
                {user?.fullName?.charAt(0) || "U"}
              </div>
              <div className="hidden sm:block text-left">
                <p className="text-sm font-semibold text-slate-800 leading-tight">
                  {user?.fullName || "Người dùng"}
                </p>
                <p className="text-xs text-slate-500">{user?.email}</p>
              </div>
              <Badge variant="shopee" className="capitalize">
                {user?.role?.replace("ROLE_", "").toLowerCase()}
              </Badge>
            </div>

            <Button variant="ghost" size="sm" onClick={handleLogout} className="text-slate-600 hover:text-red-600">
              <LogOut className="w-4 h-4 mr-1.5" />
              Đăng xuất
            </Button>
          </div>
        </div>
      </header>

      {/* Main Content Body */}
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">
        {/* Banner Section */}
        <div className="bg-gradient-to-r from-slate-900 via-slate-800 to-slate-900 rounded-2xl p-6 sm:p-8 text-white shadow-xl flex flex-col md:flex-row items-start md:items-center justify-between gap-6">
          <div>
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-orange-500/20 text-[#FF7337] text-xs font-semibold mb-3 border border-orange-500/30">
              <Zap className="w-3.5 h-3.5" />
              Shopee Silent Refresh Token Engine
            </div>
            <h1 className="text-2xl sm:text-3xl font-bold tracking-tight">
              Axios Interceptor & failedQueue Control Center
            </h1>
            <p className="text-slate-300 text-sm mt-2 max-w-2xl leading-relaxed">
              Giải pháp ngăn chặn triệt để hiện tượng spam request refresh token khi nhiều API đồng thời trả về HTTP 401. Hàng đợi <code className="text-orange-300 bg-black/30 px-1 py-0.5 rounded font-mono">failedQueue</code> gom và giữ các request, chờ duy nhất 1 lần refresh thành công rồi tái thực thi tự động.
            </p>
          </div>

          <div className="flex flex-col sm:flex-row gap-3 shrink-0">
            <Button
              variant="outline"
              onClick={handleCorruptToken}
              className="bg-white/10 hover:bg-white/20 text-white border-white/20 hover:border-white/30"
            >
              <ShieldAlert className="w-4 h-4 mr-2 text-amber-400" />
              Làm Hỏng Token (Giả lập 401)
            </Button>

            <Button
              variant="shopee"
              onClick={handleTestParallelRequests}
              isLoading={testingParallel}
            >
              <Zap className="w-4 h-4 mr-2" />
              Bắn 5 Request Song Song
            </Button>
          </div>
        </div>

        {/* Grid 2 Columns: State & Token Status vs Live Log Console */}
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
          {/* Left Column (5 cols): Token & State Info */}
          <div className="lg:col-span-5 space-y-6">
            <Card className="shadow-sm">
              <CardHeader className="pb-3 border-b border-slate-100">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <Database className="w-5 h-5 text-[#EE4D2D]" />
                    <CardTitle className="text-lg">Zustand State & Tokens</CardTitle>
                  </div>
                  {isCorrupted ? (
                    <Badge variant="destructive" className="animate-pulse">
                      Token Đã Bị Hỏng
                    </Badge>
                  ) : (
                    <Badge variant="success" className="flex items-center gap-1">
                      <CheckCircle className="w-3 h-3" />
                      Active Session
                    </Badge>
                  )}
                </div>
                <CardDescription>
                  Thông tin phiên làm việc lưu trong Zustand store và localStorage
                </CardDescription>
              </CardHeader>
              <CardContent className="pt-4 space-y-4 text-sm">
                <div>
                  <div className="flex items-center justify-between mb-1">
                    <span className="text-xs font-semibold text-slate-500 uppercase">Access Token (JWT)</span>
                    <button
                      onClick={() => handleCopy(accessToken || "", "access")}
                      className="text-xs text-slate-500 hover:text-slate-800 flex items-center gap-1"
                    >
                      {copiedToken === "access" ? <Check className="w-3 h-3 text-emerald-600" /> : <Copy className="w-3 h-3" />}
                      <span>{copiedToken === "access" ? "Đã chép" : "Sao chép"}</span>
                    </button>
                  </div>
                  <div className="p-2.5 rounded-lg bg-slate-900 text-slate-200 font-mono text-xs break-all max-h-20 overflow-y-auto border border-slate-800">
                    {accessToken || "(Trống)"}
                  </div>
                </div>

                <div>
                  <div className="flex items-center justify-between mb-1">
                    <span className="text-xs font-semibold text-slate-500 uppercase">Refresh Token (Redis)</span>
                    <button
                      onClick={() => handleCopy(refreshToken || "", "refresh")}
                      className="text-xs text-slate-500 hover:text-slate-800 flex items-center gap-1"
                    >
                      {copiedToken === "refresh" ? <Check className="w-3 h-3 text-emerald-600" /> : <Copy className="w-3 h-3" />}
                      <span>{copiedToken === "refresh" ? "Đã chép" : "Sao chép"}</span>
                    </button>
                  </div>
                  <div className="p-2.5 rounded-lg bg-slate-100 text-slate-700 font-mono text-xs break-all border border-slate-200">
                    {refreshToken || "(Trống)"}
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-3 pt-2">
                  <div className="p-3 bg-slate-50 rounded-lg border border-slate-200/80">
                    <span className="text-xs text-slate-500">Người dùng ID</span>
                    <p className="font-bold text-slate-800 mt-0.5">#{user?.id || 1}</p>
                  </div>
                  <div className="p-3 bg-slate-50 rounded-lg border border-slate-200/80">
                    <span className="text-xs text-slate-500">Vai trò</span>
                    <p className="font-bold text-slate-800 mt-0.5">{user?.role || "ROLE_CUSTOMER"}</p>
                  </div>
                </div>
              </CardContent>
            </Card>

            {/* TanStack Query Widget */}
            <Card className="shadow-sm">
              <CardHeader className="pb-3 border-b border-slate-100 flex flex-row items-center justify-between">
                <div>
                  <div className="flex items-center gap-2">
                    <Layers className="w-5 h-5 text-blue-600" />
                    <CardTitle className="text-lg">TanStack Query Caching</CardTitle>
                  </div>
                  <CardDescription>Dữ liệu danh mục hàng hóa nạp từ server</CardDescription>
                </div>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => refetchCategories()}
                  className="h-8 text-xs"
                >
                  <RefreshCw className="w-3 h-3 mr-1" />
                  Refetch
                </Button>
              </CardHeader>
              <CardContent className="pt-4">
                {isLoadingCategories ? (
                  <div className="flex items-center justify-center py-6 text-slate-400 text-sm">
                    <RefreshCw className="w-4 h-4 animate-spin mr-2" />
                    Đang nạp dữ liệu cache...
                  </div>
                ) : (
                  <div className="space-y-2">
                    <p className="text-xs text-slate-500">
                      Tổng số nhóm danh mục tải được:{" "}
                      <strong className="text-slate-800">{Array.isArray(categoriesData) ? categoriesData.length : 0}</strong>
                    </p>
                    <div className="flex flex-wrap gap-1.5">
                      {Array.isArray(categoriesData) &&
                        categoriesData.slice(0, 6).map((cat: any) => (
                          <Badge key={cat.id || cat.slug} variant="secondary" className="text-xs">
                            {cat.name}
                          </Badge>
                        ))}
                    </div>
                  </div>
                )}
              </CardContent>
            </Card>
          </div>

          {/* Right Column (7 cols): Live Interceptor Log Console */}
          <div className="lg:col-span-7">
            <Card className="shadow-sm h-full flex flex-col">
              <CardHeader className="pb-3 border-b border-slate-100 flex flex-row items-center justify-between">
                <div>
                  <div className="flex items-center gap-2">
                    <Terminal className="w-5 h-5 text-emerald-600" />
                    <CardTitle className="text-lg">Live Interceptor Inspector</CardTitle>
                  </div>
                  <CardDescription>Nhật ký thời gian thực của Axios Request & Response Interceptor</CardDescription>
                </div>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setLogs([])}
                  className="h-8 text-xs text-slate-500 hover:text-slate-900"
                >
                  Xóa Log
                </Button>
              </CardHeader>
              <CardContent className="pt-4 flex-1 flex flex-col">
                <div className="flex-1 bg-slate-950 text-slate-200 rounded-xl p-4 font-mono text-xs overflow-y-auto max-h-[520px] space-y-2.5 border border-slate-900">
                  {logs.length === 0 ? (
                    <div className="h-48 flex flex-col items-center justify-center text-slate-500 text-center">
                      <Clock className="w-8 h-8 mb-2 opacity-40" />
                      <p>Chưa có sự kiện nào. Hãy bấm "Làm Hỏng Token" rồi bấm "Bắn 5 Request Song Song" để chứng kiến failedQueue hoạt động!</p>
                    </div>
                  ) : (
                    logs.map((log) => {
                      let badgeColor = "bg-blue-900/60 text-blue-300 border-blue-700";
                      if (log.type === "401_DETECTED") badgeColor = "bg-red-900/70 text-red-300 border-red-700 font-bold";
                      if (log.type === "QUEUE_WAIT") badgeColor = "bg-amber-900/70 text-amber-300 border-amber-700";
                      if (log.type === "REFRESHING") badgeColor = "bg-purple-900/70 text-purple-300 border-purple-700 font-bold";
                      if (log.type === "REFRESH_SUCCESS") badgeColor = "bg-emerald-900/70 text-emerald-300 border-emerald-700 font-bold";
                      if (log.type === "QUEUE_RESOLVED") badgeColor = "bg-teal-900/70 text-teal-300 border-teal-700";

                      return (
                        <div key={log.id} className="flex items-start gap-2.5 leading-relaxed hover:bg-slate-900/50 p-1 rounded transition-colors">
                          <span className="text-slate-500 shrink-0 select-none">[{log.timestamp}]</span>
                          <span className={`px-1.5 py-0.5 rounded text-[10px] border shrink-0 ${badgeColor}`}>
                            {log.type}
                          </span>
                          <span className="text-slate-300 flex-1">{log.message}</span>
                        </div>
                      );
                    })
                  )}
                </div>
              </CardContent>
            </Card>
          </div>
        </div>
      </main>
    </div>
  );
};
