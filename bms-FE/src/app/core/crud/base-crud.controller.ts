import { DestroyRef, WritableSignal, computed, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AbstractControl, FormArray, FormControl, FormGroup, NonNullableFormBuilder } from '@angular/forms';
import { Observable, debounceTime, distinctUntilChanged, forkJoin } from 'rxjs';
import { ConfirmationService } from 'primeng/api';
import { PageResponse, QueryOptions } from '../models/api.models';
import { NotificationService } from '../services/notification.service';
import { extractApiError, getApiErrorMessage } from '../utils/api-error.utils';

export type LazyLoadEventLike = {
  first?: number | null;
  rows?: number | null;
  sortField?: string | string[] | null;
  sortOrder?: number | null;
};

export type CrudListState<T> = {
  items: WritableSignal<T[]>;
  total: WritableSignal<number>;
  loading: WritableSignal<boolean>;
  query: WritableSignal<QueryOptions>;
  searchControl: FormControl<string>;
  includeDeletedControl: FormControl<boolean>;
};

export type CrudControllerDeps = {
  fb: NonNullableFormBuilder;
  notificationService: NotificationService;
  confirmationService: ConfirmationService;
  destroyRef: DestroyRef;
};

export abstract class BaseCrudController<TEntity, TPayload, TForm extends AbstractControl> {
  readonly listState: CrudListState<TEntity>;
  readonly selectedItems = signal<TEntity[]>([]);
  readonly dialogVisible = signal(false);
  readonly dialogError = signal<string | null>(null);
  readonly submitting = signal(false);
  readonly submitted = signal(false);
  readonly editingAutoId = signal<string | null>(null);
  readonly dialogTitle;
  readonly form: TForm;

  protected constructor(
    protected readonly deps: CrudControllerDeps,
    private readonly config: {
      entityLabel: string;
      collectionLabel: string;
      defaultSortField: string;
    },
    private readonly afterMutation?: () => void
  ) {
    this.listState = this.createListState<TEntity>(this.config.defaultSortField);
    this.dialogTitle = computed(() => `${this.editingAutoId() ? 'Edit' : 'Create'} ${this.config.entityLabel}`);
    this.form = this.buildForm();
  }

  protected initialize(): void {
    this.bindListFilters();
    this.loadList();
  }

  protected abstract buildForm(): TForm;
  protected abstract fillForm(entity: TEntity): void;
  protected abstract resetForm(): void;
  protected abstract buildPayload(): TPayload | null;
  protected abstract listRequest(query: QueryOptions): Observable<PageResponse<TEntity>>;
  protected abstract createRequest(payload: TPayload): Observable<TEntity>;
  protected abstract updateRequest(autoId: string, payload: TPayload): Observable<TEntity>;
  protected abstract deleteRequest(autoId: string): Observable<void>;
  protected abstract getEntityAutoId(entity: TEntity): string;
  protected abstract getDeletePrompt(entity: TEntity): string;
  protected abstract getCreateSuccessDetail(payload: TPayload): string;
  protected abstract getUpdateSuccessDetail(payload: TPayload): string;
  protected abstract getDeleteSuccessDetail(entity: TEntity): string;

  openDialog(entity?: TEntity): void {
    this.dialogError.set(null);
    this.submitted.set(false);
    this.clearServerErrors(this.form);

    if (entity) {
      this.editingAutoId.set(this.getEntityAutoId(entity));
      this.fillForm(entity);
    } else {
      this.editingAutoId.set(null);
      this.resetForm();
    }

    this.form.markAsPristine();
    this.form.markAsUntouched();
    this.dialogVisible.set(true);
  }

  save(): void {
    this.submitted.set(true);
    this.dialogError.set(null);
    this.clearServerErrors(this.form);
    this.form.updateValueAndValidity();

    if (this.form.pristine) {
      this.deps.notificationService.warn(
        'No changes available to save',
        `Update the ${this.config.entityLabel.toLowerCase()} form before saving.`
      );
      return;
    }

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.deps.notificationService.warn(
        `${this.config.entityLabel} not saved`,
        'Complete the required fields before saving.'
      );
      return;
    }

    const payload = this.buildPayload();

    if (payload === null) {
      this.deps.notificationService.warn(
        `${this.config.entityLabel} not saved`,
        this.dialogError() ?? 'Please review the form and try again.'
      );
      return;
    }

    const editingAutoId = this.editingAutoId();
    const isEditing = !!editingAutoId;
    const summary = `${this.config.entityLabel} ${isEditing ? 'updated' : 'created'}`;
    const detail = isEditing ? this.getUpdateSuccessDetail(payload) : this.getCreateSuccessDetail(payload);
    const request = isEditing ? this.updateRequest(editingAutoId as string, payload) : this.createRequest(payload);

    this.submitting.set(true);
    request.pipe(takeUntilDestroyed(this.deps.destroyRef)).subscribe({
      next: () => {
        this.submitting.set(false);
        this.dialogVisible.set(false);
        this.afterMutation?.();
        this.loadList();
        this.deps.notificationService.success(summary, detail);
      },
      error: (error) => {
        this.submitting.set(false);
        this.applyServerErrors(error);
        this.deps.notificationService.error(
          `Unable to save ${this.config.entityLabel.toLowerCase()}`,
          getApiErrorMessage(error)
        );
      }
    });
  }

  confirmDelete(entity: TEntity): void {
    this.deps.confirmationService.confirm({
      header: `Delete ${this.config.entityLabel.toLowerCase()}`,
      message: this.getDeletePrompt(entity),
      acceptLabel: 'Delete',
      rejectLabel: 'Cancel',
      acceptButtonStyleClass: 'p-button-danger p-button-sm',
      accept: () => {
        this.deleteRequest(this.getEntityAutoId(entity))
          .pipe(takeUntilDestroyed(this.deps.destroyRef))
          .subscribe({
            next: () => {
              this.afterMutation?.();
              this.loadList();
              this.deps.notificationService.success(
                `${this.config.entityLabel} deleted`,
                this.getDeleteSuccessDetail(entity)
              );
            },
            error: (error) => {
              this.deps.notificationService.error(
                `Unable to delete ${this.config.entityLabel.toLowerCase()}`,
                getApiErrorMessage(error)
              );
            }
          });
      }
    });
  }

  confirmDeleteMany(entities = this.selectedItems()): void {
    if (!entities.length) {
      return;
    }

    this.deps.confirmationService.confirm({
      header: `Delete ${this.config.collectionLabel.toLowerCase()}`,
      message: `Soft delete ${entities.length} selected ${this.config.entityLabel.toLowerCase()} record(s)?`,
      acceptLabel: 'Delete',
      rejectLabel: 'Cancel',
      acceptButtonStyleClass: 'p-button-danger p-button-sm',
      accept: () => {
        forkJoin(entities.map((entity) => this.deleteRequest(this.getEntityAutoId(entity))))
          .pipe(takeUntilDestroyed(this.deps.destroyRef))
          .subscribe({
            next: () => {
              this.selectedItems.set([]);
              this.afterMutation?.();
              this.loadList();
              this.deps.notificationService.success(
                `${this.config.collectionLabel} deleted`,
                `${entities.length} record(s) were moved out of the active list.`
              );
            },
            error: (error) => {
              this.deps.notificationService.error(
                `Unable to delete ${this.config.collectionLabel.toLowerCase()}`,
                getApiErrorMessage(error)
              );
            }
          });
      }
    });
  }

  attemptCloseDialog(): void {
    if (this.form.dirty) {
      this.deps.confirmationService.confirm({
        header: `Discard ${this.config.entityLabel.toLowerCase()} changes`,
        message: 'You have unsaved changes. Do you want to close this form?',
        acceptLabel: 'Discard',
        rejectLabel: 'Keep editing',
        acceptButtonStyleClass: 'p-button-danger',
        rejectButtonStyleClass: 'p-button-outlined p-button-secondary',
        accept: () => this.dialogVisible.set(false)
      });
      return;
    }

    this.dialogVisible.set(false);
  }

  handleDialogVisibilityChange(visible: boolean): void {
    if (visible) {
      this.dialogVisible.set(true);
      return;
    }

    this.attemptCloseDialog();
  }

  onLazyLoad(event: LazyLoadEventLike): void {
    const current = this.listState.query();
    const rows = event.rows ?? current.size;
    const first = event.first ?? current.page * current.size;
    const sortField = typeof event.sortField === 'string' ? event.sortField : current.sortField;
    const sortDirection =
      event.sortOrder === 1 ? 'asc' : event.sortOrder === -1 ? 'desc' : current.sortDirection;

    this.listState.query.set({
      ...current,
      page: Math.floor(first / rows),
      size: rows,
      sortField,
      sortDirection
    });

    this.loadList();
  }

  reloadList(): void {
    this.loadList();
  }

  getError(control: AbstractControl | null): string | null {
    if (!control?.errors) {
      return null;
    }

    if (control.errors['server']) {
      return String(control.errors['server']);
    }

    if (control.errors['required']) {
      return 'This field is required.';
    }

    return null;
  }

  isInvalid(control: AbstractControl | null): boolean {
    return !!control && control.invalid && (control.touched || control.dirty || this.submitted());
  }

  protected setDialogError(message: string): null {
    this.dialogError.set(message);
    return null;
  }

  protected get fb(): NonNullableFormBuilder {
    return this.deps.fb;
  }

  private loadList(): void {
    this.listState.loading.set(true);

    this.listRequest(this.listState.query())
      .pipe(takeUntilDestroyed(this.deps.destroyRef))
      .subscribe({
        next: (page) => {
          this.listState.items.set(page.content);
          this.listState.total.set(page.totalElements);
          this.listState.loading.set(false);
        },
        error: (error) => {
          this.listState.loading.set(false);
          this.deps.notificationService.error(
            `${this.config.collectionLabel} unavailable`,
            getApiErrorMessage(error)
          );
        }
      });
  }

  private bindListFilters(): void {
    this.listState.searchControl.valueChanges
      .pipe(debounceTime(250), distinctUntilChanged(), takeUntilDestroyed(this.deps.destroyRef))
      .subscribe((search) => {
        this.listState.query.update((query) => ({
          ...query,
          search: search.trim(),
          page: 0
        }));
        this.loadList();
      });

    this.listState.includeDeletedControl.valueChanges.pipe(takeUntilDestroyed(this.deps.destroyRef)).subscribe((includeDeleted) => {
      this.listState.query.update((query) => ({
        ...query,
        includeDeleted,
        page: 0
      }));
      this.loadList();
    });
  }

  private createListState<T>(sortField: string): CrudListState<T> {
    return {
      items: signal<T[]>([]),
      total: signal(0),
      loading: signal(false),
      query: signal<QueryOptions>({
        page: 0,
        size: 10,
        sortField,
        sortDirection: 'desc',
        search: '',
        includeDeleted: false
      }),
      searchControl: this.deps.fb.control(''),
      includeDeletedControl: this.deps.fb.control(false)
    };
  }

  private applyServerErrors(error: unknown): void {
    const apiError = extractApiError(error);

    if (apiError?.details) {
      for (const [field, value] of Object.entries(apiError.details)) {
        const control = this.form.get(field);

        if (control && typeof value === 'string') {
          control.setErrors({
            ...(control.errors ?? {}),
            server: value
          });
          control.markAsTouched();
        }
      }
    }

    this.dialogError.set(getApiErrorMessage(error));
  }

  private clearServerErrors(control: AbstractControl): void {
    if (control instanceof FormControl) {
      if (control.errors?.['server']) {
        const { server, ...rest } = control.errors;
        control.setErrors(Object.keys(rest).length ? rest : null);
      }
      return;
    }

    if (control instanceof FormGroup || control instanceof FormArray) {
      Object.values(control.controls).forEach((child) => this.clearServerErrors(child));

      if (control.errors?.['server']) {
        const { server, ...rest } = control.errors;
        control.setErrors(Object.keys(rest).length ? rest : null);
      }
    }
  }
}
