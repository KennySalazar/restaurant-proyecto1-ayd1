import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';
import { InputTextModule } from 'primeng/inputtext';
import { finalize, switchMap } from 'rxjs';
import { ApiErrorService } from '../../../core/services/api-error.service';
import { AuthFlowService } from '../../../core/services/auth-flow.service';
import { AuthService } from '../../../core/services/auth.service';
import { FieldErrorComponent } from '../../../shared/components/field-error/field-error';
import { FormFeedbackComponent } from '../../../shared/components/form-feedback/form-feedback';

@Component({
  selector: 'app-login-verify-page',
  imports: [
    FieldErrorComponent,
    FormFeedbackComponent,
    InputTextModule,
    ReactiveFormsModule,
    TranslocoPipe,
  ],
  templateUrl: './login-verify.html',
  styleUrls: [
    '../login/login.scss',
    './login-verify.scss',
  ],
})
export class LoginVerifyPageComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly authFlow = inject(AuthFlowService);
  private readonly errors = inject(ApiErrorService);
  private readonly router = inject(Router);

  readonly submitted = signal(false);
  readonly isSubmitting = signal(false);
  readonly serverMessage = signal<string | null>(null);

  readonly challenge = this.authFlow.getLoginChallenge();

  readonly form = this.formBuilder.nonNullable.group({
    otp: [
      '',
      [
        Validators.required,
        Validators.pattern(/^\d{6}$/),
      ],
    ],
  });

  constructor() {
    if (!this.challenge) {
      void this.router.navigate(['/auth/login']);
      return;
    }

    if (new Date(this.challenge.expiresAt).getTime() <= Date.now()) {
      this.authFlow.clearLoginChallenge();

      void this.router.navigate(['/auth/login']);
    }
  }

  submit(): void {
    this.submitted.set(true);
    this.serverMessage.set(null);

    if (!this.challenge) {
      void this.router.navigate(['/auth/login']);
      return;
    }

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);

    this.auth
      .verifyLogin({
        challengeId: this.challenge.challengeId,
        otp: this.form.getRawValue().otp,
      })
      .pipe(
        switchMap((response) =>
          this.auth.establishLogin(response),
        ),
        finalize(() => this.isSubmitting.set(false)),
      )
      .subscribe({
        next: () => {
          const returnUrl = this.challenge?.returnUrl
            || '/app/dashboard';

          this.authFlow.clearLoginChallenge();

          void this.router.navigateByUrl(returnUrl);
        },
        error: (error: unknown) => {
          this.serverMessage.set(
            this.errors.getMessage(error),
          );
        },
      });
  }

  backToLogin(): void {
    this.authFlow.clearLoginChallenge();

    void this.router.navigate(['/auth/login']);
  }
}
