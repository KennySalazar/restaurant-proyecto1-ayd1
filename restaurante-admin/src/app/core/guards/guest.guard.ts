import { CanActivateChildFn, CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthSessionService } from '../services/auth-session.service';

function checkGuestAccess() {
  const session = inject(AuthSessionService);
  const router = inject(Router);

  return session.isAuthenticated() ? router.createUrlTree(['/app/dashboard']) : true;
}

export const guestGuard: CanActivateFn = () => checkGuestAccess();

export const guestChildGuard: CanActivateChildFn = () => checkGuestAccess();
