import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { environment } from '../../../environments/environment';
import { AuthSessionService } from '../services/auth-session.service';

const PUBLIC_AUTH_PATHS = [
  '/auth/login',
  '/auth/login/verify',
  '/auth/password-recovery',
  '/auth/password-recovery/verify',
];

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const session = inject(AuthSessionService);
  const token = session.getToken();

  const isApiRequest = request.url.startsWith(environment.apiBaseUrl);
  const isPublicAuthRequest = PUBLIC_AUTH_PATHS.some((path) =>
    request.url.endsWith(path),
  );

  if (!token || !isApiRequest || isPublicAuthRequest) {
    return next(request);
  }

  return next(
    request.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`,
      },
    }),
  );
};
