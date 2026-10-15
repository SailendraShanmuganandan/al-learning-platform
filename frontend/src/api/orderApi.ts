import { apiFetch } from './authApi';

export type PaymentStatus = 'PENDING' | 'PAID' | 'FAILED' | 'REFUNDED' | 'EXPIRED';
export type FulfillmentStatus = 'NOT_REQUIRED' | 'PENDING' | 'PROCESSING' | 'SHIPPED' | 'DELIVERED';

export interface OrderItem {
  orderItemId: number;
  productId: number;
  productName: string;
  productType: string;
  quantity: number;
  unitPrice: number;
  subtotal: number;
}

export interface Order {
  orderId: number;
  userId: number;
  paymentStatus: PaymentStatus;
  fulfillmentStatus: FulfillmentStatus;
  subtotal: number;
  deliveryFee: number;
  totalAmount: number;
  expiresAt: string;
  createdAt: string;
  updatedAt: string;
  shipRecipientName?: string | null;
  shipPhone?: string | null;
  shipAddressLine1?: string | null;
  shipAddressLine2?: string | null;
  shipCity?: string | null;
  shipDistrict?: string | null;
  shipPostalCode?: string | null;
  items: OrderItem[];
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  empty: boolean;
}

export interface CheckoutPayload {
  addressId?: number | null;
}

/**
 * Normalizes snake_case or camelCase order response from backend.
 */
export function normalizeOrder(raw: any): Order {
  return {
    orderId: raw.orderId ?? raw.order_id,
    userId: raw.userId ?? raw.user_id,
    paymentStatus: raw.paymentStatus ?? raw.payment_status,
    fulfillmentStatus: raw.fulfillmentStatus ?? raw.fulfillment_status,
    subtotal: typeof raw.subtotal === 'number' ? raw.subtotal : parseFloat(raw.subtotal ?? '0'),
    deliveryFee: typeof raw.deliveryFee === 'number' ? raw.deliveryFee : parseFloat(raw.delivery_fee ?? raw.deliveryFee ?? '0'),
    totalAmount: typeof raw.totalAmount === 'number' ? raw.totalAmount : parseFloat(raw.total_amount ?? raw.totalAmount ?? '0'),
    expiresAt: raw.expiresAt ?? raw.expires_at,
    createdAt: raw.createdAt ?? raw.created_at,
    updatedAt: raw.updatedAt ?? raw.updated_at,
    shipRecipientName: raw.shipRecipientName ?? raw.ship_recipient_name ?? null,
    shipPhone: raw.shipPhone ?? raw.ship_phone ?? null,
    shipAddressLine1: raw.shipAddressLine1 ?? raw.ship_address_line_1 ?? null,
    shipAddressLine2: raw.shipAddressLine2 ?? raw.ship_address_line_2 ?? null,
    shipCity: raw.shipCity ?? raw.ship_city ?? null,
    shipDistrict: raw.shipDistrict ?? raw.ship_district ?? null,
    shipPostalCode: raw.shipPostalCode ?? raw.ship_postal_code ?? null,
    items: (raw.items || []).map((it: any) => ({
      orderItemId: it.orderItemId ?? it.order_item_id,
      productId: it.productId ?? it.product_id,
      productName: it.productName ?? it.product_name,
      productType: it.productType ?? it.product_type,
      quantity: it.quantity ?? 1,
      unitPrice: typeof it.unitPrice === 'number' ? it.unitPrice : parseFloat(it.unit_price ?? it.unitPrice ?? '0'),
      subtotal: typeof it.subtotal === 'number' ? it.subtotal : parseFloat(it.subtotal ?? '0'),
    })),
  };
}

/** POST /api/orders */
export async function createOrder(payload: CheckoutPayload): Promise<Order> {
  const raw = await apiFetch<any>('/api/orders', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
  return normalizeOrder(raw);
}

/** GET /api/orders */
export async function getMyOrders(page = 0, size = 10): Promise<PageResponse<Order>> {
  const raw = await apiFetch<any>(`/api/orders?page=${page}&size=${size}`);
  return {
    content: (raw.content || []).map(normalizeOrder),
    totalElements: raw.totalElements ?? 0,
    totalPages: raw.totalPages ?? 0,
    size: raw.size ?? size,
    number: raw.number ?? page,
    empty: raw.empty ?? true,
  };
}

/** GET /api/orders/:id */
export async function getOrder(orderId: number): Promise<Order> {
  const raw = await apiFetch<any>(`/api/orders/${orderId}`);
  return normalizeOrder(raw);
}
