import { apiFetch, type ApiError } from './authApi';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL as string;

export type ClassType = 'PHYSICAL' | 'ONLINE' | 'HYBRID';
export type ClassStatus = 'ACTIVE' | 'INACTIVE';
export type RegistrationStatus = 'PENDING' | 'CONFIRMED' | 'CANCELLED' | 'REJECTED';

export interface PublicSubjectRef {
  subjectId: number;
  name: string;
}

export interface PublicClassResponse {
  classId: number;
  name: string;
  description: string | null;
  subject: PublicSubjectRef;
  classType: ClassType;
  scheduleDescription: string;
  fee: number;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export interface StudentRegistrationResponse {
  registrationId: number;
  classId: number;
  className: string;
  subjectName: string;
  scheduleDescription: string;
  fee: number;
  status: RegistrationStatus;
  registeredAt: string;
  updatedAt: string;
}

export interface AdminClassResponse {
  classId: number;
  name: string;
  description: string | null;
  subjectId: number;
  subjectName: string;
  classType: ClassType;
  scheduleDescription: string;
  fee: number;
  status: ClassStatus;
  pendingRegistrationsCount: number;
  confirmedRegistrationsCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface CreateClassPayload {
  name: string;
  description?: string | null;
  subjectId: number;
  classType: ClassType;
  scheduleDescription: string;
  fee: number;
}

export interface UpdateClassPayload {
  name?: string;
  description?: string | null;
  classType?: ClassType;
  scheduleDescription?: string;
  fee?: number;
}

export interface AdminRegistrationResponse {
  registrationId: number;
  studentId: number;
  studentName: string;
  studentEmail: string;
  studentPhone: string | null;
  classId: number;
  className: string;
  status: RegistrationStatus;
  registeredAt: string;
  updatedAt: string;
}

export interface PublicClassFilterParams {
  page?: number;
  size?: number;
  subjectId?: number;
  class_type?: ClassType;
}

export interface AdminRegistrationFilterParams {
  page?: number;
  size?: number;
  classId?: number;
  status?: RegistrationStatus;
  q?: string;
}

// ---------------------------------------------------------------------------
// Public endpoints
// ---------------------------------------------------------------------------

export async function getPublicClasses(
  params: PublicClassFilterParams = {}
): Promise<PageResponse<PublicClassResponse>> {
  const query = new URLSearchParams();
  if (params.page !== undefined) query.set('page', params.page.toString());
  if (params.size !== undefined) query.set('size', params.size.toString());
  if (params.subjectId !== undefined) query.set('subjectId', params.subjectId.toString());
  if (params.class_type !== undefined) query.set('class_type', params.class_type);

  const qs = query.toString();
  const url = `${API_BASE_URL}/api/classes${qs ? `?${qs}` : ''}`;
  const res = await fetch(url);
  if (!res.ok) {
    const errorBody: ApiError = await res.json().catch(() => ({
      timestamp: new Date().toISOString(),
      status: res.status,
      error: 'Error',
      message: 'Failed to fetch classes',
    }));
    throw errorBody;
  }
  return res.json();
}

export async function getPublicClassById(id: number): Promise<PublicClassResponse> {
  const res = await fetch(`${API_BASE_URL}/api/classes/${id}`);
  if (!res.ok) {
    const errorBody: ApiError = await res.json().catch(() => ({
      timestamp: new Date().toISOString(),
      status: res.status,
      error: 'Error',
      message: 'Class not found',
    }));
    throw errorBody;
  }
  return res.json();
}

// ---------------------------------------------------------------------------
// Student endpoints
// ---------------------------------------------------------------------------

export async function registerForClass(
  classId: number
): Promise<{ registrationId: number; classId: number; className: string; status: string; registeredAt: string }> {
  return apiFetch<{ registrationId: number; classId: number; className: string; status: string; registeredAt: string }>(
    `/api/classes/${classId}/register`,
    {
      method: 'POST',
    }
  );
}

export async function getMyClassRegistrations(): Promise<StudentRegistrationResponse[]> {
  return apiFetch<StudentRegistrationResponse[]>('/api/my/class-registrations');
}

export async function getMyClassRegistrationById(id: number): Promise<StudentRegistrationResponse> {
  return apiFetch<StudentRegistrationResponse>(`/api/my/class-registrations/${id}`);
}

export async function cancelMyClassRegistration(id: number): Promise<StudentRegistrationResponse> {
  return apiFetch<StudentRegistrationResponse>(`/api/my/class-registrations/${id}/cancel`, {
    method: 'PATCH',
  });
}

// ---------------------------------------------------------------------------
// Admin endpoints
// ---------------------------------------------------------------------------

export async function getAdminClasses(): Promise<AdminClassResponse[]> {
  return apiFetch<AdminClassResponse[]>('/api/admin/classes');
}

export async function getAdminClassById(id: number): Promise<AdminClassResponse> {
  return apiFetch<AdminClassResponse>(`/api/admin/classes/${id}`);
}

export async function createAdminClass(payload: CreateClassPayload): Promise<AdminClassResponse> {
  return apiFetch<AdminClassResponse>('/api/admin/classes', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export async function updateAdminClass(id: number, payload: UpdateClassPayload): Promise<AdminClassResponse> {
  return apiFetch<AdminClassResponse>(`/api/admin/classes/${id}`, {
    method: 'PATCH',
    body: JSON.stringify(payload),
  });
}

export async function setAdminClassStatus(id: number, status: ClassStatus): Promise<AdminClassResponse> {
  return apiFetch<AdminClassResponse>(`/api/admin/classes/${id}/status`, {
    method: 'PATCH',
    body: JSON.stringify({ status }),
  });
}

export async function getAdminClassRegistrations(
  params: AdminRegistrationFilterParams = {}
): Promise<PageResponse<AdminRegistrationResponse>> {
  const query = new URLSearchParams();
  if (params.page !== undefined) query.set('page', params.page.toString());
  if (params.size !== undefined) query.set('size', params.size.toString());
  if (params.classId !== undefined) query.set('classId', params.classId.toString());
  if (params.status !== undefined) query.set('status', params.status);
  if (params.q && params.q.trim()) query.set('q', params.q.trim());

  const qs = query.toString();
  return apiFetch<PageResponse<AdminRegistrationResponse>>(`/api/admin/class-registrations${qs ? `?${qs}` : ''}`);
}

export async function updateAdminRegistrationStatus(
  id: number,
  status: RegistrationStatus
): Promise<AdminRegistrationResponse> {
  return apiFetch<AdminRegistrationResponse>(`/api/admin/class-registrations/${id}/status`, {
    method: 'PATCH',
    body: JSON.stringify({ status }),
  });
}

export async function getPendingClassRegistrationsCount(): Promise<number> {
  const res = await apiFetch<{ count: number }>('/api/admin/class-registrations/pending-count');
  return res.count;
}
