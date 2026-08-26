import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiErrorService } from '../services/api-error.service';
import { AuthSessionService } from '../services/auth-session.service';

const PUBLIC_AUTH_PATHS = [
  '/auth/login',
  '/auth/login/verify',
  '/auth/password-recovery',
  '/auth/password-recovery/verify',
];

export const apiErrorInterceptor: HttpInterceptorFn = (request, next) => {
  const errors = inject(ApiErrorService);
  const session = inject(AuthSessionService);
  const router = inject(Router);

  const isApiRequest = request.url.startsWith(environment.apiBaseUrl);

  return next(request).pipe(
    catchError((error: unknown) => {
      if (!(error instanceof HttpErrorResponse) || !isApiRequest) {
        return throwError(() => error);
      }

      const isPublicAuthRequest = PUBLIC_AUTH_PATHS.some((path) =>
        request.url.endsWith(path),
      );

      if (
        error.status === 401 &&
        errors.isAuthenticationFailure(error) &&
        session.isAuthenticated() &&
        !isPublicAuthRequest
      ) {
        const returnUrl =
          router.url.startsWith('/auth') ? '/app/dashboard' : router.url;

        session.clear();

        void router.navigate(['/auth/login'], {
          queryParams: { returnUrl },
        });
      }

      errors.present(error);
      return throwError(() => error);
    }),
  );
};
