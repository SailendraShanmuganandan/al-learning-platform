import { apiFetch } from './authApi';
import type { PageResponse } from './classApi';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL as string;

export type AnnouncementStatus = 'DRAFT' | 'PUBLISHED' | 'ARCHIVED';
export type ContactMessageStatus = 'UNREAD' | 'READ' | 'RESPONDED' | 'ARCHIVED';

export interface AnnouncementListItemResponse {
  id: number;
  title: string;
  excerpt: string;
  publishedAt: string;
}

export interface AnnouncementDetailResponse {
  id: number;
  title: string;
  content: string;
  publishedAt: string;
}

export interface AdminAnnouncementResponse {
  id: number;
  title: string;
  content: string;
  status: AnnouncementStatus;
  createdBy: number;
  createdByName: string;
  publishedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface AnnouncementCreateRequest {
  title: string;
  content: string;
}

export interface AnnouncementUpdateRequest {
  title: string;
  content: string;
}

export interface ContactMessageRequest {
  name: string;
  email: string;
  phone?: string;
  message: string;
  website?: string;
}

export interface ContactSuccessResponse {
  success: boolean;
  message: string;
}

export interface AdminContactMessageListItem {
  id: number;
  name: string;
  email: string;
  phone: string | null;
  messageExcerpt: string;
  status: ContactMessageStatus;
  createdAt: string;
}

export interface AdminContactMessageDetail {
  id: number;
  name: string;
  email: string;
  phone: string | null;
  message: string;
  status: ContactMessageStatus;
  createdAt: string;
  updatedAt: string;
}

export interface UnreadCountResponse {
  unreadCount: number;
  count: number;
}

// =============================================================================
// Public Announcements API
// =============================================================================

export async function getPublishedAnnouncements(
  page: number = 0,
  size: number = 10
): Promise<PageResponse<AnnouncementListItemResponse>> {
  const query = new URLSearchParams({
    page: String(page),
    size: String(size),
  });
  return apiFetch<PageResponse<AnnouncementListItemResponse>>(
    `${API_BASE_URL}/api/announcements?${query.toString()}`
  );
}

export async function getPublishedAnnouncement(id: number): Promise<AnnouncementDetailResponse> {
  return apiFetch<AnnouncementDetailResponse>(`${API_BASE_URL}/api/announcements/${id}`);
}

// =============================================================================
// Admin Announcements API
// =============================================================================

export async function getAdminAnnouncements(
  status?: AnnouncementStatus,
  page: number = 0,
  size: number = 20
): Promise<PageResponse<AdminAnnouncementResponse>> {
  const query = new URLSearchParams({
    page: String(page),
    size: String(size),
  });
  if (status) query.append('status', status);

  return apiFetch<PageResponse<AdminAnnouncementResponse>>(
    `${API_BASE_URL}/api/admin/announcements?${query.toString()}`
  );
}

export async function getAdminAnnouncement(id: number): Promise<AdminAnnouncementResponse> {
  return apiFetch<AdminAnnouncementResponse>(`${API_BASE_URL}/api/admin/announcements/${id}`);
}

export async function createAnnouncement(
  data: AnnouncementCreateRequest
): Promise<AdminAnnouncementResponse> {
  return apiFetch<AdminAnnouncementResponse>(`${API_BASE_URL}/api/admin/announcements`, {
    method: 'POST',
    body: JSON.stringify(data),
  });
}

export async function updateAnnouncement(
  id: number,
  data: AnnouncementUpdateRequest
): Promise<AdminAnnouncementResponse> {
  return apiFetch<AdminAnnouncementResponse>(`${API_BASE_URL}/api/admin/announcements/${id}`, {
    method: 'PATCH',
    body: JSON.stringify(data),
  });
}

export async function updateAnnouncementStatus(
  id: number,
  status: AnnouncementStatus
): Promise<AdminAnnouncementResponse> {
  return apiFetch<AdminAnnouncementResponse>(`${API_BASE_URL}/api/admin/announcements/${id}/status`, {
    method: 'PATCH',
    body: JSON.stringify({ status }),
  });
}

// =============================================================================
// Public Contact API
// =============================================================================

export async function submitContact(data: ContactMessageRequest): Promise<ContactSuccessResponse> {
  return apiFetch<ContactSuccessResponse>(`${API_BASE_URL}/api/contact`, {
    method: 'POST',
    body: JSON.stringify(data),
  });
}

// =============================================================================
// Admin Contact Messages API
// =============================================================================

export async function getAdminContactMessages(
  status?: ContactMessageStatus,
  q?: string,
  page: number = 0,
  size: number = 20
): Promise<PageResponse<AdminContactMessageListItem>> {
  const query = new URLSearchParams({
    page: String(page),
    size: String(size),
  });
  if (status) query.append('status', status);
  if (q && q.trim()) query.append('q', q.trim());

  return apiFetch<PageResponse<AdminContactMessageListItem>>(
    `${API_BASE_URL}/api/admin/contact-messages?${query.toString()}`
  );
}

export async function getAdminContactMessage(id: number): Promise<AdminContactMessageDetail> {
  return apiFetch<AdminContactMessageDetail>(`${API_BASE_URL}/api/admin/contact-messages/${id}`);
}

export async function updateContactMessageStatus(
  id: number,
  status: ContactMessageStatus
): Promise<AdminContactMessageDetail> {
  return apiFetch<AdminContactMessageDetail>(`${API_BASE_URL}/api/admin/contact-messages/${id}/status`, {
    method: 'PATCH',
    body: JSON.stringify({ status }),
  });
}

export async function getUnreadContactMessagesCount(): Promise<number> {
  const res = await apiFetch<UnreadCountResponse>(
    `${API_BASE_URL}/api/admin/contact-messages/unread-count`
  );
  return res.unreadCount ?? res.count ?? 0;
}
