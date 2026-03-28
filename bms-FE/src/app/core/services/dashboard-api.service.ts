import { inject, Injectable } from '@angular/core';
import { forkJoin } from 'rxjs';
import {
  InHouseDashboardResponse,
  OverviewDashboardBundle,
  SpinningDashboardResponse,
  StitchingDashboardResponse,
  YarnDashboardResponse
} from '../models/dashboard.models';
import { ApiService } from './api.service';

@Injectable({
  providedIn: 'root'
})
export class DashboardApiService {
  private readonly api = inject(ApiService);

  getYarnDashboard() {
    return this.api.get<YarnDashboardResponse>('/yarn/dashboard');
  }

  getSpinningDashboard() {
    return this.api.get<SpinningDashboardResponse>('/spinning/dashboard');
  }

  getInHouseDashboard() {
    return this.api.get<InHouseDashboardResponse>('/inhouse/dashboard');
  }

  getStitchingDashboard() {
    return this.api.get<StitchingDashboardResponse>('/stitching/dashboard');
  }

  getOverviewDashboards() {
    return forkJoin({
      yarn: this.getYarnDashboard(),
      spinning: this.getSpinningDashboard(),
      inHouse: this.getInHouseDashboard(),
      stitching: this.getStitchingDashboard()
    });
  }
}
