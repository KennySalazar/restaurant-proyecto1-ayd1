import { Component, computed, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';
import { interval } from 'rxjs';
import { Role } from '../../core/models/auth.models';
import { Notification } from '../../core/models/notification.models';
import { AuthSessionService } from '../../core/services/auth-session.service';
import { NotificationService } from '../../core/services/notification.service';
import { BrandMarkComponent } from '../../shared/components/brand-mark/brand-mark';

interface NavigationItem {
  path: string;
  labelKey: string;
  icon: string;
  roles?: readonly Role[];
}

const NAVIGATION: readonly NavigationItem[] = [
  { path: '/app/dashboard', labelKey: 'nav.dashboard', icon: 'pi-home' },
  {
    path: '/app/supplies',
    labelKey: 'nav.supplies',
    icon: 'pi-box',
    roles: ['ADMIN'],
  },
  {
    path: '/app/menu',
    labelKey: 'nav.menu',
    icon: 'pi-book',
    roles: ['ADMIN'],
  },
  {
    path: '/app/supply-alerts',
    labelKey: 'nav.supplyAlerts',
    icon: 'pi-exclamation-triangle',
    roles: ['ADMIN'],
  },
  {
    path: '/app/recipes',
    labelKey: 'nav.recipes',
    icon: 'pi-receipt',
    roles: ['ADMIN'],
  },
  {
    path: '/app/tables',
    labelKey: 'nav.tables',
    icon: 'pi-list',
    roles: ['ADMIN'],
  },

  {
    path: '/app/occupancy',
    labelKey: 'nav.occupancy',
    icon: 'pi-chart-bar',
    roles: ['ADMIN'],
  },

  {
  path: '/app/reports/sales',
  labelKey: 'nav.salesReport',
  icon: 'pi-chart-line',
  roles: ['ADMIN'],
},

{
  path: '/app/reports/profitability',
  labelKey: 'nav.profitabilityReport',
  icon: 'pi-percentage',
  roles: ['ADMIN'],
},

{
  path: '/app/reports/table-occupancy',
  labelKey: 'nav.tableOccupancyReport',
  icon: 'pi-clock',
  roles: ['ADMIN'],
},

  {
    path: '/app/employees',
    labelKey: 'nav.employees',
    icon: 'pi-users',
    roles: ['ADMIN'],
  },

  {
    path: '/app/configuration',
    labelKey: 'nav.configuration',
    icon: 'pi-sliders-h',
    roles: ['ADMIN'],
  },

  {
    path: '/app/reservations',
    labelKey: 'nav.reservations',
    icon: 'pi-calendar',
    roles: ['ADMIN'],
  },

  {
    path: '/app/waitlist',
    labelKey: 'nav.waitlist',
    icon: 'pi-clock',
    roles: ['ADMIN'],
  },
  { path: '/app/security', labelKey: 'nav.security', icon: 'pi-shield' },
  {
    path: '/app/admin',
    labelKey: 'nav.admin',
    icon: 'pi-sliders-h',
    roles: ['ADMIN'],
  },
];

@Component({
  selector: 'app-shell',
  imports: [BrandMarkComponent, RouterLink, RouterLinkActive, RouterOutlet, TranslocoPipe],
  templateUrl: './app-shell.html',
  styleUrl: './app-shell.scss',
})
export class AppShellComponent implements OnInit {
  readonly session = inject(AuthSessionService);
  private readonly router = inject(Router);
  private readonly notificationService = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);

  readonly mobileMenuOpen = signal(false);

  readonly notifications = signal<Notification[]>([]);
  readonly unreadCount = signal(0);
  readonly notificationsOpen = signal(false);
  readonly isLoadingNotifications = signal(false);
  readonly isMarkingAllRead = signal(false);

  readonly navigation = computed(() =>
    NAVIGATION.filter((item) => this.session.hasRole(item.roles)),
  );

  ngOnInit(): void {
    this.refreshUnreadCount();

    interval(60000)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.refreshUnreadCount());
  }

  toggleNotifications(): void {
    if (this.notificationsOpen()) {
      this.closeNotifications();
      return;
    }

    this.notificationsOpen.set(true);
    this.refreshUnreadCount();
    this.loadNotifications();
  }

  closeNotifications(): void {
    this.notificationsOpen.set(false);
  }

  loadNotifications(): void {
    this.isLoadingNotifications.set(true);

    this.notificationService.list(false).subscribe({
      next: (notifications) => this.notifications.set(notifications),
      error: () => this.isLoadingNotifications.set(false),
      complete: () => this.isLoadingNotifications.set(false),
    });
  }

  refreshUnreadCount(): void {
    this.notificationService.unreadCount().subscribe({
      next: (count) => this.unreadCount.set(count.unreadCount),
    });
  }

  markNotificationRead(notification: Notification): void {
    if (notification.read) {
      return;
    }

    this.notificationService.markAsRead(notification.id).subscribe({
      next: (updated) => {
        this.notifications.update((list) =>
          list.map((item) => (item.id === updated.id ? updated : item)),
        );
        this.unreadCount.update((count) => Math.max(0, count - 1));
      },
    });
  }

  markAllNotificationsRead(): void {
    if (this.isMarkingAllRead()) {
      return;
    }

    this.isMarkingAllRead.set(true);

    this.notificationService.markAllAsRead().subscribe({
      next: () => {
        this.notifications.update((list) => list.map((item) => ({ ...item, read: true })));
        this.unreadCount.set(0);
      },
      error: () => this.isMarkingAllRead.set(false),
      complete: () => this.isMarkingAllRead.set(false),
    });
  }

  goToAlerts(): void {
    this.closeNotifications();
    void this.router.navigate(['/app/supply-alerts']);
  }

  formatNotificationTime(value: string): string {
    const date = new Date(value);

    if (Number.isNaN(date.getTime())) {
      return value;
    }

    return new Intl.DateTimeFormat('es-GT', {
      dateStyle: 'short',
      timeStyle: 'short',
    }).format(date);
  }

  roleKey(): string {
    return `roles.${this.session.currentUser?.role ?? 'ADMIN'}`;
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
