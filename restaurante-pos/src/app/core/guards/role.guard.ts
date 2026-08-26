import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { Role } from '../models/auth.models';
import { AuthSessionService } from '../services/auth-session.service';

export const roleGuard: CanActivateFn = (route) => {
  const session = inject(AuthSessionService);
  const router = inject(Router);
  const roles = (route.data['roles'] as Role[] | undefined) ?? [];

  return session.hasRole(roles) ? true : router.createUrlTree(['/unauthorized']);
};
