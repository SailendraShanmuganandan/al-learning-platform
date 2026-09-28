import { apiFetch } from './authApi';
import type { UserResponse } from './authApi';

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------

export interface UpdateProfilePayload {
  fullName: string;
  phone?: string;
}

export interface AddressPayload {
  recipientName: string;
  phone: string;
  addressLine1: string;
  addressLine2?: string;
  city: string;
  district: string;
  postalCode: string;
  isDefault?: boolean;
}

export interface AddressResponse {
  addressId: number;
  userId: number;
  recipientName: string;
  phone: string;
  addressLine1: string;
  addressLine2: string | null;
  city: string;
  district: string;
  postalCode: string;
  isDefault: boolean;
  createdAt: string;
  updatedAt: string;
}

// ---------------------------------------------------------------------------
// Profile endpoints
// ---------------------------------------------------------------------------

/** GET /api/users/me */
export async function getMyProfile(): Promise<UserResponse> {
  return apiFetch<UserResponse>('/api/users/me');
}

/** PATCH /api/users/me */
export async function updateMyProfile(payload: UpdateProfilePayload): Promise<UserResponse> {
  return apiFetch<UserResponse>('/api/users/me', {
    method: 'PATCH',
    body: JSON.stringify(payload),
  });
}

// ---------------------------------------------------------------------------
// Address endpoints
// ---------------------------------------------------------------------------

/** GET /api/addresses */
export async function listAddresses(): Promise<AddressResponse[]> {
  return apiFetch<AddressResponse[]>('/api/addresses');
}

/** POST /api/addresses */
export async function createAddress(payload: AddressPayload): Promise<AddressResponse> {
  return apiFetch<AddressResponse>('/api/addresses', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

/** GET /api/addresses/:id */
export async function getAddress(id: number): Promise<AddressResponse> {
  return apiFetch<AddressResponse>(`/api/addresses/${id}`);
}

/** PATCH /api/addresses/:id */
export async function updateAddress(id: number, payload: AddressPayload): Promise<AddressResponse> {
  return apiFetch<AddressResponse>(`/api/addresses/${id}`, {
    method: 'PATCH',
    body: JSON.stringify(payload),
  });
}

/** DELETE /api/addresses/:id */
export async function deleteAddress(id: number): Promise<void> {
  return apiFetch<void>(`/api/addresses/${id}`, { method: 'DELETE' });
}

/** PATCH /api/addresses/:id — set as default by sending isDefault: true */
export async function setDefaultAddress(id: number, current: AddressResponse): Promise<AddressResponse> {
  return apiFetch<AddressResponse>(`/api/addresses/${id}`, {
    method: 'PATCH',
    body: JSON.stringify({
      recipientName: current.recipientName,
      phone: current.phone,
      addressLine1: current.addressLine1,
      addressLine2: current.addressLine2 ?? '',
      city: current.city,
      district: current.district,
      postalCode: current.postalCode,
      isDefault: true,
    }),
  });
}
