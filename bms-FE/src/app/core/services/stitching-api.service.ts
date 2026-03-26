import { inject, Injectable } from '@angular/core';
import { PageResponse, QueryOptions } from '../models/api.models';
import { GarmentSize } from '../models/common.models';
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

  getAvailableOrderPieces(styleAutoId: string, size: GarmentSize) {
    return this.api.get<StitchingAvailability>('/stitching/available-order-pieces', { styleAutoId, garmentSize: size });
  }

  updateOrderStatus(orderAutoId: string, payload: StitchingOrderStatusUpdateRequest) {
    return this.api.patch<StitchingOrder, StitchingOrderStatusUpdateRequest>(`/stitching/orders/${orderAutoId}/status`, payload);
  }

  deleteOrder(orderAutoId: string) {
    return this.api.delete(`/stitching/orders/${orderAutoId}`);
  }

  getDeliveries(
    query: QueryOptions & { styleAutoId?: string; sectionAutoId?: string; garmentSize?: GarmentSize | null; fromDate?: string; toDate?: string }
  ) {
    return this.api.get<PageResponse<StitchingDelivery>>('/stitching/deliveries', this.toPageParams(query));
  }

  createDelivery(payload: StitchingDeliveryCreateRequest) {
    return this.api.post<StitchingDelivery, StitchingDeliveryCreateRequest>('/stitching/deliveries', payload);
  }

  getAvailableDeliveryPieces(sectionAutoId: string, styleAutoId: string, size: GarmentSize) {
    return this.api.get<StitchingAvailability>('/stitching/available-delivery-pieces', {
      sectionAutoId,
      styleAutoId,
      garmentSize: size
    });
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
    query: QueryOptions & { styleAutoId?: string; sectionAutoId?: string; garmentSize?: GarmentSize | null; status?: string | null; fromDate?: string; toDate?: string }
  ) {
    const sort = query.sortField ? `${query.sortField},${query.sortDirection ?? 'asc'}` : undefined;
    const params: Record<string, string | number | boolean | undefined> = {
      page: query.page,
      size: query.size,
      styleAutoId: query.styleAutoId,
      sectionAutoId: query.sectionAutoId,
      status: query.status ?? undefined,
      fromDate: query.fromDate,
      toDate: query.toDate,
      includeDeleted: query.includeDeleted ?? false,
      sort
    };
    if (query.garmentSize) {
      params['garmentSize'] = query.garmentSize;
    }
    return params;
  }
}
