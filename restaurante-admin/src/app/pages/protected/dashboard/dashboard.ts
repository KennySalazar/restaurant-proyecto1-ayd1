import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';
import { AuthSessionService } from '../../../core/services/auth-session.service';
import { PageHeadingComponent } from '../../../shared/components/page-heading/page-heading';

@Component({
  selector: 'app-dashboard-page',
  imports: [PageHeadingComponent, RouterLink, TranslocoPipe],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class DashboardPageComponent {
  readonly session = inject(AuthSessionService);

  roleKey(): string {
    return `roles.${this.session.currentUser?.role ?? 'ADMIN'}`;
  }
}
