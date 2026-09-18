import { Component, computed, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';
import { Role } from '../../core/models/auth.models';
import { AuthSessionService } from '../../core/services/auth-session.service';
import { BrandMarkComponent } from '../../shared/components/brand-mark/brand-mark';

interface NavigationItem {
  path: string;
  labelKey: string;
  icon: string;
  roles?: readonly Role[];
}

const NAVIGATION: readonly NavigationItem[] = [
  {
    path: '/app/dashboard',
    labelKey: 'nav.dashboard',
    icon: 'pi-home',
  },
  {
    path: '/app/mesas',
    labelKey: 'nav.tables',
    icon: 'pi-th-large',
    roles: ['WAITER'],
  },
  {
    path: '/app/cocina',
    labelKey: 'nav.kitchen',
    icon: 'pi-bolt',
    roles: ['KITCHEN', 'ADMIN'],
  },
  {
    path: '/app/caja/turno',
    labelKey: 'nav.cashShift',
    icon: 'pi-credit-card',
    roles: ['CASHIER'],
  },
  {
    path: '/app/caja/cobros',
    labelKey: 'nav.billing',
    icon: 'pi-receipt',
    roles: ['CASHIER'],
  },

  {
    path: '/app/caja/facturas',
    labelKey: 'nav.invoices',
    icon: 'pi-file',
    roles: ['CASHIER'],
  },


  {
    path: '/app/account',
    labelKey: 'nav.account',
    icon: 'pi-user',
  },
];

@Component({
  selector: 'app-shell',
  imports: [BrandMarkComponent, RouterLink, RouterLinkActive, RouterOutlet, TranslocoPipe],
  templateUrl: './app-shell.html',
  styleUrl: './app-shell.scss',
})
export class AppShellComponent {
  readonly session = inject(AuthSessionService);
  private readonly router = inject(Router);

  readonly mobileMenuOpen = signal(false);

  readonly navigation = computed(() =>
    NAVIGATION.filter((item) => this.session.hasRole(item.roles)),
  );

  roleKey(): string {
    return `roles.${this.session.currentUser?.role ?? 'WAITER'}`;
  }

  initials(): string {
    const email = this.session.currentUser?.email ?? '';
    return email.slice(0, 2).toUpperCase();
  }

  closeMobileMenu(): void {
    this.mobileMenuOpen.set(false);
  }

  toggleMobileMenu(): void {
    this.mobileMenuOpen.update((open) => !open);
  }

  logout(): void {
    this.session.clear();
    void this.router.navigate(['/auth/login']);
  }
}
