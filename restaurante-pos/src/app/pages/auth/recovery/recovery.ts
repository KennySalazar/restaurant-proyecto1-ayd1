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
  selector: 'app-recovery-page',
  imports: [
    FieldErrorComponent,
    FormFeedbackComponent,
    InputTextModule,
    ReactiveFormsModule,
    RouterLink,
    TranslocoPipe,
  ],
  templateUrl: './recovery.html',
  styleUrls: [
    '../login/login.scss',
    './recovery.scss',
  ],
})
export class RecoveryPageComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly authFlow = inject(AuthFlowService);
  private readonly errors = inject(ApiErrorService);
  private readonly router = inject(Router);

  readonly submitted = signal(false);
  readonly isSubmitting = signal(false);
  readonly serverMessage = signal<string | null>(null);

  readonly form = this.formBuilder.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
  });

  submit(): void {
    this.submitted.set(true);
    this.serverMessage.set(null);

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);

    const email = this.form.getRawValue().email;

    this.auth
      .requestPasswordRecovery({ email })
      .pipe(
        finalize(() => this.isSubmitting.set(false)),
      )
      .subscribe({
        next: (response) => {
          this.authFlow.setRecoveryChallenge({
            challengeId: response.challengeId,
            expiresAt: response.expiresAt,
            email,
          });

          void this.router.navigate([
            '/auth/recovery-reset',
          ]);
        },
        error: (error: unknown) => {
          this.serverMessage.set(
            this.errors.getMessage(error),
          );
        },
      });
  }
}
