import {
  AbstractControl,
  ControlValueAccessor,
  FormArray,
  FormBuilder,
  FormGroup,
  NG_VALUE_ACCESSOR,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
  Validators,
} from '@angular/forms';
import { Component, forwardRef, inject, Input, OnDestroy } from '@angular/core';
import { Subscription } from 'rxjs';

import { TranslocoPipe } from '@jsverse/transloco';
import { MeasurementUnit, Supply } from '../../../core/models/supply.models';
import { RecipeIngredientEdit } from '../../../core/models/recipe.models';

@Component({
  selector: 'app-ingredient-editor',
  imports: [ReactiveFormsModule, TranslocoPipe],
  templateUrl: './ingredient-editor.html',
  styleUrl: './ingredient-editor.scss',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => IngredientEditorComponent),
      multi: true,
    },
  ],
})
export class IngredientEditorComponent implements ControlValueAccessor, OnDestroy {
  @Input() supplies: Supply[] = [];
  @Input() units: MeasurementUnit[] = [];
  @Input() submitted = false;

  private readonly formBuilder = inject(FormBuilder);
  private readonly rowsSubscription: Subscription;
  private readonly rowSubscriptions = new Map<FormGroup, Subscription>();

  readonly rows = new FormArray<FormGroup>([]);

  private onChange: (value: RecipeIngredientEdit[]) => void = () => {};
  private onTouched: () => void = () => {};
  private writing = false;

  constructor() {
    this.rowsSubscription = this.rows.valueChanges.subscribe(() => this.emitValue());
  }

  ngOnDestroy(): void {
    this.rowSubscriptions.forEach((subscription) => subscription.unsubscribe());
    this.rowSubscriptions.clear();
    this.rowsSubscription.unsubscribe();
  }

  writeValue(value: RecipeIngredientEdit[] | null): void {
    this.writing = true;
    this.rows.clear();
    for (const item of value ?? []) {
      this.addRow(item);
    }
    this.writing = false;
  }

  registerOnChange(change: (value: RecipeIngredientEdit[]) => void): void {
    this.onChange = change;
  }

  registerOnTouched(touched: () => void): void {
    this.onTouched = touched;
  }

  setDisabledState(disabled: boolean): void {
    if (disabled) {
      this.rows.disable({ emitEvent: false });
    } else {
      this.rows.enable({ emitEvent: false });
    }
  }

  addEmptyRow(): void {
    this.addRow();
  }

  removeRow(index: number): void {
    const group = this.rows.at(index) as FormGroup;
    this.rowSubscriptions.get(group)?.unsubscribe();
    this.rowSubscriptions.delete(group);
    this.rows.removeAt(index);
  }

  onBlur(): void {
    this.onTouched();
  }

  quantityStep(index: number): string {
    return this.isConteo(index) ? '1' : '0.01';
  }

  unitCostFor(index: number): number | null {
    const supplyId = Number(this.rows.at(index)?.get('supplyId')?.value ?? 0);
    if (!supplyId) {
      return null;
    }
    return this.supplies.find((supply) => supply.id === supplyId)?.unitCost ?? null;
  }

  ingredientCost(index: number): number | null {
    const cost = this.unitCostFor(index);
    const quantity = Number(this.rows.at(index)?.get('quantity')?.value ?? 0);
    if (cost == null || !quantity || quantity <= 0) {
      return null;
    }
    return cost * quantity;
  }

  costIsExact(index: number): boolean {
    const supplyId = Number(this.rows.at(index)?.get('supplyId')?.value ?? 0);
    const supply = this.supplies.find((item) => item.id === supplyId);
    const selectedUnitId = this.rows.at(index)?.get('measurementUnitId')?.value;
    if (!supply) {
      return false;
    }
    return selectedUnitId == null || Number(selectedUnitId) === supply.unitId;
  }

  availableUnitsFor(index: number): MeasurementUnit[] {
    const group = this.rows.at(index);
    if (!group) {
      return [];
    }
    const supplyId = Number(group.get('supplyId')?.value ?? 0);
    if (!supplyId) {
      return [];
    }
    const supply = this.supplies.find((s) => s.id === supplyId);
    if (!supply) {
      return [];
    }
    const dimension = this.units.find((unit) => unit.id === supply.unitId)?.dimension;
    if (!dimension) {
      return [];
    }
    return this.units.filter((unit) => unit.dimension === dimension);
  }

  supplyRequiredError(index: number): boolean {
    return this.showError(index, 'supplyId');
  }

  quantityErrorKind(index: number): 'required' | 'positive' | 'integer' | null {
    return this.showError(index, 'quantity') ? this.quantityKind(index) : null;
  }

  notesLengthError(index: number): boolean {
    return this.showError(index, 'notes');
  }

  formatCost(value: number): string {
    return new Intl.NumberFormat('es-GT', {
      maximumFractionDigits: 2,
      minimumFractionDigits: 2,
      style: 'currency',
      currency: 'GTQ',
    }).format(value);
  }

  private readonly notIntegerValidator: ValidatorFn = (
    control: AbstractControl,
  ): ValidationErrors | null => {
    const value = Number(control.value);
    if (control.value === null || control.value === '' || !Number.isFinite(value)) {
      return null;
    }
    return Number.isInteger(value) ? null : { notInteger: true };
  };

  private readonly positiveValidator: ValidatorFn = (
    control: AbstractControl,
  ): ValidationErrors | null => {
    const value = Number(control.value);
    if (control.value === null || control.value === '' || !Number.isFinite(value)) {
      return null;
    }
    return value > 0 ? null : { positive: true };
  };

  private addRow(value?: RecipeIngredientEdit): void {
    const group = this.formBuilder.group({
      supplyId: [value?.supplyId ?? 0, [Validators.required, Validators.min(1)]],
      quantity: [
        value?.quantity ?? null,
        [Validators.required, this.positiveValidator].concat(this.quantityValidators(value)),
      ],
      measurementUnitId: [value?.measurementUnitId ?? null],
      notes: [value?.notes ?? '', [Validators.maxLength(255)]],
    });

    const rowSubscription = new Subscription();
    rowSubscription.add(
      group.get('supplyId')!.valueChanges.subscribe((supplyId) => {
        const supply = this.supplies.find((s) => s.id === Number(supplyId));
        const unitControl = group.get('measurementUnitId')!;
        unitControl.setValue(supply?.unitId ?? null, { emitEvent: false });
        this.syncQuantityValidators(group);
      }),
    );
    rowSubscription.add(
      group
        .get('measurementUnitId')!
        .valueChanges.subscribe(() => this.syncQuantityValidators(group)),
    );

    this.rowSubscriptions.set(group, rowSubscription);
    this.rows.push(group);
  }

  private quantityValidators(value?: RecipeIngredientEdit): ValidatorFn[] {
    const effectiveUnitId = this.effectiveUnitId(value);
    const isConteo = this.unitIsConteo(effectiveUnitId);
    return isConteo ? [this.notIntegerValidator] : [];
  }

  private syncQuantityValidators(group: FormGroup): void {
    const isConteo = this.unitIsConteo(this.effectiveUnitIdFromGroup(group));
    const quantityControl = group.get('quantity')!;
    quantityControl.setValidators([
      Validators.required,
      this.positiveValidator,
      ...(isConteo ? [this.notIntegerValidator] : []),
    ]);
    quantityControl.updateValueAndValidity({ emitEvent: false });
  }

  private effectiveUnitIdFromGroup(group: FormGroup): number | null {
    const selectedUnitId = group.get('measurementUnitId')?.value;
    if (selectedUnitId) {
      return Number(selectedUnitId);
    }
    const supplyId = Number(group.get('supplyId')?.value ?? 0);
    return this.supplies.find((supply) => supply.id === supplyId)?.unitId ?? null;
  }

  private effectiveUnitId(value?: RecipeIngredientEdit): number | null {
    if (value?.measurementUnitId != null) {
      return Number(value.measurementUnitId);
    }
    if (value?.supplyId != null) {
      return this.supplies.find((supply) => supply.id === value.supplyId)?.unitId ?? null;
    }
    return null;
  }

  private unitIsConteo(unitId: number | null): boolean {
    if (unitId == null) {
      return false;
    }
    return this.units.find((unit) => unit.id === unitId)?.dimension === 'CONTEO';
  }

  private isConteo(index: number): boolean {
    const group = this.rows.at(index);
    if (!group) {
      return false;
    }
    return this.unitIsConteo(this.effectiveUnitIdFromGroup(group));
  }

  private quantityKind(index: number): 'required' | 'positive' | 'integer' | null {
    const quantityControl = this.rows.at(index)?.get('quantity');
    if (!quantityControl) {
      return null;
    }
    if (quantityControl.hasError('required')) {
      return 'required';
    }
    if (quantityControl.hasError('positive')) {
      return 'positive';
    }
    if (quantityControl.hasError('notInteger')) {
      return 'integer';
    }
    return null;
  }

  private showError(index: number, controlName: string): boolean {
    const control = this.rows.at(index)?.get(controlName);
    if (!control) {
      return false;
    }
    return control.invalid && (control.touched || this.submitted);
  }

  private emitValue(): void {
    if (this.writing) {
      return;
    }
    const value = this.rows.controls.map((row) => {
      const raw = row.getRawValue();
      return {
        supplyId: Number(raw.supplyId) || null,
        quantity: raw.quantity === null || raw.quantity === '' ? null : Number(raw.quantity),
        measurementUnitId: raw.measurementUnitId == null ? null : Number(raw.measurementUnitId),
        notes:
          typeof raw.notes === 'string' && raw.notes.trim().length > 0 ? raw.notes.trim() : null,
      };
    });
    this.onChange(value);
  }
}
