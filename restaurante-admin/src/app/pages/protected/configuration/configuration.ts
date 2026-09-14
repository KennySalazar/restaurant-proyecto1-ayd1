import {
  Component,
  computed,
  inject,
  OnInit,
  signal,
} from '@angular/core';
import { DatePipe } from '@angular/common';
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

import { TipConfiguration } from '../../../core/models/configuration.models';
import { ApiErrorService } from '../../../core/services/api-error.service';
import { RestaurantConfigurationService } from '../../../core/services/restaurant-configuration.service';
import { FormFeedbackComponent } from '../../../shared/components/form-feedback/form-feedback';
import { PageHeadingComponent } from '../../../shared/components/page-heading/page-heading';

@Component({
  selector: 'app-configuration-page',
  imports: [
    DatePipe,
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

  readonly isTipEnabled = computed(
    () => (this.configuration()?.porcentaje ?? 0) > 0,
  );

  ngOnInit(): void {
    this.loadConfiguration();
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
}