import { FormArray, FormControl, FormGroup, Validators } from '@angular/forms';
import { QueryOptions } from '../../core/models/api.models';
import {
  Dia,
  DiaUpsertRequest,
  StitchingSection,
  StitchingSectionType,
  StitchingSectionUpsertRequest,
  Style,
  StyleUpsertRequest
} from '../../core/models/master-data.models';
import { MasterDataApiService } from '../../core/services/master-data-api.service';
import { BaseCrudController, CrudControllerDeps } from '../../core/crud/base-crud.controller';

type StyleFormGroup = FormGroup<{
  styleName: FormControl<string>;
  colors: FormArray<FormControl<string>>;
}>;

type DiaFormGroup = FormGroup<{
  diaValue: FormControl<string>;
}>;

type SectionFormGroup = FormGroup<{
  sectionName: FormControl<string>;
  type: FormControl<StitchingSectionType>;
}>;

export class StyleCrudController extends BaseCrudController<Style, StyleUpsertRequest, StyleFormGroup> {
  constructor(
    private readonly masterDataApi: MasterDataApiService,
    deps: CrudControllerDeps,
    afterMutation?: () => void
  ) {
    super(
      deps,
      {
        entityLabel: 'Style',
        collectionLabel: 'Styles',
        defaultSortField: 'updatedAt'
      },
      afterMutation
    );
    this.initialize();
  }

  get colorControls(): FormControl<string>[] {
    return this.form.controls.colors.controls;
  }

  protected buildForm(): StyleFormGroup {
    return this.fb.group({
      styleName: this.fb.control('', Validators.required),
      colors: this.fb.array([this.createColorControl()])
    });
  }

  protected fillForm(entity: Style): void {
    this.form.controls.styleName.setValue(entity.styleName);
    this.form.setControl(
      'colors',
      this.fb.array(entity.colors.length ? entity.colors.map((color) => this.createColorControl(color)) : [this.createColorControl()])
    );
  }

  protected resetForm(): void {
    this.form.reset({ styleName: '' });
    this.form.setControl('colors', this.fb.array([this.createColorControl()]));
  }

  protected buildPayload(): StyleUpsertRequest | null {
    const colors = Array.from(
      new Set(
        this.form.controls.colors
          .getRawValue()
          .map((color) => color.trim())
          .filter((color) => color.length > 0)
      )
    );

    if (!colors.length) {
      this.form.controls.colors.setErrors({
        ...(this.form.controls.colors.errors ?? {}),
        required: true
      });
      return this.setDialogError('Add at least one color before saving the style.');
    }

    return {
      styleName: this.form.controls.styleName.getRawValue().trim(),
      colors
    };
  }

  protected listRequest(query: QueryOptions) {
    return this.masterDataApi.getStyles(query);
  }

  protected createRequest(payload: StyleUpsertRequest) {
    return this.masterDataApi.createStyle(payload);
  }

  protected updateRequest(autoId: string, payload: StyleUpsertRequest) {
    return this.masterDataApi.updateStyle(autoId, payload);
  }

  protected deleteRequest(autoId: string) {
    return this.masterDataApi.deleteStyle(autoId);
  }

  protected getEntityAutoId(entity: Style): string {
    return entity.autoId;
  }

  protected getDeletePrompt(entity: Style): string {
    return `Soft delete ${entity.autoId} - ${entity.styleName}?`;
  }

  protected getCreateSuccessDetail(payload: StyleUpsertRequest): string {
    return `${payload.styleName} has been saved.`;
  }

  protected getUpdateSuccessDetail(payload: StyleUpsertRequest): string {
    return `${payload.styleName} has been saved.`;
  }

  protected getDeleteSuccessDetail(entity: Style): string {
    return `${entity.autoId} was moved out of the active list.`;
  }

  createColorField(): void {
    this.form.controls.colors.push(this.createColorControl());
  }

  removeColorField(index: number): void {
    if (this.form.controls.colors.length === 1) {
      return;
    }

    this.form.controls.colors.removeAt(index);
  }

  private createColorControl(value = ''): FormControl<string> {
    return this.fb.control(value, Validators.required);
  }
}

export class DiaCrudController extends BaseCrudController<Dia, DiaUpsertRequest, DiaFormGroup> {
  constructor(
    private readonly masterDataApi: MasterDataApiService,
    deps: CrudControllerDeps,
    afterMutation?: () => void
  ) {
    super(
      deps,
      {
        entityLabel: 'Dia',
        collectionLabel: 'Dia values',
        defaultSortField: 'updatedAt'
      },
      afterMutation
    );
    this.initialize();
  }

  protected buildForm(): DiaFormGroup {
    return this.fb.group({
      diaValue: this.fb.control('', Validators.required)
    });
  }

  protected fillForm(entity: Dia): void {
    this.form.controls.diaValue.setValue(entity.diaValue);
  }

  protected resetForm(): void {
    this.form.reset({ diaValue: '' });
  }

  protected buildPayload(): DiaUpsertRequest {
    return {
      diaValue: this.form.controls.diaValue.getRawValue().trim()
    };
  }

  protected listRequest(query: QueryOptions) {
    return this.masterDataApi.getDias(query);
  }

  protected createRequest(payload: DiaUpsertRequest) {
    return this.masterDataApi.createDia(payload);
  }

  protected updateRequest(autoId: string, payload: DiaUpsertRequest) {
    return this.masterDataApi.updateDia(autoId, payload);
  }

  protected deleteRequest(autoId: string) {
    return this.masterDataApi.deleteDia(autoId);
  }

  protected getEntityAutoId(entity: Dia): string {
    return entity.autoId;
  }

  protected getDeletePrompt(entity: Dia): string {
    return `Soft delete ${entity.autoId} - ${entity.diaValue}?`;
  }

  protected getCreateSuccessDetail(payload: DiaUpsertRequest): string {
    return `${payload.diaValue} has been saved.`;
  }

  protected getUpdateSuccessDetail(payload: DiaUpsertRequest): string {
    return `${payload.diaValue} has been saved.`;
  }

  protected getDeleteSuccessDetail(entity: Dia): string {
    return `${entity.autoId} was moved out of the active list.`;
  }
}

export class SectionCrudController extends BaseCrudController<
  StitchingSection,
  StitchingSectionUpsertRequest,
  SectionFormGroup
> {
  constructor(
    private readonly masterDataApi: MasterDataApiService,
    deps: CrudControllerDeps,
    afterMutation?: () => void
  ) {
    super(
      deps,
      {
        entityLabel: 'Stitching Section',
        collectionLabel: 'Stitching sections',
        defaultSortField: 'updatedAt'
      },
      afterMutation
    );
    this.initialize();
  }

  protected buildForm(): SectionFormGroup {
    return this.fb.group({
      sectionName: this.fb.control('', Validators.required),
      type: this.fb.control<StitchingSectionType>('INTERNAL', Validators.required)
    });
  }

  protected fillForm(entity: StitchingSection): void {
    this.form.setValue({
      sectionName: entity.sectionName,
      type: entity.type
    });
  }

  protected resetForm(): void {
    this.form.reset({
      sectionName: '',
      type: 'INTERNAL'
    });
  }

  protected buildPayload(): StitchingSectionUpsertRequest {
    return {
      sectionName: this.form.controls.sectionName.getRawValue().trim(),
      type: this.form.controls.type.getRawValue()
    };
  }

  protected listRequest(query: QueryOptions) {
    return this.masterDataApi.getSections(query);
  }

  protected createRequest(payload: StitchingSectionUpsertRequest) {
    return this.masterDataApi.createSection(payload);
  }

  protected updateRequest(autoId: string, payload: StitchingSectionUpsertRequest) {
    return this.masterDataApi.updateSection(autoId, payload);
  }

  protected deleteRequest(autoId: string) {
    return this.masterDataApi.deleteSection(autoId);
  }

  protected getEntityAutoId(entity: StitchingSection): string {
    return entity.autoId;
  }

  protected getDeletePrompt(entity: StitchingSection): string {
    return `Soft delete ${entity.autoId} - ${entity.sectionName}?`;
  }

  protected getCreateSuccessDetail(payload: StitchingSectionUpsertRequest): string {
    return `${payload.sectionName} has been saved.`;
  }

  protected getUpdateSuccessDetail(payload: StitchingSectionUpsertRequest): string {
    return `${payload.sectionName} has been saved.`;
  }

  protected getDeleteSuccessDetail(entity: StitchingSection): string {
    return `${entity.autoId} was moved out of the active list.`;
  }
}
