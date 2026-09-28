const API_BASE_URL = import.meta.env.VITE_API_BASE_URL as string;
const SESSION_TOKEN_KEY = 'al_session_token';

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------

export interface RegisterPayload {
  fullName: string;
  email: string;
  password: string;
  phone?: string;
}

export interface LoginPayload {
  email: string;
  password: string;
}

export interface UserResponse {
  userId: number;
  fullName: string;
  email: string;
  phone: string | null;
  role: 'STUDENT' | 'ADMIN';
  accountStatus: 'ACTIVE' | 'SUSPENDED' | 'DELETED';
  isEmailVerified: boolean;
  is_email_verified?: boolean;
  createdAt: string;
}

export interface AuthResponse {
  token: string;
  tokenType: string;
  user: UserResponse;
}

export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  fields?: Record<string, string>;
}

export interface MessageResponse {
  message: string;
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

async function handleResponse<T>(res: Response): Promise<T> {
  if (res.ok) {
    if (res.status === 204 || res.headers.get('content-length') === '0') {
      return undefined as T;
    }
    return res.json() as Promise<T>;
  }
  // Parse the structured error body from GlobalExceptionHandler
  const errorBody: ApiError = await res.json().catch(() => ({
    timestamp: new Date().toISOString(),
    status: res.status,
    error: 'Unknown Error',
    message: 'An unexpected error occurred',
  }));

  // Shared handler for EMAIL_NOT_VERIFIED error code across any future endpoint
  if (errorBody.error === 'EMAIL_NOT_VERIFIED') {
    window.dispatchEvent(
      new CustomEvent('al:email-not-verified', {
        detail: {
          error: errorBody.error,
          message: errorBody.message,
          status: errorBody.status,
        },
      })
    );
  }

  throw errorBody;
}

/**
 * Authenticated fetch wrapper that automatically attaches the stored JWT as a
 * Bearer token. Any component or service that calls authenticated endpoints
 * should use this instead of raw `fetch`.
 */
export async function apiFetch<T>(
  path: string,
  options: RequestInit = {},
): Promise<T> {
  const token = sessionStorage.getItem(SESSION_TOKEN_KEY);
  const headers: HeadersInit = {
    'Content-Type': 'application/json',
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...(options.headers ?? {}),
  };
  const res = await fetch(`${API_BASE_URL}${path}`, { ...options, headers });
  return handleResponse<T>(res);
}

// ---------------------------------------------------------------------------
// Auth endpoints
// ---------------------------------------------------------------------------

/**
 * POST /api/auth/register
 * Returns the created user's safe fields on 201.
 * Throws an {@link ApiError} on 4xx / 5xx.
 */
export async function registerUser(payload: RegisterPayload): Promise<UserResponse> {
  const res = await fetch(`${API_BASE_URL}/api/auth/register`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  });
  return handleResponse<UserResponse>(res);
}

/**
 * POST /api/auth/login
 * Returns the JWT token and user profile on 200.
 * Throws an {@link ApiError} on 4xx / 5xx.
 */
export async function loginUser(payload: LoginPayload): Promise<AuthResponse> {
  const res = await fetch(`${API_BASE_URL}/api/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  });
  return handleResponse<AuthResponse>(res);
}

/**
 * GET /api/auth/me
 * Returns the authenticated user's profile on 200.
 * Requires a valid Bearer token.
 * Throws an {@link ApiError} on 4xx / 5xx.
 */
export async function getCurrentUser(token: string): Promise<UserResponse> {
  const res = await fetch(`${API_BASE_URL}/api/auth/me`, {
    method: 'GET',
    headers: {
      Authorization: `Bearer ${token}`,
    },
  });
  return handleResponse<UserResponse>(res);
}

/**
 * GET /api/auth/verify-email?token=...
 * Validates verification token and marks email as verified.
 */
export async function verifyEmail(token: string): Promise<MessageResponse> {
  const res = await fetch(`${API_BASE_URL}/api/auth/verify-email?token=${encodeURIComponent(token)}`);
  return handleResponse<MessageResponse>(res);
}

/**
 * POST /api/auth/resend-verification
 * Authenticated request to resend the verification email to the current user.
 */
export async function resendVerification(): Promise<MessageResponse> {
  return apiFetch<MessageResponse>('/api/auth/resend-verification', {
    method: 'POST',
  });
}

/**
 * POST /api/auth/forgot-password
 * Public. Sends a reset email if the account exists; always returns the same generic message.
 */
export async function forgotPassword(email: string): Promise<MessageResponse> {
  const res = await fetch(`${API_BASE_URL}/api/auth/forgot-password`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email }),
  });
  return handleResponse<MessageResponse>(res);
}

/**
 * POST /api/auth/reset-password
 * Public. Validates the token and sets the new password.
 * Throws ApiError with status 400 if token is invalid, expired, or already used.
 */
export async function resetPassword(token: string, newPassword: string): Promise<MessageResponse> {
  const res = await fetch(`${API_BASE_URL}/api/auth/reset-password`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ token, newPassword }),
  });
  return handleResponse<MessageResponse>(res);
}
