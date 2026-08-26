import { Component, inject, OnInit, signal } from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';
import { finalize } from 'rxjs';
import { ApiErrorService } from '../../../core/services/api-error.service';
import { AuthService } from '../../../core/services/auth.service';
import { FormFeedbackComponent } from '../../../shared/components/form-feedback/form-feedback';
import { PageHeadingComponent } from '../../../shared/components/page-heading/page-heading';

@Component({
  selector: 'app-admin-page',
  imports: [FormFeedbackComponent, PageHeadingComponent, TranslocoPipe],
  templateUrl: './admin.html',
  styleUrl: './admin.scss',
})
export class AdminPageComponent implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly errors = inject(ApiErrorService);

  readonly isLoading = signal(true);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);

  ngOnInit(): void {
    this.auth
      .adminPing()
      .pipe(finalize(() => this.isLoading.set(false)))
      .subscribe({
        next: (response) => this.successMessage.set(response.message),
        error: (error: unknown) => this.errorMessage.set(this.errors.getMessage(error)),
      });
  }
}
