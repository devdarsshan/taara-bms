import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule } from '@angular/forms';
import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { CheckboxModule } from 'primeng/checkbox';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { SelectButtonModule } from 'primeng/selectbutton';
import { TableModule } from 'primeng/table';
import { TabViewModule } from 'primeng/tabview';
import { TagModule } from 'primeng/tag';
import { Dia, MasterDashboardResponse, StitchingSection, Style } from '../../core/models/master-data.models';
import { MasterDataApiService } from '../../core/services/master-data-api.service';
import { NotificationService } from '../../core/services/notification.service';
import { getApiErrorMessage } from '../../core/utils/api-error.utils';
import { CrudControllerDeps } from '../../core/crud/base-crud.controller';
import { DiaCrudController, SectionCrudController, StyleCrudController } from './master-data-crud.controllers';

@Component({
  selector: 'app-master-data-page',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    DatePipe,
    ButtonModule,
    CardModule,
    CheckboxModule,
    DialogModule,
    InputTextModule,
    MessageModule,
    SelectButtonModule,
    TableModule,
    TabViewModule,
    TagModule
  ],
  templateUrl: './master-data-page.component.html',
  styleUrl: './master-data-page.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class MasterDataPageComponent {
  private readonly masterDataApi = inject(MasterDataApiService);
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly notificationService = inject(NotificationService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly destroyRef = inject(DestroyRef);

  readonly dashboard = signal<MasterDashboardResponse | null>(null);
  readonly dashboardLoading = signal(false);
  readonly activeTab = signal(0);
  readonly styleDetail = signal<Style | null>(null);
  readonly diaDetail = signal<Dia | null>(null);
  readonly sectionDetail = signal<StitchingSection | null>(null);
  readonly styleDetailVisible = signal(false);
  readonly diaDetailVisible = signal(false);
  readonly sectionDetailVisible = signal(false);
  readonly sectionTypeOptions = [
    { label: 'Internal', value: 'INTERNAL' as const },
    { label: 'External', value: 'EXTERNAL' as const }
  ];
  readonly sectionProcessOptions = [
    { label: 'Stitching', value: 'STITCHING' as const },
    { label: 'Printing', value: 'PRINTING' as const },
    { label: 'Knitting', value: 'KNITTING' as const }
  ];

  readonly stylesCrud: StyleCrudController;
  readonly diasCrud: DiaCrudController;
  readonly sectionsCrud: SectionCrudController;
  readonly totalRecords = computed(() => {
    const data = this.dashboard();
    return (data?.totalStyles ?? 0) + (data?.totalDiaTypes ?? 0) + (data?.totalStitchingSections ?? 0);
  });

  constructor() {
    const deps: CrudControllerDeps = {
      fb: this.fb,
      notificationService: this.notificationService,
      confirmationService: this.confirmationService,
      destroyRef: this.destroyRef
    };

    this.stylesCrud = new StyleCrudController(this.masterDataApi, deps, () => this.loadDashboard());
    this.diasCrud = new DiaCrudController(this.masterDataApi, deps, () => this.loadDashboard());
    this.sectionsCrud = new SectionCrudController(this.masterDataApi, deps, () => this.loadDashboard());

    this.loadDashboard();
  }

  reloadWorkspace(): void {
    this.loadDashboard();
    this.stylesCrud.reloadList();
    this.diasCrud.reloadList();
    this.sectionsCrud.reloadList();
  }

  openStyleDetail(style: Style): void {
    this.styleDetail.set(style);
    this.styleDetailVisible.set(true);
  }

  openDiaDetail(dia: Dia): void {
    this.diaDetail.set(dia);
    this.diaDetailVisible.set(true);
  }

  openSectionDetail(section: StitchingSection): void {
    this.sectionDetail.set(section);
    this.sectionDetailVisible.set(true);
  }

  editStyleFromDetail(): void {
    const style = this.styleDetail();
    if (!style) {
      return;
    }
    this.styleDetailVisible.set(false);
    this.stylesCrud.openDialog(style);
  }

  editDiaFromDetail(): void {
    const dia = this.diaDetail();
    if (!dia) {
      return;
    }
    this.diaDetailVisible.set(false);
    this.diasCrud.openDialog(dia);
  }

  editSectionFromDetail(): void {
    const section = this.sectionDetail();
    if (!section) {
      return;
    }
    this.sectionDetailVisible.set(false);
    this.sectionsCrud.openDialog(section);
  }

  deleteStyleFromDetail(): void {
    const style = this.styleDetail();
    if (!style) {
      return;
    }
    this.styleDetailVisible.set(false);
    this.stylesCrud.confirmDelete(style);
  }

  deleteDiaFromDetail(): void {
    const dia = this.diaDetail();
    if (!dia) {
      return;
    }
    this.diaDetailVisible.set(false);
    this.diasCrud.confirmDelete(dia);
  }

  deleteSectionFromDetail(): void {
    const section = this.sectionDetail();
    if (!section) {
      return;
    }
    this.sectionDetailVisible.set(false);
    this.sectionsCrud.confirmDelete(section);
  }

  private loadDashboard(): void {
    this.dashboardLoading.set(true);
    this.masterDataApi
      .getDashboard()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (dashboard) => {
          this.dashboard.set(dashboard);
          this.dashboardLoading.set(false);
        },
        error: (error) => {
          this.dashboardLoading.set(false);
          this.notificationService.error('Master dashboard unavailable', getApiErrorMessage(error));
        }
      });
  }
}
