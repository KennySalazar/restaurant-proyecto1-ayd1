import { Component, EventEmitter, inject, Input, Output, signal } from '@angular/core';
import { FormArray, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';
import { MessageService } from 'primeng/api';
import { finalize } from 'rxjs';

import { DishSummary } from '../../../../core/models/dish.models';
import {
  CreateModifierRequest,
  ModifierSummary,
  UpdateModifierRequest,
} from '../../../../core/models/modifier.models';
import { ApiErrorService } from '../../../../core/services/api-error.service';
import { ModifierService } from '../../../../core/services/modifier.service';
import { FormFeedbackComponent } from '../../../../shared/components/form-feedback/form-feedback';

@Component({
  selector: 'app-menu-modifier-form-dialog',
  imports: [FormFeedbackComponent, ReactiveFormsModule, TranslocoPipe],
  templateUrl: './modifier-form-dialog.html',
  styleUrl: './modifier-form-dialog.scss',
})
export class ModifierFormDialogComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly modifierService = inject(ModifierService);
  private readonly errors = inject(ApiErrorService);
  private readonly messages = inject(MessageService);
  private readonly transloco = inject(TranslocoService);

  @Input() dishes: DishSummary[] = [];
  @Input() modifier: ModifierSummary | null = null;
  @Output() readonly saved = new EventEmitter<ModifierSummary>();
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
    name: ['', [Validators.required, Validators.maxLength(100)]],
    description: ['', [Validators.maxLength(255)]],
    additionalPrice: [0, [Validators.required, Validators.min(0)]],
    dishIds: this.formBuilder.array<boolean[]>([], { validators: Validators.minLength(1) }),
  });

  get dishIdsArray(): FormArray {
    return this.form.get('dishIds') as FormArray;
  }

  close(): void {
    if (this.isSaving()) {
      return;
    }

    this.dismissed.emit();
  }

  submit(): void {
    this.submitted.set(true);
    this.serverMessage.set(null);

    const selectedDishIds = this.getSelectedDishIds();

    if (selectedDishIds.length === 0) {
      this.form.controls.dishIds.setErrors({ atLeastOne: true });
      this.form.controls.dishIds.markAsTouched();
    } else {
      this.form.controls.dishIds.setErrors(null);
    }

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const editing = this.modifier;
    this.isSaving.set(true);

    const request = editing
      ? this.modifierService.updateModifier(editing.id, this.buildUpdatePayload(selectedDishIds))
      : this.modifierService.registerModifier(this.buildCreatePayload(selectedDishIds));

    request.pipe(finalize(() => this.isSaving.set(false))).subscribe({
      next: (response) => {
        const successKey = editing
          ? 'menu.modifiers.update.success'
          : 'menu.modifiers.registration.success';

        this.messages.add({
          severity: 'success',
          summary: this.transloco.translate(`${successKey}.title`),
          detail: this.transloco.translate(`${successKey}.message`, {
            name: response.modifier.name,
          }),
          life: 6000,
        });

        this.saved.emit(response.modifier);
      },
      error: (error: unknown) => {
        this.serverMessage.set(this.errors.getMessage(error));
      },
    });
  }

  private prepareForm(): void {
    this.submitted.set(false);
    this.serverMessage.set(null);

    if (this.modifier) {
      this.form.reset({
        code: this.modifier.code ?? '',
        name: this.modifier.name,
        description: this.modifier.description ?? '',
        additionalPrice: this.modifier.additionalPrice ?? 0,
      });

      this.buildDishIdControls(this.modifier.associatedDishes.map((d) => d.id));
      return;
    }

    this.form.reset({ code: '', name: '', description: '', additionalPrice: 0 });
    this.buildDishIdControls([]);
  }

  private buildDishIdControls(selectedIds: number[]): void {
    this.dishIdsArray.clear();

    for (const dish of this.dishes) {
      this.dishIdsArray.push(this.formBuilder.control(selectedIds.includes(dish.id)));
    }
  }

  private getSelectedDishIds(): number[] {
    return this.dishes.filter((_, index) => this.dishIdsArray.at(index)?.value).map((d) => d.id);
  }

  private buildCreatePayload(dishIds: number[]): CreateModifierRequest {
    const raw = this.form.getRawValue();

    return {
      code: raw.code?.trim() || null,
      name: raw.name!.trim(),
      description: raw.description?.trim() || null,
      additionalPrice: raw.additionalPrice ?? 0,
      dishIds,
    };
  }

  private buildUpdatePayload(dishIds: number[]): UpdateModifierRequest {
    const raw = this.form.getRawValue();

    return {
      code: raw.code?.trim() || null,
      name: raw.name!.trim(),
      description: raw.description?.trim() || null,
      additionalPrice: raw.additionalPrice ?? 0,
      dishIds,
    };
  }

  invalid(controlName: keyof typeof this.form.controls): boolean {
    const control = this.form.controls[controlName];

    return control.invalid && (control.touched || this.submitted());
  }

  dishIdInvalid(): boolean {
    const control = this.form.get('dishIds');

    return control != null && control.invalid && (control.touched || this.submitted());
  }

  formatPrice(value: number): string {
    return new Intl.NumberFormat('es-GT', {
      style: 'currency',
      currency: 'GTQ',
    }).format(value);
  }
}
