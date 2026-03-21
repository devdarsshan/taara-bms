import { inject, Injectable } from '@angular/core';
import { map } from 'rxjs';
import { PageResponse, QueryOptions } from '../models/api.models';
import {
  Dia,
  DiaUpsertRequest,
  MasterDashboardResponse,
  StitchingSection,
  StitchingSectionUpsertRequest,
  Style,
  StyleUpsertRequest
} from '../models/master-data.models';
import { ApiService } from './api.service';

@Injectable({
  providedIn: 'root'
})
export class MasterDataApiService {
  private readonly api = inject(ApiService);

  getDashboard() {
    return this.api.get<MasterDashboardResponse>('/master/dashboard');
  }

  getStyles(query: QueryOptions) {
    return this.api.get<PageResponse<Style>>('/master/styles', this.toPageParams(query));
  }

  getStyleOptions() {
    return this.getStyles({
      page: 0,
      size: 200,
      sortField: 'styleName',
      sortDirection: 'asc',
      includeDeleted: false
    }).pipe(map((page) => page.content));
  }

  createStyle(payload: StyleUpsertRequest) {
    return this.api.post<Style, StyleUpsertRequest>('/master/styles', payload);
  }

  updateStyle(autoId: string, payload: StyleUpsertRequest) {
    return this.api.put<Style, StyleUpsertRequest>(`/master/styles/${autoId}`, payload);
  }

  deleteStyle(autoId: string) {
    return this.api.delete(`/master/styles/${autoId}`);
  }

  getDias(query: QueryOptions) {
    return this.api.get<PageResponse<Dia>>('/master/dias', this.toPageParams(query));
  }

  getDiaOptions() {
    return this.getDias({
      page: 0,
      size: 200,
      sortField: 'diaValue',
      sortDirection: 'asc',
      includeDeleted: false
    }).pipe(map((page) => page.content));
  }

  createDia(payload: DiaUpsertRequest) {
    return this.api.post<Dia, DiaUpsertRequest>('/master/dias', payload);
  }

  updateDia(autoId: string, payload: DiaUpsertRequest) {
    return this.api.put<Dia, DiaUpsertRequest>(`/master/dias/${autoId}`, payload);
  }

  deleteDia(autoId: string) {
    return this.api.delete(`/master/dias/${autoId}`);
  }

  getSections(query: QueryOptions) {
    return this.api.get<PageResponse<StitchingSection>>('/master/stitching-sections', this.toPageParams(query));
  }

  getSectionOptions() {
    return this.getSections({
      page: 0,
      size: 200,
      sortField: 'sectionName',
      sortDirection: 'asc',
      includeDeleted: false
    }).pipe(map((page) => page.content));
  }

  createSection(payload: StitchingSectionUpsertRequest) {
    return this.api.post<StitchingSection, StitchingSectionUpsertRequest>('/master/stitching-sections', payload);
  }

  updateSection(autoId: string, payload: StitchingSectionUpsertRequest) {
    return this.api.put<StitchingSection, StitchingSectionUpsertRequest>(`/master/stitching-sections/${autoId}`, payload);
  }

  deleteSection(autoId: string) {
    return this.api.delete(`/master/stitching-sections/${autoId}`);
  }

  private toPageParams(query: QueryOptions): Record<string, string | number | boolean | undefined> {
    const sort = query.sortField ? `${query.sortField},${query.sortDirection ?? 'asc'}` : undefined;

    return {
      page: query.page,
      size: query.size,
      search: query.search,
      includeDeleted: query.includeDeleted ?? false,
      sort
    };
  }
}
