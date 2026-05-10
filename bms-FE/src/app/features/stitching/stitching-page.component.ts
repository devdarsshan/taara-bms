import { DatePipe, CurrencyPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormArray, FormControl, FormGroup, NonNullableFormBuilder, ReactiveFormsModule, Validators, AbstractControl } from '@angular/forms';
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
import { GarmentSize, OptionItem, SectionProcessType } from '../../core/models/common.models';
import { StitchingSection, Style } from '../../core/models/master-data.models';
import {
  StitchingDashboardResponse,
  StitchingDelivery,
  StitchingOrder,
  StitchingOrderCreateRequest,
  StitchingOrderRowRequest,
  StitchingOrderStatus
} from '../../core/models/stitching.models';
import { MasterDataApiService } from '../../core/services/master-data-api.service';
import { NotificationService } from '../../core/services/notification.service';
import { StitchingApiService } from '../../core/services/stitching-api.service';
import { getApiErrorMessage } from '../../core/utils/api-error.utils';

type OrderRowForm = FormGroup<{
  styleAutoId: FormControl<string>;
  size: FormControl<GarmentSize>;
  availablePieces: FormControl<number | null>;
  piecesTaken: FormControl<number | null>;
  ratePerPiece: FormControl<number | null>;
}>;

@Component({
  selector: 'app-stitching-page',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    CurrencyPipe,
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
  readonly loading = signal(true);

  readonly orderDialogVisible = signal(false);
  readonly deliveryDialogVisible = signal(false);
  readonly orderDetailVisible = signal(false);
  readonly orderDetail = signal<StitchingOrder | null>(null);
  readonly deliveryDetailVisible = signal(false);
  readonly deliveryDetail = signal<StitchingDelivery | null>(null);
  readonly deliveryAvailability = signal<number | null>(null);
  readonly selectedOrders = signal<StitchingOrder[]>([]);
  readonly selectedDeliveries = signal<StitchingDelivery[]>([]);
  readonly editingOrder = signal<StitchingOrder | null>(null);

  readonly orderFilters = this.fb.group({
    styleAutoId: this.fb.control(''),
    fromDate: this.fb.control<Date | null>(null),
    toDate: this.fb.control<Date | null>(null)
  });

  readonly deliveryFilters = this.fb.group({
    styleAutoId: this.fb.control(''),
    fromDate: this.fb.control<Date | null>(null),
    toDate: this.fb.control<Date | null>(null)
  });

  readonly orderForm = this.fb.group({
    orderDate: this.fb.control<Date | null>(new Date(), Validators.required),
    stitchingSectionAutoId: this.fb.control('', Validators.required),
    notes: this.fb.control(''),
    rows: this.fb.array<OrderRowForm>([])
  });

  readonly deliveryForm = this.fb.group({
    deliveryDate: this.fb.control<Date | null>(new Date(), Validators.required),
    stitchingSectionAutoId: this.fb.control('', Validators.required),
    styleAutoId: this.fb.control('', Validators.required),
    size: this.fb.control<GarmentSize>('M', Validators.required),
    availablePieces: this.fb.control<number | null>({ value: null, disabled: true }),
    piecesDelivered: this.fb.control<number | null>(null, [Validators.required, Validators.min(1)])
  });

  readonly statusForm = this.fb.group({
    status: this.fb.control<StitchingOrderStatus>('PENDING', Validators.required)
  });

  readonly sizeOptions: OptionItem<GarmentSize>[] = [
    { label: 'XS', value: 'XS' },
    { label: 'S', value: 'S' },
    { label: 'M', value: 'M' },
    { label: 'L', value: 'L' },
    { label: 'XL', value: 'XL' },
    { label: '2XL', value: '2XL' }
  ];

  readonly styleOptions = computed<OptionItem[]>(() => this.styles().map((style) => ({ label: style.styleName, value: style.autoId })));
  readonly sectionOptions = computed<OptionItem[]>(() =>
    this.sections()
      .filter((section) => section.processType === 'STITCHING')
      .map((section) => ({ label: section.sectionName, value: section.autoId }))
  );
  readonly statusOptions: OptionItem<StitchingOrderStatus>[] = [
    { label: 'Pending', value: 'PENDING' },
    { label: 'Partially Delivered', value: 'PARTIALLY_DELIVERED' },
    { label: 'Complete', value: 'COMPLETE' },
    { label: 'Auto Closed', value: 'AUTO_CLOSED' }
  ];

  readonly summaryCards = computed(() => {
    const dashboard = this.dashboard();
    if (!dashboard) {
      return [];
    }
    return [
      { title: 'Pieces In Stitching', value: `${dashboard.totalPiecesInStitching}`, note: 'Pieces currently issued to sections.', icon: 'pi pi-sitemap', tone: 'ocean' },
      { title: 'Delivered', value: `${dashboard.piecesDeliveredFromStitching}`, note: 'Pieces returned from stitching.', icon: 'pi pi-send', tone: 'teal' },
      { title: 'Pending Orders', value: `${dashboard.pendingOrdersCount}`, note: 'Open stitching order groups.', icon: 'pi pi-clock', tone: 'indigo' },
      { title: 'Defective', value: `${dashboard.defectivePiecesFinalized}`, note: 'Finalized defects from stitching.', icon: 'pi pi-exclamation-triangle', tone: 'amber' }
    ];
  });

  get orderRows(): FormArray<OrderRowForm> {
    return this.orderForm.controls.rows;
  }

  private readonly orderFormValue = toSignal(this.orderForm.valueChanges);

  readonly totalTaken = computed(() => {
    this.orderFormValue();
    return this.orderRows.controls.reduce((sum, row) => sum + Number(row.controls.piecesTaken.getRawValue() ?? 0), 0);
  });

  readonly totalCost = computed(() => {
    this.orderFormValue();
    return this.orderRows.controls.reduce((sum, row) => {
      const pcs = Number(row.controls.piecesTaken.getRawValue() ?? 0);
      const rate = Number(row.controls.ratePerPiece.getRawValue() ?? 0);
      return sum + (pcs * rate);
    }, 0);
  });

  constructor() {
    this.loadData();
  }

  loadData(): void {
    this.loading.set(true);
    forkJoin({
      dashboard: this.stitchingApi.getDashboard(),
      orders: this.stitchingApi.getOrders({ page: 0, size: 100, sortField: 'orderDate', sortDirection: 'desc', includeDeleted: false }),
      deliveries: this.stitchingApi.getDeliveries({ page: 0, size: 100, sortField: 'deliveryDate', sortDirection: 'desc', includeDeleted: false }),
      styles: this.masterDataApi.getStyleOptions(),
      sections: this.masterDataApi.getSectionOptions('STITCHING' as SectionProcessType)
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
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
          this.notificationService.error('Unable to load stitching module', getApiErrorMessage(error));
        }
      });
  }

  openCreateOrder(): void {
    this.editingOrder.set(null);
    this.orderDialogVisible.set(true);
    this.orderForm.reset({ orderDate: new Date(), stitchingSectionAutoId: '', notes: '' });
    this.orderRows.clear();
    this.addOrderRow();
  }

  openEditOrder(record: StitchingOrder): void {
    this.editingOrder.set(record);
    this.orderDialogVisible.set(true);
    this.orderForm.reset({
      orderDate: new Date(record.orderDate),
      stitchingSectionAutoId: record.rows[0]?.stitchingSection?.autoId || '',
      notes: record.notes ?? ''
    });
    this.orderRows.clear();
    for (const row of record.rows) {
      const group = this.fb.group({
        styleAutoId: this.fb.control(row.style.autoId, Validators.required),
        size: this.fb.control<GarmentSize>(row.size, Validators.required),
        availablePieces: this.fb.control<number | null>({ value: null, disabled: true }),
        piecesTaken: this.fb.control<number | null>(row.piecesTaken, [Validators.required, Validators.min(1)]),
        ratePerPiece: this.fb.control<number | null>(row.ratePerPiece ?? null, [Validators.min(0)])
      });
      this.orderRows.push(group as OrderRowForm);
      
      this.stitchingApi.getAvailableOrderPieces(row.style.autoId, row.size)
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({
          next: (response) => group.controls.availablePieces.setValue(response.availablePieces),
          error: () => group.controls.availablePieces.setValue(null)
        });
    }
  }

  addOrderRow(): void {
    this.orderRows.push(this.fb.group({
      styleAutoId: this.fb.control('', Validators.required),
      size: this.fb.control<GarmentSize>('M', Validators.required),
      availablePieces: this.fb.control<number | null>({ value: null, disabled: true }),
      piecesTaken: this.fb.control<number | null>(null, [Validators.required, Validators.min(1)]),
      ratePerPiece: this.fb.control<number | null>(null, [Validators.min(0)])
    }) as OrderRowForm);
  }

  removeOrderRow(index: number): void {
    if (this.orderRows.length > 1) {
      this.orderRows.removeAt(index);
    }
  }

  onOrderRowComboChange(index: number): void {
    const row = this.orderRows.at(index);
    const styleAutoId = row.controls.styleAutoId.getRawValue();
    const size = row.controls.size.getRawValue();
    if (!styleAutoId || !size) {
      row.controls.availablePieces.setValue(null);
      return;
    }
    this.stitchingApi.getAvailableOrderPieces(styleAutoId, size)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (response) => row.controls.availablePieces.setValue(response.availablePieces),
        error: () => row.controls.availablePieces.setValue(null)
      });
  }

  saveOrder(): void {
    if (this.orderForm.invalid) {
      this.orderForm.markAllAsTouched();
      return;
    }
    const value = this.orderForm.getRawValue();
    const payload: StitchingOrderCreateRequest = {
      orderDate: this.toApiDate(value.orderDate ?? new Date()),
      expectedSize: value.rows[0]?.size || 'M',
      expectedPieces: this.totalTaken(),
      notes: value.notes?.trim() || null,
      rows: value.rows.map((row) => ({
        stitchingSectionAutoId: value.stitchingSectionAutoId,
        styleAutoId: row.styleAutoId,
        size: row.size,
        piecesTaken: Number(row.piecesTaken ?? 0),
        ratePerPiece: row.ratePerPiece ? Number(row.ratePerPiece) : null
      } satisfies StitchingOrderRowRequest))
    };
    
    const editingOrder = this.editingOrder();
    const request$ = editingOrder 
      ? this.stitchingApi.updateOrder(editingOrder.autoId, payload)
      : this.stitchingApi.createOrder(payload);

    request$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.notificationService.success('Stitching order saved', 'The stitching order was saved successfully.');
          this.orderDialogVisible.set(false);
          this.loadData();
        },
        error: (error) => this.notificationService.error('Unable to save stitching order', getApiErrorMessage(error))
      });
  }

  openCreateDelivery(): void {
    this.deliveryDialogVisible.set(true);
    this.deliveryAvailability.set(null);
    this.deliveryForm.reset({ deliveryDate: new Date(), stitchingSectionAutoId: '', styleAutoId: '', size: 'M', availablePieces: null, piecesDelivered: null });
  }

  refreshDeliveryAvailability(): void {
    const value = this.deliveryForm.getRawValue();
    if (!value.stitchingSectionAutoId || !value.styleAutoId || !value.size) {
      this.deliveryForm.controls.availablePieces.setValue(null);
      return;
    }
    this.stitchingApi.getAvailableDeliveryPieces(value.stitchingSectionAutoId, value.styleAutoId, value.size)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (response) => this.deliveryForm.controls.availablePieces.setValue(response.availablePieces),
        error: () => this.deliveryForm.controls.availablePieces.setValue(null)
      });
  }

  saveDelivery(): void {
    if (this.deliveryForm.invalid) {
      this.deliveryForm.markAllAsTouched();
      return;
    }
    const value = this.deliveryForm.getRawValue();
    this.stitchingApi.createDelivery({
      deliveryDate: this.toApiDate(value.deliveryDate ?? new Date()),
      stitchingSectionAutoId: value.stitchingSectionAutoId,
      styleAutoId: value.styleAutoId,
      size: value.size,
      piecesDelivered: Number(value.piecesDelivered ?? 0)
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.notificationService.success('Stitching delivery saved', 'The stitching delivery was saved successfully.');
          this.deliveryDialogVisible.set(false);
          this.loadData();
        },
        error: (error) => this.notificationService.error('Unable to save stitching delivery', getApiErrorMessage(error))
      });
  }

  openOrderDetail(record: StitchingOrder): void {
    this.orderDetail.set(record);
    this.statusForm.reset({ status: record.status });
    this.orderDetailVisible.set(true);
  }

  openDeliveryDetail(record: StitchingDelivery): void {
    this.deliveryDetail.set(record);
    this.deliveryDetailVisible.set(true);
  }

  saveOrderStatus(): void {
    const record = this.orderDetail();
    if (!record) {
      return;
    }
    this.stitchingApi.updateOrderStatus(record.autoId, { status: this.statusForm.getRawValue().status })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.notificationService.success('Status updated', 'The stitching order status was updated.');
          this.orderDetailVisible.set(false);
          this.loadData();
        },
        error: (error) => this.notificationService.error('Unable to update status', getApiErrorMessage(error))
      });
  }

  confirmDeleteOrder(record: StitchingOrder): void {
    this.confirmationService.confirm({
      header: 'Delete stitching order',
      message: `Delete ${record.autoId}?`,
      acceptButtonStyleClass: 'p-button-danger',
      rejectButtonStyleClass: 'p-button-outlined p-button-secondary',
      accept: () => {
        this.stitchingApi.deleteOrder(record.autoId)
          .pipe(takeUntilDestroyed(this.destroyRef))
          .subscribe({
            next: () => {
              this.notificationService.success('Order deleted', 'The order was removed.');
              this.loadData();
            },
            error: (error) => this.notificationService.error('Unable to delete order', getApiErrorMessage(error))
          });
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
      message: `Soft delete ${selected.length} selected order(s)?`,
      acceptLabel: 'Delete',
      rejectLabel: 'Cancel',
      acceptButtonStyleClass: 'p-button-danger',
      rejectButtonStyleClass: 'p-button-outlined p-button-secondary',
      accept: () => {
        forkJoin(selected.map((record) => this.stitchingApi.deleteOrder(record.autoId)))
          .pipe(takeUntilDestroyed(this.destroyRef))
          .subscribe({
            next: () => {
              this.selectedOrders.set([]);
              this.notificationService.success('Orders deleted', 'Selected stitching orders were removed from the active list.');
              this.loadData();
            },
            error: (error) => this.notificationService.error('Unable to delete stitching orders', getApiErrorMessage(error))
          });
      }
    });
  }

  confirmDeleteDelivery(record: StitchingDelivery): void {
    this.confirmationService.confirm({
      header: 'Delete stitching delivery',
      message: `Delete ${record.autoId}?`,
      acceptButtonStyleClass: 'p-button-danger',
      rejectButtonStyleClass: 'p-button-outlined p-button-secondary',
      accept: () => {
        this.stitchingApi.deleteDelivery(record.autoId)
          .pipe(takeUntilDestroyed(this.destroyRef))
          .subscribe({
            next: () => {
              this.notificationService.success('Delivery deleted', 'The delivery was removed.');
              this.loadData();
            },
            error: (error) => this.notificationService.error('Unable to delete delivery', getApiErrorMessage(error))
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
      message: `Soft delete ${selected.length} selected delivery record(s)?`,
      acceptLabel: 'Delete',
      rejectLabel: 'Cancel',
      acceptButtonStyleClass: 'p-button-danger',
      rejectButtonStyleClass: 'p-button-outlined p-button-secondary',
      accept: () => {
        forkJoin(selected.map((record) => this.stitchingApi.deleteDelivery(record.autoId)))
          .pipe(takeUntilDestroyed(this.destroyRef))
          .subscribe({
            next: () => {
              this.selectedDeliveries.set([]);
              this.notificationService.success('Deliveries deleted', 'Selected stitching deliveries were removed from the active list.');
              this.loadData();
            },
            error: (error) => this.notificationService.error('Unable to delete stitching deliveries', getApiErrorMessage(error))
          });
      }
    });
  }

  severity(status: StitchingOrderStatus): 'info' | 'warn' | 'success' {
    if (status === 'AUTO_CLOSED' || status === 'COMPLETE') {
      return 'success';
    }
    if (status === 'PARTIALLY_DELIVERED') {
      return 'warn';
    }
    return 'info';
  }

  isInvalid(control: AbstractControl | null): boolean {
    return !!control && control.invalid && (control.touched || control.dirty);
  }

  private toApiDate(value: Date): string {
    return new Date(Date.UTC(value.getFullYear(), value.getMonth(), value.getDate())).toISOString().slice(0, 10);
  }
}