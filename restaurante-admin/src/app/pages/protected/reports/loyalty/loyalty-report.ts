import {
  Component,
  inject,
  signal,
} from '@angular/core';
import {
  FormControl,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import {
  TranslocoPipe,
  TranslocoService,
} from '@jsverse/transloco';
import { finalize } from 'rxjs';

import {
  LoyaltyReport,
} from '../../../../core/models/loyalty-report.models';
import {
  ApiErrorService,
} from '../../../../core/services/api-error.service';
import {
  LoyaltyReportService,
} from '../../../../core/services/loyalty-report.service';
import {
  ReportExportActionsComponent,
} from '../../../../shared/components/report-export-actions/report-export-actions';

@Component({
  selector: 'app-loyalty-report-page',
  imports: [
    ReactiveFormsModule,
    TranslocoPipe,
    ReportExportActionsComponent,
  ],
  templateUrl: './loyalty-report.html',
  styleUrl: './loyalty-report.scss',
})
export class LoyaltyReportPageComponent {
  private readonly loyaltyService =
    inject(LoyaltyReportService);

  private readonly apiErrors =
    inject(ApiErrorService);

  private readonly transloco =
    inject(TranslocoService);

  readonly startDateControl =
    new FormControl<string>(
      '',
      {
        nonNullable: true,
        validators: [
          Validators.required,
        ],
      },
    );

  readonly endDateControl =
    new FormControl<string>(
      '',
      {
        nonNullable: true,
        validators: [
          Validators.required,
        ],
      },
    );

  readonly report =
    signal<LoyaltyReport | null>(
      null,
    );

  readonly loading =
    signal(false);

  readonly generated =
    signal(false);

  readonly periodError =
    signal<string | null>(
      null,
    );

  readonly errorMessage =
    signal<string | null>(
      null,
    );

  generateReport(): void {
    this.startDateControl.markAsTouched();
    this.endDateControl.markAsTouched();

    this.periodError.set(null);
    this.errorMessage.set(null);

    if (
      this.startDateControl.invalid ||
      this.endDateControl.invalid
    ) {
      return;
    }

    const startDate =
      this.startDateControl.value;

    const endDate =
      this.endDateControl.value;

    if (startDate > endDate) {
      this.report.set(null);
      this.generated.set(false);

      this.periodError.set(
        this.transloco.translate(
          'loyaltyReport.errors.invalidPeriod',
        ),
      );

      return;
    }

    this.loading.set(true);

    this.loyaltyService
      .getReport(
        startDate,
        endDate,
      )
      .pipe(
        finalize(() =>
          this.loading.set(false),
        ),
      )
      .subscribe({
        next: (report) => {
          this.report.set(report);
          this.generated.set(true);
        },
        error: (error: unknown) => {
          this.report.set(null);
          this.generated.set(false);

          this.errorMessage.set(
            this.apiErrors.getMessage(error),
          );
        },
      });
  }

  formatDate(
    value: string,
  ): string {
    const parts =
      value.split('-');

    if (parts.length !== 3) {
      return value;
    }

    return (
      `${parts[2]}/` +
      `${parts[1]}/` +
      `${parts[0]}`
    );
  }
}