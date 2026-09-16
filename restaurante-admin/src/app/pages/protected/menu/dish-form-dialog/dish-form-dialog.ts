import { Component, EventEmitter, inject, Input, Output, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';
import { MessageService } from 'primeng/api';
import { finalize } from 'rxjs';

import { CreateDishRequest, DishCategory, DishSummary } from '../../../../core/models/dish.models';
import { ApiErrorService } from '../../../../core/services/api-error.service';
import { DishService } from '../../../../core/services/dish.service';
import { FormFeedbackComponent } from '../../../../shared/components/form-feedback/form-feedback';

@Component({
  selector: 'app-menu-dish-form-dialog',
  imports: [FormFeedbackComponent, ReactiveFormsModule, TranslocoPipe],
  templateUrl: './dish-form-dialog.html',
  styleUrl: './dish-form-dialog.scss',
})
export class DishFormDialogComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly dishService = inject(DishService);
  private readonly errors = inject(ApiErrorService);
  private readonly messages = inject(MessageService);
  private readonly transloco = inject(TranslocoService);

  @Input() categories: DishCategory[] = [];
  @Input() open = false;
  @Output() readonly saved = new EventEmitter<DishSummary>();
  @Output() readonly dismissed = new EventEmitter<void>();

  readonly isSaving = signal(false);
  readonly submitted = signal(false);
  readonly serverMessage = signal<string | null>(null);

  readonly registrationForm = this.formBuilder.group({
    code: ['', [Validators.maxLength(30)]],
    name: ['', [Validators.required, Validators.maxLength(120)]],
    description: ['', [Validators.maxLength(500)]],
    categoryId: [0 as number, [Validators.required, Validators.min(1)]],
    salePrice: [null as number | null, [Validators.required, Validators.min(0.01)]],
    preparationTimeMinutes: [null as number | null, [Validators.required, Validators.min(1)]],
  });

  close(): void {
    if (this.isSaving()) {
      return;
    }

    this.dismissed.emit();
  }

  submit(): void {
    this.submitted.set(true);
    this.serverMessage.set(null);

    if (this.registrationForm.invalid) {
      this.registrationForm.markAllAsTouched();
      return;
    }

    this.isSaving.set(true);

    this.dishService
      .registerDish(this.buildPayload())
      .pipe(finalize(() => this.isSaving.set(false)))
      .subscribe({
        next: (response) => {
          this.messages.add({
            severity: 'success',
            summary: this.transloco.translate('menu.registration.success.title'),
            detail: this.transloco.translate('menu.registration.success.message', {
              name: response.dish.name,
            }),
            life: 6000,
          });

          this.saved.emit(response.dish);
        },
        error: (error: unknown) => {
          this.serverMessage.set(this.errors.getMessage(error));
        },
      });
  }

  private buildPayload(): CreateDishRequest {
    const raw = this.registrationForm.getRawValue();

    return {
      code: raw.code?.trim() || null,
      name: raw.name!.trim(),
      description: raw.description?.trim() || null,
      categoryId: raw.categoryId!,
      salePrice: raw.salePrice!,
      preparationTimeMinutes: raw.preparationTimeMinutes!,
    };
  }

  invalid(controlName: keyof typeof this.registrationForm.controls): boolean {
    const control = this.registrationForm.controls[controlName];

    return control.invalid && (control.touched || this.submitted());
  }
}
