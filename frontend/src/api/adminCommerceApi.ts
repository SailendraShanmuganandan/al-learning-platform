import { apiFetch } from './authApi';
import type { PageResponse, OrderItem, PaymentStatus, FulfillmentStatus } from './orderApi';

export interface AdminOrderListItem {
  orderId: number;
  customerName: string;
  customerEmail: string;
  subtotal: number;
  deliveryFee: number;
  totalAmount: number;
  paymentStatus: PaymentStatus;
  fulfillmentStatus: FulfillmentStatus;
  createdAt: string;
  needsReview: boolean;
}

export interface AdminPaymentDetail {
  paymentId: number;
  orderId: number;
  paymentMethod: string;
  paymentStatus: string;
  amount: number;
  transactionReference: string | null;
  paidAt: string | null;
  createdAt: string;
  needsReview: boolean;
  reviewReason: string | null;
  reviewNote: string | null;
  reviewedAt: string | null;
  refundedAt: string | null;
}

export interface AdminOrderDetail {
  orderId: number;
  customerName: string;
  customerEmail: string;
  customerPhone: string | null;
  subtotal: number;
  deliveryFee: number;
  totalAmount: number;
  paymentStatus: PaymentStatus;
  fulfillmentStatus: FulfillmentStatus;
  trackingNote: string | null;
  expiresAt: string;
  createdAt: string;
  shipRecipientName: string | null;
  shipPhone: string | null;
  shipAddressLine1: string | null;
  shipAddressLine2: string | null;
  shipCity: string | null;
  shipDistrict: string | null;
  shipPostalCode: string | null;
  items: OrderItem[];
  payments: AdminPaymentDetail[];
}

export interface AdminPaymentListItem {
  paymentId: number;
  orderId: number;
  amount: number;
  status: string;
  transactionReference: string | null;
  needsReview: boolean;
  reviewReason: string | null;
  paidAt: string | null;
  createdAt: string;
}

export interface OrderFilterParams {
  paymentStatus?: string;
  fulfillmentStatus?: string;
  q?: string;
  needsReview?: boolean;
  from?: string;
  to?: string;
  page?: number;
  size?: number;
}

export interface PaymentFilterParams {
  status?: string;
  needsReview?: boolean;
  orderId?: number;
  page?: number;
  size?: number;
}

function normalizeAdminOrderListItem(raw: any): AdminOrderListItem {
  return {
    orderId: raw.orderId ?? raw.order_id,
    customerName: raw.customerName ?? raw.customer_name ?? '—',
    customerEmail: raw.customerEmail ?? raw.customer_email ?? '—',
    subtotal: typeof raw.subtotal === 'number' ? raw.subtotal : parseFloat(raw.subtotal ?? '0'),
    deliveryFee: typeof raw.deliveryFee === 'number' ? raw.deliveryFee : parseFloat(raw.delivery_fee ?? raw.deliveryFee ?? '0'),
    totalAmount: typeof raw.totalAmount === 'number' ? raw.totalAmount : parseFloat(raw.total_amount ?? raw.totalAmount ?? '0'),
    paymentStatus: raw.paymentStatus ?? raw.payment_status,
    fulfillmentStatus: raw.fulfillmentStatus ?? raw.fulfillment_status,
    createdAt: raw.createdAt ?? raw.created_at,
    needsReview: raw.needsReview ?? raw.needs_review ?? false,
  };
}

function normalizePaymentDetail(raw: any): AdminPaymentDetail {
  return {
    paymentId: raw.paymentId ?? raw.payment_id,
    orderId: raw.orderId ?? raw.order_id,
    paymentMethod: raw.paymentMethod ?? raw.payment_method,
    paymentStatus: raw.paymentStatus ?? raw.payment_status,
    amount: typeof raw.amount === 'number' ? raw.amount : parseFloat(raw.amount ?? '0'),
    transactionReference: raw.transactionReference ?? raw.transaction_reference ?? null,
    paidAt: raw.paidAt ?? raw.paid_at ?? null,
    createdAt: raw.createdAt ?? raw.created_at,
    needsReview: raw.needsReview ?? raw.needs_review ?? false,
    reviewReason: raw.reviewReason ?? raw.review_reason ?? null,
    reviewNote: raw.reviewNote ?? raw.review_note ?? null,
    reviewedAt: raw.reviewedAt ?? raw.reviewed_at ?? null,
    refundedAt: raw.refundedAt ?? raw.refunded_at ?? null,
  };
}

function normalizeAdminOrderDetail(raw: any): AdminOrderDetail {
  return {
    orderId: raw.orderId ?? raw.order_id,
    customerName: raw.customerName ?? raw.customer_name ?? '—',
    customerEmail: raw.customerEmail ?? raw.customer_email ?? '—',
    customerPhone: raw.customerPhone ?? raw.customer_phone ?? null,
    subtotal: typeof raw.subtotal === 'number' ? raw.subtotal : parseFloat(raw.subtotal ?? '0'),
    deliveryFee: typeof raw.deliveryFee === 'number' ? raw.deliveryFee : parseFloat(raw.delivery_fee ?? raw.deliveryFee ?? '0'),
    totalAmount: typeof raw.totalAmount === 'number' ? raw.totalAmount : parseFloat(raw.total_amount ?? raw.totalAmount ?? '0'),
    paymentStatus: raw.paymentStatus ?? raw.payment_status,
    fulfillmentStatus: raw.fulfillmentStatus ?? raw.fulfillment_status,
    trackingNote: raw.trackingNote ?? raw.tracking_note ?? null,
    expiresAt: raw.expiresAt ?? raw.expires_at,
    createdAt: raw.createdAt ?? raw.created_at,
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
    payments: (raw.payments || []).map(normalizePaymentDetail),
  };
}

function normalizeAdminPaymentListItem(raw: any): AdminPaymentListItem {
  return {
    paymentId: raw.paymentId ?? raw.payment_id,
    orderId: raw.orderId ?? raw.order_id,
    amount: typeof raw.amount === 'number' ? raw.amount : parseFloat(raw.amount ?? '0'),
    status: raw.status ?? raw.paymentStatus ?? raw.payment_status,
    transactionReference: raw.transactionReference ?? raw.transaction_reference ?? null,
    needsReview: raw.needsReview ?? raw.needs_review ?? false,
    reviewReason: raw.reviewReason ?? raw.review_reason ?? null,
    paidAt: raw.paidAt ?? raw.paid_at ?? null,
    createdAt: raw.createdAt ?? raw.created_at,
  };
}

// ── Orders Endpoints ─────────────────────────────────────────────────────────

export async function getAdminOrders(params: OrderFilterParams = {}): Promise<PageResponse<AdminOrderListItem>> {
  const query = new URLSearchParams();
  if (params.paymentStatus) query.set('paymentStatus', params.paymentStatus);
  if (params.fulfillmentStatus) query.set('fulfillmentStatus', params.fulfillmentStatus);
  if (params.q) query.set('q', params.q);
  if (params.needsReview !== undefined) query.set('needsReview', String(params.needsReview));
  if (params.from) query.set('from', params.from);
  if (params.to) query.set('to', params.to);
  if (params.page !== undefined) query.set('page', String(params.page));
  if (params.size !== undefined) query.set('size', String(params.size));

  const raw = await apiFetch<any>(`/api/admin/orders?${query.toString()}`);
  return {
    content: (raw.content || []).map(normalizeAdminOrderListItem),
    totalElements: raw.totalElements ?? 0,
    totalPages: raw.totalPages ?? 0,
    size: raw.size ?? (params.size ?? 10),
    number: raw.number ?? (params.page ?? 0),
    empty: raw.empty ?? true,
  };
}

export async function getAdminOrderDetail(orderId: number): Promise<AdminOrderDetail> {
  const raw = await apiFetch<any>(`/api/admin/orders/${orderId}`);
  return normalizeAdminOrderDetail(raw);
}

export async function updateFulfillmentStatus(
  orderId: number,
  status: string,
  trackingNote?: string
): Promise<AdminOrderDetail> {
  const body: { status: string; tracking_note?: string; trackingNote?: string } = {
    status,
  };
  if (trackingNote !== undefined) {
    body.trackingNote = trackingNote;
    body.tracking_note = trackingNote;
  }
  const raw = await apiFetch<any>(`/api/admin/orders/${orderId}/fulfillment-status`, {
    method: 'PATCH',
    body: JSON.stringify(body),
  });
  return normalizeAdminOrderDetail(raw);
}

// ── Payments Endpoints ───────────────────────────────────────────────────────

export async function getAdminPayments(params: PaymentFilterParams = {}): Promise<PageResponse<AdminPaymentListItem>> {
  const query = new URLSearchParams();
  if (params.status) query.set('status', params.status);
  if (params.needsReview !== undefined) query.set('needsReview', String(params.needsReview));
  if (params.orderId !== undefined) query.set('orderId', String(params.orderId));
  if (params.page !== undefined) query.set('page', String(params.page));
  if (params.size !== undefined) query.set('size', String(params.size));

  const raw = await apiFetch<any>(`/api/admin/payments?${query.toString()}`);
  return {
    content: (raw.content || []).map(normalizeAdminPaymentListItem),
    totalElements: raw.totalElements ?? 0,
    totalPages: raw.totalPages ?? 0,
    size: raw.size ?? (params.size ?? 10),
    number: raw.number ?? (params.page ?? 0),
    empty: raw.empty ?? true,
  };
}

export async function getNeedsReviewCount(): Promise<number> {
  try {
    const raw = await apiFetch<{ count: number }>('/api/admin/payments/needs-review-count');
    return raw.count ?? 0;
  } catch {
    return 0;
  }
}

export async function resolvePaymentReview(paymentId: number, note: string): Promise<AdminPaymentDetail> {
  const raw = await apiFetch<any>(`/api/admin/payments/${paymentId}/resolve-review`, {
    method: 'PATCH',
    body: JSON.stringify({ note }),
  });
  return normalizePaymentDetail(raw);
}

export async function refundPayment(paymentId: number, note: string): Promise<AdminPaymentDetail> {
  const raw = await apiFetch<any>(`/api/admin/payments/${paymentId}/refund`, {
    method: 'PATCH',
    body: JSON.stringify({ note }),
  });
  return normalizePaymentDetail(raw);
}
