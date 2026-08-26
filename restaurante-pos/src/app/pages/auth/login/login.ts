import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import {
  ActivatedRoute,
  Router,
  RouterLink,
} from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';
import { InputTextModule } from 'primeng/inputtext';
import { finalize } from 'rxjs';
import { LoginResponse } from '../../../core/models/auth.models';
import { ApiErrorService } from '../../../core/services/api-error.service';
import { AuthFlowService } from '../../../core/services/auth-flow.service';
import { AuthService } from '../../../core/services/auth.service';
import { FieldErrorComponent } from '../../../shared/components/field-error/field-error';
import { FormFeedbackComponent } from '../../../shared/components/form-feedback/form-feedback';

@Component({
  selector: 'app-login-page',
  imports: [
    FieldErrorComponent,
    FormFeedbackComponent,
    InputTextModule,
    ReactiveFormsModule,
    TranslocoPipe,
    RouterLink,
  ],
  templateUrl: './login.html',
  styleUrl: './login.scss',
})
export class LoginPageComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly authFlow = inject(AuthFlowService);
  private readonly errors = inject(ApiErrorService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  readonly submitted = signal(false);
  readonly isSubmitting = signal(false);
  readonly serverMessage = signal<string | null>(null);

  readonly form = this.formBuilder.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]],
  });

  submit(): void {
    this.submitted.set(true);
    this.serverMessage.set(null);

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);

    this.auth
      .login(this.form.getRawValue())
      .pipe(
        finalize(() => this.isSubmitting.set(false)),
      )
      .subscribe({
        next: (response) => {
          this.handleLoginResponse(response);
        },
        error: (error: unknown) => {
          this.serverMessage.set(
            this.errors.getMessage(error),
          );
        },
      });
  }

  private handleLoginResponse(response: LoginResponse): void {
    if (response.requiresTwoFactor) {
      if (
        !response.challengeId ||
        !response.challengeExpiresAt
      ) {
        this.serverMessage.set(
          'No fue posible iniciar la verificación de dos factores.',
        );
        return;
      }

      this.authFlow.setLoginChallenge({
        challengeId: response.challengeId,
        expiresAt: response.challengeExpiresAt,
        returnUrl: this.targetAfterLogin(),
      });

      void this.router.navigate([
        '/auth/login-verify',
      ]);

      return;
    }

    this.auth.establishLogin(response).subscribe({
      next: () => {
        void this.router.navigateByUrl(
          this.targetAfterLogin(),
        );
      },
      error: (error: unknown) => {
        this.serverMessage.set(
          this.errors.getMessage(error),
        );
      },
    });
  }

  private targetAfterLogin(): string {
    const returnUrl =
      this.route.snapshot.queryParamMap.get('returnUrl');

    return returnUrl?.startsWith('/')
      ? returnUrl
      : '/app/dashboard';
  }
}
