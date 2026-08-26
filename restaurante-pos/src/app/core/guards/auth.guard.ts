import { CanActivateChildFn, CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthSessionService } from '../services/auth-session.service';

function checkAuthentication(url: string) {
  const session = inject(AuthSessionService);
  const router = inject(Router);

  if (session.isAuthenticated()) {
    return true;
  }

  return router.createUrlTree(['/auth/login'], {
    queryParams: { returnUrl: url },
  });
}

export const authGuard: CanActivateFn = (_route, state) => checkAuthentication(state.url);

export const authChildGuard: CanActivateChildFn = (_childRoute, state) =>
  checkAuthentication(state.url);
