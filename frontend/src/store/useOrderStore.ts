import { create } from "zustand";
import { persist } from "zustand/middleware";
import { Order, OrderStatus } from "@/types";

interface OrderState {
  orders: Order[];
  addOrder: (order: Order) => void;
  updateOrderStatus: (orderId: number, status: OrderStatus) => void;
  getOrderById: (orderId: number) => Order | undefined;
  getOrderByCode: (orderCode: string) => Order | undefined;
}

export const useOrderStore = create<OrderState>()(
  persist(
    (set, get) => ({
      orders: [],

      addOrder: (newOrder) => {
        set((state) => ({
          orders: [newOrder, ...state.orders],
        }));
      },

      updateOrderStatus: (orderId, status) => {
        set((state) => ({
          orders: state.orders.map((o) =>
            o.id === orderId ? { ...o, status } : o
          ),
        }));
      },

      getOrderById: (orderId) => {
        return get().orders.find((o) => o.id === orderId);
      },

      getOrderByCode: (orderCode) => {
        return get().orders.find((o) => o.orderCode === orderCode);
      },
    }),
    {
      name: "helishop-orders-storage-v2",
    }
  )
);

// Clear old mock orders data from legacy localStorage key if present
try {
  localStorage.removeItem("helishop-orders-storage");
} catch {
  // ignore in non-browser environments
}
