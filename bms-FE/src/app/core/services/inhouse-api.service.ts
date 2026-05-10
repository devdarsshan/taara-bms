import { inject, Injectable } from '@angular/core';
import { PageResponse, QueryOptions } from '../models/api.models';
import {
  CuttingAvailability,
  CuttingCreateRequest,
  CuttingEntry,
  CuttingStatus,
  CuttingUpdateRequest,
  ExistingStockCreateRequest,
  ExistingStockResponse,
  InHouseDashboardResponse,
  InHouseDelivery,
  InHouseSplit,
  InHouseSplitBatchRequest,
  InHouseStock,
  StitchedStock
} from '../models/inhouse.models';
import { ApiService } from './api.service';

@Injectable({
  providedIn: 'root'
})
export class InHouseApiService {
  private readonly api = inject(ApiService);

  getDeliveries(query: QueryOptions & { styleAutoId?: string; fromDate?: string; toDate?: string }) {
    return this.api.get<PageResponse<InHouseDelivery>>('/inhouse/deliveries', this.toPageParams(query));
  }

  getSplits(deliveryAutoId: string) {
    return this.api.get<InHouseSplit[]>(`/inhouse/deliveries/${deliveryAutoId}/splits`);
  }

  createSplits(deliveryAutoId: string, payload: InHouseSplitBatchRequest) {
    return this.api.post<InHouseSplit[], InHouseSplitBatchRequest>(`/inhouse/deliveries/${deliveryAutoId}/splits`, payload);
  }

  updateSplit(deliveryAutoId: string, splitAutoId: string, payload: { diaAutoId: string; quantityKgs: number }) {
    return this.api.patch<InHouseSplit, { diaAutoId: string; quantityKgs: number }>(`/inhouse/deliveries/${deliveryAutoId}/splits/${splitAutoId}`, payload);
  }

  deleteSplit(deliveryAutoId: string, splitAutoId: string) {
    return this.api.delete(`/inhouse/deliveries/${deliveryAutoId}/splits/${splitAutoId}`);
  }

  getStock(filters?: { diaAutoId?: string; styleAutoId?: string }) {
    return this.api.get<InHouseStock[]>('/inhouse/stock', filters);
  }

  createExistingStock(payload: ExistingStockCreateRequest) {
    return this.api.post<ExistingStockResponse, ExistingStockCreateRequest>('/inhouse/existing-stocks', payload);
  }

  getExistingStocks() {
    return this.api.get<ExistingStockResponse[]>('/inhouse/existing-stocks');
  }

  getCuttings(
    query: QueryOptions & { diaAutoId?: string; styleAutoId?: string; status?: CuttingStatus | null; fromDate?: string; toDate?: string }
  ) {
    return this.api.get<PageResponse<CuttingEntry>>('/inhouse/cuttings', this.toPageParams(query));
  }

  createCutting(payload: CuttingCreateRequest) {
    return this.api.post<CuttingEntry, CuttingCreateRequest>('/inhouse/cuttings', payload);
  }

  getAvailableCuttingQuantity(diaAutoId: string, styleAutoId: string) {
    return this.api.get<CuttingAvailability>('/inhouse/cuttings/available-quantity', { diaAutoId, styleAutoId });
  }

  updateCutting(cuttingAutoId: string, payload: CuttingUpdateRequest) {
    return this.api.patch<CuttingEntry, CuttingUpdateRequest>(`/inhouse/cuttings/${cuttingAutoId}`, payload);
  }

  deleteCutting(cuttingAutoId: string) {
    return this.api.delete(`/inhouse/cuttings/${cuttingAutoId}`);
  }

  getStitchedStock(filters?: { styleAutoId?: string; fromDate?: string; toDate?: string }) {
    return this.api.get<StitchedStock[]>('/inhouse/stitched-stock', filters);
  }

  getDashboard(filters?: { diaAutoId?: string; styleAutoId?: string; fromDate?: string; toDate?: string }) {
    return this.api.get<InHouseDashboardResponse>('/inhouse/dashboard', filters);
  }

  private toPageParams(
    query: QueryOptions & { diaAutoId?: string; styleAutoId?: string; status?: string | null; fromDate?: string; toDate?: string }
  ) {
    const sort = query.sortField ? `${query.sortField},${query.sortDirection ?? 'asc'}` : undefined;

    return {
      page: query.page,
      size: query.size,
      diaAutoId: query.diaAutoId,
      styleAutoId: query.styleAutoId,
      status: query.status ?? undefined,
      fromDate: query.fromDate,
      toDate: query.toDate,
      includeDeleted: query.includeDeleted ?? false,
      sort
    };
  }
}
