import {
  Component,
  EventEmitter,
  inject,
  Input,
  OnChanges,
  Output,
  signal,
  SimpleChanges,
} from '@angular/core';
import {
  FormBuilder,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import {
  TranslocoPipe,
  TranslocoService,
} from '@jsverse/transloco';
import { MessageService } from 'primeng/api';
import { finalize } from 'rxjs';

import { CreateWaitlistEntryRequest } from '../../../../core/models/waitlist.models';
import { ApiErrorService } from '../../../../core/services/api-error.service';
import { WaitlistService } from '../../../../core/services/waitlist.service';
import { FormFeedbackComponent } from '../../../../shared/components/form-feedback/form-feedback';

@Component({
  selector: 'app-waitlist-form-dialog',
  imports: [
    FormFeedbackComponent,
    ReactiveFormsModule,
    TranslocoPipe,
  ],
  templateUrl: './waitlist-form-dialog.html',
  styleUrl: './waitlist-form-dialog.scss',
})
export class WaitlistFormDialogComponent
  implements OnChanges
{
  private readonly fb = inject(FormBuilder);
  private readonly waitlistService =
    inject(WaitlistService);
  private readonly errors = inject(ApiErrorService);
  private readonly messages = inject(MessageService);
  private readonly transloco =
    inject(TranslocoService);

  @Input() open = false;

  @Output() closed = new EventEmitter<void>();
  @Output() saved = new EventEmitter<void>();

  readonly isSaving = signal(false);
  readonly submitted = signal(false);
  readonly serverMessage =
    signal<string | null>(null);

  readonly waitlistForm =
    this.fb.nonNullable.group({
      nombreCliente: [
        '',
        [
          Validators.required,
          Validators.maxLength(150),
        ],
      ],
      telefonoCliente: [
        '',
        [
          Validators.required,
          Validators.maxLength(25),
        ],
      ],
      cantidadPersonas: [
        1,
        [
          Validators.required,
          Validators.min(1),
        ],
      ],
      notas: [
        '',
        Validators.maxLength(500),
      ],
    });

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['open']?.currentValue === true) {
      this.prepareForm();
    }
  }

  invalid(controlName: string): boolean {
    const control =
      this.waitlistForm.get(controlName);

    return Boolean(
      control &&
        control.invalid &&
        (control.touched || this.submitted()),
    );
  }

  close(): void {
    if (this.isSaving()) {
      return;
    }

    this.closed.emit();
  }

  submit(): void {
    this.submitted.set(true);
    this.serverMessage.set(null);

    if (this.waitlistForm.invalid) {
      this.waitlistForm.markAllAsTouched();
      return;
    }

    const raw =
      this.waitlistForm.getRawValue();

    const payload: CreateWaitlistEntryRequest = {
      nombreCliente: raw.nombreCliente.trim(),
      telefonoCliente:
        raw.telefonoCliente.trim(),
      cantidadPersonas:
        raw.cantidadPersonas,
      notas: raw.notas.trim() || null,
    };

    this.isSaving.set(true);

    this.waitlistService
      .createWaitlistEntry(payload)
      .pipe(
        finalize(() =>
          this.isSaving.set(false),
        ),
      )
      .subscribe({
        next: () => {
          this.messages.add({
            severity: 'success',
            summary: this.transloco.translate(
              'waitlist.registration.successTitle',
            ),
            detail: this.transloco.translate(
              'waitlist.registration.successMessage',
              {
                name: payload.nombreCliente,
              },
            ),
            life: 5000,
          });

          this.saved.emit();
        },
        error: (error: unknown) => {
          this.serverMessage.set(
            this.errors.getMessage(error),
          );
        },
      });
  }

  private prepareForm(): void {
    this.submitted.set(false);
    this.serverMessage.set(null);

    this.waitlistForm.reset({
      nombreCliente: '',
      telefonoCliente: '',
      cantidadPersonas: 1,
      notas: '',
    });
  }
}