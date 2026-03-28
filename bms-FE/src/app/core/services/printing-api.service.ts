import { inject, Injectable } from '@angular/core';
import { PageResponse, QueryOptions } from '../models/api.models';
import { GarmentSize, PieceAvailability } from '../models/common.models';
import {
  PrintingDashboardResponse,
  PrintingDelivery,
  PrintingDeliveryCreateRequest,
  PrintingOrder,
  PrintingOrderCreateRequest
} from '../models/printing.models';
import { StitchingOrderStatus, StitchingOrderStatusUpdateRequest } from '../models/stitching.models';
import { ApiService } from './api.service';

@Injectable({
  providedIn: 'root'
})
export class PrintingApiService {
  private readonly api = inject(ApiService);

  getOrders(
    query: QueryOptions & { styleAutoId?: string; sectionAutoId?: string; garmentSize?: GarmentSize | null; status?: StitchingOrderStatus | null; fromDate?: string; toDate?: string }
  ) {
    return this.api.get<PageResponse<PrintingOrder>>('/printing/orders', this.toOrderPageParams(query));
  }

  createOrder(payload: PrintingOrderCreateRequest) {
    return this.api.post<PrintingOrder, PrintingOrderCreateRequest>('/printing/orders', payload);
  }

  getAvailableOrderPieces(styleAutoId: string, size: GarmentSize) {
    return this.api.get<PieceAvailability>('/printing/available-order-pieces', { styleAutoId, garmentSize: size });
  }

  updateOrderStatus(orderAutoId: string, payload: StitchingOrderStatusUpdateRequest) {
    return this.api.patch<PrintingOrder, StitchingOrderStatusUpdateRequest>(`/printing/orders/${orderAutoId}/status`, payload);
  }

  deleteOrder(orderAutoId: string) {
    return this.api.delete(`/printing/orders/${orderAutoId}`);
  }

  getDeliveries(
    query: QueryOptions & { styleAutoId?: string; orderAutoId?: string; garmentSize?: GarmentSize | null; fromDate?: string; toDate?: string }
  ) {
    return this.api.get<PageResponse<PrintingDelivery>>('/printing/deliveries', this.toDeliveryPageParams(query));
  }

  createDelivery(payload: PrintingDeliveryCreateRequest) {
    return this.api.post<PrintingDelivery, PrintingDeliveryCreateRequest>('/printing/deliveries', payload);
  }

  getAvailableDeliveryPieces(orderAutoId: string) {
    return this.api.get<PieceAvailability>('/printing/available-delivery-pieces', { orderAutoId });
  }

  deleteDelivery(deliveryAutoId: string) {
    return this.api.delete(`/printing/deliveries/${deliveryAutoId}`);
  }

  getDashboard(filters?: {
    styleAutoId?: string;
    sectionAutoId?: string;
    garmentSize?: GarmentSize | null;
    status?: StitchingOrderStatus | null;
    fromDate?: string;
    toDate?: string;
  }) {
    return this.api.get<PrintingDashboardResponse>('/printing/dashboard', {
      ...filters,
      garmentSize: filters?.garmentSize
    });
  }

  private toOrderPageParams(
    query: QueryOptions & { styleAutoId?: string; sectionAutoId?: string; garmentSize?: GarmentSize | null; status?: StitchingOrderStatus | null; fromDate?: string; toDate?: string }
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

  private toDeliveryPageParams(
    query: QueryOptions & { styleAutoId?: string; orderAutoId?: string; garmentSize?: GarmentSize | null; fromDate?: string; toDate?: string }
  ) {
    const sort = query.sortField ? `${query.sortField},${query.sortDirection ?? 'asc'}` : undefined;
    const params: Record<string, string | number | boolean | undefined> = {
      page: query.page,
      size: query.size,
      styleAutoId: query.styleAutoId,
      orderAutoId: query.orderAutoId,
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
