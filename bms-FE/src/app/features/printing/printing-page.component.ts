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
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { SelectModule } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { TabViewModule } from 'primeng/tabview';
import { TagModule } from 'primeng/tag';
import { TextareaModule } from 'primeng/textarea';
import { GarmentSize, OptionItem } from '../../core/models/common.models';
import { StitchingSection, Style } from '../../core/models/master-data.models';
import { PrintingDashboardResponse, PrintingDelivery, PrintingOrder } from '../../core/models/printing.models';
import { StitchingOrderStatus } from '../../core/models/stitching.models';
import { MasterDataApiService } from '../../core/services/master-data-api.service';
import { NotificationService } from '../../core/services/notification.service';
import { PrintingApiService } from '../../core/services/printing-api.service';
import { getApiErrorMessage } from '../../core/utils/api-error.utils';

@Component({
  selector: 'app-printing-page',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    DatePipe,
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
  templateUrl: './printing-page.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PrintingPageComponent {
  private readonly printingApi = inject(PrintingApiService);
  private readonly masterDataApi = inject(MasterDataApiService);
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly notificationService = inject(NotificationService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly destroyRef = inject(DestroyRef);

  readonly dashboard = signal<PrintingDashboardResponse | null>(null);
  readonly orders = signal<PrintingOrder[]>([]);
  readonly deliveries = signal<PrintingDelivery[]>([]);
  readonly styles = signal<Style[]>([]);
  readonly sections = signal<StitchingSection[]>([]);
  readonly loading = signal(true);

  readonly orderDialogVisible = signal(false);
  readonly deliveryDialogVisible = signal(false);
  readonly orderDetailVisible = signal(false);
  readonly deliveryDetailVisible = signal(false);
  readonly orderDetail = signal<PrintingOrder | null>(null);
  readonly deliveryDetail = signal<PrintingDelivery | null>(null);
  readonly orderAvailability = signal<number | null>(null);
  readonly deliveryAvailability = signal<number | null>(null);
  readonly selectedOrders = signal<PrintingOrder[]>([]);
  readonly selectedDeliveries = signal<PrintingDelivery[]>([]);

  readonly orderForm = this.fb.group({
    orderDate: this.fb.control<Date | null>(new Date(), Validators.required),
    printingSectionAutoId: this.fb.control('', Validators.required),
    styleAutoId: this.fb.control('', Validators.required),
    size: this.fb.control<GarmentSize>('M', Validators.required),
    availablePieces: this.fb.control<number | null>({ value: null, disabled: true }),
    piecesOrdered: this.fb.control<number | null>(null, [Validators.required, Validators.min(1)]),
    notes: this.fb.control('')
  });

  readonly deliveryForm = this.fb.group({
    deliveryDate: this.fb.control<Date | null>(new Date(), Validators.required),
    printingOrderAutoId: this.fb.control('', Validators.required),
    availablePieces: this.fb.control<number | null>({ value: null, disabled: true }),
    piecesDelivered: this.fb.control<number | null>(null, [Validators.required, Validators.min(1)])
  });

  readonly sizeOptions: OptionItem<GarmentSize>[] = [
    { label: 'XS', value: 'XS' }, { label: 'S', value: 'S' }, { label: 'M', value: 'M' }, { label: 'L', value: 'L' }, { label: 'XL', value: 'XL' }, { label: '2XL', value: '2XL' }
  ];
  readonly styleOptions = computed<OptionItem[]>(() => this.styles().map((style) => ({ label: style.styleName, value: style.autoId })));
  readonly sectionOptions = computed<OptionItem[]>(() => this.sections().map((section) => ({ label: section.sectionName, value: section.autoId })));
  readonly orderOptions = computed<OptionItem[]>(() => this.orders().map((order) => ({ label: `${order.autoId} - ${order.style.styleName} ${order.size}`, value: order.autoId })));

  readonly summaryCards = computed(() => {
    const dashboard = this.dashboard();
    if (!dashboard) {
      return [];
    }
    return [
      { title: 'Pieces In Printing', value: `${dashboard.totalPiecesInPrinting}`, note: 'Plain stitched stock issued to printing.', icon: 'pi pi-palette', tone: 'ocean' },
      { title: 'Delivered', value: `${dashboard.deliveredPiecesFromPrinting}`, note: 'Printed pieces returned from sections.', icon: 'pi pi-send', tone: 'teal' },
      { title: 'Pending Orders', value: `${dashboard.pendingOrdersCount}`, note: 'Open printing orders still pending.', icon: 'pi pi-clock', tone: 'indigo' },
      { title: 'Defective', value: `${dashboard.defectivePiecesFinalized}`, note: 'Finalized printing defects.', icon: 'pi pi-exclamation-triangle', tone: 'amber' }
    ];
  });

  constructor() {
    this.loadData();
  }

  loadData(): void {
    this.loading.set(true);
    forkJoin({
      dashboard: this.printingApi.getDashboard(),
      orders: this.printingApi.getOrders({ page: 0, size: 100, sortField: 'orderDate', sortDirection: 'desc', includeDeleted: false }),
      deliveries: this.printingApi.getDeliveries({ page: 0, size: 100, sortField: 'deliveryDate', sortDirection: 'desc', includeDeleted: false }),
      styles: this.masterDataApi.getStyleOptions(),
      sections: this.masterDataApi.getSectionOptions('PRINTING')
    }).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: ({ dashboard, orders, deliveries, styles, sections }) => {
        this.dashboard.set(dashboard);
        this.orders.set(orders.content);
        this.deliveries.set(deliveries.content);
        this.styles.set(styles);
        this.sections.set(sections);
        this.loading.set(false);
      },
      error: (error) => {
        this.loading.set(false);
        this.notificationService.error('Unable to load printing module', getApiErrorMessage(error));
      }
    });
  }

  openCreateOrder(): void {
    this.orderDialogVisible.set(true);
    this.orderForm.reset({ orderDate: new Date(), printingSectionAutoId: '', styleAutoId: '', size: 'M', availablePieces: null, piecesOrdered: null, notes: '' });
  }

  refreshOrderAvailability(): void {
    const value = this.orderForm.getRawValue();
    if (!value.styleAutoId || !value.size) {
      this.orderForm.controls.availablePieces.setValue(null);
      return;
    }
    this.printingApi.getAvailableOrderPieces(value.styleAutoId, value.size)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({ next: (response) => this.orderForm.controls.availablePieces.setValue(response.availablePieces) });
  }

  saveOrder(): void {
    if (this.orderForm.invalid) {
      this.orderForm.markAllAsTouched();
      return;
    }
    const value = this.orderForm.getRawValue();
    this.printingApi.createOrder({
      orderDate: this.toApiDate(value.orderDate ?? new Date()),
      printingSectionAutoId: value.printingSectionAutoId,
      styleAutoId: value.styleAutoId,
      size: value.size,
      piecesOrdered: Number(value.piecesOrdered ?? 0),
      notes: value.notes?.trim() || null
    }).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.notificationService.success('Printing order saved', 'The printing order was saved.');
        this.orderDialogVisible.set(false);
        this.loadData();
      },
      error: (error) => this.notificationService.error('Unable to save printing order', getApiErrorMessage(error))
    });
  }

  openCreateDelivery(): void {
    this.deliveryDialogVisible.set(true);
    this.deliveryForm.reset({ deliveryDate: new Date(), printingOrderAutoId: '', availablePieces: null, piecesDelivered: null });
  }

  openOrderDetail(record: PrintingOrder): void {
    this.orderDetail.set(record);
    this.orderDetailVisible.set(true);
  }

  openDeliveryDetail(record: PrintingDelivery): void {
    this.deliveryDetail.set(record);
    this.deliveryDetailVisible.set(true);
  }

  refreshDeliveryAvailability(): void {
    const orderAutoId = this.deliveryForm.controls.printingOrderAutoId.getRawValue();
    if (!orderAutoId) {
      this.deliveryForm.controls.availablePieces.setValue(null);
      return;
    }
    this.printingApi.getAvailableDeliveryPieces(orderAutoId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({ next: (response) => this.deliveryForm.controls.availablePieces.setValue(response.availablePieces) });
  }

  saveDelivery(): void {
    if (this.deliveryForm.invalid) {
      this.deliveryForm.markAllAsTouched();
      return;
    }
    const value = this.deliveryForm.getRawValue();
    this.printingApi.createDelivery({
      deliveryDate: this.toApiDate(value.deliveryDate ?? new Date()),
      printingOrderAutoId: value.printingOrderAutoId,
      piecesDelivered: Number(value.piecesDelivered ?? 0)
    }).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.notificationService.success('Printing delivery saved', 'The printing delivery was saved.');
        this.deliveryDialogVisible.set(false);
        this.loadData();
      },
      error: (error) => this.notificationService.error('Unable to save printing delivery', getApiErrorMessage(error))
    });
  }

  confirmDeleteOrder(record: PrintingOrder): void {
    this.confirmationService.confirm({
      header: 'Delete printing order',
      message: `Delete ${record.autoId}?`,
      acceptButtonStyleClass: 'p-button-danger',
      rejectButtonStyleClass: 'p-button-outlined p-button-secondary',
      accept: () => this.printingApi.deleteOrder(record.autoId).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({ next: () => this.loadData() })
    });
  }

  confirmDeleteOrders(): void {
    const selected = this.selectedOrders();
    if (!selected.length) {
      return;
    }
    this.confirmationService.confirm({
      header: 'Delete printing orders',
      message: `Soft delete ${selected.length} selected order(s)?`,
      acceptLabel: 'Delete',
      rejectLabel: 'Cancel',
      acceptButtonStyleClass: 'p-button-danger',
      rejectButtonStyleClass: 'p-button-outlined p-button-secondary',
      accept: () => forkJoin(selected.map((record) => this.printingApi.deleteOrder(record.autoId)))
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({
          next: () => {
            this.selectedOrders.set([]);
            this.notificationService.success('Orders deleted', 'Selected printing orders were removed from the active list.');
            this.loadData();
          },
          error: (error) => this.notificationService.error('Unable to delete printing orders', getApiErrorMessage(error))
        })
    });
  }

  confirmDeleteDelivery(record: PrintingDelivery): void {
    this.confirmationService.confirm({
      header: 'Delete printing delivery',
      message: `Delete ${record.autoId}?`,
      acceptButtonStyleClass: 'p-button-danger',
      rejectButtonStyleClass: 'p-button-outlined p-button-secondary',
      accept: () => this.printingApi.deleteDelivery(record.autoId).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({ next: () => this.loadData() })
    });
  }

  confirmDeleteDeliveries(): void {
    const selected = this.selectedDeliveries();
    if (!selected.length) {
      return;
    }
    this.confirmationService.confirm({
      header: 'Delete printing deliveries',
      message: `Soft delete ${selected.length} selected delivery record(s)?`,
      acceptLabel: 'Delete',
      rejectLabel: 'Cancel',
      acceptButtonStyleClass: 'p-button-danger',
      rejectButtonStyleClass: 'p-button-outlined p-button-secondary',
      accept: () => forkJoin(selected.map((record) => this.printingApi.deleteDelivery(record.autoId)))
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({
          next: () => {
            this.selectedDeliveries.set([]);
            this.notificationService.success('Deliveries deleted', 'Selected printing deliveries were removed from the active list.');
            this.loadData();
          },
          error: (error) => this.notificationService.error('Unable to delete printing deliveries', getApiErrorMessage(error))
        })
    });
  }

  severity(status: StitchingOrderStatus): 'info' | 'warn' | 'success' {
    if (status === 'AUTO_CLOSED' || status === 'COMPLETE') return 'success';
    if (status === 'PARTIALLY_DELIVERED') return 'warn';
    return 'info';
  }

  private toApiDate(value: Date): string {
    return new Date(Date.UTC(value.getFullYear(), value.getMonth(), value.getDate())).toISOString().slice(0, 10);
  }
}
