import { apiFetch, type ApiError } from './authApi';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL as string;

export interface SubjectResponse {
  subjectId: number;
  name: string;
  description: string | null;
  status: 'ACTIVE' | 'INACTIVE';
  createdAt: string;
  updatedAt: string;
}

export interface SubjectPayload {
  name: string;
  description?: string | null;
}

export interface SubjectStatusPayload {
  status: 'ACTIVE' | 'INACTIVE';
}

/**
 * Public: Fetch all active subjects (no auth required).
 */
export async function getPublicSubjects(): Promise<SubjectResponse[]> {
  const res = await fetch(`${API_BASE_URL}/api/subjects`);
  if (!res.ok) {
    const errorBody: ApiError = await res.json().catch(() => ({
      timestamp: new Date().toISOString(),
      status: res.status,
      error: 'Error',
      message: 'Failed to fetch subjects',
    }));
    throw errorBody;
  }
  return res.json();
}

/**
 * Public: Fetch an active subject by id (404 if inactive or not found).
 */
export async function getPublicSubjectById(id: number): Promise<SubjectResponse> {
  const res = await fetch(`${API_BASE_URL}/api/subjects/${id}`);
  if (!res.ok) {
    const errorBody: ApiError = await res.json().catch(() => ({
      timestamp: new Date().toISOString(),
      status: res.status,
      error: 'Error',
      message: 'Subject not found',
    }));
    throw errorBody;
  }
  return res.json();
}

/**
 * Admin: Fetch all subjects including inactive ones.
 */
export async function getAdminSubjects(): Promise<SubjectResponse[]> {
  return apiFetch<SubjectResponse[]>('/api/admin/subjects');
}

/**
 * Admin: Create a new subject (409 on duplicate name).
 */
export async function createAdminSubject(payload: SubjectPayload): Promise<SubjectResponse> {
  return apiFetch<SubjectResponse>('/api/admin/subjects', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

/**
 * Admin: Update subject name and/or description (409 on duplicate name).
 */
export async function updateAdminSubject(id: number, payload: SubjectPayload): Promise<SubjectResponse> {
  return apiFetch<SubjectResponse>(`/api/admin/subjects/${id}`, {
    method: 'PATCH',
    body: JSON.stringify(payload),
  });
}

/**
 * Admin: Change subject status to ACTIVE or INACTIVE.
 */
export async function setAdminSubjectStatus(id: number, status: 'ACTIVE' | 'INACTIVE'): Promise<SubjectResponse> {
  return apiFetch<SubjectResponse>(`/api/admin/subjects/${id}/status`, {
    method: 'PATCH',
    body: JSON.stringify({ status }),
  });
}
