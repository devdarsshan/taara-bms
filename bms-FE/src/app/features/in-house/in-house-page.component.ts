import { DatePipe, DecimalPipe, CurrencyPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { AbstractControl, FormArray, FormControl, FormGroup, FormsModule, NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
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
  ExistingStockCreateRequest,
  ExistingStockResponse,
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
  availableQuantityKgs: FormControl<number | null>;
  quantityUsedKgs: FormControl<number | null>;
  ratePerPiece: FormControl<number | null>;
}>;

@Component({
  selector: 'app-in-house-page',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    FormsModule,
    CurrencyPipe,
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
  readonly existingStocks = signal<ExistingStockResponse[]>([]);
  readonly styles = signal<Style[]>([]);
  readonly dias = signal<Dia[]>([]);
  readonly loading = signal(true);

  readonly deliveryDetail = signal<InHouseDelivery | null>(null);
  readonly deliverySplits = signal<InHouseSplit[]>([]);
  readonly deliveryDialogVisible = signal(false);
  readonly existingStockDialogVisible = signal(false);
  readonly splitError = signal<string | null>(null);
  readonly selectedCuttings = signal<CuttingEntry[]>([]);
  readonly editingSplitId = signal<string | null>(null);

  readonly deliveryFilters = this.fb.group({
    styleAutoId: this.fb.control(''),
    fromDate: this.fb.control<Date | null>(null),
    toDate: this.fb.control<Date | null>(null)
  });

  readonly cuttingFilters = this.fb.group({
    styleAutoId: this.fb.control(''),
    fromDate: this.fb.control<Date | null>(null),
    toDate: this.fb.control<Date | null>(null)
  });

  readonly cuttingDialogVisible = signal(false);
  readonly editingCutting = signal<CuttingEntry | null>(null);

  readonly splitForm = this.fb.group({
    rows: this.fb.array<SplitRowForm>([])
  });

  readonly existingStockForm = this.fb.group({
    entryDate: this.fb.control<Date | null>(new Date(), Validators.required),
    diaAutoId: this.fb.control('', Validators.required),
    styleAutoId: this.fb.control('', Validators.required),
    quantityKgs: this.fb.control<number | null>(null, [Validators.required, Validators.min(0.01)]),
    notes: this.fb.control('')
  });

  readonly cuttingForm = this.fb.group({
    cuttingDate: this.fb.control<Date | null>(new Date(), Validators.required),
    totalOutputPieces: this.fb.control<number | null>(null, [Validators.required, Validators.min(1)]),
    styleAutoId: this.fb.control('', Validators.required),
    size: this.fb.control<GarmentSize>('M', Validators.required),
    ratePerPiece: this.fb.control<number | null>(null, [Validators.min(0)]),
    notes: this.fb.control(''),
    rows: this.fb.array<CuttingRowForm>([])
  });

  readonly sizeOptions: OptionItem<GarmentSize>[] = [
    { label: 'XS', value: 'XS' },
    { label: 'S', value: 'S' },
    { label: 'M', value: 'M' },
    { label: 'L', value: 'L' },
    { label: 'XL', value: 'XL' },
    { label: '2XL', value: '2XL' }
  ];

  readonly styleOptions = computed<OptionItem[]>(() =>
    this.styles().map((style) => ({ label: style.styleName, value: style.autoId }))
  );

  readonly diaOptions = computed<OptionItem[]>(() =>
    this.dias().map((dia) => ({ label: dia.diaValue, value: dia.autoId }))
  );

  getAvailableDiaOptions(index: number): OptionItem[] {
    const allOptions = this.diaOptions();
    const rows = this.cuttingRows.getRawValue();
    const selectedDias = rows
      .map((r, i) => i !== index ? r.diaAutoId : null)
      .filter((id): id is string => !!id);
    
    return allOptions.filter(opt => !selectedDias.includes(opt.value));
  }

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

  private readonly cuttingFormValue = toSignal(this.cuttingForm.valueChanges);

  readonly totalKgUsed = computed(() => {
    this.cuttingFormValue();
    return this.cuttingRows.controls.reduce((sum, row) => sum + Number(row.controls.quantityUsedKgs.getRawValue() ?? 0), 0);
  });

  readonly totalOutputPieces = computed(() => {
    this.cuttingFormValue();
    return Number(this.cuttingForm.controls.totalOutputPieces.getRawValue() ?? 0);
  });

  readonly totalCost = computed(() => {
    this.cuttingFormValue();
    const pcs = this.totalOutputPieces();
    const rate = Number(this.cuttingForm.controls.ratePerPiece.getRawValue() ?? 0);
    return pcs * rate;
  });

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
    
    this.deliveryFilters.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => this.loadData());
    this.cuttingFilters.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => this.loadData());

    this.cuttingForm.controls.styleAutoId.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      for (const row of this.cuttingRows.controls) {
        this.refreshRowAvailability(row);
      }
    });
  }

  loadData(): void {
    const deliveryF = this.deliveryFilters.getRawValue();
    const cuttingF = this.cuttingFilters.getRawValue();

    this.loading.set(true);
    forkJoin({
      dashboard: this.inHouseApi.getDashboard({
        styleAutoId: deliveryF.styleAutoId || undefined,
        fromDate: deliveryF.fromDate ? this.toApiDate(deliveryF.fromDate) : undefined,
        toDate: deliveryF.toDate ? this.toApiDate(deliveryF.toDate) : undefined
      }),
      deliveries: this.inHouseApi.getDeliveries({ 
        page: 0, 
        size: 100, 
        sortField: 'deliveryDate', 
        sortDirection: 'desc', 
        includeDeleted: false,
        styleAutoId: deliveryF.styleAutoId || undefined,
        fromDate: deliveryF.fromDate ? this.toApiDate(deliveryF.fromDate) : undefined,
        toDate: deliveryF.toDate ? this.toApiDate(deliveryF.toDate) : undefined
      }),
      stock: this.inHouseApi.getStock({
        styleAutoId: deliveryF.styleAutoId || undefined
      }),
      existingStocks: this.inHouseApi.getExistingStocks(),
      stitchedStock: this.inHouseApi.getStitchedStock({
        styleAutoId: cuttingF.styleAutoId || undefined
      }),
      cuttings: this.inHouseApi.getCuttings({ 
        page: 0, 
        size: 100, 
        sortField: 'cuttingDate', 
        sortDirection: 'desc', 
        includeDeleted: false,
        styleAutoId: cuttingF.styleAutoId || undefined,
        fromDate: cuttingF.fromDate ? this.toApiDate(cuttingF.fromDate) : undefined,
        toDate: cuttingF.toDate ? this.toApiDate(cuttingF.toDate) : undefined
      }),
      styles: this.masterDataApi.getStyleOptions(),
      dias: this.masterDataApi.getDiaOptions()
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ({ dashboard, deliveries, stock, existingStocks, stitchedStock, cuttings, styles, dias }) => {
          this.dashboard.set(dashboard);
          this.deliveries.set(deliveries.content);
          this.stockRows.set(stock);
          this.existingStocks.set(existingStocks);
          this.stitchedStockRows.set(stitchedStock);
          this.cuttings.set(cuttings.content);
          this.styles.set(styles);
          this.dias.set(dias);
          
          const currentDetail = this.deliveryDetail();
          if (currentDetail) {
            const updatedDelivery = deliveries.content.find(d => d.autoId === currentDetail.autoId);
            if (updatedDelivery) {
              this.deliveryDetail.set(updatedDelivery);
            }
          }
          
          this.loading.set(false);
        },
        error: (error: any) => {
          this.loading.set(false);
          this.notificationService.error('Unable to load in-house module', getApiErrorMessage(error));
        }
      });
  }

  handleDeliveryDialogVisibilityChange(visible: boolean): void {
    if (!visible) {
      this.deliveryDetail.set(null);
      this.splitForm.reset();
      this.splitRows.clear();
      this.splitError.set(null);
    }
    this.deliveryDialogVisible.set(visible);
  }

  handleExistingStockDialogVisibilityChange(visible: boolean): void {
    if (!visible) {
      this.existingStockForm.reset({ entryDate: new Date(), diaAutoId: '', styleAutoId: '', quantityKgs: null, notes: '' });
    }
    this.existingStockDialogVisible.set(visible);
  }

  handleCuttingDialogVisibilityChange(visible: boolean): void {
    if (!visible) {
      this.editingCutting.set(null);
      this.cuttingForm.reset({ cuttingDate: new Date(), totalOutputPieces: null, styleAutoId: '', size: 'M', ratePerPiece: null, notes: '' });
      this.cuttingRows.clear();
    }
    this.cuttingDialogVisible.set(visible);
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
        next: (splits) => {
          this.deliverySplits.set(splits);
        },
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
    } else {
      this.splitRows.at(0).reset({ diaAutoId: '', quantityKgs: null });
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

    const newRows = this.splitRows.getRawValue().filter(row => !!row.diaAutoId && (row.quantityKgs ?? 0) > 0);
    if (newRows.length === 0) {
      this.splitError.set('Add at least one valid split row.');
      return;
    }

    this.splitError.set(null);
    const payload = {
      splits: newRows.map((row) => ({
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

  startSplitEdit(split: InHouseSplit): void {
    this.editingSplitId.set(split.autoId);
  }

  cancelSplitEdit(): void {
    this.editingSplitId.set(null);
    if (this.deliveryDetail()) {
      this.openDelivery(this.deliveryDetail()!);
    }
  }

  saveSplitEdit(split: InHouseSplit): void {
    const delivery = this.deliveryDetail();
    if (!delivery) return;
    this.inHouseApi.updateSplit(delivery.autoId, split.autoId, { diaAutoId: split.dia.autoId, quantityKgs: split.quantityKgs })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.notificationService.success('Split updated', 'The fabric split was updated successfully.');
          this.editingSplitId.set(null);
          this.openDelivery(delivery);
          this.loadData();
        },
        error: (error) => this.notificationService.error('Unable to update split', getApiErrorMessage(error))
      });
  }

  openCreateCutting(): void {
    this.editingCutting.set(null);
    this.cuttingDialogVisible.set(true);
    this.cuttingForm.reset({ cuttingDate: new Date(), totalOutputPieces: null, styleAutoId: '', size: 'M', ratePerPiece: null, notes: '' });
    this.cuttingRows.clear();
    this.addCuttingRow();
  }

  openAddExistingStock(): void {
    this.existingStockForm.reset({ entryDate: new Date(), diaAutoId: '', styleAutoId: '', quantityKgs: null, notes: '' });
    this.existingStockDialogVisible.set(true);
  }

  saveExistingStock(): void {
    if (this.existingStockForm.invalid) {
      this.existingStockForm.markAllAsTouched();
      this.notificationService.warn('Form Invalid', this.getFormValidationMessage(this.existingStockForm));
      return;
    }
    const value = this.existingStockForm.getRawValue();
    const payload: ExistingStockCreateRequest = {
      entryDate: this.toApiDate(value.entryDate ?? new Date()),
      diaAutoId: value.diaAutoId,
      styleAutoId: value.styleAutoId,
      quantityKgs: Number(value.quantityKgs ?? 0),
      notes: value.notes?.trim() || null
    };
    this.inHouseApi.createExistingStock(payload)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.notificationService.success('Stock added', 'Existing stock was added successfully.');
          this.existingStockDialogVisible.set(false);
          this.loadData();
        },
        error: (error) => this.notificationService.error('Unable to add stock', getApiErrorMessage(error))
      });
  }

  openCutting(record: CuttingEntry): void {
    this.editingCutting.set(record);
    
    const firstRow = record.rows[0];
    this.cuttingForm.patchValue({ 
        cuttingDate: new Date(record.cuttingDate), 
        totalOutputPieces: record.totalOutputPieces, 
        styleAutoId: firstRow?.style?.autoId || '',
        size: firstRow?.size || 'M',
        ratePerPiece: firstRow?.ratePerPiece || null,
        notes: record.notes ?? '' 
    });

    this.cuttingRows.clear();
    for (const row of record.rows) {
      const group = this.createCuttingRow();
      group.patchValue({
        diaAutoId: row.dia?.autoId || '',
        quantityUsedKgs: row.quantityUsedKgs,
        ratePerPiece: row.ratePerPiece ?? null,
        availableQuantityKgs: null
      });
      this.cuttingRows.push(group);
    }

    // Refresh availability for all added rows
    this.refreshCuttingAvailability();
    
    this.cuttingDialogVisible.set(true);
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

  refreshCuttingAvailability(): void {
    this.cuttingRows.controls.forEach((row) => this.refreshRowAvailability(row));
  }

  saveCutting(): void {
    if (this.cuttingForm.invalid) {
      this.cuttingForm.markAllAsTouched();
      this.notificationService.warn('Form Invalid', this.getFormValidationMessage(this.cuttingForm));
      return;
    }
    const value = this.cuttingForm.getRawValue();
    if (this.cuttingRows.length === 0) {
      this.notificationService.warn('No Rows', 'Please add at least one cutting row (fabric used).');
      return;
    }

    const payload: CuttingCreateRequest = {
      cuttingDate: this.toApiDate(value.cuttingDate ?? new Date()),
      totalOutputPieces: Number(value.totalOutputPieces ?? 0),
      notes: value.notes?.trim() || null,
      rows: value.rows.map((row) => ({
        diaAutoId: row.diaAutoId,
        styleAutoId: value.styleAutoId,
        size: value.size,
        quantityUsedKgs: Number(row.quantityUsedKgs ?? 0),
        ratePerPiece: value.ratePerPiece ? Number(value.ratePerPiece) : null
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
      availableQuantityKgs: this.fb.control<number | null>({ value: null, disabled: true }),
      quantityUsedKgs: this.fb.control<number | null>(null, [Validators.required, Validators.min(0.01)]),
      ratePerPiece: this.fb.control<number | null>(null, [Validators.min(0)])
    });
    return group;
  }

  private refreshRowAvailability(row: CuttingRowForm): void {
    const diaAutoId = row.controls.diaAutoId.getRawValue();
    const styleAutoId = this.cuttingForm.controls.styleAutoId.getRawValue();
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

  private getFormValidationMessage(form: FormGroup): string {
    const invalidFields: string[] = [];
    Object.keys(form.controls).forEach(key => {
      const control = form.get(key);
      if (control?.invalid) {
        if (control instanceof FormArray) {
          control.controls.forEach((group, index) => {
            if (group.invalid && group instanceof FormGroup) {
              Object.keys(group.controls).forEach(subKey => {
                if (group.get(subKey)?.invalid) {
                  invalidFields.push(`${key} row ${index + 1}: ${subKey}`);
                }
              });
            }
          });
        } else {
          invalidFields.push(key);
        }
      }
    });

    if (invalidFields.length > 0) {
      return `Invalid fields: ${invalidFields.join(', ')}`;
    }
    return 'Please check all required fields.';
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
