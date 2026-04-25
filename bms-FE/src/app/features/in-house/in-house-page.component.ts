import { DatePipe, DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AbstractControl, FormArray, FormControl, FormGroup, NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
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
import { GarmentSize, OptionItem } from '../../core/models/common.models';
import { Dia, Style } from '../../core/models/master-data.models';
import {
  CuttingCreateRequest,
  CuttingEntry,
  CuttingRowRequest,
  InHouseDashboardResponse,
  InHouseDelivery,
  InHouseSplit,
  InHouseStock,
  StitchedStock
} from '../../core/models/inhouse.models';
import { InHouseApiService } from '../../core/services/inhouse-api.service';
import { MasterDataApiService } from '../../core/services/master-data-api.service';
import { NotificationService } from '../../core/services/notification.service';
import { extractApiError, getApiErrorMessage } from '../../core/utils/api-error.utils';

type SplitRowForm = FormGroup<{
  diaAutoId: FormControl<string>;
  quantityKgs: FormControl<number | null>;
}>;

type CuttingRowForm = FormGroup<{
  diaAutoId: FormControl<string>;
  styleAutoId: FormControl<string>;
  size: FormControl<GarmentSize>;
  availableQuantityKgs: FormControl<number | null>;
  quantityUsedKgs: FormControl<number | null>;
  outputPieces: FormControl<number | null>;
}>;

@Component({
  selector: 'app-in-house-page',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    DatePipe,
    DecimalPipe,
    ButtonModule,
    CheckboxModule,
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
  readonly deliveries = signal<InHouseDelivery[]>([]);
  readonly stockRows = signal<InHouseStock[]>([]);
  readonly stitchedStockRows = signal<StitchedStock[]>([]);
  readonly cuttings = signal<CuttingEntry[]>([]);
  readonly styles = signal<Style[]>([]);
  readonly dias = signal<Dia[]>([]);
  readonly loading = signal(true);

  readonly deliveryDetail = signal<InHouseDelivery | null>(null);
  readonly deliverySplits = signal<InHouseSplit[]>([]);
  readonly deliveryDialogVisible = signal(false);
  readonly splitError = signal<string | null>(null);
  readonly selectedCuttings = signal<CuttingEntry[]>([]);

  readonly cuttingDialogVisible = signal(false);
  readonly editingCutting = signal<CuttingEntry | null>(null);

  readonly splitForm = this.fb.group({
    rows: this.fb.array<SplitRowForm>([])
  });

  readonly cuttingForm = this.fb.group({
    cuttingDate: this.fb.control<Date | null>(new Date(), Validators.required),
    notes: this.fb.control(''),
    rows: this.fb.array<CuttingRowForm>([])
  });

  readonly sizeOptions: OptionItem<GarmentSize>[] = [
    { label: 'XS', value: 'XS' },
    { label: 'S', value: 'S' },
    { label: 'M', value: 'M' },
    { label: 'L', value: 'L' },
    { label: 'XL', value: 'XL' }
  ];

  readonly styleOptions = computed<OptionItem[]>(() =>
    this.styles().map((style) => ({ label: style.styleName, value: style.autoId }))
  );

  readonly diaOptions = computed<OptionItem[]>(() =>
    this.dias().map((dia) => ({ label: dia.diaValue, value: dia.autoId }))
  );

  readonly summaryCards = computed(() => {
    const dashboard = this.dashboard();
    if (!dashboard) {
      return [];
    }
    return [
      { title: 'Fabric In Stock', value: `${this.formatNumber(dashboard.totalFabricInStock)} kg`, icon: 'pi pi-box', tone: 'ocean' },
      { title: 'Ready To Stitch', value: `${dashboard.readyToStitchPieces}`, icon: 'pi pi-arrow-right', tone: 'teal' },
      { title: 'Stitched Plain Stock', value: `${dashboard.stitchedPlainStockTotal}`, icon: 'pi pi-clone', tone: 'indigo' },
      { title: 'Printed Stock', value: `${dashboard.printedStockTotal}`, icon: 'pi pi-palette', tone: 'ocean' },
      { title: 'Defective', value: `${dashboard.defectiveStockTotal}`, icon: 'pi pi-exclamation-triangle', tone: 'amber' }
    ];
  });

  readonly totalKgUsed = computed(() =>
    this.cuttingRows.controls.reduce((sum, row) => sum + Number(row.controls.quantityUsedKgs.getRawValue() ?? 0), 0)
  );

  readonly totalOutputPieces = computed(() =>
    this.cuttingRows.controls.reduce((sum, row) => sum + Number(row.controls.outputPieces.getRawValue() ?? 0), 0)
  );

  readonly pcsPerKg = computed(() => {
    const totalKg = this.totalKgUsed();
    const totalPieces = this.totalOutputPieces();
    if (!totalKg || !totalPieces) {
      return 0;
    }
    return totalPieces / totalKg;
  });

  get splitRows(): FormArray<SplitRowForm> {
    return this.splitForm.controls.rows;
  }

  get cuttingRows(): FormArray<CuttingRowForm> {
    return this.cuttingForm.controls.rows;
  }

  constructor() {
    this.loadData();
  }

  loadData(): void {
    this.loading.set(true);
    forkJoin({
      dashboard: this.inHouseApi.getDashboard(),
      deliveries: this.inHouseApi.getDeliveries({ page: 0, size: 100, sortField: 'deliveryDate', sortDirection: 'desc', includeDeleted: false }),
      stock: this.inHouseApi.getStock(),
      stitchedStock: this.inHouseApi.getStitchedStock(),
      cuttings: this.inHouseApi.getCuttings({ page: 0, size: 100, sortField: 'cuttingDate', sortDirection: 'desc', includeDeleted: false }),
      styles: this.masterDataApi.getStyleOptions(),
      dias: this.masterDataApi.getDiaOptions()
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ({ dashboard, deliveries, stock, stitchedStock, cuttings, styles, dias }) => {
          this.dashboard.set(dashboard);
          this.deliveries.set(deliveries.content);
          this.stockRows.set(stock);
          this.stitchedStockRows.set(stitchedStock);
          this.cuttings.set(cuttings.content);
          this.styles.set(styles);
          this.dias.set(dias);
          this.loading.set(false);
        },
        error: (error) => {
          this.loading.set(false);
          this.notificationService.error('Unable to load in-house module', getApiErrorMessage(error));
        }
      });
  }

  openDelivery(record: InHouseDelivery): void {
    this.deliveryDetail.set(record);
    this.deliveryDialogVisible.set(true);
    this.splitError.set(null);
    this.splitRows.clear();
    this.addSplitRow();
    this.inHouseApi.getSplits(record.autoId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (splits) => this.deliverySplits.set(splits),
        error: (error) => this.notificationService.error('Unable to load splits', getApiErrorMessage(error))
      });
  }

  addSplitRow(): void {
    this.splitError.set(null);
    this.splitRows.push(this.fb.group({
      diaAutoId: this.fb.control('', Validators.required),
      quantityKgs: this.fb.control<number | null>(null, [Validators.required, Validators.min(0.01)])
    }));
  }

  removeSplitRow(index: number): void {
    this.splitError.set(null);
    if (this.splitRows.length > 1) {
      this.splitRows.removeAt(index);
    }
  }

  saveSplits(): void {
    const delivery = this.deliveryDetail();
    if (!delivery || this.splitForm.invalid) {
      this.splitForm.markAllAsTouched();
      this.splitError.set('Choose a Dia and enter a positive quantity for every split row.');
      this.notificationService.warn('Splits not saved', 'Complete the highlighted split fields before saving.');
      return;
    }
    this.splitError.set(null);
    const payload = {
      splits: this.splitRows.getRawValue().map((row) => ({
        diaAutoId: row.diaAutoId,
        quantityKgs: Number(row.quantityKgs ?? 0)
      }))
    };
    this.inHouseApi.createSplits(delivery.autoId, payload)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (splits) => {
          this.deliverySplits.set(splits);
          this.notificationService.success('Splits saved', 'Delivery splits were saved successfully.');
          this.splitRows.clear();
          this.addSplitRow();
          this.loadData();
        },
        error: (error) => {
          const message = this.buildSplitErrorMessage(error);
          this.splitError.set(message);
          this.notificationService.error('Unable to save splits', message);
        }
      });
  }

  confirmDeleteSplit(split: InHouseSplit): void {
    const delivery = this.deliveryDetail();
    if (!delivery || !split.canDelete) {
      return;
    }
    this.confirmationService.confirm({
      header: 'Delete split',
      message: `Delete ${split.autoId}?`,
      acceptButtonStyleClass: 'p-button-danger',
      rejectButtonStyleClass: 'p-button-outlined p-button-secondary',
      accept: () => {
        this.inHouseApi.deleteSplit(delivery.autoId, split.autoId)
          .pipe(takeUntilDestroyed(this.destroyRef))
          .subscribe({
            next: () => {
              this.notificationService.success('Split deleted', 'The split was removed.');
              this.openDelivery(delivery);
              this.loadData();
            },
            error: (error) => this.notificationService.error('Unable to delete split', getApiErrorMessage(error))
          });
      }
    });
  }

  openCreateCutting(): void {
    this.editingCutting.set(null);
    this.cuttingDialogVisible.set(true);
    this.cuttingForm.reset({ cuttingDate: new Date(), notes: '' });
    this.cuttingRows.clear();
    this.addCuttingRow();
  }

  openCutting(record: CuttingEntry): void {
    this.editingCutting.set(record);
    this.cuttingDialogVisible.set(true);
    this.cuttingForm.reset({ cuttingDate: new Date(record.cuttingDate), notes: record.notes ?? '' });
    this.cuttingRows.clear();
    for (const row of record.rows) {
      const group = this.createCuttingRow();
      group.patchValue({
        diaAutoId: row.dia.autoId,
        styleAutoId: row.style.autoId,
        size: row.size,
        quantityUsedKgs: row.quantityUsedKgs,
        outputPieces: row.outputPieces,
        availableQuantityKgs: null
      });
      this.cuttingRows.push(group);
      this.refreshRowAvailability(group);
    }
  }

  addCuttingRow(): void {
    const row = this.createCuttingRow();
    this.cuttingRows.push(row);
  }

  removeCuttingRow(index: number): void {
    if (this.cuttingRows.length > 1) {
      this.cuttingRows.removeAt(index);
    }
  }

  onCuttingComboChange(index: number): void {
    this.refreshRowAvailability(this.cuttingRows.at(index));
  }

  saveCutting(): void {
    if (this.cuttingForm.invalid) {
      this.cuttingForm.markAllAsTouched();
      return;
    }
    const value = this.cuttingForm.getRawValue();
    const payload: CuttingCreateRequest = {
      cuttingDate: this.toApiDate(value.cuttingDate ?? new Date()),
      notes: value.notes?.trim() || null,
      rows: value.rows.map((row) => ({
        diaAutoId: row.diaAutoId,
        styleAutoId: row.styleAutoId,
        size: row.size,
        quantityUsedKgs: Number(row.quantityUsedKgs ?? 0),
        outputPieces: row.outputPieces ?? null
      } satisfies CuttingRowRequest))
    };
    const request$ = this.editingCutting()
      ? this.inHouseApi.updateCutting(this.editingCutting()!.autoId, payload)
      : this.inHouseApi.createCutting(payload);
    request$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.notificationService.success('Cutting saved', 'The cutting entry was saved successfully.');
          this.cuttingDialogVisible.set(false);
          this.loadData();
        },
        error: (error) => this.notificationService.error('Unable to save cutting', getApiErrorMessage(error))
      });
  }

  confirmDeleteCutting(record: CuttingEntry): void {
    this.confirmationService.confirm({
      header: 'Delete cutting',
      message: `Delete ${record.autoId}?`,
      acceptButtonStyleClass: 'p-button-danger',
      rejectButtonStyleClass: 'p-button-outlined p-button-secondary',
      accept: () => {
        this.inHouseApi.deleteCutting(record.autoId)
          .pipe(takeUntilDestroyed(this.destroyRef))
          .subscribe({
            next: () => {
              this.notificationService.success('Cutting deleted', 'The cutting entry was removed.');
              this.loadData();
            },
            error: (error) => this.notificationService.error('Unable to delete cutting', getApiErrorMessage(error))
          });
      }
    });
  }

  confirmDeleteCuttings(): void {
    const selected = this.selectedCuttings();
    if (!selected.length) {
      return;
    }
    this.confirmationService.confirm({
      header: 'Delete cutting entries',
      message: `Soft delete ${selected.length} selected cutting record(s)?`,
      acceptLabel: 'Delete',
      rejectLabel: 'Cancel',
      acceptButtonStyleClass: 'p-button-danger',
      rejectButtonStyleClass: 'p-button-outlined p-button-secondary',
      accept: () => {
        forkJoin(selected.map((record) => this.inHouseApi.deleteCutting(record.autoId)))
          .pipe(takeUntilDestroyed(this.destroyRef))
          .subscribe({
            next: () => {
              this.selectedCuttings.set([]);
              this.notificationService.success('Cuttings deleted', 'Selected cutting entries were removed from the active list.');
              this.loadData();
            },
            error: (error) => this.notificationService.error('Unable to delete cuttings', getApiErrorMessage(error))
          });
      }
    });
  }

  private createCuttingRow(): CuttingRowForm {
    const group = this.fb.group({
      diaAutoId: this.fb.control('', Validators.required),
      styleAutoId: this.fb.control('', Validators.required),
      size: this.fb.control<GarmentSize>('M', Validators.required),
      availableQuantityKgs: this.fb.control<number | null>({ value: null, disabled: true }),
      quantityUsedKgs: this.fb.control<number | null>(null, [Validators.required, Validators.min(0.01)]),
      outputPieces: this.fb.control<number | null>(null)
    });
    return group;
  }

  private refreshRowAvailability(row: CuttingRowForm): void {
    const diaAutoId = row.controls.diaAutoId.getRawValue();
    const styleAutoId = row.controls.styleAutoId.getRawValue();
    if (!diaAutoId || !styleAutoId) {
      row.controls.availableQuantityKgs.setValue(null);
      return;
    }
    this.inHouseApi.getAvailableCuttingQuantity(diaAutoId, styleAutoId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (response) => row.controls.availableQuantityKgs.setValue(response.availableQuantityKgs),
        error: () => row.controls.availableQuantityKgs.setValue(null)
      });
  }

  splitSeverity(status: string): 'info' | 'warn' | 'success' {
    if (status === 'FULLY_SPLIT') {
      return 'success';
    }
    if (status === 'PARTIALLY_SPLIT') {
      return 'warn';
    }
    return 'info';
  }

  cuttingSeverity(status: string): 'success' | 'warn' {
    return status === 'COMPLETED' ? 'success' : 'warn';
  }

  isInvalid(control: AbstractControl | null): boolean {
    return !!control && control.invalid && (control.touched || control.dirty);
  }

  private buildSplitErrorMessage(error: unknown): string {
    const apiError = extractApiError(error);
    if (!apiError) {
      return getApiErrorMessage(error);
    }

    if (apiError.code === 'SPLIT_EXCEEDS_DELIVERY') {
      const available = Number(apiError.details['deliveryQuantityKgs'] ?? 0) - Number(apiError.details['alreadyAllocatedQuantityKgs'] ?? 0);
      const requested = Number(apiError.details['requestedQuantityKgs'] ?? 0);
      return `Split quantity exceeds available delivery quantity. Available: ${this.formatNumber(available)} kg, requested: ${this.formatNumber(requested)} kg.`;
    }

    if (apiError.code === 'VALIDATION_ERROR') {
      const validationMessages = Object.values(apiError.details)
        .filter((value): value is string => typeof value === 'string')
        .join(' ');
      return validationMessages || apiError.message;
    }

    return apiError.message;
  }

  private toApiDate(value: Date): string {
    return new Date(Date.UTC(value.getFullYear(), value.getMonth(), value.getDate())).toISOString().slice(0, 10);
  }

  private formatNumber(value: number): string {
    return new Intl.NumberFormat('en-IN', { maximumFractionDigits: 2 }).format(value ?? 0);
  }
}
