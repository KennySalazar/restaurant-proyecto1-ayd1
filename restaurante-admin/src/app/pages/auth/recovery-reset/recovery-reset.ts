import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';
import { InputTextModule } from 'primeng/inputtext';
import { finalize } from 'rxjs';
import { ApiErrorService } from '../../../core/services/api-error.service';
import { AuthFlowService } from '../../../core/services/auth-flow.service';
import { AuthService } from '../../../core/services/auth.service';
import { FieldErrorComponent } from '../../../shared/components/field-error/field-error';
import { FormFeedbackComponent } from '../../../shared/components/form-feedback/form-feedback';

@Component({
  selector: 'app-recovery-reset-page',
  imports: [
    FieldErrorComponent,
    FormFeedbackComponent,
    InputTextModule,
    ReactiveFormsModule,
    RouterLink,
    TranslocoPipe,
  ],
  templateUrl: './recovery-reset.html',
  styleUrls: [
    '../login/login.scss',
    './recovery-reset.scss',
  ],
})
export class RecoveryResetPageComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly authFlow = inject(AuthFlowService);
  private readonly errors = inject(ApiErrorService);
  private readonly router = inject(Router);

  readonly submitted = signal(false);
  readonly isSubmitting = signal(false);
  readonly serverMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);

  readonly challenge =
    this.authFlow.getRecoveryChallenge();

  readonly form = this.formBuilder.nonNullable.group({
    otp: [
      '',
      [
        Validators.required,
        Validators.pattern(/^\d{6}$/),
      ],
    ],
    newPassword: [
      '',
      [
        Validators.required,
        Validators.minLength(8),
        Validators.maxLength(72),
        Validators.pattern(
          /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).+$/,
        ),
      ],
    ],
    confirmPassword: [
      '',
      [Validators.required],
    ],
  });

  constructor() {
    if (!this.challenge) {
      void this.router.navigate(['/auth/recovery']);
      return;
    }

    if (
      new Date(this.challenge.expiresAt).getTime()
      <= Date.now()
    ) {
      this.authFlow.clearRecoveryChallenge();

      void this.router.navigate(['/auth/recovery']);
    }
  }

  submit(): void {
    this.submitted.set(true);
    this.serverMessage.set(null);

    if (!this.challenge) {
      void this.router.navigate(['/auth/recovery']);
      return;
    }

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const values = this.form.getRawValue();

    if (values.newPassword !== values.confirmPassword) {
      this.serverMessage.set(
        'Las contraseñas no coinciden.',
      );
      return;
    }

    this.isSubmitting.set(true);

    this.auth
      .verifyPasswordRecovery({
        challengeId: this.challenge.challengeId,
        otp: values.otp,
        newPassword: values.newPassword,
      })
      .pipe(
        finalize(() => this.isSubmitting.set(false)),
      )
      .subscribe({
        next: (response) => {
          this.authFlow.clearRecoveryChallenge();
          this.successMessage.set(response.message);
          this.form.disable();
        },
        error: (error: unknown) => {
          this.serverMessage.set(
            this.errors.getMessage(error),
          );
        },
      });
  }
}
