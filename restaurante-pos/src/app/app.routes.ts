import { Routes } from '@angular/router';
import { authChildGuard, authGuard } from './core/guards/auth.guard';
import { guestChildGuard, guestGuard } from './core/guards/guest.guard';
import { roleGuard } from './core/guards/role.guard';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'app/dashboard',
  },
  {
    path: 'auth',
    loadComponent: () =>
      import('./layouts/public-layout/public-layout').then(
        (module) => module.PublicLayoutComponent,
      ),
    canActivate: [guestGuard],
    canActivateChild: [guestChildGuard],
    children: [
      {
        path: '',
        pathMatch: 'full',
        redirectTo: 'login',
      },
      {
        path: 'login',
        loadComponent: () =>
          import('./pages/auth/login/login').then(
            (module) => module.LoginPageComponent,
          ),
      },
      {
        path: 'login-verify',
        loadComponent: () =>
          import('./pages/auth/login-verify/login-verify').then(
            (module) => module.LoginVerifyPageComponent,
          ),
      },
      {
        path: 'recovery',
        loadComponent: () =>
          import('./pages/auth/recovery/recovery').then(
            (module) => module.RecoveryPageComponent,
          ),
      },
      {
        path: 'recovery-reset',
        loadComponent: () =>
          import('./pages/auth/recovery-reset/recovery-reset').then(
            (module) => module.RecoveryResetPageComponent,
          ),
      },
    ],
  },
  {
    path: 'app',
    loadComponent: () =>
      import('./layouts/app-shell/app-shell').then(
        (module) => module.AppShellComponent,
      ),
    canActivate: [authGuard, roleGuard],
    canActivateChild: [authChildGuard],
    data: {
      roles: ['WAITER', 'KITCHEN', 'CASHIER'],
    },
    children: [
      {
        path: '',
        pathMatch: 'full',
        redirectTo: 'dashboard',
      },
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./pages/protected/dashboard/dashboard').then(
            (module) => module.DashboardPageComponent,
          ),
      },
      {
        path: 'account',
        loadComponent: () =>
          import('./pages/protected/security/security').then(
            (module) => module.SecurityPageComponent,
          ),
      },
    ],
  },
  {
    path: 'unauthorized',
    loadComponent: () =>
      import('./pages/unauthorized/unauthorized').then(
        (module) => module.UnauthorizedPageComponent,
      ),
    canActivate: [authGuard],
  },
  {
    path: '**',
    redirectTo: 'app/dashboard',
  },
];
