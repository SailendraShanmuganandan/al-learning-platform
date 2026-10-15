import { apiFetch } from './authApi';
import type { ProductType } from './productApi';

export interface CartItem {
  id: number;
  productId: number;
  name: string;
  slug: string;
  type: ProductType;
  thumbnailUrl: string | null;
  quantity: number;
  unitPrice: number;
  lineTotal: number;
  currentPrice: number;
  priceChanged: boolean;
  unavailable: boolean;
  outOfStock: boolean;
  insufficientStock: boolean;
}

export interface CartResponse {
  cartId: number | null;
  items: CartItem[];
  itemCount: number;
  subtotal: number;
}

export interface AddToCartPayload {
  productId: number;
  quantity: number;
}

export interface UpdateCartItemPayload {
  quantity: number;
}

/**
 * Normalizes backend response which can contain snake_case or camelCase keys.
 */
function normalizeCartResponse(raw: any): CartResponse {
  if (!raw) {
    return { cartId: null, items: [], itemCount: 0, subtotal: 0 };
  }

  const items: CartItem[] = (raw.items || []).map((it: any) => ({
    id: it.id ?? it.item_id ?? it.cartItemId,
    productId: it.productId ?? it.product_id,
    name: it.name ?? '',
    slug: it.slug ?? '',
    type: it.type ?? 'PHYSICAL_NOTE',
    thumbnailUrl: it.thumbnailUrl ?? it.thumbnail_url ?? null,
    quantity: it.quantity ?? 1,
    unitPrice: typeof it.unitPrice === 'number' ? it.unitPrice : parseFloat(it.unit_price ?? it.unitPrice ?? '0'),
    lineTotal: typeof it.lineTotal === 'number' ? it.lineTotal : parseFloat(it.line_total ?? it.lineTotal ?? '0'),
    currentPrice: typeof it.currentPrice === 'number' ? it.currentPrice : parseFloat(it.current_price ?? it.currentPrice ?? '0'),
    priceChanged: Boolean(it.priceChanged ?? it.price_changed),
    unavailable: Boolean(it.unavailable),
    outOfStock: Boolean(it.outOfStock ?? it.out_of_stock),
    insufficientStock: Boolean(it.insufficientStock ?? it.insufficient_stock),
  }));

  const itemCount = typeof raw.itemCount === 'number' ? raw.itemCount : (raw.item_count ?? items.reduce((s, i) => s + i.quantity, 0));
  const subtotal = typeof raw.subtotal === 'number' ? raw.subtotal : parseFloat(raw.subtotal ?? '0');

  return {
    cartId: raw.cartId ?? raw.cart_id ?? null,
    items,
    itemCount,
    subtotal,
  };
}

/** GET /api/cart */
export async function getCart(): Promise<CartResponse> {
  const raw = await apiFetch<any>('/api/cart');
  return normalizeCartResponse(raw);
}

/** POST /api/cart/items */
export async function addToCart(payload: AddToCartPayload): Promise<CartResponse> {
  const raw = await apiFetch<any>('/api/cart/items', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
  return normalizeCartResponse(raw);
}

/** PATCH /api/cart/items/{id} */
export async function updateCartItem(id: number, payload: UpdateCartItemPayload): Promise<CartResponse> {
  const raw = await apiFetch<any>(`/api/cart/items/${id}`, {
    method: 'PATCH',
    body: JSON.stringify(payload),
  });
  return normalizeCartResponse(raw);
}

/** DELETE /api/cart/items/{id} */
export async function removeCartItem(id: number): Promise<void> {
  await apiFetch<void>(`/api/cart/items/${id}`, {
    method: 'DELETE',
  });
}

/** DELETE /api/cart/items */
export async function clearCart(): Promise<void> {
  await apiFetch<void>('/api/cart/items', {
    method: 'DELETE',
  });
}
