import { DatePipe, DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AbstractControl, NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { debounceTime, distinctUntilChanged, forkJoin } from 'rxjs';
import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { DatePickerModule } from 'primeng/datepicker';
import { DialogModule } from 'primeng/dialog';
import { InputNumberModule } from 'primeng/inputnumber';
import { MessageModule } from 'primeng/message';
import { SelectModule } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { TabViewModule } from 'primeng/tabview';
import { TextareaModule } from 'primeng/textarea';
import { QueryOptions } from '../../core/models/api.models';
import { Style } from '../../core/models/master-data.models';
import { SpinningDashboardResponse, SpinningDelivery, SpinningOrder } from '../../core/models/spinning.models';
import { YarnOrder } from '../../core/models/yarn.models';
import { MasterDataApiService } from '../../core/services/master-data-api.service';
import { NotificationService } from '../../core/services/notification.service';
import { SpinningApiService } from '../../core/services/spinning-api.service';
import { YarnApiService } from '../../core/services/yarn-api.service';
import { getApiErrorMessage } from '../../core/utils/api-error.utils';

@Component({
  selector: 'app-spinning-page',
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
    MessageModule,
    SelectModule,
    TableModule,
    TabViewModule,
    TextareaModule
  ],
  templateUrl: './spinning-page.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SpinningPageComponent {
  private readonly spinningApi = inject(SpinningApiService);
  private readonly yarnApi = inject(YarnApiService);
  private readonly masterDataApi = inject(MasterDataApiService);
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly notificationService = inject(NotificationService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly destroyRef = inject(DestroyRef);

  readonly dashboard = signal<SpinningDashboardResponse | null>(null);
  readonly styles = signal<Style[]>([]);
  readonly yarnOrders = signal<YarnOrder[]>([]);
  readonly orders = signal<SpinningOrder[]>([]);
  readonly deliveries = signal<SpinningDelivery[]>([]);
  readonly totalOrders = signal(0);
  readonly totalDeliveries = signal(0);
  readonly loadingOrders = signal(false);
  readonly loadingDeliveries = signal(false);
  readonly selectedOrders = signal<SpinningOrder[]>([]);
  readonly selectedDeliveries = signal<SpinningDelivery[]>([]);
  readonly activeTab = signal(0);
  readonly orderDetail = signal<SpinningOrder | null>(null);
  readonly deliveryDetail = signal<SpinningDelivery | null>(null);
  readonly orderDetailVisible = signal(false);
  readonly deliveryDetailVisible = signal(false);
  readonly orderCreateVisible = signal(false);
  readonly deliveryCreateVisible = signal(false);
  readonly orderSubmitting = signal(false);
  readonly deliverySubmitting = signal(false);
  readonly orderError = signal<string | null>(null);
  readonly deliveryError = signal<string | null>(null);

  readonly orderFilters = this.fb.group({
    styleAutoId: this.fb.control(''),
    linkedYarnOrder: this.fb.control<'ALL' | 'LINKED' | 'MANUAL'>('ALL')
  });

  readonly deliveryFilters = this.fb.group({
    styleAutoId: this.fb.control('')
  });

  readonly createOrderForm = this.fb.group({
    dispatchDate: this.fb.control<Date | null>(new Date(), [Validators.required]),
    styleAutoId: this.fb.control('', [Validators.required]),
    linkedYarnOrderAutoId: this.fb.control(''),
    quantitySentKgs: this.fb.control<number | null>(null, [Validators.required, Validators.min(0.01)]),
    factoryNotes: this.fb.control('')
  });

  readonly createDeliveryForm = this.fb.group({
    deliveryDate: this.fb.control<Date | null>(new Date(), [Validators.required]),
    styleAutoId: this.fb.control('', [Validators.required]),
    actualQuantityKgs: this.fb.control<number | null>(null, [Validators.required, Validators.min(0.01)]),
    bufferQuantityKgs: this.fb.control<number | null>(0),
    notes: this.fb.control('')
  });

  readonly orderQuery = signal<QueryOptions>({
    page: 0,
    size: 10,
    sortField: 'updatedAt',
    sortDirection: 'desc',
    includeDeleted: false
  });

  readonly deliveryQuery = signal<QueryOptions>({
    page: 0,
    size: 10,
    sortField: 'updatedAt',
    sortDirection: 'desc',
    includeDeleted: false
  });

  readonly styleOptions = computed(() => this.styles().map((style) => ({ label: `${style.autoId} - ${style.styleName}`, value: style.autoId })));
  readonly yarnOrderOptions = computed(() => this.yarnOrders().map((order) => ({ label: `${order.autoId} - ${order.style.styleName}`, value: order.autoId })));
  readonly linkedOptions = [
    { label: 'All orders', value: 'ALL' },
    { label: 'Linked only', value: 'LINKED' },
    { label: 'Manual only', value: 'MANUAL' }
  ];

  readonly summaryCards = computed(() => {
    const dashboard = this.dashboard();
    if (!dashboard) {
      return [];
    }

    return [
      { title: 'Dispatched', value: `${this.formatNumber(dashboard.totalDispatchedToSpinning)} kg`, note: 'Quantity sent to knitting.', icon: 'pi pi-arrow-right-arrow-left', tone: 'ocean' },
      { title: 'Received', value: `${this.formatNumber(dashboard.totalReceivedFromSpinning)} kg`, note: 'Quantity received from deliveries.', icon: 'pi pi-download', tone: 'teal' },
      { title: 'Pending', value: `${this.formatNumber(dashboard.netPendingAtFactory)} kg`, note: 'Net quantity still pending at factory.', icon: 'pi pi-sync', tone: 'indigo' }
    ];
  });

  constructor() {
    this.bindFilters();
    this.loadInitialData();
  }

  loadInitialData(): void {
    forkJoin({
      dashboard: this.spinningApi.getDashboard(),
      styles: this.masterDataApi.getStyleOptions(),
      yarnOrders: this.yarnApi.getOrders({ page: 0, size: 200, sortField: 'orderDate', sortDirection: 'desc', includeDeleted: false }),
      orders: this.loadOrdersRequest(),
      deliveries: this.loadDeliveriesRequest()
    }).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: ({ dashboard, styles, yarnOrders, orders, deliveries }) => {
        this.dashboard.set(dashboard);
        this.styles.set(styles);
        this.yarnOrders.set(yarnOrders.content);
        this.orders.set(orders.content);
        this.totalOrders.set(orders.totalElements);
        this.deliveries.set(deliveries.content);
        this.totalDeliveries.set(deliveries.totalElements);
      },
      error: (error) => this.notificationService.error('Unable to load knitting workspace', getApiErrorMessage(error))
    });
  }

  onOrderLazyLoad(event: { first?: number | null; rows?: number | null; sortField?: string | string[] | null; sortOrder?: number | null }): void {
    this.orderQuery.set(this.resolveQuery(this.orderQuery(), event));
    this.reloadOrders();
  }

  onDeliveryLazyLoad(event: { first?: number | null; rows?: number | null; sortField?: string | string[] | null; sortOrder?: number | null }): void {
    this.deliveryQuery.set(this.resolveQuery(this.deliveryQuery(), event));
    this.reloadDeliveries();
  }

  openCreateOrder(): void {
    this.orderError.set(null);
    this.createOrderForm.reset({ dispatchDate: new Date(), styleAutoId: '', linkedYarnOrderAutoId: '', quantitySentKgs: null, factoryNotes: '' });
    this.orderCreateVisible.set(true);
  }

  openCreateDelivery(): void {
    this.deliveryError.set(null);
    this.createDeliveryForm.reset({ deliveryDate: new Date(), styleAutoId: '', actualQuantityKgs: null, bufferQuantityKgs: 0, notes: '' });
    this.deliveryCreateVisible.set(true);
  }

  createOrder(): void {
    if (this.createOrderForm.pristine) {
      this.notificationService.warn('No changes available to save', 'Update the knitting order form before saving.');
      return;
    }

    if (this.createOrderForm.invalid) {
      this.createOrderForm.markAllAsTouched();
      this.notificationService.warn('Knitting order not saved', 'Complete the required order fields before saving.');
      return;
    }

    const value = this.createOrderForm.getRawValue();
    if (!value.dispatchDate || !value.styleAutoId || !value.quantitySentKgs) {
      this.notificationService.warn('Knitting order not saved', 'Dispatch date, style, and quantity are required.');
      return;
    }

    this.orderSubmitting.set(true);
    this.spinningApi.createOrder({
      dispatchDate: this.toApiDate(value.dispatchDate),
      styleAutoId: value.styleAutoId,
      linkedYarnOrderAutoId: value.linkedYarnOrderAutoId || null,
      quantitySentKgs: value.quantitySentKgs,
      factoryNotes: value.factoryNotes?.trim() || null
    }).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.orderSubmitting.set(false);
        this.orderCreateVisible.set(false);
        this.notificationService.success('Knitting order created', 'The knitting order has been saved.');
        this.reloadOrders();
      },
      error: (error) => {
        this.orderSubmitting.set(false);
        const message = getApiErrorMessage(error);
        this.orderError.set(message);
        this.notificationService.error('Unable to create knitting order', message);
      }
    });
  }

  createDelivery(): void {
    if (this.createDeliveryForm.pristine) {
      this.notificationService.warn('No changes available to save', 'Update the knitting delivery form before saving.');
      return;
    }

    if (this.createDeliveryForm.invalid) {
      this.createDeliveryForm.markAllAsTouched();
      this.notificationService.warn('Knitting delivery not saved', 'Complete the required delivery fields before saving.');
      return;
    }

    const value = this.createDeliveryForm.getRawValue();
    if (!value.deliveryDate || !value.styleAutoId || !value.actualQuantityKgs) {
      this.notificationService.warn('Knitting delivery not saved', 'Delivery date, style, and actual quantity are required.');
      return;
    }

    this.deliverySubmitting.set(true);
    this.spinningApi.createDelivery({
      deliveryDate: this.toApiDate(value.deliveryDate),
      styleAutoId: value.styleAutoId,
      actualQuantityKgs: value.actualQuantityKgs,
      bufferQuantityKgs: value.bufferQuantityKgs ?? 0,
      notes: value.notes?.trim() || null
    }).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.deliverySubmitting.set(false);
        this.deliveryCreateVisible.set(false);
        this.notificationService.success('Knitting delivery created', 'The knitting delivery has been saved.');
        this.reloadDeliveries();
      },
      error: (error) => {
        this.deliverySubmitting.set(false);
        const message = getApiErrorMessage(error);
        this.deliveryError.set(message);
        this.notificationService.error('Unable to create knitting delivery', message);
      }
    });
  }

  confirmDeleteOrders(): void {
    this.confirmDelete(
      'Delete knitting orders',
      this.selectedOrders(),
      (record) => this.spinningApi.deleteOrder(record.autoId),
      () => {
        this.selectedOrders.set([]);
        this.notificationService.success('Knitting orders deleted', 'Selected knitting orders were moved out of the active list.');
        this.reloadOrders();
      },
      'Unable to delete knitting orders'
    );
  }

  confirmDeleteDeliveries(): void {
    this.confirmDelete(
      'Delete knitting deliveries',
      this.selectedDeliveries(),
      (record) => this.spinningApi.deleteDelivery(record.autoId),
      () => {
        this.selectedDeliveries.set([]);
        this.notificationService.success('Knitting deliveries deleted', 'Selected knitting deliveries were moved out of the active list.');
        this.reloadDeliveries();
      },
      'Unable to delete knitting deliveries'
    );
  }

  openOrderDetail(record: SpinningOrder): void {
    this.orderDetail.set(record);
    this.orderDetailVisible.set(true);
  }

  openDeliveryDetail(record: SpinningDelivery): void {
    this.deliveryDetail.set(record);
    this.deliveryDetailVisible.set(true);
  }

  closeOrderCreateDialog(): void {
    if (this.createOrderForm.dirty) {
      this.confirmationService.confirm({
        header: 'Discard knitting order changes',
        message: 'You have unsaved changes. Do you want to close this form?',
        acceptLabel: 'Discard',
        rejectLabel: 'Keep editing',
        acceptButtonStyleClass: 'p-button-danger',
        rejectButtonStyleClass: 'p-button-outlined p-button-secondary',
        accept: () => this.orderCreateVisible.set(false)
      });
      return;
    }

    this.orderCreateVisible.set(false);
  }

  closeDeliveryCreateDialog(): void {
    if (this.createDeliveryForm.dirty) {
      this.confirmationService.confirm({
        header: 'Discard knitting delivery changes',
        message: 'You have unsaved changes. Do you want to close this form?',
        acceptLabel: 'Discard',
        rejectLabel: 'Keep editing',
        acceptButtonStyleClass: 'p-button-danger',
        rejectButtonStyleClass: 'p-button-outlined p-button-secondary',
        accept: () => this.deliveryCreateVisible.set(false)
      });
      return;
    }

    this.deliveryCreateVisible.set(false);
  }

  handleOrderCreateVisibilityChange(visible: boolean): void {
    if (visible) {
      this.orderCreateVisible.set(true);
      return;
    }

    this.closeOrderCreateDialog();
  }

  handleDeliveryCreateVisibilityChange(visible: boolean): void {
    if (visible) {
      this.deliveryCreateVisible.set(true);
      return;
    }

    this.closeDeliveryCreateDialog();
  }

  isInvalid(control: AbstractControl | null): boolean {
    return !!control && control.invalid && (control.touched || control.dirty);
  }

  private bindFilters(): void {
    this.orderFilters.controls.styleAutoId.valueChanges.pipe(debounceTime(250), distinctUntilChanged(), takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      this.orderQuery.update((query) => ({ ...query, page: 0 }));
      this.reloadOrders();
    });
    this.orderFilters.controls.linkedYarnOrder.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      this.orderQuery.update((query) => ({ ...query, page: 0 }));
      this.reloadOrders();
    });
    this.deliveryFilters.controls.styleAutoId.valueChanges.pipe(debounceTime(250), distinctUntilChanged(), takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      this.deliveryQuery.update((query) => ({ ...query, page: 0 }));
      this.reloadDeliveries();
    });
  }

  private reloadOrders(): void {
    this.loadingOrders.set(true);
    forkJoin({ dashboard: this.spinningApi.getDashboard(this.buildOrderFilters()), orders: this.loadOrdersRequest() })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ({ dashboard, orders }) => {
          this.dashboard.set(dashboard);
          this.orders.set(orders.content);
          this.totalOrders.set(orders.totalElements);
          this.loadingOrders.set(false);
        },
        error: (error) => {
          this.loadingOrders.set(false);
          this.notificationService.error('Unable to refresh knitting orders', getApiErrorMessage(error));
        }
      });
  }

  private reloadDeliveries(): void {
    this.loadingDeliveries.set(true);
    forkJoin({ dashboard: this.spinningApi.getDashboard(this.buildDeliveryFilters()), deliveries: this.loadDeliveriesRequest() })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ({ dashboard, deliveries }) => {
          this.dashboard.set(dashboard);
          this.deliveries.set(deliveries.content);
          this.totalDeliveries.set(deliveries.totalElements);
          this.loadingDeliveries.set(false);
        },
        error: (error) => {
          this.loadingDeliveries.set(false);
          this.notificationService.error('Unable to refresh knitting deliveries', getApiErrorMessage(error));
        }
      });
  }

  private loadOrdersRequest() {
    return this.spinningApi.getOrders({ ...this.orderQuery(), ...this.buildOrderFilters() });
  }

  private loadDeliveriesRequest() {
    return this.spinningApi.getDeliveries({ ...this.deliveryQuery(), ...this.buildDeliveryFilters() });
  }

  private buildOrderFilters() {
    const value = this.orderFilters.getRawValue();
    return {
      styleAutoId: value.styleAutoId || undefined,
      linkedYarnOrder: value.linkedYarnOrder === 'LINKED' ? true : value.linkedYarnOrder === 'MANUAL' ? false : undefined
    };
  }

  private buildDeliveryFilters() {
    const value = this.deliveryFilters.getRawValue();
    return {
      styleAutoId: value.styleAutoId || undefined
    };
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

  private confirmDelete<T>(
    header: string,
    selected: T[],
    deleteFn: (record: T) => unknown,
    onSuccess: () => void,
    errorSummary: string
  ): void {
    if (!selected.length) {
      return;
    }

    this.confirmationService.confirm({
      header,
      message: `Soft delete ${selected.length} selected record(s)?`,
      acceptLabel: 'Delete',
      rejectLabel: 'Cancel',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => {
        forkJoin(selected.map((record) => deleteFn(record) as any)).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
          next: () => onSuccess(),
          error: (error) => this.notificationService.error(errorSummary, getApiErrorMessage(error))
        });
      }
    });
  }

  private toApiDate(value: Date): string {
    return new Date(Date.UTC(value.getFullYear(), value.getMonth(), value.getDate())).toISOString().slice(0, 10);
  }

  private formatNumber(value: number): string {
    return new Intl.NumberFormat('en-IN', { maximumFractionDigits: 2 }).format(value ?? 0);
  }
}
