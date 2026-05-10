import { inject, Injectable } from '@angular/core';
import { PageResponse, QueryOptions } from '../models/api.models';
import { YarnDashboardResponse, YarnOrder, YarnOrderCreateRequest, YarnOrderUpdateRequest } from '../models/yarn.models';
import { ApiService } from './api.service';

@Injectable({
  providedIn: 'root'
})
export class YarnApiService {
  private readonly api = inject(ApiService);

  getOrders(query: QueryOptions & { styleAutoId?: string; sectionAutoId?: string; fromDate?: string; toDate?: string }) {
    return this.api.get<PageResponse<YarnOrder>>('/yarn/orders', this.toPageParams(query));
  }

  createOrder(payload: YarnOrderCreateRequest) {
    return this.api.post<YarnOrder, YarnOrderCreateRequest>('/yarn/orders', payload);
  }

  updateOrder(autoId: string, payload: YarnOrderUpdateRequest) {
    return this.api.put<YarnOrder, YarnOrderUpdateRequest>(`/yarn/orders/${autoId}`, payload);
  }

  deleteOrder(autoId: string) {
    return this.api.delete(`/yarn/orders/${autoId}`);
  }

  getDashboard(filters?: { styleAutoId?: string; sectionAutoId?: string; fromDate?: string; toDate?: string }) {
    return this.api.get<YarnDashboardResponse>('/yarn/dashboard', filters);
  }

  private toPageParams(query: QueryOptions & { styleAutoId?: string; sectionAutoId?: string; fromDate?: string; toDate?: string }) {
    const sort = query.sortField ? `${query.sortField},${query.sortDirection ?? 'asc'}` : undefined;

    return {
      page: query.page,
      size: query.size,
      styleAutoId: query.styleAutoId,
      sectionAutoId: query.sectionAutoId,
      fromDate: query.fromDate,
      toDate: query.toDate,
      includeDeleted: query.includeDeleted ?? false,
      sort
    };
  }
}
