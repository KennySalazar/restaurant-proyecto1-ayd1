import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslocoPipe } from '@jsverse/transloco';
import { InputTextModule } from 'primeng/inputtext';
import { finalize } from 'rxjs';
import { ApiErrorService } from '../../../core/services/api-error.service';
import { AuthService } from '../../../core/services/auth.service';
import { AuthSessionService } from '../../../core/services/auth-session.service';
import { FieldErrorComponent } from '../../../shared/components/field-error/field-error';
import { FormFeedbackComponent } from '../../../shared/components/form-feedback/form-feedback';
import { PageHeadingComponent } from '../../../shared/components/page-heading/page-heading';

const PASSWORD_PATTERN = /^(?=.*[A-Z])(?=.*[a-z])(?=.*\d).+$/;

type TwoFactorAction = 'enable' | 'disable';

@Component({
  selector: 'app-security-page',
  imports: [
    FieldErrorComponent,
    FormFeedbackComponent,
    InputTextModule,
    PageHeadingComponent,
    ReactiveFormsModule,
    TranslocoPipe,
  ],
  templateUrl: './security.html',
  styleUrl: './security.scss',
})
export class SecurityPageComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly errors = inject(ApiErrorService);

  readonly session = inject(AuthSessionService);

  readonly submitted = signal(false);
  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);

  readonly twoFactorSubmitted = signal(false);
  readonly twoFactorSubmitting = signal(false);
  readonly twoFactorError = signal<string | null>(null);
  readonly twoFactorSuccess = signal<string | null>(null);

  readonly pendingAction = signal<TwoFactorAction | null>(null);
  readonly challengeId = signal<string | null>(null);

  readonly passwordForm = this.formBuilder.nonNullable.group({
    currentPassword: ['', [Validators.required]],
    newPassword: [
      '',
      [
        Validators.required,
        Validators.minLength(8),
        Validators.maxLength(72),
        Validators.pattern(PASSWORD_PATTERN),
      ],
    ],
  });

  readonly twoFactorPasswordForm = this.formBuilder.nonNullable.group({
    currentPassword: ['', [Validators.required]],
  });

  readonly twoFactorOtpForm = this.formBuilder.nonNullable.group({
    otp: [
      '',
      [
        Validators.required,
        Validators.pattern(/^\d{6}$/),
      ],
    ],
  });

  submitPasswordChange(): void {
    this.submitted.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    if (this.passwordForm.invalid) {
      this.passwordForm.markAllAsTouched();
      return;
    }

    this.submitting.set(true);

    this.auth
      .changePassword(this.passwordForm.getRawValue())
      .pipe(finalize(() => this.submitting.set(false)))
      .subscribe({
        next: (response) => {
          this.session.establish(response);
          this.successMessage.set(response.message);
          this.passwordForm.reset();
          this.submitted.set(false);
        },
        error: (error: unknown) => {
          this.errorMessage.set(this.errors.getMessage(error));
        },
      });
  }

  requestTwoFactorChange(): void {
    this.twoFactorSubmitted.set(true);
    this.twoFactorError.set(null);
    this.twoFactorSuccess.set(null);

    if (this.twoFactorPasswordForm.invalid) {
      this.twoFactorPasswordForm.markAllAsTouched();
      return;
    }

    const action: TwoFactorAction =
      this.session.currentUser?.twoFactorEnabled
        ? 'disable'
        : 'enable';

    this.twoFactorSubmitting.set(true);

    const request$ =
      action === 'enable'
        ? this.auth.requestTwoFactorEnable(
            this.twoFactorPasswordForm.getRawValue(),
          )
        : this.auth.requestTwoFactorDisable(
            this.twoFactorPasswordForm.getRawValue(),
          );

    request$
      .pipe(
        finalize(() => this.twoFactorSubmitting.set(false)),
      )
      .subscribe({
        next: (response) => {
          this.pendingAction.set(action);
          this.challengeId.set(response.challengeId);

          this.twoFactorPasswordForm.reset();
          this.twoFactorSubmitted.set(false);

          this.twoFactorSuccess.set(response.message);
        },
        error: (error: unknown) => {
          this.twoFactorError.set(
            this.errors.getMessage(error),
          );
        },
      });
  }

  confirmTwoFactorChange(): void {
    this.twoFactorSubmitted.set(true);
    this.twoFactorError.set(null);
    this.twoFactorSuccess.set(null);

    const action = this.pendingAction();
    const challengeId = this.challengeId();

    if (!action || !challengeId) {
      this.cancelTwoFactorChallenge();
      return;
    }

    if (this.twoFactorOtpForm.invalid) {
      this.twoFactorOtpForm.markAllAsTouched();
      return;
    }

    this.twoFactorSubmitting.set(true);

    const request = {
      challengeId,
      otp: this.twoFactorOtpForm.getRawValue().otp,
    };

    const verification$ =
      action === 'enable'
        ? this.auth.confirmTwoFactorEnable(request)
        : this.auth.confirmTwoFactorDisable(request);

    verification$.subscribe({
      next: (response) => {
        this.auth.loadCurrentUser().subscribe({
          next: () => {
            this.twoFactorSuccess.set(response.message);
            this.resetTwoFactorFlow();
            this.twoFactorSubmitting.set(false);
          },
          error: (error: unknown) => {
            this.twoFactorError.set(
              this.errors.getMessage(error),
            );
            this.twoFactorSubmitting.set(false);
          },
        });
      },
      error: (error: unknown) => {
        this.twoFactorError.set(
          this.errors.getMessage(error),
        );
        this.twoFactorSubmitting.set(false);
      },
    });
  }

  cancelTwoFactorChallenge(): void {
    this.resetTwoFactorFlow();
    this.twoFactorError.set(null);
    this.twoFactorSuccess.set(null);
  }

  private resetTwoFactorFlow(): void {
    this.pendingAction.set(null);
    this.challengeId.set(null);
    this.twoFactorOtpForm.reset();
    this.twoFactorPasswordForm.reset();
    this.twoFactorSubmitted.set(false);
  }
}
