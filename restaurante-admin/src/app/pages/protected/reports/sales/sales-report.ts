import { DecimalPipe } from '@angular/common';
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

import { SalesReport } from '../../../../core/models/sales-report.models';
import { ApiErrorService } from '../../../../core/services/api-error.service';
import { SalesReportService } from '../../../../core/services/sales-report.service';
import { ReportExportActionsComponent } from '../../../../shared/components/report-export-actions/report-export-actions';

@Component({
  selector: 'app-sales-report-page',
  imports: [
    DecimalPipe,
    ReactiveFormsModule,
    TranslocoPipe,
    ReportExportActionsComponent,
  ],
  templateUrl: './sales-report.html',
  styleUrl: './sales-report.scss',
})
export class SalesReportPageComponent {
  private readonly salesReportService =
    inject(SalesReportService);

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
    signal<SalesReport | null>(null);

  readonly loading =
    signal(false);

  readonly generated =
    signal(false);

  readonly periodError =
    signal<string | null>(null);

  readonly errorMessage =
    signal<string | null>(null);

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
          'salesReport.errors.invalidPeriod',
        ),
      );

      return;
    }

    this.loading.set(true);

    this.salesReportService
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