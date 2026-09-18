import { DecimalPipe } from '@angular/common';
import {
  Component,
  OnInit,
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
  CurrentDishProfitability,
  HistoricalProfitabilityReport,
} from '../../../../core/models/profitability-report.models';
import { ApiErrorService } from '../../../../core/services/api-error.service';
import { ProfitabilityReportService } from '../../../../core/services/profitability-report.service';
import { ReportExportActionsComponent } from '../../../../shared/components/report-export-actions/report-export-actions';

@Component({
  selector: 'app-profitability-report-page',
  imports: [
    DecimalPipe,
    ReactiveFormsModule,
    TranslocoPipe,
    ReportExportActionsComponent,
  ],
  templateUrl: './profitability-report.html',
  styleUrl: './profitability-report.scss',
})
export class ProfitabilityReportPageComponent
  implements OnInit {

  private readonly profitabilityService =
    inject(ProfitabilityReportService);

  private readonly apiErrors =
    inject(ApiErrorService);

  private readonly transloco =
    inject(TranslocoService);

  readonly currentDishes =
    signal<CurrentDishProfitability[]>([]);

  readonly currentLoading =
    signal(false);

  readonly currentGenerated =
    signal(false);

  readonly currentError =
    signal<string | null>(null);

  readonly historicalReport =
    signal<HistoricalProfitabilityReport | null>(
      null,
    );

  readonly historicalLoading =
    signal(false);

  readonly historicalGenerated =
    signal(false);

  readonly historicalError =
    signal<string | null>(null);

  readonly periodError =
    signal<string | null>(null);

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

  readonly calculableCount =
    computed(
      () =>
        this.currentDishes().filter(
          (dish) =>
            dish.rentabilidadCalculable,
        ).length,
    );

  readonly positiveProfitCount =
    computed(
      () =>
        this.currentDishes().filter(
          (dish) =>
            dish.rentabilidadCalculable &&
            dish.gananciaUnitaria !== null &&
            dish.gananciaUnitaria > 0,
        ).length,
    );

  readonly nonPositiveProfitCount =
    computed(
      () =>
        this.currentDishes().filter(
          (dish) =>
            dish.rentabilidadCalculable &&
            dish.gananciaUnitaria !== null &&
            dish.gananciaUnitaria <= 0,
        ).length,
    );

  readonly notCalculableCount =
    computed(
      () =>
        this.currentDishes().filter(
          (dish) =>
            !dish.rentabilidadCalculable,
        ).length,
    );

  ngOnInit(): void {
    this.loadCurrentProfitability();
  }

  loadCurrentProfitability(): void {
    if (this.currentLoading()) {
      return;
    }

    this.currentLoading.set(true);
    this.currentError.set(null);

    this.profitabilityService
      .getCurrentProfitability()
      .pipe(
        finalize(() =>
          this.currentLoading.set(false),
        ),
      )
      .subscribe({
        next: (dishes) => {
          this.currentDishes.set(dishes);
          this.currentGenerated.set(true);
        },
        error: (error: unknown) => {
          this.currentDishes.set([]);
          this.currentGenerated.set(false);

          this.currentError.set(
            this.apiErrors.getMessage(error),
          );
        },
      });
  }

  generateHistoricalReport(): void {
    this.startDateControl.markAsTouched();
    this.endDateControl.markAsTouched();

    this.periodError.set(null);
    this.historicalError.set(null);

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
      this.historicalReport.set(null);
      this.historicalGenerated.set(false);

      this.periodError.set(
        this.transloco.translate(
          'profitabilityReport.errors.invalidPeriod',
        ),
      );

      return;
    }

    this.historicalLoading.set(true);

    this.profitabilityService
      .getHistoricalProfitability(
        startDate,
        endDate,
      )
      .pipe(
        finalize(() =>
          this.historicalLoading.set(false),
        ),
      )
      .subscribe({
        next: (report) => {
          this.historicalReport.set(report);
          this.historicalGenerated.set(true);
        },
        error: (error: unknown) => {
          this.historicalReport.set(null);
          this.historicalGenerated.set(false);

          this.historicalError.set(
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

  hasPositiveProfit(
    dish: CurrentDishProfitability,
  ): boolean {
    return (
      dish.rentabilidadCalculable &&
      dish.gananciaUnitaria !== null &&
      dish.gananciaUnitaria > 0
    );
  }

  hasNonPositiveProfit(
    dish: CurrentDishProfitability,
  ): boolean {
    return (
      dish.rentabilidadCalculable &&
      dish.gananciaUnitaria !== null &&
      dish.gananciaUnitaria <= 0
    );
  }
}