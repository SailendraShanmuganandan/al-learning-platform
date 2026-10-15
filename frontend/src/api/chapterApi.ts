import { apiFetch, type ApiError } from './authApi';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL as string;

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------

export interface ChapterResponse {
  chapterId: number;
  subjectId: number;
  name: string;
  chapterOrder: number;
  description: string | null;
  status: 'ACTIVE' | 'INACTIVE';
  createdAt: string;
  updatedAt: string;
}

export interface ChapterCreatePayload {
  subjectId: number;
  name: string;
  chapterOrder?: number | null;
  description?: string | null;
}

export interface ChapterUpdatePayload {
  name: string;
  chapterOrder?: number | null;
  description?: string | null;
}

export interface ChapterStatusPayload {
  status: 'ACTIVE' | 'INACTIVE';
}

// ---------------------------------------------------------------------------
// Public endpoints (no auth)
// ---------------------------------------------------------------------------

/**
 * GET /api/subjects/{subjectId}/chapters
 * Returns ACTIVE chapters of an ACTIVE subject, sorted by chapter_order.
 * Throws ApiError with status 404 if the subject is missing or INACTIVE.
 */
export async function getPublicChapters(subjectId: number): Promise<ChapterResponse[]> {
  const res = await fetch(`${API_BASE_URL}/api/subjects/${subjectId}/chapters`);
  if (!res.ok) {
    const errorBody: ApiError = await res.json().catch(() => ({
      timestamp: new Date().toISOString(),
      status: res.status,
      error: 'Error',
      message: 'Failed to fetch chapters',
    }));
    throw errorBody;
  }
  return res.json();
}

/**
 * GET /api/chapters/{id}
 * Returns a single ACTIVE chapter.
 * Throws ApiError with status 404 if the chapter is missing, INACTIVE, or
 * belongs to an INACTIVE subject.
 */
export async function getPublicChapter(id: number): Promise<ChapterResponse> {
  const res = await fetch(`${API_BASE_URL}/api/chapters/${id}`);
  if (!res.ok) {
    const errorBody: ApiError = await res.json().catch(() => ({
      timestamp: new Date().toISOString(),
      status: res.status,
      error: 'Error',
      message: 'Chapter not found',
    }));
    throw errorBody;
  }
  return res.json();
}

// ---------------------------------------------------------------------------
// Admin endpoints (require ADMIN JWT)
// ---------------------------------------------------------------------------

/**
 * GET /api/admin/chapters
 * Returns all chapters across all subjects for admin view.
 */
export async function getAllAdminChapters(): Promise<ChapterResponse[]> {
  return apiFetch<ChapterResponse[]>('/api/admin/chapters');
}

/**
 * GET /api/admin/subjects/{subjectId}/chapters
 * Returns all chapters including INACTIVE, sorted by chapter_order.
 */
export async function getAdminChapters(subjectId: number): Promise<ChapterResponse[]> {
  return apiFetch<ChapterResponse[]>(`/api/admin/subjects/${subjectId}/chapters`);
}

/**
 * POST /api/admin/chapters
 * Creates a chapter. Omit chapterOrder to auto-append.
 * Throws ApiError 409 if the order is already taken in the subject.
 */
export async function createAdminChapter(payload: ChapterCreatePayload): Promise<ChapterResponse> {
  return apiFetch<ChapterResponse>('/api/admin/chapters', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

/**
 * PATCH /api/admin/chapters/{id}
 * Updates name, description, and/or chapterOrder.
 * Throws ApiError 409 if the new order conflicts with an existing chapter.
 */
export async function updateAdminChapter(id: number, payload: ChapterUpdatePayload): Promise<ChapterResponse> {
  return apiFetch<ChapterResponse>(`/api/admin/chapters/${id}`, {
    method: 'PATCH',
    body: JSON.stringify(payload),
  });
}

/**
 * PATCH /api/admin/chapters/{id}/status
 * Sets chapter status to ACTIVE or INACTIVE.
 */
export async function setAdminChapterStatus(
  id: number,
  status: 'ACTIVE' | 'INACTIVE',
): Promise<ChapterResponse> {
  return apiFetch<ChapterResponse>(`/api/admin/chapters/${id}/status`, {
    method: 'PATCH',
    body: JSON.stringify({ status }),
  });
}
