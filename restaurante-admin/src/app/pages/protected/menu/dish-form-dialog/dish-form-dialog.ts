import { Component, EventEmitter, inject, Input, Output, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';
import { MessageService } from 'primeng/api';
import { finalize } from 'rxjs';

import { DishCategory, DishSummary, UpdateDishRequest } from '../../../../core/models/dish.models';
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
  @Input() dish: DishSummary | null = null;
  @Output() readonly saved = new EventEmitter<DishSummary>();
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

    const editing = this.dish;
    this.isSaving.set(true);

    const request = editing
      ? this.dishService.updateDish(editing.id, this.buildPayload())
      : this.dishService.registerDish(this.buildPayload());

    request.pipe(finalize(() => this.isSaving.set(false))).subscribe({
      next: (response) => {
        const successKey = editing ? 'menu.update.success' : 'menu.registration.success';

        this.messages.add({
          severity: 'success',
          summary: this.transloco.translate(`${successKey}.title`),
          detail: this.transloco.translate(`${successKey}.message`, { name: response.dish.name }),
          life: 6000,
        });

        this.saved.emit(response.dish);
      },
      error: (error: unknown) => {
        this.serverMessage.set(this.errors.getMessage(error));
      },
    });
  }

  private prepareForm(): void {
    this.submitted.set(false);
    this.serverMessage.set(null);

    if (this.dish) {
      this.registrationForm.reset({
        code: this.dish.code ?? '',
        name: this.dish.name,
        description: this.dish.description ?? '',
        categoryId: this.dish.categoryId,
        salePrice: this.dish.salePrice,
        preparationTimeMinutes: this.dish.preparationTimeMinutes,
      });

      return;
    }

    this.registrationForm.reset({
      code: '',
      name: '',
      description: '',
      categoryId: 0,
      salePrice: null,
      preparationTimeMinutes: null,
    });
  }

  private buildPayload(): UpdateDishRequest {
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
