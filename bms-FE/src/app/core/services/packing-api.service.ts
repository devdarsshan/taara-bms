import { inject, Injectable } from '@angular/core';
import { PageResponse, QueryOptions } from '../models/api.models';
import { GarmentSize, PieceAvailability } from '../models/common.models';
import { PackingCreateRequest, PackingDashboardResponse, PackingEntry, PackingStockType, PackingUpdateRequest } from '../models/packing.models';
import { ApiService } from './api.service';

@Injectable({
  providedIn: 'root'
})
export class PackingApiService {
  private readonly api = inject(ApiService);

  getEntries(
    query: QueryOptions & { styleAutoId?: string; garmentSize?: GarmentSize | null; stockType?: PackingStockType | null; fromDate?: string; toDate?: string }
  ) {
    const sort = query.sortField ? `${query.sortField},${query.sortDirection ?? 'asc'}` : undefined;
    const params: Record<string, string | number | boolean | undefined> = {
      page: query.page,
      size: query.size,
      styleAutoId: query.styleAutoId,
      stockType: query.stockType ?? undefined,
      fromDate: query.fromDate,
      toDate: query.toDate,
      includeDeleted: query.includeDeleted ?? false,
      sort
    };
    if (query.garmentSize) {
      params['garmentSize'] = query.garmentSize;
    }
    return this.api.get<PageResponse<PackingEntry>>('/packing/entries', params);
  }

  createEntry(payload: PackingCreateRequest) {
    return this.api.post<PackingEntry, PackingCreateRequest>('/packing/entries', payload);
  }

  updateEntry(packingAutoId: string, payload: PackingUpdateRequest) {
    return this.api.put<PackingEntry, PackingUpdateRequest>(`/packing/entries/${packingAutoId}`, payload);
  }

  getAvailablePieces(styleAutoId: string, size: GarmentSize, stockType: PackingStockType) {
    return this.api.get<PieceAvailability>('/packing/available-pieces', { styleAutoId, garmentSize: size, stockType });
  }

  deleteEntry(packingAutoId: string) {
    return this.api.delete(`/packing/entries/${packingAutoId}`);
  }

  getDashboard(filters?: { styleAutoId?: string; garmentSize?: GarmentSize | null; stockType?: PackingStockType | null; fromDate?: string; toDate?: string }) {
    return this.api.get<PackingDashboardResponse>('/packing/dashboard', {
      styleAutoId: filters?.styleAutoId,
      garmentSize: filters?.garmentSize ?? undefined,
      stockType: filters?.stockType ?? undefined,
      fromDate: filters?.fromDate,
      toDate: filters?.toDate
    });
  }
}
