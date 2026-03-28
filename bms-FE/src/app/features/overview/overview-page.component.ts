import { DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { SkeletonModule } from 'primeng/skeleton';
import { OverviewDashboardBundle } from '../../core/models/dashboard.models';
import { DashboardApiService } from '../../core/services/dashboard-api.service';
import { getApiErrorMessage } from '../../core/utils/api-error.utils';

type SummaryCard = {
  label: string;
  value: string;
  eyebrow: string;
  note: string;
  tone: 'ocean' | 'teal' | 'indigo' | 'amber';
  icon: string;
};

type ModuleCard = {
  title: string;
  eyebrow: string;
  description: string;
  route: string;
  tone: 'ocean' | 'teal' | 'indigo' | 'amber' | 'slate';
  icon: string;
  primaryLabel: string;
  primaryValue: string;
  metrics: Array<{
    label: string;
    value: string;
  }>;
};

@Component({
  selector: 'app-overview-page',
  standalone: true,
  imports: [RouterLink, DecimalPipe, ButtonModule, SkeletonModule],
  templateUrl: './overview-page.component.html',
  styleUrl: './overview-page.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class OverviewPageComponent {
  private readonly dashboardApi = inject(DashboardApiService);
  private readonly destroyRef = inject(DestroyRef);

  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);
  readonly dashboards = signal<OverviewDashboardBundle | null>(null);

  readonly summaryCards = computed<SummaryCard[]>(() => {
    const data = this.dashboards();

    if (!data) {
      return [];
    }

    return [
      {
        label: 'Yarn Ordered',
        value: `${this.formatNumber(data.yarn.totalYarnOrdered)} kg`,
        eyebrow: 'Supply intake',
        note: `${this.formatNumber(data.yarn.yarnDispatchedToSpinning)} kg dispatched to knitting`,
        tone: 'ocean',
        icon: 'pi pi-box'
      },
      {
        label: 'Factory Pending',
        value: `${this.formatNumber(data.spinning.netPendingAtFactory)} kg`,
        eyebrow: 'Knitting floor',
        note: `${this.formatNumber(data.spinning.totalReceivedFromSpinning)} kg received from knitting`,
        tone: 'teal',
        icon: 'pi pi-sync'
      },
      {
        label: 'Fabric In Stock',
        value: `${this.formatNumber(data.inHouse.totalFabricInStock)} kg`,
        eyebrow: 'In-house stock',
        note: `${this.formatNumber(data.inHouse.totalPiecesCut)} pieces cut so far`,
        tone: 'indigo',
        icon: 'pi pi-building'
      },
      {
        label: 'Stitched Plain Stock',
        value: this.formatNumber(data.inHouse.stitchedPlainStockTotal),
        eyebrow: 'Ready output',
        note: `${this.formatNumber(data.inHouse.defectiveStockTotal)} defective pieces finalized`,
        tone: 'amber',
        icon: 'pi pi-check-circle'
      }
    ];
  });

  readonly moduleCards = computed<ModuleCard[]>(() => {
    const data = this.dashboards();

    if (!data) {
      return [];
    }

    return [
      {
        title: 'Yarn',
        eyebrow: 'Upstream planning',
        description: 'Order intake and raw material visibility.',
        route: '/yarn',
        tone: 'ocean',
        icon: 'pi pi-box',
        primaryLabel: 'Ordered yarn',
        primaryValue: `${this.formatNumber(data.yarn.totalYarnOrdered)} kg`,
        metrics: [
          { label: 'Open balance', value: `${this.formatNumber(data.yarn.yarnInOrder)} kg` },
          { label: 'Dispatched', value: `${this.formatNumber(data.yarn.yarnDispatchedToSpinning)} kg` }
        ]
      },
      {
        title: 'Knitting',
        eyebrow: 'Factory movement',
        description: 'Order dispatch and factory receipt tracking.',
        route: '/spinning',
        tone: 'teal',
        icon: 'pi pi-sync',
        primaryLabel: 'Pending at knitting unit',
        primaryValue: `${this.formatNumber(data.spinning.netPendingAtFactory)} kg`,
        metrics: [
          { label: 'Dispatched', value: `${this.formatNumber(data.spinning.totalDispatchedToSpinning)} kg` },
          { label: 'Received', value: `${this.formatNumber(data.spinning.totalReceivedFromSpinning)} kg` }
        ]
      },
      {
        title: 'In-house',
        eyebrow: 'Cutting floor',
        description: 'Fabric stock, cutting progress, and stitched inventory.',
        route: '/in-house',
        tone: 'indigo',
        icon: 'pi pi-building',
        primaryLabel: 'Fabric in stock',
        primaryValue: `${this.formatNumber(data.inHouse.totalFabricInStock)} kg`,
        metrics: [
          { label: 'Cuttings active', value: this.formatNumber(data.inHouse.cuttingInProgressCount) },
          { label: 'Pieces cut', value: this.formatNumber(data.inHouse.totalPiecesCut) }
        ]
      },
      {
        title: 'Stitching',
        eyebrow: 'Finishing',
        description: 'Order execution, deliveries, and defective stock closure.',
        route: '/stitching',
        tone: 'amber',
        icon: 'pi pi-sitemap',
        primaryLabel: 'Pieces in stitching',
        primaryValue: this.formatNumber(data.stitching.totalPiecesInStitching),
        metrics: [
          { label: 'Delivered', value: this.formatNumber(data.stitching.piecesDeliveredFromStitching) },
          { label: 'Pending orders', value: this.formatNumber(data.stitching.pendingOrdersCount) }
        ]
      }
    ];
  });

  readonly sectionHighlights = computed(() => this.dashboards()?.stitching.ordersBySection ?? []);

  constructor() {
    this.loadDashboards();
  }

  loadDashboards(): void {
    this.loading.set(true);
    this.errorMessage.set(null);

    this.dashboardApi
      .getOverviewDashboards()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (dashboards) => {
          this.dashboards.set(dashboards);
          this.loading.set(false);
        },
        error: (error) => {
          this.errorMessage.set(getApiErrorMessage(error, 'Unable to load module dashboards right now.'));
          this.loading.set(false);
        }
      });
  }

  private formatNumber(value: number): string {
    return new Intl.NumberFormat('en-IN', {
      maximumFractionDigits: 2
    }).format(value ?? 0);
  }
}
