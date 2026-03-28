import { inject, Injectable } from '@angular/core';
import { PageResponse, QueryOptions } from '../models/api.models';
import {
  SpinningDashboardResponse,
  SpinningDelivery,
  SpinningDeliveryCreateRequest,
  SpinningOrder,
  SpinningOrderCreateRequest
} from '../models/spinning.models';
import { ApiService } from './api.service';

@Injectable({
  providedIn: 'root'
})
export class SpinningApiService {
  private readonly api = inject(ApiService);

  getOrders(query: QueryOptions & { styleAutoId?: string; fromDate?: string; toDate?: string; linkedYarnOrder?: boolean | null }) {
    return this.api.get<PageResponse<SpinningOrder>>('/spinning/orders', this.toPageParams(query));
  }

  createOrder(payload: SpinningOrderCreateRequest) {
    return this.api.post<SpinningOrder, SpinningOrderCreateRequest>('/spinning/orders', payload);
  }

  deleteOrder(autoId: string) {
    return this.api.delete(`/spinning/orders/${autoId}`);
  }

  getDeliveries(query: QueryOptions & { styleAutoId?: string; fromDate?: string; toDate?: string }) {
    return this.api.get<PageResponse<SpinningDelivery>>('/spinning/deliveries', this.toPageParams(query));
  }

  createDelivery(payload: SpinningDeliveryCreateRequest) {
    return this.api.post<SpinningDelivery, SpinningDeliveryCreateRequest>('/spinning/deliveries', payload);
  }

  deleteDelivery(autoId: string) {
    return this.api.delete(`/spinning/deliveries/${autoId}`);
  }

  getDashboard(filters?: { styleAutoId?: string; fromDate?: string; toDate?: string }) {
    return this.api.get<SpinningDashboardResponse>('/spinning/dashboard', filters);
  }

  private toPageParams(
    query: QueryOptions & { styleAutoId?: string; fromDate?: string; toDate?: string; linkedYarnOrder?: boolean | null }
  ) {
    const sort = query.sortField ? `${query.sortField},${query.sortDirection ?? 'asc'}` : undefined;

    return {
      page: query.page,
      size: query.size,
      styleAutoId: query.styleAutoId,
      fromDate: query.fromDate,
      toDate: query.toDate,
      linkedYarnOrder: query.linkedYarnOrder ?? undefined,
      includeDeleted: query.includeDeleted ?? false,
      sort
    };
  }
}
