import { create } from "zustand";
import type { OrderDto } from "@/services/checkoutService";
import { orderService } from "@/services/orderService";
import { readAuthSession, subscribeAuthSession } from "@/lib/authSession";

interface OrderState {
  orders: OrderDto[];
  loading: boolean;
  error: string | null;
  fetchOrders: () => Promise<void>;
  cancelOrder: (id: number) => Promise<boolean>;
}
let version = 0;
let owner = readAuthSession()?.refreshToken || null;
localStorage.removeItem("helishop-orders-storage");
localStorage.removeItem("helishop-orders-storage-v2");

export const useOrderStore = create<OrderState>()((set, get) => {
  const perform = async (action: () => Promise<OrderDto[]>) => {
    const requestVersion = ++version;
    const token = readAuthSession()?.refreshToken;
    set({ loading: true, error: null });
    try {
      const orders = await action();
      if (requestVersion !== version || token !== readAuthSession()?.refreshToken) return false;
      set({ orders });
      return true;
    } catch (error: unknown) {
      if (requestVersion === version && token === readAuthSession()?.refreshToken) {
        set({ error: (error as { response?: { data?: { message?: string } } }).response?.data?.message || "Không thể tải hoặc cập nhật đơn hàng. Vui lòng thử lại." });
      }
      return false;
    } finally { if (requestVersion === version) set({ loading: false }); }
  };
  return {
    orders: [], loading: false, error: null,
    fetchOrders: async () => { await perform(() => orderService.mine()); },
    cancelOrder: id => perform(async () => {
      const updated = await orderService.cancel(id);
      return get().orders.map(order => order.id === updated.id ? updated : order);
    }),
  };
});

subscribeAuthSession(session => {
  const next = session?.refreshToken || null;
  if (next === owner) return;
  owner = next; version++;
  useOrderStore.setState({ orders: [], loading: false, error: null });
  if (session) void useOrderStore.getState().fetchOrders();
});
