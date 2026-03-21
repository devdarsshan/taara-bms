import { DatePipe, DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AbstractControl, FormArray, NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { debounceTime, distinctUntilChanged, forkJoin } from 'rxjs';
import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { DatePickerModule } from 'primeng/datepicker';
import { DialogModule } from 'primeng/dialog';
import { InputNumberModule } from 'primeng/inputnumber';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { SelectModule } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { TabViewModule } from 'primeng/tabview';
import { TagModule } from 'primeng/tag';
import { TextareaModule } from 'primeng/textarea';
import { QueryOptions } from '../../core/models/api.models';
import { OptionItem } from '../../core/models/common.models';
import { Dia, Style } from '../../core/models/master-data.models';
import {
  CuttingEntry,
  CuttingStatus,
  InHouseDashboardResponse,
  InHouseDelivery,
  InHouseSplit,
  InHouseStock,
  SplitStatus,
  StitchedStock
} from '../../core/models/inhouse.models';
import { InHouseApiService } from '../../core/services/inhouse-api.service';
import { MasterDataApiService } from '../../core/services/master-data-api.service';
import { NotificationService } from '../../core/services/notification.service';
import { getApiErrorMessage } from '../../core/utils/api-error.utils';

@Component({
  selector: 'app-in-house-page',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    DatePipe,
    DecimalPipe,
    ButtonModule,
    DatePickerModule,
    DialogModule,
    InputNumberModule,
    InputTextModule,
    MessageModule,
    SelectModule,
    TableModule,
    TabViewModule,
    TagModule,
    TextareaModule
  ],
  templateUrl: './in-house-page.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class InHousePageComponent {
  private readonly inHouseApi = inject(InHouseApiService);
  private readonly masterDataApi = inject(MasterDataApiService);
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly notificationService = inject(NotificationService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly destroyRef = inject(DestroyRef);

  readonly dashboard = signal<InHouseDashboardResponse | null>(null);
  readonly styles = signal<Style[]>([]);
  readonly dias = signal<Dia[]>([]);

  readonly deliveries = signal<InHouseDelivery[]>([]);
  readonly totalDeliveries = signal(0);
  readonly loadingDeliveries = signal(false);
  readonly deliveryDetail = signal<InHouseDelivery | null>(null);
  readonly deliveryDetailVisible = signal(false);
  readonly deliverySplits = signal<InHouseSplit[]>([]);
  readonly loadingSplits = signal(false);
  readonly splitSubmitting = signal(false);
  readonly splitError = signal<string | null>(null);

  readonly stockRows = signal<InHouseStock[]>([]);
  readonly loadingStock = signal(false);
  readonly stitchedStockRows = signal<StitchedStock[]>([]);
  readonly loadingStitchedStock = signal(false);

  readonly cuttings = signal<CuttingEntry[]>([]);
  readonly totalCuttings = signal(0);
  readonly loadingCuttings = signal(false);
  readonly selectedCuttings = signal<CuttingEntry[]>([]);
  readonly cuttingCreateVisible = signal(false);
  readonly cuttingSubmitting = signal(false);
  readonly cuttingCreateError = signal<string | null>(null);
  readonly cuttingAvailableQuantity = signal<number | null>(null);
  readonly cuttingDetail = signal<CuttingEntry | null>(null);
  readonly cuttingDetailVisible = signal(false);
  readonly cuttingDetailSubmitting = signal(false);
  readonly cuttingDetailError = signal<string | null>(null);

  readonly activeTab = signal(0);

  readonly deliveryFilters = this.fb.group({
    styleAutoId: this.fb.control('')
  });

  readonly stockFilters = this.fb.group({
    diaAutoId: this.fb.control(''),
    styleAutoId: this.fb.control('')
  });

  readonly cuttingFilters = this.fb.group({
    diaAutoId: this.fb.control(''),
    styleAutoId: this.fb.control(''),
    status: this.fb.control<'ALL' | CuttingStatus>('ALL')
  });

  readonly stitchedFilters = this.fb.group({
    styleAutoId: this.fb.control('')
  });

  readonly createCuttingForm = this.fb.group({
    cuttingDate: this.fb.control<Date | null>(new Date(), [Validators.required]),
    diaAutoId: this.fb.control('', [Validators.required]),
    styleAutoId: this.fb.control('', [Validators.required]),
    quantityUsedKgs: this.fb.control<number | null>(null, [Validators.required, Validators.min(0.01)]),
    outputPieces: this.fb.control<number | null>(null),
    notes: this.fb.control('')
  });

  readonly updateCuttingForm = this.fb.group({
    outputPieces: this.fb.control<number | null>(null),
    notes: this.fb.control('')
  });

  readonly splitForm = this.fb.group({
    splits: this.fb.array([this.createSplitRow()])
  });

  readonly deliveryQuery = signal<QueryOptions>({
    page: 0,
    size: 10,
    sortField: 'updatedAt',
    sortDirection: 'desc',
    includeDeleted: false
  });

  readonly cuttingQuery = signal<QueryOptions>({
    page: 0,
    size: 10,
    sortField: 'updatedAt',
    sortDirection: 'desc',
    includeDeleted: false
  });

  readonly styleOptions = computed<OptionItem[]>(() =>
    this.styles().map((style) => ({
      label: `${style.autoId} - ${style.styleName}`,
      value: style.autoId
    }))
  );

  readonly diaOptions = computed<OptionItem[]>(() =>
    this.dias().map((dia) => ({
      label: `${dia.autoId} - ${dia.diaValue}`,
      value: dia.autoId
    }))
  );

  readonly cuttingStatusOptions: OptionItem<'ALL' | CuttingStatus>[] = [
    { label: 'All status', value: 'ALL' },
    { label: 'In progress', value: 'IN_PROGRESS' },
    { label: 'Completed', value: 'COMPLETED' }
  ];

  readonly summaryCards = computed(() => {
    const dashboard = this.dashboard();
    if (!dashboard) {
      return [];
    }

    return [
      {
        title: 'Fabric In Stock',
        value: `${this.formatNumber(dashboard.totalFabricInStock)} kg`,
        note: 'Available split stock after active cutting usage.',
        icon: 'pi pi-box',
        tone: 'ocean'
      },
      {
        title: 'Cutting In Progress',
        value: `${dashboard.cuttingInProgressCount}`,
        note: 'Cutting records still awaiting final output piece entry.',
        icon: 'pi pi-stopwatch',
        tone: 'teal'
      },
      {
        title: 'Pieces Cut',
        value: `${dashboard.totalPiecesCut}`,
        note: 'Completed cutting output pieces ready for stitching flow.',
        icon: 'pi pi-clone',
        tone: 'indigo'
      },
      {
        title: 'Ready To Stitch',
        value: `${dashboard.readyToStitchPieces}`,
        note: 'Pieces still available to move into stitching orders.',
        icon: 'pi pi-arrow-right',
        tone: 'teal'
      },
      {
        title: 'Defective Finalized',
        value: `${dashboard.defectiveStockTotal}`,
        note: 'Defective stitched pieces computed from completed orders.',
        icon: 'pi pi-exclamation-triangle',
        tone: 'amber'
      }
    ];
  });

  get splitRows(): FormArray {
    return this.splitForm.controls.splits;
  }

  readonly remainingToSplit = computed(() => {
    const delivery = this.deliveryDetail();
    if (!delivery) {
      return 0;
    }

    return Math.max(Number(delivery.quantityKgs) - Number(delivery.allocatedQuantityKgs), 0);
  });

  constructor() {
    this.bindFilters();
    this.bindCuttingAvailability();
    this.loadInitialData();
  }

  loadInitialData(): void {
    this.loadingDeliveries.set(true);
    this.loadingStock.set(true);
    this.loadingCuttings.set(true);
    this.loadingStitchedStock.set(true);

    forkJoin({
      dashboard: this.inHouseApi.getDashboard(),
      styles: this.masterDataApi.getStyleOptions(),
      dias: this.masterDataApi.getDiaOptions(),
      deliveries: this.loadDeliveriesRequest(),
      stock: this.inHouseApi.getStock(),
      cuttings: this.loadCuttingsRequest(),
      stitchedStock: this.inHouseApi.getStitchedStock()
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ({ dashboard, styles, dias, deliveries, stock, cuttings, stitchedStock }) => {
          this.dashboard.set(dashboard);
          this.styles.set(styles);
          this.dias.set(dias);
          this.deliveries.set(deliveries.content);
          this.totalDeliveries.set(deliveries.totalElements);
          this.stockRows.set(stock);
          this.cuttings.set(cuttings.content);
          this.totalCuttings.set(cuttings.totalElements);
          this.stitchedStockRows.set(stitchedStock);
          this.loadingDeliveries.set(false);
          this.loadingStock.set(false);
          this.loadingCuttings.set(false);
          this.loadingStitchedStock.set(false);
        },
        error: (error) => {
          this.loadingDeliveries.set(false);
          this.loadingStock.set(false);
          this.loadingCuttings.set(false);
          this.loadingStitchedStock.set(false);
          this.notificationService.error('Unable to load in-house workspace', getApiErrorMessage(error));
        }
      });
  }

  onDeliveryLazyLoad(event: { first?: number | null; rows?: number | null; sortField?: string | string[] | null; sortOrder?: number | null }): void {
    this.deliveryQuery.set(this.resolveQuery(this.deliveryQuery(), event));
    this.reloadDeliveries();
  }

  onCuttingLazyLoad(event: { first?: number | null; rows?: number | null; sortField?: string | string[] | null; sortOrder?: number | null }): void {
    this.cuttingQuery.set(this.resolveQuery(this.cuttingQuery(), event));
    this.reloadCuttings();
  }

  openDeliveryDetail(record: InHouseDelivery): void {
    this.deliveryDetail.set(record);
    this.deliveryDetailVisible.set(true);
    this.resetSplitForm();
    this.fetchDeliverySplits(record.autoId);
  }

  openCreateCutting(): void {
    this.cuttingCreateError.set(null);
    this.cuttingAvailableQuantity.set(null);
    this.createCuttingForm.reset({
      cuttingDate: new Date(),
      diaAutoId: '',
      styleAutoId: '',
      quantityUsedKgs: null,
      outputPieces: null,
      notes: ''
    });
    this.cuttingCreateVisible.set(true);
  }

  openCuttingDetail(record: CuttingEntry): void {
    this.cuttingDetail.set(record);
    this.cuttingDetailError.set(null);
    this.updateCuttingForm.reset({
      outputPieces: record.outputPieces,
      notes: record.notes ?? ''
    });
    this.cuttingDetailVisible.set(true);
  }

  addSplitRow(): void {
    if (this.remainingToSplit() <= 0) {
      return;
    }
    this.splitRows.push(this.createSplitRow());
  }

  removeSplitRow(index: number): void {
    if (this.splitRows.length === 1) {
      return;
    }

    this.splitRows.removeAt(index);
  }

  saveSplits(): void {
    const delivery = this.deliveryDetail();
    if (!delivery) {
      return;
    }

    if (this.splitForm.pristine) {
      this.notificationService.warn('No changes available to save', 'Update the split rows before saving.');
      return;
    }

    if (this.remainingToSplit() <= 0) {
      this.notificationService.warn('No split capacity left', 'This delivery is already fully split.');
      return;
    }

    this.splitError.set(null);
    if (this.splitForm.invalid) {
      this.splitRows.markAllAsTouched();
      this.notificationService.warn('Splits not saved', 'Complete dia and quantity for each split row before saving.');
      return;
    }

    const payload = this.splitRows.getRawValue()
      .filter((split): split is { diaAutoId: string; quantityKgs: number } => !!split.diaAutoId && !!split.quantityKgs)
      .map((split) => ({
        diaAutoId: split.diaAutoId,
        quantityKgs: Number(split.quantityKgs)
      }));

    if (!payload.length) {
      this.notificationService.warn('Splits not saved', 'Add at least one valid split row.');
      return;
    }

    this.splitSubmitting.set(true);
    this.inHouseApi.createSplits(delivery.autoId, { splits: payload })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.splitSubmitting.set(false);
          this.notificationService.success('Splits saved', 'The selected delivery has been allocated to dia stock.');
          this.resetSplitForm();
          this.fetchDeliverySplits(delivery.autoId);
          this.reloadDashboard();
          this.reloadDeliveries(() => this.syncDeliveryDetail(delivery.autoId));
          this.reloadStock();
        },
        error: (error) => {
          this.splitSubmitting.set(false);
          const message = getApiErrorMessage(error);
          this.splitError.set(message);
          this.notificationService.error('Unable to save splits', message);
        }
      });
  }

  confirmDeleteSplit(split: InHouseSplit): void {
    const delivery = this.deliveryDetail();
    if (!delivery) {
      return;
    }

    if (!split.canDelete) {
      this.notificationService.warn('Split cannot be deleted', 'This split is already being used by downstream cutting records.');
      return;
    }

    this.confirmationService.confirm({
      header: 'Delete stock split',
      message: `Delete split ${split.autoId} from delivery ${delivery.autoId}?`,
      acceptLabel: 'Delete',
      rejectLabel: 'Cancel',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => {
        this.inHouseApi.deleteSplit(delivery.autoId, split.autoId)
          .pipe(takeUntilDestroyed(this.destroyRef))
          .subscribe({
            next: () => {
              this.notificationService.success('Split deleted', 'The selected split has been removed.');
              this.fetchDeliverySplits(delivery.autoId);
              this.reloadDashboard();
              this.reloadDeliveries(() => this.syncDeliveryDetail(delivery.autoId));
              this.reloadStock();
            },
            error: (error) => {
              this.notificationService.error('Unable to delete split', getApiErrorMessage(error));
            }
          });
      }
    });
  }

  createCutting(): void {
    this.cuttingCreateError.set(null);
    if (this.createCuttingForm.pristine) {
      this.notificationService.warn('No changes available to save', 'Update the cutting form before saving.');
      return;
    }

    if (this.createCuttingForm.invalid) {
      this.createCuttingForm.markAllAsTouched();
      this.notificationService.warn('Cutting not saved', 'Complete the required cutting fields before saving.');
      return;
    }

    const value = this.createCuttingForm.getRawValue();
    if (!value.cuttingDate || !value.diaAutoId || !value.styleAutoId || !value.quantityUsedKgs) {
      this.notificationService.warn('Cutting not saved', 'Date, dia, style, and quantity are required.');
      return;
    }

    this.cuttingSubmitting.set(true);
    this.inHouseApi.createCutting({
      cuttingDate: this.toApiDate(value.cuttingDate),
      diaAutoId: value.diaAutoId,
      styleAutoId: value.styleAutoId,
      quantityUsedKgs: Number(value.quantityUsedKgs),
      outputPieces: value.outputPieces ?? null,
      notes: value.notes?.trim() || null
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.cuttingSubmitting.set(false);
          this.cuttingCreateVisible.set(false);
          this.notificationService.success('Cutting created', 'The cutting entry has been saved.');
          this.reloadDashboard();
          this.reloadCuttings();
          this.reloadStock();
        },
        error: (error) => {
          this.cuttingSubmitting.set(false);
          const message = getApiErrorMessage(error);
          this.cuttingCreateError.set(message);
          this.notificationService.error('Unable to create cutting', message);
        }
      });
  }

  saveCuttingDetail(): void {
    const record = this.cuttingDetail();
    if (!record) {
      return;
    }

    if (this.updateCuttingForm.pristine) {
      this.notificationService.warn('No changes available to save', 'Update the cutting detail before saving.');
      return;
    }

    this.cuttingDetailError.set(null);
    this.cuttingDetailSubmitting.set(true);
    const value = this.updateCuttingForm.getRawValue();

    this.inHouseApi.updateCutting(record.autoId, {
      outputPieces: value.outputPieces ?? null,
      notes: value.notes?.trim() || null
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (updated) => {
          this.cuttingDetailSubmitting.set(false);
          this.cuttingDetail.set(updated);
          this.updateCuttingForm.reset({
            outputPieces: updated.outputPieces,
            notes: updated.notes ?? ''
          });
          this.notificationService.success('Cutting updated', 'The cutting entry has been updated.');
          this.reloadDashboard();
          this.reloadCuttings();
        },
        error: (error) => {
          this.cuttingDetailSubmitting.set(false);
          const message = getApiErrorMessage(error);
          this.cuttingDetailError.set(message);
          this.notificationService.error('Unable to update cutting', message);
        }
      });
  }

  confirmDeleteCuttings(): void {
    const selected = this.selectedCuttings();
    if (!selected.length) {
      return;
    }

    this.confirmationService.confirm({
      header: 'Delete cutting records',
      message: `Soft delete ${selected.length} selected cutting record(s)?`,
      acceptLabel: 'Delete',
      rejectLabel: 'Cancel',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => {
        forkJoin(selected.map((record) => this.inHouseApi.deleteCutting(record.autoId)))
          .pipe(takeUntilDestroyed(this.destroyRef))
          .subscribe({
            next: () => {
              this.selectedCuttings.set([]);
              this.notificationService.success('Cuttings deleted', 'Selected cuttings were removed from the active list.');
              this.reloadDashboard();
              this.reloadCuttings();
              this.reloadStock();
            },
            error: (error) => {
              this.notificationService.error('Unable to delete cuttings', getApiErrorMessage(error));
            }
          });
      }
    });
  }

  splitStatusSeverity(status: SplitStatus): 'info' | 'warn' | 'success' {
    switch (status) {
      case 'FULLY_SPLIT':
        return 'success';
      case 'PARTIALLY_SPLIT':
        return 'warn';
      default:
        return 'info';
    }
  }

  cuttingStatusSeverity(status: CuttingStatus): 'warn' | 'success' {
    return status === 'COMPLETED' ? 'success' : 'warn';
  }

  deliveryRemaining(record: InHouseDelivery): number {
    return Math.max(Number(record.quantityKgs) - Number(record.allocatedQuantityKgs), 0);
  }

  closeCuttingCreateDialog(): void {
    if (this.createCuttingForm.dirty) {
      this.confirmationService.confirm({
        header: 'Discard cutting changes',
        message: 'You have unsaved changes. Do you want to close this form?',
        acceptLabel: 'Discard',
        rejectLabel: 'Keep editing',
        acceptButtonStyleClass: 'p-button-danger',
        rejectButtonStyleClass: 'p-button-outlined p-button-secondary',
        accept: () => this.cuttingCreateVisible.set(false)
      });
      return;
    }

    this.cuttingCreateVisible.set(false);
  }

  handleDeliveryDetailVisibilityChange(visible: boolean): void {
    if (visible) {
      this.deliveryDetailVisible.set(true);
      return;
    }

    if (this.splitForm.dirty) {
      this.confirmationService.confirm({
        header: 'Discard split changes',
        message: 'You have unsaved split changes. Do you want to close this dialog?',
        acceptLabel: 'Discard',
        rejectLabel: 'Keep editing',
        acceptButtonStyleClass: 'p-button-danger',
        rejectButtonStyleClass: 'p-button-outlined p-button-secondary',
        accept: () => this.deliveryDetailVisible.set(false)
      });
      return;
    }

    this.deliveryDetailVisible.set(false);
  }

  handleCuttingCreateVisibilityChange(visible: boolean): void {
    if (visible) {
      this.cuttingCreateVisible.set(true);
      return;
    }

    this.closeCuttingCreateDialog();
  }

  handleCuttingDetailVisibilityChange(visible: boolean): void {
    if (visible) {
      this.cuttingDetailVisible.set(true);
      return;
    }

    if (this.updateCuttingForm.dirty) {
      this.confirmationService.confirm({
        header: 'Discard cutting changes',
        message: 'You have unsaved cutting updates. Do you want to close this dialog?',
        acceptLabel: 'Discard',
        rejectLabel: 'Keep editing',
        acceptButtonStyleClass: 'p-button-danger',
        rejectButtonStyleClass: 'p-button-outlined p-button-secondary',
        accept: () => this.cuttingDetailVisible.set(false)
      });
      return;
    }

    this.cuttingDetailVisible.set(false);
  }

  isInvalid(control: AbstractControl | null): boolean {
    return !!control && control.invalid && (control.touched || control.dirty);
  }

  private bindFilters(): void {
    this.deliveryFilters.controls.styleAutoId.valueChanges
      .pipe(debounceTime(250), distinctUntilChanged(), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.deliveryQuery.update((query) => ({ ...query, page: 0 }));
        this.reloadDeliveries();
      });

    this.stockFilters.valueChanges
      .pipe(debounceTime(250), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.reloadStock();
      });

    this.cuttingFilters.valueChanges
      .pipe(debounceTime(250), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.cuttingQuery.update((query) => ({ ...query, page: 0 }));
        this.reloadCuttings();
      });

    this.stitchedFilters.controls.styleAutoId.valueChanges
      .pipe(debounceTime(250), distinctUntilChanged(), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.reloadStitchedStock();
      });
  }

  private bindCuttingAvailability(): void {
    this.createCuttingForm.controls.diaAutoId.valueChanges
      .pipe(debounceTime(200), distinctUntilChanged(), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.loadCuttingAvailability());

    this.createCuttingForm.controls.styleAutoId.valueChanges
      .pipe(debounceTime(200), distinctUntilChanged(), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.loadCuttingAvailability());
  }

  private fetchDeliverySplits(deliveryAutoId: string): void {
    this.loadingSplits.set(true);
    this.inHouseApi.getSplits(deliveryAutoId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (splits) => {
          this.deliverySplits.set(splits);
          this.syncDeliveryDetail(deliveryAutoId);
          this.loadingSplits.set(false);
        },
        error: (error) => {
          this.loadingSplits.set(false);
          this.notificationService.error('Unable to load delivery splits', getApiErrorMessage(error));
        }
      });
  }

  reloadDashboard(): void {
    this.inHouseApi.getDashboard()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (dashboard) => this.dashboard.set(dashboard),
        error: (error) => this.notificationService.error('Unable to refresh in-house dashboard', getApiErrorMessage(error))
      });
  }

  reloadDeliveries(afterReload?: () => void): void {
    this.loadingDeliveries.set(true);
    this.loadDeliveriesRequest()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (page) => {
          this.deliveries.set(page.content);
          this.totalDeliveries.set(page.totalElements);
          this.loadingDeliveries.set(false);
          afterReload?.();
        },
        error: (error) => {
          this.loadingDeliveries.set(false);
          this.notificationService.error('Unable to refresh deliveries', getApiErrorMessage(error));
        }
      });
  }

  reloadStock(): void {
    this.loadingStock.set(true);
    this.inHouseApi.getStock(this.buildStockFilters())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (rows) => {
          this.stockRows.set(rows);
          this.loadingStock.set(false);
        },
        error: (error) => {
          this.loadingStock.set(false);
          this.notificationService.error('Unable to refresh stock', getApiErrorMessage(error));
        }
      });
  }

  reloadCuttings(): void {
    this.loadingCuttings.set(true);
    this.loadCuttingsRequest()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (page) => {
          this.cuttings.set(page.content);
          this.totalCuttings.set(page.totalElements);
          this.loadingCuttings.set(false);
        },
        error: (error) => {
          this.loadingCuttings.set(false);
          this.notificationService.error('Unable to refresh cuttings', getApiErrorMessage(error));
        }
      });
  }

  reloadStitchedStock(): void {
    this.loadingStitchedStock.set(true);
    this.inHouseApi.getStitchedStock(this.buildStitchedFilters())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (rows) => {
          this.stitchedStockRows.set(rows);
          this.loadingStitchedStock.set(false);
        },
        error: (error) => {
          this.loadingStitchedStock.set(false);
          this.notificationService.error('Unable to refresh stitched stock', getApiErrorMessage(error));
        }
      });
  }

  private loadDeliveriesRequest() {
    return this.inHouseApi.getDeliveries({
      ...this.deliveryQuery(),
      ...this.buildDeliveryFilters()
    });
  }

  private loadCuttingsRequest() {
    return this.inHouseApi.getCuttings({
      ...this.cuttingQuery(),
      ...this.buildCuttingFilters()
    });
  }

  private buildDeliveryFilters() {
    const value = this.deliveryFilters.getRawValue();
    return {
      styleAutoId: value.styleAutoId || undefined
    };
  }

  private buildStockFilters() {
    const value = this.stockFilters.getRawValue();
    return {
      diaAutoId: value.diaAutoId || undefined,
      styleAutoId: value.styleAutoId || undefined
    };
  }

  private buildCuttingFilters() {
    const value = this.cuttingFilters.getRawValue();
    return {
      diaAutoId: value.diaAutoId || undefined,
      styleAutoId: value.styleAutoId || undefined,
      status: value.status === 'ALL' ? undefined : value.status
    };
  }

  private buildStitchedFilters() {
    const value = this.stitchedFilters.getRawValue();
    return {
      styleAutoId: value.styleAutoId || undefined
    };
  }

  private createSplitRow() {
    return this.fb.group({
      diaAutoId: this.fb.control('', [Validators.required]),
      quantityKgs: this.fb.control<number | null>(null, [Validators.required, Validators.min(0.01)])
    });
  }

  private resetSplitForm(): void {
    this.splitForm.setControl('splits', this.fb.array([this.createSplitRow()]));
    this.splitError.set(null);
    this.splitForm.markAsPristine();
    this.splitForm.markAsUntouched();
  }

  private syncDeliveryDetail(deliveryAutoId: string): void {
    const matchedDelivery = this.deliveries().find((delivery) => delivery.autoId === deliveryAutoId);
    if (matchedDelivery) {
      this.deliveryDetail.set(matchedDelivery);
    }
  }

  private loadCuttingAvailability(): void {
    const diaAutoId = this.createCuttingForm.controls.diaAutoId.getRawValue();
    const styleAutoId = this.createCuttingForm.controls.styleAutoId.getRawValue();

    if (!diaAutoId || !styleAutoId) {
      this.cuttingAvailableQuantity.set(null);
      return;
    }

    this.inHouseApi.getAvailableCuttingQuantity(diaAutoId, styleAutoId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (response) => this.cuttingAvailableQuantity.set(response.availableQuantityKgs),
        error: (error) => {
          this.cuttingAvailableQuantity.set(null);
          this.notificationService.error('Unable to load cutting availability', getApiErrorMessage(error));
        }
      });
  }

  private resolveQuery(
    current: QueryOptions,
    event: { first?: number | null; rows?: number | null; sortField?: string | string[] | null; sortOrder?: number | null }
  ): QueryOptions {
    const rows = event.rows ?? current.size;
    const first = event.first ?? current.page * current.size;
    const sortField = typeof event.sortField === 'string' ? event.sortField : current.sortField;
    const sortDirection = event.sortOrder === 1 ? 'asc' : event.sortOrder === -1 ? 'desc' : current.sortDirection;

    return {
      ...current,
      page: Math.floor(first / rows),
      size: rows,
      sortField,
      sortDirection
    };
  }

  private toApiDate(value: Date): string {
    return new Date(Date.UTC(value.getFullYear(), value.getMonth(), value.getDate())).toISOString().slice(0, 10);
  }

  private formatNumber(value: number): string {
    return new Intl.NumberFormat('en-IN', { maximumFractionDigits: 2 }).format(value ?? 0);
  }
}
