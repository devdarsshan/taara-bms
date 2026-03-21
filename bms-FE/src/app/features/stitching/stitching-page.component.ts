import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AbstractControl, NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
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
import { OptionItem, StitchingSectionType } from '../../core/models/common.models';
import { StitchingSection, Style } from '../../core/models/master-data.models';
import {
  StitchingDashboardResponse,
  StitchingDelivery,
  StitchingOrder,
  StitchingOrderStatus
} from '../../core/models/stitching.models';
import { MasterDataApiService } from '../../core/services/master-data-api.service';
import { NotificationService } from '../../core/services/notification.service';
import { StitchingApiService } from '../../core/services/stitching-api.service';
import { extractWarningResponse, getApiErrorMessage } from '../../core/utils/api-error.utils';

@Component({
  selector: 'app-stitching-page',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    DatePipe,
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
  templateUrl: './stitching-page.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class StitchingPageComponent {
  private readonly stitchingApi = inject(StitchingApiService);
  private readonly masterDataApi = inject(MasterDataApiService);
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly notificationService = inject(NotificationService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly destroyRef = inject(DestroyRef);

  readonly dashboard = signal<StitchingDashboardResponse | null>(null);
  readonly styles = signal<Style[]>([]);
  readonly sections = signal<StitchingSection[]>([]);
  readonly orders = signal<StitchingOrder[]>([]);
  readonly deliveries = signal<StitchingDelivery[]>([]);
  readonly totalOrders = signal(0);
  readonly totalDeliveries = signal(0);
  readonly loadingOrders = signal(false);
  readonly loadingDeliveries = signal(false);
  readonly selectedOrders = signal<StitchingOrder[]>([]);
  readonly selectedDeliveries = signal<StitchingDelivery[]>([]);
  readonly activeTab = signal(0);

  readonly orderCreateVisible = signal(false);
  readonly deliveryCreateVisible = signal(false);
  readonly orderSubmitting = signal(false);
  readonly deliverySubmitting = signal(false);
  readonly orderError = signal<string | null>(null);
  readonly deliveryError = signal<string | null>(null);
  readonly createOrderAvailablePieces = signal<number | null>(null);
  readonly createDeliveryAvailablePieces = signal<number | null>(null);
  readonly createDeliveryStyleAutoId = signal<string | null>(null);

  readonly orderDetail = signal<StitchingOrder | null>(null);
  readonly deliveryDetail = signal<StitchingDelivery | null>(null);
  readonly orderDetailVisible = signal(false);
  readonly deliveryDetailVisible = signal(false);
  readonly orderStatusSubmitting = signal(false);
  readonly orderStatusError = signal<string | null>(null);

  readonly orderFilters = this.fb.group({
    styleAutoId: this.fb.control(''),
    sectionAutoId: this.fb.control(''),
    status: this.fb.control<'ALL' | StitchingOrderStatus>('ALL')
  });

  readonly deliveryFilters = this.fb.group({
    orderAutoId: this.fb.control(''),
    styleAutoId: this.fb.control(''),
    sectionAutoId: this.fb.control('')
  });

  readonly createOrderForm = this.fb.group({
    orderDate: this.fb.control<Date | null>(new Date(), [Validators.required]),
    stitchingSectionAutoId: this.fb.control('', [Validators.required]),
    styleAutoId: this.fb.control('', [Validators.required]),
    piecesOrdered: this.fb.control<number | null>(null, [Validators.required, Validators.min(1)]),
    notes: this.fb.control('')
  });

  readonly createDeliveryForm = this.fb.group({
    deliveryDate: this.fb.control<Date | null>(new Date(), [Validators.required]),
    stitchingOrderAutoId: this.fb.control('', [Validators.required]),
    piecesDelivered: this.fb.control<number | null>(null, [Validators.required, Validators.min(1)])
  });

  readonly updateStatusForm = this.fb.group({
    status: this.fb.control<StitchingOrderStatus>('PENDING', [Validators.required])
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

  readonly styleOptions = computed<OptionItem[]>(() =>
    this.styles().map((style) => ({
      label: `${style.autoId} - ${style.styleName}`,
      value: style.autoId
    }))
  );

  readonly sectionOptions = computed<OptionItem[]>(() =>
    this.sections().map((section) => ({
      label: `${section.autoId} - ${section.sectionName}`,
      value: section.autoId
    }))
  );

  readonly orderOptions = computed<OptionItem[]>(() =>
    this.orders().map((order) => ({
      label: `${order.autoId} - ${order.style.styleName}`,
      value: order.autoId
    }))
  );

  readonly orderStatusOptions: OptionItem<'ALL' | StitchingOrderStatus>[] = [
    { label: 'All status', value: 'ALL' },
    { label: 'Pending', value: 'PENDING' },
    { label: 'Partially delivered', value: 'PARTIALLY_DELIVERED' },
    { label: 'Complete', value: 'COMPLETE' }
  ];

  readonly updateStatusOptions: OptionItem<StitchingOrderStatus>[] = [
    { label: 'Pending', value: 'PENDING' },
    { label: 'Partially delivered', value: 'PARTIALLY_DELIVERED' },
    { label: 'Complete', value: 'COMPLETE' }
  ];

  readonly summaryCards = computed(() => {
    const dashboard = this.dashboard();
    if (!dashboard) {
      return [];
    }

    return [
      {
        title: 'Pieces In Stitching',
        value: `${dashboard.totalPiecesInStitching}`,
        note: 'Open pieces still moving through active stitching orders.',
        icon: 'pi pi-sitemap',
        tone: 'ocean'
      },
      {
        title: 'Delivered Pieces',
        value: `${dashboard.piecesDeliveredFromStitching}`,
        note: 'Pieces already returned from stitching deliveries.',
        icon: 'pi pi-send',
        tone: 'teal'
      },
      {
        title: 'Pending Orders',
        value: `${dashboard.pendingOrdersCount}`,
        note: 'Orders that have not yet started delivering back.',
        icon: 'pi pi-clock',
        tone: 'indigo'
      },
      {
        title: 'Defective Finalized',
        value: `${dashboard.defectivePiecesFinalized}`,
        note: 'Defective pieces derived from completed stitching orders.',
        icon: 'pi pi-exclamation-triangle',
        tone: 'amber'
      }
    ];
  });

  constructor() {
    this.bindFilters();
    this.bindAvailabilityLookups();
    this.loadInitialData();
  }

  loadInitialData(): void {
    this.loadingOrders.set(true);
    this.loadingDeliveries.set(true);
    forkJoin({
      dashboard: this.stitchingApi.getDashboard(),
      styles: this.masterDataApi.getStyleOptions(),
      sections: this.masterDataApi.getSectionOptions(),
      orders: this.loadOrdersRequest(),
      deliveries: this.loadDeliveriesRequest()
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ({ dashboard, styles, sections, orders, deliveries }) => {
          this.dashboard.set(dashboard);
          this.styles.set(styles);
          this.sections.set(sections);
          this.orders.set(orders.content);
          this.totalOrders.set(orders.totalElements);
          this.deliveries.set(deliveries.content);
          this.totalDeliveries.set(deliveries.totalElements);
          this.loadingOrders.set(false);
          this.loadingDeliveries.set(false);
        },
        error: (error) => {
          this.loadingOrders.set(false);
          this.loadingDeliveries.set(false);
          this.notificationService.error('Unable to load stitching workspace', getApiErrorMessage(error));
        }
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
    this.createOrderAvailablePieces.set(null);
    this.createOrderForm.reset({
      orderDate: new Date(),
      stitchingSectionAutoId: '',
      styleAutoId: '',
      piecesOrdered: null,
      notes: ''
    });
    this.orderCreateVisible.set(true);
  }

  openCreateDelivery(): void {
    this.deliveryError.set(null);
    this.createDeliveryAvailablePieces.set(null);
    this.createDeliveryStyleAutoId.set(null);
    this.createDeliveryForm.reset({
      deliveryDate: new Date(),
      stitchingOrderAutoId: '',
      piecesDelivered: null
    });
    this.deliveryCreateVisible.set(true);
  }

  openOrderDetail(record: StitchingOrder): void {
    this.orderDetail.set(record);
    this.orderStatusError.set(null);
    this.updateStatusForm.reset({ status: record.status });
    this.orderDetailVisible.set(true);
  }

  openDeliveryDetail(record: StitchingDelivery): void {
    this.deliveryDetail.set(record);
    this.deliveryDetailVisible.set(true);
  }

  createOrder(): void {
    this.orderError.set(null);
    if (this.createOrderForm.pristine) {
      this.notificationService.warn('No changes available to save', 'Update the stitching order form before saving.');
      return;
    }

    if (this.createOrderForm.invalid) {
      this.createOrderForm.markAllAsTouched();
      this.notificationService.warn('Stitching order not saved', 'Complete the required order fields before saving.');
      return;
    }

    const value = this.createOrderForm.getRawValue();
    if (!value.orderDate || !value.stitchingSectionAutoId || !value.styleAutoId || !value.piecesOrdered) {
      this.notificationService.warn('Stitching order not saved', 'Date, section, style, and ordered pieces are required.');
      return;
    }

    this.orderSubmitting.set(true);
    this.stitchingApi.createOrder({
      orderDate: this.toApiDate(value.orderDate),
      stitchingSectionAutoId: value.stitchingSectionAutoId,
      styleAutoId: value.styleAutoId,
      piecesOrdered: Number(value.piecesOrdered),
      notes: value.notes?.trim() || null
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.orderSubmitting.set(false);
          this.orderCreateVisible.set(false);
          this.notificationService.success('Stitching order created', 'The stitching order has been saved.');
          this.reloadDashboard();
          this.reloadOrders();
        },
        error: (error) => {
          this.orderSubmitting.set(false);
          const message = getApiErrorMessage(error);
          this.orderError.set(message);
          this.notificationService.error('Unable to create stitching order', message);
        }
      });
  }

  createDelivery(overrideWarnings = false): void {
    this.deliveryError.set(null);
    if (!overrideWarnings && this.createDeliveryForm.pristine) {
      this.notificationService.warn('No changes available to save', 'Update the stitching delivery form before saving.');
      return;
    }

    if (this.createDeliveryForm.invalid) {
      this.createDeliveryForm.markAllAsTouched();
      this.notificationService.warn('Stitching delivery not saved', 'Complete the required delivery fields before saving.');
      return;
    }

    const value = this.createDeliveryForm.getRawValue();
    if (!value.deliveryDate || !value.stitchingOrderAutoId || !value.piecesDelivered) {
      this.notificationService.warn('Stitching delivery not saved', 'Date, order, and delivered pieces are required.');
      return;
    }

    this.deliverySubmitting.set(true);
    this.stitchingApi.createDelivery({
      deliveryDate: this.toApiDate(value.deliveryDate),
      stitchingOrderAutoId: value.stitchingOrderAutoId,
      piecesDelivered: Number(value.piecesDelivered),
      overrideWarnings
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.deliverySubmitting.set(false);
          this.deliveryCreateVisible.set(false);
          this.notificationService.success('Stitching delivery created', 'The stitching delivery has been saved.');
          this.reloadDashboard();
          this.reloadOrders();
          this.reloadDeliveries();
        },
        error: (error) => {
          this.deliverySubmitting.set(false);
          const warning = extractWarningResponse(error);
          if (warning && !overrideWarnings) {
            this.confirmationService.confirm({
              header: 'Delivery exceeds order quantity',
              message: `${warning.message} Do you want to proceed anyway?`,
              acceptLabel: 'Proceed',
              rejectLabel: 'Cancel',
              accept: () => this.createDelivery(true)
            });
            return;
          }

          const message = getApiErrorMessage(error);
          this.deliveryError.set(message);
          this.notificationService.error('Unable to create stitching delivery', message);
        }
      });
  }

  saveOrderStatus(): void {
    const record = this.orderDetail();
    if (!record) {
      return;
    }

    if (this.updateStatusForm.pristine) {
      this.notificationService.warn('No changes available to save', 'Update the stitching order status before saving.');
      return;
    }

    this.orderStatusError.set(null);
    this.orderStatusSubmitting.set(true);
    this.stitchingApi.updateOrderStatus(record.autoId, {
      status: this.updateStatusForm.getRawValue().status
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (updated) => {
          this.orderStatusSubmitting.set(false);
          this.orderDetail.set(updated);
          this.updateStatusForm.reset({ status: updated.status });
          this.notificationService.success('Order status updated', 'The stitching order status has been updated.');
          this.reloadDashboard();
          this.reloadOrders();
        },
        error: (error) => {
          this.orderStatusSubmitting.set(false);
          const message = getApiErrorMessage(error);
          this.orderStatusError.set(message);
          this.notificationService.error('Unable to update order status', message);
        }
      });
  }

  confirmDeleteOrders(): void {
    const selected = this.selectedOrders();
    if (!selected.length) {
      return;
    }

    this.confirmationService.confirm({
      header: 'Delete stitching orders',
      message: `Soft delete ${selected.length} selected stitching order(s)?`,
      acceptLabel: 'Delete',
      rejectLabel: 'Cancel',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => {
        forkJoin(selected.map((record) => this.stitchingApi.deleteOrder(record.autoId)))
          .pipe(takeUntilDestroyed(this.destroyRef))
          .subscribe({
            next: () => {
              this.selectedOrders.set([]);
              this.notificationService.success('Stitching orders deleted', 'Selected stitching orders were moved out of the active list.');
              this.reloadDashboard();
              this.reloadOrders();
            },
            error: (error) => {
              this.notificationService.error('Unable to delete stitching orders', getApiErrorMessage(error));
            }
          });
      }
    });
  }

  confirmDeleteDeliveries(): void {
    const selected = this.selectedDeliveries();
    if (!selected.length) {
      return;
    }

    this.confirmationService.confirm({
      header: 'Delete stitching deliveries',
      message: `Soft delete ${selected.length} selected stitching delivery record(s)?`,
      acceptLabel: 'Delete',
      rejectLabel: 'Cancel',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => {
        forkJoin(selected.map((record) => this.stitchingApi.deleteDelivery(record.autoId)))
          .pipe(takeUntilDestroyed(this.destroyRef))
          .subscribe({
            next: () => {
              this.selectedDeliveries.set([]);
              this.notificationService.success('Stitching deliveries deleted', 'Selected stitching deliveries were removed from the active list.');
              this.reloadDashboard();
              this.reloadOrders();
              this.reloadDeliveries();
            },
            error: (error) => {
              this.notificationService.error('Unable to delete stitching deliveries', getApiErrorMessage(error));
            }
          });
      }
    });
  }

  orderStatusSeverity(status: StitchingOrderStatus): 'info' | 'warn' | 'success' {
    switch (status) {
      case 'COMPLETE':
        return 'success';
      case 'PARTIALLY_DELIVERED':
        return 'warn';
      default:
        return 'info';
    }
  }

  sectionTypeLabel(type: StitchingSectionType): string {
    return type === 'INTERNAL' ? 'Internal' : 'External';
  }

  closeOrderCreateDialog(): void {
    if (this.createOrderForm.dirty) {
      this.confirmationService.confirm({
        header: 'Discard stitching order changes',
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
        header: 'Discard stitching delivery changes',
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

  handleOrderDetailVisibilityChange(visible: boolean): void {
    if (visible) {
      this.orderDetailVisible.set(true);
      return;
    }

    if (this.updateStatusForm.dirty) {
      this.confirmationService.confirm({
        header: 'Discard status changes',
        message: 'You have unsaved status changes. Do you want to close this dialog?',
        acceptLabel: 'Discard',
        rejectLabel: 'Keep editing',
        acceptButtonStyleClass: 'p-button-danger',
        rejectButtonStyleClass: 'p-button-outlined p-button-secondary',
        accept: () => this.orderDetailVisible.set(false)
      });
      return;
    }

    this.orderDetailVisible.set(false);
  }

  isInvalid(control: AbstractControl | null): boolean {
    return !!control && control.invalid && (control.touched || control.dirty);
  }

  private bindFilters(): void {
    this.orderFilters.valueChanges
      .pipe(debounceTime(250), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.orderQuery.update((query) => ({ ...query, page: 0 }));
        this.reloadOrders();
      });

    this.deliveryFilters.valueChanges
      .pipe(debounceTime(250), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.deliveryQuery.update((query) => ({ ...query, page: 0 }));
        this.reloadDeliveries();
      });
  }

  private bindAvailabilityLookups(): void {
    this.createOrderForm.controls.styleAutoId.valueChanges
      .pipe(debounceTime(200), distinctUntilChanged(), takeUntilDestroyed(this.destroyRef))
      .subscribe((styleAutoId) => {
        if (!styleAutoId) {
          this.createOrderAvailablePieces.set(null);
          return;
        }

        this.stitchingApi.getAvailableOrderPieces(styleAutoId)
          .pipe(takeUntilDestroyed(this.destroyRef))
          .subscribe({
            next: (response) => this.createOrderAvailablePieces.set(response.availablePieces),
            error: (error) => {
              this.createOrderAvailablePieces.set(null);
              this.notificationService.error('Unable to load available pieces', getApiErrorMessage(error));
            }
          });
      });

    this.createDeliveryForm.controls.stitchingOrderAutoId.valueChanges
      .pipe(debounceTime(200), distinctUntilChanged(), takeUntilDestroyed(this.destroyRef))
      .subscribe((orderAutoId) => {
        if (!orderAutoId) {
          this.createDeliveryAvailablePieces.set(null);
          this.createDeliveryStyleAutoId.set(null);
          return;
        }

        this.stitchingApi.getAvailableDeliveryPieces(orderAutoId)
          .pipe(takeUntilDestroyed(this.destroyRef))
          .subscribe({
            next: (response) => {
              this.createDeliveryAvailablePieces.set(response.availablePieces);
              this.createDeliveryStyleAutoId.set(response.styleAutoId);
            },
            error: (error) => {
              this.createDeliveryAvailablePieces.set(null);
              this.createDeliveryStyleAutoId.set(null);
              this.notificationService.error('Unable to load available pieces', getApiErrorMessage(error));
            }
          });
      });
  }

  private reloadDashboard(): void {
    this.stitchingApi.getDashboard()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (dashboard) => this.dashboard.set(dashboard),
        error: (error) => this.notificationService.error('Unable to refresh stitching dashboard', getApiErrorMessage(error))
      });
  }

  private reloadOrders(): void {
    this.loadingOrders.set(true);
    this.loadOrdersRequest()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (page) => {
          this.orders.set(page.content);
          this.totalOrders.set(page.totalElements);
          this.loadingOrders.set(false);
        },
        error: (error) => {
          this.loadingOrders.set(false);
          this.notificationService.error('Unable to refresh stitching orders', getApiErrorMessage(error));
        }
      });
  }

  private reloadDeliveries(): void {
    this.loadingDeliveries.set(true);
    this.loadDeliveriesRequest()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (page) => {
          this.deliveries.set(page.content);
          this.totalDeliveries.set(page.totalElements);
          this.loadingDeliveries.set(false);
        },
        error: (error) => {
          this.loadingDeliveries.set(false);
          this.notificationService.error('Unable to refresh stitching deliveries', getApiErrorMessage(error));
        }
      });
  }

  private loadOrdersRequest() {
    return this.stitchingApi.getOrders({
      ...this.orderQuery(),
      ...this.buildOrderFilters()
    });
  }

  private loadDeliveriesRequest() {
    return this.stitchingApi.getDeliveries({
      ...this.deliveryQuery(),
      ...this.buildDeliveryFilters()
    });
  }

  private buildOrderFilters() {
    const value = this.orderFilters.getRawValue();
    return {
      styleAutoId: value.styleAutoId || undefined,
      sectionAutoId: value.sectionAutoId || undefined,
      status: value.status === 'ALL' ? undefined : value.status
    };
  }

  private buildDeliveryFilters() {
    const value = this.deliveryFilters.getRawValue();
    return {
      orderAutoId: value.orderAutoId || undefined,
      styleAutoId: value.styleAutoId || undefined,
      sectionAutoId: value.sectionAutoId || undefined
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

  private toApiDate(value: Date): string {
    return new Date(Date.UTC(value.getFullYear(), value.getMonth(), value.getDate())).toISOString().slice(0, 10);
  }
}
