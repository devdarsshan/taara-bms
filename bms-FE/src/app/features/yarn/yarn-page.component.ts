import { ChangeDetectionStrategy, Component, computed, DestroyRef, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AbstractControl, NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { forkJoin, debounceTime, distinctUntilChanged } from 'rxjs';
import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { DatePickerModule } from 'primeng/datepicker';
import { DialogModule } from 'primeng/dialog';
import { InputNumberModule } from 'primeng/inputnumber';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { SelectModule } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { TextareaModule } from 'primeng/textarea';
import { QueryOptions } from '../../core/models/api.models';
import { SectionProcessType, StitchingSection, Style } from '../../core/models/master-data.models';
import { YarnDashboardResponse, YarnOrder } from '../../core/models/yarn.models';
import { MasterDataApiService } from '../../core/services/master-data-api.service';
import { NotificationService } from '../../core/services/notification.service';
import { YarnApiService } from '../../core/services/yarn-api.service';
import { getApiErrorMessage } from '../../core/utils/api-error.utils';

@Component({
  selector: 'app-yarn-page',
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
    TextareaModule
  ],
  templateUrl: './yarn-page.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class YarnPageComponent {
  private readonly yarnApi = inject(YarnApiService);
  private readonly masterDataApi = inject(MasterDataApiService);
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly notificationService = inject(NotificationService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly destroyRef = inject(DestroyRef);

  readonly dashboard = signal<YarnDashboardResponse | null>(null);
  readonly orders = signal<YarnOrder[]>([]);
  readonly totalOrders = signal(0);
  readonly loading = signal(false);
  readonly styles = signal<Style[]>([]);
  readonly sections = signal<StitchingSection[]>([]);
  readonly selectedOrders = signal<YarnOrder[]>([]);
  readonly createVisible = signal(false);
  readonly submitting = signal(false);
  readonly createError = signal<string | null>(null);
  readonly editingOrder = signal<YarnOrder | null>(null);

  readonly filtersForm = this.fb.group({
    styleAutoId: this.fb.control(''),
    sectionAutoId: this.fb.control(''),
    fromDate: this.fb.control<Date | null>(null),
    toDate: this.fb.control<Date | null>(null)
  });

  readonly createForm = this.fb.group({
    orderDate: this.fb.control<Date | null>(new Date(), [Validators.required]),
    styleAutoId: this.fb.control('', [Validators.required]),
    quantityKgs: this.fb.control<number | null>(null, [Validators.required, Validators.min(0.01)]),
    stitchingSectionAutoId: this.fb.control(''),
    supplierNotes: this.fb.control('')
  });

  readonly tableQuery = signal<QueryOptions>({
    page: 0,
    size: 10,
    sortField: 'updatedAt',
    sortDirection: 'desc',
    includeDeleted: false
  });

  readonly styleOptions = computed(() =>
    this.styles().map((style) => ({
      label: `${style.autoId} - ${style.styleName}`,
      value: style.autoId
    }))
  );

  readonly sectionOptions = computed(() =>
    this.sections().map((section) => ({
      label: `${section.autoId} - ${section.sectionName}`,
      value: section.autoId
    }))
  );

  readonly summaryCards = computed(() => {
    const dashboard = this.dashboard();

    if (!dashboard) {
      return [];
    }

    return [
      {
        title: 'Ordered Yarn',
        value: `${this.formatNumber(dashboard.totalYarnOrdered)} kg`,
        note: 'Total yarn ordered across active records.',
        icon: 'pi pi-box',
        tone: 'ocean'
      },
      {
        title: 'In Order',
        value: `${this.formatNumber(dashboard.yarnInOrder)} kg`,
        note: 'Quantity still open before knitting dispatch.',
        icon: 'pi pi-clock',
        tone: 'teal'
      },
      {
        title: 'Dispatched',
        value: `${this.formatNumber(dashboard.yarnDispatchedToSpinning)} kg`,
        note: 'Quantity already moved into knitting.',
        icon: 'pi pi-arrow-right-arrow-left',
        tone: 'indigo'
      }
    ];
  });

  constructor() {
    this.bindFilters();
    this.loadInitialData();
  }

  loadInitialData(): void {
    this.loading.set(true);

    forkJoin({
      dashboard: this.yarnApi.getDashboard(),
      orders: this.loadOrdersRequest(),
      styles: this.masterDataApi.getStyleOptions(),
      sections: this.masterDataApi.getSectionOptions('KNITTING' as SectionProcessType)
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ({ dashboard, orders, styles, sections }) => {
          this.dashboard.set(dashboard);
          this.orders.set(orders.content);
          this.totalOrders.set(orders.totalElements);
          this.styles.set(styles);
          this.sections.set(sections);
          this.loading.set(false);
        },
        error: (error: any) => {
          this.loading.set(false);
          this.notificationService.error('Unable to load yarn workspace', getApiErrorMessage(error));
        }
      });
  }

  reloadOrders(): void {
    this.loading.set(true);
    forkJoin({
      dashboard: this.yarnApi.getDashboard(this.buildFilters()),
      orders: this.loadOrdersRequest()
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ({ dashboard, orders }) => {
          this.dashboard.set(dashboard);
          this.orders.set(orders.content);
          this.totalOrders.set(orders.totalElements);
          this.loading.set(false);
        },
        error: (error: any) => {
          this.loading.set(false);
          this.notificationService.error('Unable to refresh yarn data', getApiErrorMessage(error));
        }
      });
  }

  onLazyLoad(event: { first?: number | null; rows?: number | null; sortField?: string | string[] | null; sortOrder?: number | null }): void {
    const current = this.tableQuery();
    const rows = event.rows ?? current.size;
    const first = event.first ?? current.page * current.size;
    const sortField = typeof event.sortField === 'string' ? event.sortField : current.sortField;
    const sortDirection = event.sortOrder === 1 ? 'asc' : event.sortOrder === -1 ? 'desc' : current.sortDirection;

    this.tableQuery.set({
      ...current,
      page: Math.floor(first / rows),
      size: rows,
      sortField,
      sortDirection
    });
    this.reloadOrders();
  }

  openCreate(): void {
    this.createError.set(null);
    this.editingOrder.set(null);
    this.createForm.reset({
      orderDate: new Date(),
      styleAutoId: '',
      quantityKgs: null,
      stitchingSectionAutoId: '',
      supplierNotes: ''
    });
    this.createVisible.set(true);
  }

  openEdit(record: YarnOrder): void {
    this.createError.set(null);
    this.editingOrder.set(record);
    this.createForm.reset({
      orderDate: record.orderDate ? new Date(record.orderDate) : null,
      styleAutoId: record.style.autoId,
      quantityKgs: record.quantityKgs,
      stitchingSectionAutoId: record.stitchingSection?.autoId || '',
      supplierNotes: record.supplierNotes || ''
    });
    this.createVisible.set(true);
  }

  createOrder(): void {
    this.createError.set(null);

    if (this.createForm.pristine) {
      this.notificationService.warn('No changes available to save', 'Update the yarn order form before saving.');
      return;
    }

    if (this.createForm.invalid) {
      this.createForm.markAllAsTouched();
      this.notificationService.warn('Yarn order not saved', 'Complete the required fields before saving.');
      return;
    }

    const value = this.createForm.getRawValue();
    if (!value.orderDate || !value.styleAutoId || !value.quantityKgs) {
      this.notificationService.warn('Yarn order not saved', 'Order date, style, and quantity are required.');
      return;
    }

    this.submitting.set(true);
    const orderDate = this.toApiDate(value.orderDate);
    const payload = {
      orderDate: orderDate ?? '',
      styleAutoId: value.styleAutoId,
      quantityKgs: value.quantityKgs,
      stitchingSectionAutoId: value.stitchingSectionAutoId || undefined,
      supplierNotes: value.supplierNotes?.trim() || null
    };

    const editingOrder = this.editingOrder();
    const request$ = editingOrder
      ? this.yarnApi.updateOrder(editingOrder.autoId, payload)
      : this.yarnApi.createOrder(payload);

    request$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.submitting.set(false);
          this.createVisible.set(false);
          this.notificationService.success('Yarn order saved', 'The yarn order has been saved.');
          this.reloadOrders();
        },
        error: (error: any) => {
          this.submitting.set(false);
          const message = getApiErrorMessage(error);
          this.createError.set(message);
          this.notificationService.error('Unable to save yarn order', message);
        }
      });
  }

  confirmDeleteSelected(): void {
    const selected = this.selectedOrders();
    if (!selected.length) {
      return;
    }

    this.confirmationService.confirm({
      header: 'Delete yarn orders',
      message: `Soft delete ${selected.length} selected yarn order(s)?`,
      acceptLabel: 'Delete',
      rejectLabel: 'Cancel',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => {
        forkJoin(selected.map((record) => this.yarnApi.deleteOrder(record.autoId)))
          .pipe(takeUntilDestroyed(this.destroyRef))
          .subscribe({
            next: () => {
              this.selectedOrders.set([]);
              this.notificationService.success('Yarn orders deleted', 'Selected yarn orders were moved out of the active list.');
              this.reloadOrders();
            },
            error: (error: any) => {
              this.notificationService.error('Unable to delete yarn orders', getApiErrorMessage(error));
            }
          });
      }
    });
  }

  closeCreateDialog(): void {
    if (this.createForm.dirty) {
      this.confirmationService.confirm({
        header: 'Discard yarn order changes',
        message: 'You have unsaved changes. Do you want to close this form?',
        acceptLabel: 'Discard',
        rejectLabel: 'Keep editing',
        acceptButtonStyleClass: 'p-button-danger',
        rejectButtonStyleClass: 'p-button-outlined p-button-secondary',
        accept: () => this.createVisible.set(false)
      });
      return;
    }

    this.createVisible.set(false);
  }

  handleCreateDialogVisibilityChange(visible: boolean): void {
    if (visible) {
      this.createVisible.set(true);
      return;
    }

    if (this.createForm.dirty) {
      this.confirmationService.confirm({
        header: 'Discard yarn order changes',
        message: 'You have unsaved changes. Do you want to close this form?',
        acceptLabel: 'Discard',
        rejectLabel: 'Keep editing',
        acceptButtonStyleClass: 'p-button-danger',
        rejectButtonStyleClass: 'p-button-outlined p-button-secondary',
        accept: () => {
          this.createVisible.set(false);
          this.resetCreateForm();
        }
      });
    } else {
      this.createVisible.set(false);
      this.resetCreateForm();
    }
  }

  private resetCreateForm(): void {
    this.editingOrder.set(null);
    this.createError.set(null);
    this.createForm.reset({
      orderDate: new Date(),
      styleAutoId: '',
      quantityKgs: null,
      stitchingSectionAutoId: '',
      supplierNotes: ''
    });
  }

  isInvalid(control: AbstractControl | null): boolean {
    return !!control && control.invalid && (control.touched || control.dirty);
  }

  private bindFilters(): void {
    this.filtersForm.controls.styleAutoId.valueChanges
      .pipe(debounceTime(250), distinctUntilChanged(), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.tableQuery.update((query) => ({ ...query, page: 0 }));
        this.reloadOrders();
      });
      
    this.filtersForm.controls.sectionAutoId.valueChanges
      .pipe(debounceTime(250), distinctUntilChanged(), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.tableQuery.update((query) => ({ ...query, page: 0 }));
        this.reloadOrders();
      });

    this.filtersForm.controls.fromDate.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      this.tableQuery.update((query) => ({ ...query, page: 0 }));
      this.reloadOrders();
    });

    this.filtersForm.controls.toDate.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      this.tableQuery.update((query) => ({ ...query, page: 0 }));
      this.reloadOrders();
    });
  }

  private loadOrdersRequest() {
    const filters = this.buildFilters();
    const query = this.tableQuery();
    return this.yarnApi.getOrders({
      ...query,
      ...filters
    });
  }

  private buildFilters() {
    const filters = this.filtersForm.getRawValue();

    return {
      styleAutoId: filters.styleAutoId || undefined,
      sectionAutoId: filters.sectionAutoId || undefined,
      fromDate: this.toApiDate(filters.fromDate),
      toDate: this.toApiDate(filters.toDate)
    };
  }

  private toApiDate(value: Date | null): string | undefined {
    if (!value) {
      return undefined;
    }

    return new Date(Date.UTC(value.getFullYear(), value.getMonth(), value.getDate())).toISOString().slice(0, 10);
  }

  private formatNumber(value: number): string {
    return new Intl.NumberFormat('en-IN', { maximumFractionDigits: 2 }).format(value ?? 0);
  }
}
