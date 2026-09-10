export interface Category {
  id: number;
  name: string;
  slug: string;
  parentId?: number | null;
  children?: Category[];
  icon?: string;
}

export interface ProductSku {
  id: number;
  skuCode: string;
  price: number;
  originalPrice?: number;
  stockQuantity: number;
  attributes: Record<string, string>; // e.g. { "Màu sắc": "Titan Tự Nhiên", "Dung lượng": "256GB" }
  imageUrl?: string;
}

export interface ProductImage {
  id: number;
  imageUrl: string;
  isThumbnail: boolean;
  displayOrder: number;
}

export interface Shop {
  id: number;
  shopName: string;
  avatarUrl?: string;
  rating?: number;
  responseRate?: string;
  joinedTime?: string;
  productsCount?: number;
}

export interface Product {
  id: number;
  name: string;
  slug: string;
  description: string;
  price: number;
  originalPrice?: number;
  rating: number;
  reviewCount: number;
  soldCount: number;
  category: Category;
  shop: Shop;
  isFavorite?: boolean;
  isMall?: boolean;
  images: ProductImage[];
  productSkus: ProductSku[];
  specs?: Record<string, string>;
  origin?: string;
}

export interface CartItem {
  skuId: number;
  productId: number;
  productName: string;
  skuCode: string;
  attributes: Record<string, string>;
  price: number;
  originalPrice?: number;
  quantity: number;
  stockQuantity: number;
  imageUrl: string;
  shopId: number;
  shopName: string;
}

export interface CartShopGroup {
  shopId: number;
  shopName: string;
  shopAvatar?: string;
  items: CartItem[];
  subtotal: number;
}

export interface CartResponse {
  userId: number;
  shopGroups: CartShopGroup[];
  totalItems: number;
  totalAmount: number;
}

export interface Address {
  id: number;
  receiverName: string;
  phone: string;
  detailAddress: string;
  ward: string;
  district: string;
  province: string;
  isDefault?: boolean;
}

export interface Voucher {
  id: number;
  code: string;
  name: string;
  discountType: "FIXED_AMOUNT" | "PERCENTAGE";
  discountValue: number;
  minOrderValue: number;
  maxDiscountAmount?: number;
  expiryDate: string;
}

export type OrderStatus =
  | "PENDING"
  | "CONFIRMED"
  | "PROCESSING"
  | "SHIPPING"
  | "DELIVERED"
  | "CANCELLED"
  | "RETURNED";

export interface OrderItem {
  id: number;
  skuId: number;
  productName: string;
  skuCode: string;
  attributes: Record<string, string>;
  unitPrice: number;
  quantity: number;
  totalPrice: number;
  imageUrl?: string;
}

export interface Order {
  id: number;
  orderCode: string;
  shopId: number;
  shopName: string;
  status: OrderStatus;
  paymentStatus: "PENDING" | "PAID" | "FAILED" | "REFUNDED";
  paymentMethod: "COD" | "VNPAY";
  shippingAddressSnapshot: string;
  receiverName: string;
  receiverPhone: string;
  items: OrderItem[];
  totalAmount: number;
  shippingFee: number;
  discountAmount: number;
  createdAt: string;
}
