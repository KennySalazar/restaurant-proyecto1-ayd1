import { Component, EventEmitter, inject, Input, Output, signal } from '@angular/core';
import {
  AbstractControl,
  FormArray,
  FormBuilder,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';
import { MessageService } from 'primeng/api';
import { finalize } from 'rxjs';

import {
  ComboItemRequest,
  ComboSummary,
  CreateComboRequest,
  UpdateComboRequest,
} from '../../../../core/models/combo.models';
import { DishSummary } from '../../../../core/models/dish.models';
import { ApiErrorService } from '../../../../core/services/api-error.service';
import { ComboService } from '../../../../core/services/combo.service';
import { FormFeedbackComponent } from '../../../../shared/components/form-feedback/form-feedback';

@Component({
  selector: 'app-menu-combo-form-dialog',
  imports: [FormFeedbackComponent, ReactiveFormsModule, TranslocoPipe],
  templateUrl: './combo-form-dialog.html',
  styleUrl: './combo-form-dialog.scss',
})
export class ComboFormDialogComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly comboService = inject(ComboService);
  private readonly errors = inject(ApiErrorService);
  private readonly messages = inject(MessageService);
  private readonly transloco = inject(TranslocoService);

  @Input() dishes: DishSummary[] = [];
  @Input() combo: ComboSummary | null = null;
  @Output() readonly saved = new EventEmitter<ComboSummary>();
  @Output() readonly dismissed = new EventEmitter<void>();

  private isOpen = false;

  @Input()
  set open(value: boolean) {
    this.isOpen = value;

    if (value) {
      this.prepareForm();
    }
  }

  get open(): boolean {
    return this.isOpen;
  }

  readonly isSaving = signal(false);
  readonly submitted = signal(false);
  readonly serverMessage = signal<string | null>(null);

  readonly form = this.formBuilder.group({
    code: ['', [Validators.maxLength(30)]],
    name: ['', [Validators.required, Validators.maxLength(120)]],
    description: ['', [Validators.maxLength(500)]],
    salePrice: [null as number | null, [Validators.required, Validators.min(0.01)]],
    preparationTimeMinutes: [null as number | null, [Validators.min(1)]],
    startDate: [''],
    endDate: [''],
    items: new FormArray<AbstractControl<unknown>>([], Validators.minLength(2)),
  });

  get itemsArray(): FormArray {
    return this.form.get('items') as FormArray;
  }

  close(): void {
    if (this.isSaving()) {
      return;
    }

    this.dismissed.emit();
  }

  addItem(): void {
    this.itemsArray.push(
      this.formBuilder.group({
        dishId: [this.defaultDishId(), [Validators.required, Validators.min(1)]],
        quantity: [1 as number, [Validators.required, Validators.min(1)]],
      }),
    );
  }

  removeItem(index: number): void {
    if (this.isSaving()) {
      return;
    }

    this.itemsArray.removeAt(index);
  }

  submit(): void {
    this.submitted.set(true);
    this.serverMessage.set(null);

    if (this.form.invalid) {
      this.markItemsTouched();
      this.form.markAllAsTouched();
      return;
    }

    const editing = this.combo;
    this.isSaving.set(true);

    const request = editing
      ? this.comboService.updateCombo(editing.id, this.buildUpdatePayload())
      : this.comboService.createCombo(this.buildCreatePayload());

    request.pipe(finalize(() => this.isSaving.set(false))).subscribe({
      next: (response) => {
        const successKey = editing
          ? 'menu.combos.update.success'
          : 'menu.combos.registration.success';

        this.messages.add({
          severity: 'success',
          summary: this.transloco.translate(`${successKey}.title`),
          detail: this.transloco.translate(`${successKey}.message`, { name: response.combo.name }),
          life: 6000,
        });

        this.saved.emit(response.combo);
      },
      error: (error: unknown) => {
        this.serverMessage.set(this.errors.getMessage(error));
      },
    });
  }

  private prepareForm(): void {
    this.submitted.set(false);
    this.serverMessage.set(null);
    this.itemsArray.clear();

    if (this.combo) {
      this.form.reset({
        code: this.combo.code ?? '',
        name: this.combo.name,
        description: this.combo.description ?? '',
        salePrice: this.combo.salePrice,
        preparationTimeMinutes: this.combo.preparationTimeMinutes ?? null,
        startDate: this.toLocalInput(this.combo.startDate),
        endDate: this.toLocalInput(this.combo.endDate),
      });

      for (const item of this.combo.items) {
        this.itemsArray.push(
          this.formBuilder.group({
            dishId: [item.dishId, [Validators.required, Validators.min(1)]],
            quantity: [item.quantity, [Validators.required, Validators.min(1)]],
          }),
        );
      }

      return;
    }

    this.form.reset({
      code: '',
      name: '',
      description: '',
      salePrice: null,
      preparationTimeMinutes: null,
      startDate: '',
      endDate: '',
    });
  }

  private markItemsTouched(): void {
    for (const group of this.itemsArray.controls) {
      group.markAsTouched();
      group.updateValueAndValidity();
    }
  }

  private buildCreatePayload(): CreateComboRequest {
    const raw = this.form.getRawValue();

    return {
      code: raw.code?.trim() || null,
      name: raw.name!.trim(),
      description: raw.description?.trim() || null,
      salePrice: raw.salePrice!,
      preparationTimeMinutes: raw.preparationTimeMinutes ?? null,
      startDate: this.fromLocalInput(raw.startDate),
      endDate: this.fromLocalInput(raw.endDate),
      items: this.buildItems(raw.items),
    };
  }

  private buildUpdatePayload(): UpdateComboRequest {
    const raw = this.form.getRawValue();

    return {
      code: raw.code?.trim() || null,
      name: raw.name!.trim(),
      description: raw.description?.trim() || null,
      salePrice: raw.salePrice!,
      preparationTimeMinutes: raw.preparationTimeMinutes ?? null,
      startDate: this.fromLocalInput(raw.startDate),
      endDate: this.fromLocalInput(raw.endDate),
      items: this.buildItems(raw.items),
    };
  }

  private buildItems(items: unknown[]): ComboItemRequest[] {
    return (items as Array<{ dishId: number; quantity: number }>)
      .filter((item) => item.dishId > 0 && item.quantity > 0)
      .map((item) => ({ dishId: item.dishId, quantity: item.quantity }));
  }

  private getSelectedDishIds(): number[] {
    return this.itemsArray.controls
      .map((group) => (group.get('dishId')?.value as number | null) ?? 0)
      .filter((dishId) => dishId > 0);
  }

  private defaultDishId(): number {
    const used = this.getSelectedDishIds();
    const firstAvailable = this.dishes.find((dish) => !used.includes(dish.id));
    return firstAvailable?.id ?? 0;
  }

  private toLocalInput(iso: string | null | undefined): string {
    if (!iso) {
      return '';
    }

    const date = new Date(iso);
    if (Number.isNaN(date.getTime())) {
      return '';
    }

    const pad = (value: number) => String(value).padStart(2, '0');

    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
  }

  private fromLocalInput(value: string | null | undefined): string | null {
    if (!value) {
      return null;
    }

    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? null : date.toISOString();
  }

  invalid(controlName: keyof typeof this.form.controls): boolean {
    const control = this.form.controls[controlName];

    return control.invalid && (control.touched || this.submitted());
  }

  itemsInvalid(): boolean {
    return this.itemsArray.invalid && (this.itemsArray.touched || this.submitted());
  }

  itemFieldInvalid(index: number, field: 'dishId' | 'quantity'): boolean {
    const control = this.itemsArray.at(index)?.get(field);

    return control != null && control.invalid && (control.touched || this.submitted());
  }
}
