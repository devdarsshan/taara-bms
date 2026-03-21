export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  numberOfElements: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

export interface ApiErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  code: string;
  message: string;
  details: Record<string, unknown>;
}

export interface WarningResponse {
  code: string;
  message: string;
  action: string;
  details: Record<string, unknown>;
}

export type SortDirection = 'asc' | 'desc';

export interface QueryOptions {
  page: number;
  size: number;
  search?: string;
  includeDeleted?: boolean;
  sortField?: string;
  sortDirection?: SortDirection;
}
