import { HttpErrorResponse } from '@angular/common/http';
import { ApiErrorResponse, WarningResponse } from '../models/api.models';

function isApiErrorResponse(value: unknown): value is ApiErrorResponse {
  if (!value || typeof value !== 'object') {
    return false;
  }

  return 'message' in value && 'code' in value;
}

export function extractApiError(error: unknown): ApiErrorResponse | null {
  if (!(error instanceof HttpErrorResponse)) {
    return null;
  }

  return isApiErrorResponse(error.error) ? error.error : null;
}

function isWarningResponse(value: unknown): value is WarningResponse {
  if (!value || typeof value !== 'object') {
    return false;
  }

  return 'message' in value && 'code' in value && 'action' in value;
}

export function extractWarningResponse(error: unknown): WarningResponse | null {
  if (!(error instanceof HttpErrorResponse)) {
    return null;
  }

  return isWarningResponse(error.error) ? error.error : null;
}

export function getApiErrorMessage(error: unknown, fallback = 'Something went wrong. Please try again.'): string {
  if (error instanceof HttpErrorResponse) {
    const apiError = extractApiError(error);
    return apiError?.message ?? error.message ?? fallback;
  }

  if (error instanceof Error) {
    return error.message;
  }

  return fallback;
}
