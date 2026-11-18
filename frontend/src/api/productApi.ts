import { apiFetch } from './authApi';

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------

export type ProductType = 'PHYSICAL_NOTE' | 'VIDEO';
export type ProductStatus = 'DRAFT' | 'PUBLISHED' | 'ARCHIVED';

export interface ProductSummaryResponse {
  productId: number;
  name: string;
  slug: string;
  productType: ProductType;
  price: number;
  status: ProductStatus;
  thumbnailUrl: string | null;
  stockQuantity: number | null;
  pageCount: number | null;
  durationSeconds: number | null;
  hasAttachmentPdf?: boolean;
  chapterCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface ProductDetailResponse {
  productId: number;
  name: string;
  slug: string;
  description: string | null;
  productType: ProductType;
  price: number;
  status: ProductStatus;
  thumbnailUrl: string | null;
  chapterIds: number[];
  pageCount: number | null;
  stockQuantity: number | null;
  providerVideoId: string | null;
  durationSeconds: number | null;
  videoDescription: string | null;
  hasAttachmentPdf?: boolean;
  attachmentPdfName?: string | null;
  attachmentPdfSize?: number | null;
  createdAt: string;
  updatedAt: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}

export interface ProductCreatePayload {
  name: string;
  description?: string | null;
  productType: ProductType;
  price: number;
  thumbnailUrl?: string | null;
  chapterIds?: number[];
  pageCount?: number | null;
  stockQuantity?: number | null;
  providerVideoId?: string | null;
  durationSeconds?: number | null;
  videoDescription?: string | null;
}

export interface ProductUpdatePayload {
  name?: string;
  description?: string | null;
  price?: number;
  thumbnailUrl?: string | null;
  chapterIds?: number[];
  pageCount?: number | null;
  stockQuantity?: number | null;
  providerVideoId?: string | null;
  durationSeconds?: number | null;
  videoDescription?: string | null;
}

export interface ProductFilterParams {
  type?: ProductType | '';
  status?: ProductStatus | '';
  search?: string;
  page?: number;
  size?: number;
}

// ---------------------------------------------------------------------------
// Admin API Functions
// ---------------------------------------------------------------------------

/**
 * GET /api/admin/products
 * Returns a paginated list of products, optionally filtered by type, status, and search query.
 */
export async function getAdminProducts(
  params: ProductFilterParams = {}
): Promise<PageResponse<ProductSummaryResponse>> {
  const query = new URLSearchParams();
  if (params.type) query.append('type', params.type);
  if (params.status) query.append('status', params.status);
  if (params.search && params.search.trim() !== '') query.append('search', params.search.trim());
  if (params.page !== undefined) query.append('page', params.page.toString());
  if (params.size !== undefined) query.append('size', params.size.toString());

  const qs = query.toString();
  return apiFetch<PageResponse<ProductSummaryResponse>>(
    `/api/admin/products${qs ? `?${qs}` : ''}`
  );
}

/**
 * GET /api/admin/products/{id}
 * Returns full details for a product.
 */
export async function getAdminProductById(id: number): Promise<ProductDetailResponse> {
  return apiFetch<ProductDetailResponse>(`/api/admin/products/${id}`);
}

/**
 * POST /api/admin/products
 * Creates a new product starting in DRAFT status.
 */
export async function createAdminProduct(
  payload: ProductCreatePayload
): Promise<ProductDetailResponse> {
  return apiFetch<ProductDetailResponse>('/api/admin/products', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

/**
 * PATCH /api/admin/products/{id}
 * Updates product common or subtype-specific fields.
 */
export async function updateAdminProduct(
  id: number,
  payload: ProductUpdatePayload
): Promise<ProductDetailResponse> {
  return apiFetch<ProductDetailResponse>(`/api/admin/products/${id}`, {
    method: 'PATCH',
    body: JSON.stringify(payload),
  });
}

/**
 * PATCH /api/admin/products/{id}/status
 * Transitions product status (DRAFT -> PUBLISHED, PUBLISHED -> ARCHIVED, etc.)
 */
export async function setAdminProductStatus(
  id: number,
  status: ProductStatus
): Promise<ProductDetailResponse> {
  return apiFetch<ProductDetailResponse>(`/api/admin/products/${id}/status`, {
    method: 'PATCH',
    body: JSON.stringify({ status }),
  });
}

/**
 * POST /api/admin/products/{id}/attachment-pdf
 * Uploads a supplementary lecture notes PDF for a video course.
 */
export async function uploadAdminProductAttachmentPdf(
  id: number,
  file: File
): Promise<ProductDetailResponse> {
  const formData = new FormData();
  formData.append('file', file);
  return apiFetch<ProductDetailResponse>(`/api/admin/products/${id}/attachment-pdf`, {
    method: 'POST',
    body: formData,
  });
}

/**
 * DELETE /api/admin/products/{id}/attachment-pdf
 * Removes the attached lecture notes PDF from a video course.
 */
export async function deleteAdminProductAttachmentPdf(
  id: number
): Promise<ProductDetailResponse> {
  return apiFetch<ProductDetailResponse>(`/api/admin/products/${id}/attachment-pdf`, {
    method: 'DELETE',
  });
}

// ---------------------------------------------------------------------------
// Public Types (separate from admin DTOs — security requirements)
// ---------------------------------------------------------------------------

export interface PublicChapterRef {
  chapterId: number;
  name: string;
  subjectName: string;
}

/**
 * Public product summary — never includes video_url or stock_quantity.
 * Notes expose in_stock boolean; videos expose duration_seconds.
 */
export interface PublicProductSummaryResponse {
  id: number;
  name: string;
  slug: string;
  type: ProductType;
  price: number;
  thumbnailUrl: string | null;
  // Note-specific
  pageCount: number | null;
  inStock: boolean | null;
  // Video-specific
  durationSeconds: number | null;
  hasAttachmentPdf?: boolean;
  chapterCount: number;
}

/**
 * Full public product detail — never includes video_url or stock_quantity.
 */
export interface PublicProductDetailResponse {
  id: number;
  name: string;
  slug: string;
  description: string | null;
  type: ProductType;
  price: number;
  thumbnailUrl: string | null;
  chapters: PublicChapterRef[];
  // Note-specific
  pageCount: number | null;
  inStock: boolean | null;
  // Video-specific
  durationSeconds: number | null;
  videoDescription: string | null;
  hasAttachmentPdf?: boolean;
  attachmentPdfName?: string | null;
  attachmentPdfSize?: number | null;
}

export interface PublicProductFilterParams {
  type?: ProductType | '';
  subjectId?: number | null;
  chapterId?: number | null;
  q?: string;
  sort?: 'newest' | 'price_asc' | 'price_desc' | '';
  page?: number;
  size?: number;
}

export interface PublicPageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}

// ---------------------------------------------------------------------------
// Public API Functions (no auth required)
// ---------------------------------------------------------------------------

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL as string;

async function publicFetch<T>(path: string): Promise<T> {
  const res = await fetch(`${API_BASE_URL}${path}`);
  if (!res.ok) {
    const err = await res.json().catch(() => ({
      timestamp: new Date().toISOString(),
      status: res.status,
      error: 'Error',
      message: `Request failed with status ${res.status}`,
    }));
    throw err;
  }
  return res.json();
}

/**
 * GET /api/products
 * Returns PUBLISHED products only with optional filters/sort/pagination.
 */
export async function getPublicProducts(
  params: PublicProductFilterParams = {}
): Promise<PublicPageResponse<PublicProductSummaryResponse>> {
  const query = new URLSearchParams();
  if (params.type) query.append('type', params.type);
  if (params.subjectId != null) query.append('subjectId', params.subjectId.toString());
  if (params.chapterId != null) query.append('chapterId', params.chapterId.toString());
  if (params.q && params.q.trim()) query.append('q', params.q.trim());
  if (params.sort) query.append('sort', params.sort);
  if (params.page !== undefined) query.append('page', params.page.toString());
  if (params.size !== undefined) query.append('size', params.size.toString());
  const qs = query.toString();
  return publicFetch<PublicPageResponse<PublicProductSummaryResponse>>(
    `/api/products${qs ? `?${qs}` : ''}`
  );
}

/**
 * GET /api/products/{slug}
 * Returns full public detail for a PUBLISHED product, or throws 404.
 */
export async function getPublicProductBySlug(
  slug: string
): Promise<PublicProductDetailResponse> {
  return publicFetch<PublicProductDetailResponse>(`/api/products/${encodeURIComponent(slug)}`);
}

/**
 * GET /api/chapters/{chapterId}/products
 * Returns PUBLISHED products for a chapter, or throws 404 if chapter not visible.
 */
export async function getChapterProducts(
  chapterId: number
): Promise<PublicProductSummaryResponse[]> {
  return publicFetch<PublicProductSummaryResponse[]>(`/api/chapters/${chapterId}/products`);
}

/**
 * GET /api/admin/products/{productId}/playback
 * Returns signed embed URL for admin preview playback.
 */
export async function getAdminProductPlayback(
  productId: number
): Promise<{ embedUrl: string; expiresAt: string }> {
  return apiFetch<{ embedUrl: string; expiresAt: string }>(
    `/api/admin/products/${productId}/playback`,
    { method: 'GET' }
  );
}
