import { inject, Injectable } from '@angular/core';
import { PageResponse, QueryOptions } from '../models/api.models';
import {
  StitchingAvailability,
  StitchingDashboardResponse,
  StitchingDelivery,
  StitchingDeliveryCreateRequest,
  StitchingOrder,
  StitchingOrderCreateRequest,
  StitchingOrderStatus,
  StitchingOrderStatusUpdateRequest
} from '../models/stitching.models';
import { ApiService } from './api.service';

@Injectable({
  providedIn: 'root'
})
export class StitchingApiService {
  private readonly api = inject(ApiService);

  getOrders(
    query: QueryOptions & { styleAutoId?: string; sectionAutoId?: string; status?: StitchingOrderStatus | null; fromDate?: string; toDate?: string }
  ) {
    return this.api.get<PageResponse<StitchingOrder>>('/stitching/orders', this.toPageParams(query));
  }

  createOrder(payload: StitchingOrderCreateRequest) {
    return this.api.post<StitchingOrder, StitchingOrderCreateRequest>('/stitching/orders', payload);
  }

  getAvailableOrderPieces(styleAutoId: string) {
    return this.api.get<StitchingAvailability>('/stitching/available-order-pieces', { styleAutoId });
  }

  updateOrderStatus(orderAutoId: string, payload: StitchingOrderStatusUpdateRequest) {
    return this.api.patch<StitchingOrder, StitchingOrderStatusUpdateRequest>(`/stitching/orders/${orderAutoId}/status`, payload);
  }

  deleteOrder(orderAutoId: string) {
    return this.api.delete(`/stitching/orders/${orderAutoId}`);
  }

  getDeliveries(
    query: QueryOptions & { orderAutoId?: string; styleAutoId?: string; sectionAutoId?: string; fromDate?: string; toDate?: string }
  ) {
    return this.api.get<PageResponse<StitchingDelivery>>('/stitching/deliveries', this.toPageParams(query));
  }

  createDelivery(payload: StitchingDeliveryCreateRequest) {
    return this.api.post<StitchingDelivery, StitchingDeliveryCreateRequest>('/stitching/deliveries', payload);
  }

  getAvailableDeliveryPieces(orderAutoId: string) {
    return this.api.get<StitchingAvailability>(`/stitching/orders/${orderAutoId}/available-delivery-pieces`);
  }

  deleteDelivery(deliveryAutoId: string) {
    return this.api.delete(`/stitching/deliveries/${deliveryAutoId}`);
  }

  getDashboard(filters?: {
    styleAutoId?: string;
    sectionAutoId?: string;
    status?: StitchingOrderStatus | null;
    fromDate?: string;
    toDate?: string;
  }) {
    return this.api.get<StitchingDashboardResponse>('/stitching/dashboard', filters);
  }

  private toPageParams(
    query: QueryOptions & { orderAutoId?: string; styleAutoId?: string; sectionAutoId?: string; status?: string | null; fromDate?: string; toDate?: string }
  ) {
    const sort = query.sortField ? `${query.sortField},${query.sortDirection ?? 'asc'}` : undefined;

    return {
      page: query.page,
      size: query.size,
      orderAutoId: query.orderAutoId,
      styleAutoId: query.styleAutoId,
      sectionAutoId: query.sectionAutoId,
      status: query.status ?? undefined,
      fromDate: query.fromDate,
      toDate: query.toDate,
      includeDeleted: query.includeDeleted ?? false,
      sort
    };
  }
}
