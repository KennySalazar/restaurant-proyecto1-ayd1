import {
  Component,
  computed,
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
  TableOccupancyReport,
  TableOccupancySlot,
} from '../../../../core/models/table-occupancy-report.models';
import { ApiErrorService } from '../../../../core/services/api-error.service';
import { TableOccupancyReportService } from '../../../../core/services/table-occupancy-report.service';
import { ReportExportActionsComponent } from '../../../../shared/components/report-export-actions/report-export-actions';

@Component({
  selector: 'app-table-occupancy-report-page',
  imports: [
    ReactiveFormsModule,
    TranslocoPipe,
    ReportExportActionsComponent,
  ],
  templateUrl: './table-occupancy-report.html',
  styleUrl: './table-occupancy-report.scss',
})
export class TableOccupancyReportPageComponent {
  private readonly tableOccupancyReportService =
    inject(TableOccupancyReportService);

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
    signal<TableOccupancyReport | null>(
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

  readonly slots =
    computed(
      () =>
        this.report()
          ?.ocupacionPorHorario ??
        [],
    );

  readonly maximumOccupancy =
    computed(() => {
      const values =
        this.slots().map(
          (slot) =>
            slot.mesasOcupadas,
        );

      if (values.length === 0) {
        return 0;
      }

      return Math.max(...values);
    });

  readonly minimumOccupancy =
    computed(() => {
      const values =
        this.slots().map(
          (slot) =>
            slot.mesasOcupadas,
        );

      if (values.length === 0) {
        return 0;
      }

      return Math.min(...values);
    });

  readonly uniformOccupancy =
    computed(
      () =>
        this.slots().length > 0 &&
        this.maximumOccupancy() ===
          this.minimumOccupancy(),
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
          'tableOccupancyReport.errors.invalidPeriod',
        ),
      );

      return;
    }

    this.loading.set(true);

    this.tableOccupancyReportService
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

  formatHour(
    hour: number,
  ): string {
    const start =
      String(hour).padStart(
        2,
        '0',
      );

    const endHour =
      (hour + 1) % 24;

    const end =
      String(endHour).padStart(
        2,
        '0',
      );

    return `${start}:00 - ${end}:00`;
  }

  isMaximum(
    slot: TableOccupancySlot,
  ): boolean {
    return (
      this.slots().length > 0 &&
      slot.mesasOcupadas ===
        this.maximumOccupancy()
    );
  }

  isMinimum(
    slot: TableOccupancySlot,
  ): boolean {
    return (
      this.slots().length > 0 &&
      slot.mesasOcupadas ===
        this.minimumOccupancy()
    );
  }

  occupancyPercentage(
    slot: TableOccupancySlot,
  ): number {
    const maximum =
      this.maximumOccupancy();

    if (maximum === 0) {
      return 0;
    }

    return (
      slot.mesasOcupadas /
      maximum
    ) * 100;
  }
}