import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { DatePickerModule } from 'primeng/datepicker';
import { DialogModule } from 'primeng/dialog';
import { InputNumberModule } from 'primeng/inputnumber';
import { TabViewModule } from 'primeng/tabview';
import { SelectModule } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { GarmentSize, OptionItem } from '../../core/models/common.models';
import { PackingDashboardResponse, PackingEntry, PackingStockType } from '../../core/models/packing.models';
import { Style } from '../../core/models/master-data.models';
import { MasterDataApiService } from '../../core/services/master-data-api.service';
import { NotificationService } from '../../core/services/notification.service';
import { PackingApiService } from '../../core/services/packing-api.service';
import { getApiErrorMessage } from '../../core/utils/api-error.utils';

@Component({
  selector: 'app-packing-page',
  standalone: true,
  imports: [ReactiveFormsModule, DatePipe, ButtonModule, CheckboxModule, DatePickerModule, DialogModule, InputNumberModule, SelectModule, TableModule, TabViewModule],
  templateUrl: './packing-page.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PackingPageComponent {
  private readonly packingApi = inject(PackingApiService);
  private readonly masterDataApi = inject(MasterDataApiService);
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly notificationService = inject(NotificationService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly destroyRef = inject(DestroyRef);

  readonly dashboard = signal<PackingDashboardResponse | null>(null);
  readonly entries = signal<PackingEntry[]>([]);
  readonly styles = signal<Style[]>([]);
  readonly availablePieces = signal<number | null>(null);
  readonly loading = signal(true);
  readonly dialogVisible = signal(false);
  readonly detailVisible = signal(false);
  readonly detail = signal<PackingEntry | null>(null);
  readonly selectedEntries = signal<PackingEntry[]>([]);

  readonly form = this.fb.group({
    packingDate: this.fb.control<Date | null>(new Date(), Validators.required),
    styleAutoId: this.fb.control('', Validators.required),
    size: this.fb.control<GarmentSize>('M', Validators.required),
    stockType: this.fb.control<PackingStockType>('PLAIN', Validators.required),
    availablePieces: this.fb.control<number | null>({ value: null, disabled: true }),
    correctlyPackedPieces: this.fb.control<number | null>(null, [Validators.required, Validators.min(0)]),
    defectivePieces: this.fb.control<number | null>(0, [Validators.required, Validators.min(0)])
  });

  readonly styleOptions = computed<OptionItem[]>(() => this.styles().map((style) => ({ label: style.styleName, value: style.autoId })));
  readonly sizeOptions: OptionItem<GarmentSize>[] = [
    { label: 'XS', value: 'XS' }, { label: 'S', value: 'S' }, { label: 'M', value: 'M' }, { label: 'L', value: 'L' }, { label: 'XL', value: 'XL' }, { label: '2XL', value: 'XXL' }
  ];
  readonly stockTypeOptions: OptionItem<PackingStockType>[] = [
    { label: 'Plain Stock', value: 'PLAIN' },
    { label: 'Printed Stock', value: 'PRINTED' }
  ];

  readonly summaryCards = computed(() => {
    const dashboard = this.dashboard();
    if (!dashboard) {
      return [];
    }
    return [
      { title: 'Packed Pieces', value: `${dashboard.totalPackedPieces}`, note: 'Pieces packed successfully.', icon: 'pi pi-box', tone: 'ocean' },
      { title: 'Defective Pieces', value: `${dashboard.totalDefectivePieces}`, note: 'Pieces rejected during packing.', icon: 'pi pi-exclamation-triangle', tone: 'amber' }
    ];
  });

  constructor() {
    this.loadData();
  }

  loadData(): void {
    this.loading.set(true);
    forkJoin({
      dashboard: this.packingApi.getDashboard(),
      entries: this.packingApi.getEntries({ page: 0, size: 100, sortField: 'packingDate', sortDirection: 'desc', includeDeleted: false }),
      styles: this.masterDataApi.getStyleOptions()
    }).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: ({ dashboard, entries, styles }) => {
        this.dashboard.set(dashboard);
        this.entries.set(entries.content);
        this.styles.set(styles);
        this.loading.set(false);
      },
      error: (error) => {
        this.loading.set(false);
        this.notificationService.error('Unable to load packing module', getApiErrorMessage(error));
      }
    });
  }

  openCreate(): void {
    this.dialogVisible.set(true);
    this.form.reset({ packingDate: new Date(), styleAutoId: '', size: 'M', stockType: 'PLAIN', availablePieces: null, correctlyPackedPieces: null, defectivePieces: 0 });
  }

  openDetail(record: PackingEntry): void {
    this.detail.set(record);
    this.detailVisible.set(true);
  }

  refreshAvailability(): void {
    const value = this.form.getRawValue();
    if (!value.styleAutoId || !value.size || !value.stockType) {
      this.form.controls.availablePieces.setValue(null);
      return;
    }
    this.packingApi.getAvailablePieces(value.styleAutoId, value.size, value.stockType)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({ next: (response) => this.form.controls.availablePieces.setValue(response.availablePieces) });
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();
    this.packingApi.createEntry({
      packingDate: this.toApiDate(value.packingDate ?? new Date()),
      styleAutoId: value.styleAutoId,
      size: value.size,
      stockType: value.stockType,
      correctlyPackedPieces: Number(value.correctlyPackedPieces ?? 0),
      defectivePieces: Number(value.defectivePieces ?? 0)
    }).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.notificationService.success('Packing saved', 'The packing entry was saved.');
        this.dialogVisible.set(false);
        this.loadData();
      },
      error: (error) => this.notificationService.error('Unable to save packing entry', getApiErrorMessage(error))
    });
  }

  confirmDelete(record: PackingEntry): void {
    this.confirmationService.confirm({
      header: 'Delete packing entry',
      message: `Delete ${record.autoId}?`,
      acceptButtonStyleClass: 'p-button-danger',
      rejectButtonStyleClass: 'p-button-outlined p-button-secondary',
      accept: () => this.packingApi.deleteEntry(record.autoId).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({ next: () => this.loadData() })
    });
  }

  confirmDeleteEntries(): void {
    const selected = this.selectedEntries();
    if (!selected.length) {
      return;
    }
    this.confirmationService.confirm({
      header: 'Delete packing entries',
      message: `Soft delete ${selected.length} selected pack record(s)?`,
      acceptLabel: 'Delete',
      rejectLabel: 'Cancel',
      acceptButtonStyleClass: 'p-button-danger',
      rejectButtonStyleClass: 'p-button-outlined p-button-secondary',
      accept: () => forkJoin(selected.map((record) => this.packingApi.deleteEntry(record.autoId)))
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({
          next: () => {
            this.selectedEntries.set([]);
            this.notificationService.success('Entries deleted', 'Selected packing entries were removed from the active list.');
            this.loadData();
          },
          error: (error) => this.notificationService.error('Unable to delete packing entries', getApiErrorMessage(error))
        })
    });
  }

  private toApiDate(value: Date): string {
    return new Date(Date.UTC(value.getFullYear(), value.getMonth(), value.getDate())).toISOString().slice(0, 10);
  }
}
