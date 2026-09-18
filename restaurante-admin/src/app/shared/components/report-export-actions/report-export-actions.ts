import {
  Component,
  Input,
  inject,
  signal,
} from '@angular/core';
import {
  TranslocoPipe,
  TranslocoService,
} from '@jsverse/transloco';
import { finalize } from 'rxjs';

import {
  ReportExportFormat,
  ReportType,
} from '../../../core/models/report-export.models';
import {
  ReportExportService,
} from '../../../core/services/report-export.service';

@Component({
  selector: 'app-report-export-actions',
  imports: [
    TranslocoPipe,
  ],
  templateUrl:
    './report-export-actions.html',
  styleUrl:
    './report-export-actions.scss',
})
export class ReportExportActionsComponent {
  private readonly exportService =
    inject(ReportExportService);

  private readonly transloco =
    inject(TranslocoService);

  @Input({
    required: true,
  })
  reportType!: ReportType;

  @Input()
  startDate: string | null = null;

  @Input()
  endDate: string | null = null;

  @Input()
  disabled = false;

  readonly exporting =
    signal<ReportExportFormat | null>(
      null,
    );

  readonly errorMessage =
    signal<string | null>(null);

  exportReport(
    format: ReportExportFormat,
  ): void {
    if (
      this.disabled ||
      this.exporting() !== null
    ) {
      return;
    }

    this.errorMessage.set(null);
    this.exporting.set(format);

    const request = {
      tipo: this.reportType,
      formato: format,
      fechaInicio: this.startDate,
      fechaFin: this.endDate,
    };

    this.exportService
      .exportReport(request)
      .pipe(
        finalize(() =>
          this.exporting.set(null),
        ),
      )
      .subscribe({
        next: (response) => {
          try {
            this.exportService.download(
              response,
              request,
            );
          } catch {
            this.errorMessage.set(
              this.transloco.translate(
                'reportExport.errors.failed',
              ),
            );
          }
        },
        error: () => {
          this.errorMessage.set(
            this.transloco.translate(
              'reportExport.errors.failed',
            ),
          );
        },
      });
  }
}