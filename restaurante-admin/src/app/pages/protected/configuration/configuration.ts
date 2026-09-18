import {
  Component,
  computed,
  inject,
  OnInit,
  signal,
} from '@angular/core';
import {
  FormBuilder,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import {
  TranslocoPipe,
  TranslocoService,
} from '@jsverse/transloco';
import { MessageService } from 'primeng/api';
import { finalize } from 'rxjs';

import {
  PointsAccumulationConfiguration,
  TipConfiguration,
} from '../../../core/models/configuration.models';
import { ApiErrorService } from '../../../core/services/api-error.service';
import { RestaurantConfigurationService } from '../../../core/services/restaurant-configuration.service';
import { FormFeedbackComponent } from '../../../shared/components/form-feedback/form-feedback';
import { PageHeadingComponent } from '../../../shared/components/page-heading/page-heading';

@Component({
  selector: 'app-configuration-page',
  imports: [
    FormFeedbackComponent,
    PageHeadingComponent,
    ReactiveFormsModule,
    TranslocoPipe,
  ],
  templateUrl: './configuration.html',
  styleUrl: './configuration.scss',
})
export class ConfigurationPageComponent implements OnInit {
  private readonly formBuilder = inject(FormBuilder);
  private readonly configurationService =
    inject(RestaurantConfigurationService);
  private readonly errors = inject(ApiErrorService);
  private readonly messages = inject(MessageService);
  private readonly transloco = inject(TranslocoService);

  readonly configuration =
    signal<TipConfiguration | null>(null);

  readonly isLoading = signal(true);
  readonly isSaving = signal(false);
  readonly submitted = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly tipForm = this.formBuilder.nonNullable.group({
    porcentaje: [
      0,
      [
        Validators.required,
        Validators.min(0),
        Validators.max(100),
      ],
    ],
  });

  readonly pointsConfiguration =
  signal<PointsAccumulationConfiguration | null>(null);

    readonly isPointsLoading = signal(true);
    readonly isPointsSaving = signal(false);
    readonly pointsSubmitted = signal(false);
    readonly pointsErrorMessage = signal<string | null>(null);

  readonly isTipEnabled = computed(
    () => (this.configuration()?.porcentaje ?? 0) > 0,
  );

  formatDate(value: string | null): string {
    if (!value) {
      return '-';
    }

    const date = new Date(value);

    if (Number.isNaN(date.getTime())) {
      return '-';
    }

    return new Intl.DateTimeFormat('es-GT', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      hourCycle: 'h23',
    }).format(date);
  }


  readonly pointsForm = this.formBuilder.nonNullable.group({
  puntosPorMoneda: [
    1,
    [
      Validators.required,
      Validators.min(0.0001),
    ],
  ],
});

  ngOnInit(): void {
  this.loadConfiguration();
  this.loadPointsConfiguration();
}

  loadConfiguration(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.configurationService
      .getTipConfiguration()
      .pipe(finalize(() => this.isLoading.set(false)))
      .subscribe({
        next: (configuration) => {
          this.configuration.set(configuration);

          this.tipForm.patchValue({
            porcentaje: configuration.porcentaje,
          });
        },
        error: (error: unknown) => {
          this.errorMessage.set(
            this.errors.getMessage(error),
          );
        },
      });
  }

  saveTipConfiguration(): void {
    this.submitted.set(true);
    this.errorMessage.set(null);

    if (this.tipForm.invalid) {
      this.tipForm.markAllAsTouched();
      return;
    }

    const porcentaje =
      Number(this.tipForm.getRawValue().porcentaje);

    this.isSaving.set(true);

    this.configurationService
      .updateTipConfiguration({ porcentaje })
      .pipe(finalize(() => this.isSaving.set(false)))
      .subscribe({
        next: (configuration) => {
          this.configuration.set(configuration);

          this.tipForm.patchValue({
            porcentaje: configuration.porcentaje,
          });

          this.submitted.set(false);

          this.messages.add({
            severity: 'success',
            summary: this.transloco.translate(
              'configuration.tip.successTitle',
            ),
            detail: this.transloco.translate(
              porcentaje === 0
                ? 'configuration.tip.disabledSuccess'
                : 'configuration.tip.successMessage',
              {
                percentage: configuration.porcentaje,
              },
            ),
            life: 5000,
          });
        },
        error: (error: unknown) => {
          this.errors.present(error);
        },
      });
  }

  percentageInvalid(): boolean {
    const control = this.tipForm.controls.porcentaje;

    return (
      control.invalid &&
      (control.touched || this.submitted())
    );
  }

  loadPointsConfiguration(): void {
  this.isPointsLoading.set(true);
  this.pointsErrorMessage.set(null);

  this.configurationService
    .getPointsAccumulationConfiguration()
    .pipe(finalize(() => this.isPointsLoading.set(false)))
    .subscribe({
      next: (configuration) => {
        this.pointsConfiguration.set(configuration);

        this.pointsForm.patchValue({
          puntosPorMoneda: configuration.puntosPorMoneda,
        });
      },
      error: (error: unknown) => {
        this.pointsErrorMessage.set(
          this.errors.getMessage(error),
        );
      },
    });
}

savePointsConfiguration(): void {
  this.pointsSubmitted.set(true);
  this.pointsErrorMessage.set(null);

  if (this.pointsForm.invalid) {
    this.pointsForm.markAllAsTouched();
    return;
  }

  const puntosPorMoneda = Number(
    this.pointsForm.getRawValue().puntosPorMoneda,
  );

  this.isPointsSaving.set(true);

  this.configurationService
    .updatePointsAccumulationConfiguration({
      puntosPorMoneda,
    })
    .pipe(
      finalize(() => this.isPointsSaving.set(false)),
    )
    .subscribe({
      next: (configuration) => {
        this.pointsConfiguration.set(configuration);

        this.pointsForm.patchValue({
          puntosPorMoneda:
            configuration.puntosPorMoneda,
        });

        this.pointsSubmitted.set(false);

        this.messages.add({
          severity: 'success',
          summary: this.transloco.translate(
            'configuration.loyalty.accumulation.successTitle',
          ),
          detail: this.transloco.translate(
            'configuration.loyalty.accumulation.successMessage',
            {
              points: configuration.puntosPorMoneda,
            },
          ),
          life: 5000,
        });
      },
      error: (error: unknown) => {
        this.errors.present(error);
      },
    });
}

pointsPerCurrencyInvalid(): boolean {
  const control =
    this.pointsForm.controls.puntosPorMoneda;

  return (
    control.invalid &&
    (control.touched || this.pointsSubmitted())
  );
}
}
