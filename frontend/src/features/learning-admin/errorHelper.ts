import type { UseFormSetError, FieldValues, Path } from 'react-hook-form';

export interface BackendApiError {
  timestamp?: string;
  status?: number;
  error?: string;
  message?: string;
  code?: string;
  fields?: Record<string, string>;
}

export function getErrorMessage(err: unknown): string {
  if (!err) return 'An unexpected error occurred.';
  const apiErr = err as BackendApiError;

  // Check custom backend code or standard error string
  const code = apiErr.code || apiErr.error;

  switch (code) {
    case 'INVALID_STATUS_TRANSITION':
      return apiErr.message || 'This status transition is not allowed.';
    case 'OPTIONS_IMMUTABLE':
      return 'This question already has student answers. Changing options or the correct answer is not permitted. Please archive it and create a replacement.';
    case 'DUPLICATE_TOPIC_NAME':
      return 'A topic with this name already exists in this chapter.';
    case 'NOT_ENOUGH_QUESTIONS':
      return apiErr.message || 'Not enough eligible questions exist to fulfill this request.';
    case 'UNAUTHORIZED':
      return 'Your session has expired. Please log in again.';
    case 'FORBIDDEN':
      return 'You do not have permission to perform this action.';
    case 'NOT_FOUND':
      return apiErr.message || 'The requested resource was not found.';
    default:
      if (apiErr.status === 401) return 'Session expired. Please sign in again.';
      if (apiErr.status === 403) return 'Access denied. Administrator privileges required.';
      if (apiErr.status === 404) return apiErr.message || 'Resource not found.';
      if (apiErr.status === 409) return apiErr.message || 'A conflicting change occurred. Please refresh and try again.';
      return apiErr.message || 'Operation failed. Please try again.';
  }
}

/**
 * Maps backend validation `fields` map to react-hook-form errors, if present.
 */
export function applyFormFieldErrors<T extends FieldValues>(
  err: unknown,
  setError: UseFormSetError<T>
): boolean {
  const apiErr = err as BackendApiError;
  if (apiErr && apiErr.fields && typeof apiErr.fields === 'object') {
    let appliedAny = false;
    for (const [field, message] of Object.entries(apiErr.fields)) {
      try {
        setError(field as Path<T>, { type: 'server', message });
        appliedAny = true;
      } catch {
        // field might not be part of form
      }
    }
    return appliedAny;
  }
  return false;
}

