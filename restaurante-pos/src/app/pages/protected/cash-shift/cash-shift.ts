import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import {
  FormBuilder,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { TranslocoPipe } from '@jsverse/transloco';
import { InputTextModule } from 'primeng/inputtext';
import { finalize } from 'rxjs';
import {
  CashRegisterAvailability,
  CashShiftResponse,
  CloseCashShiftResponse,
  CurrentCashShiftResponse,
} from '../../../core/models/cash-shift.models';
import { ApiErrorService } from '../../../core/services/api-error.service';
import { CashShiftService } from '../../../core/services/cash-shift.service';
import { FormFeedbackComponent } from '../../../shared/components/form-feedback/form-feedback';
import { PageHeadingComponent } from '../../../shared/components/page-heading/page-heading';

@Component({
  selector: 'app-cash-shift-page',
  imports: [
    DatePipe,
    DecimalPipe,
    FormFeedbackComponent,
    InputTextModule,
    PageHeadingComponent,
    ReactiveFormsModule,
    TranslocoPipe,
  ],
  templateUrl: './cash-shift.html',
  styleUrl: './cash-shift.scss',
})
export class CashShiftPageComponent implements OnInit {
  private readonly formBuilder = inject(FormBuilder);
  private readonly cashShiftService = inject(CashShiftService);
  private readonly errors = inject(ApiErrorService);

  readonly cashRegisters = signal<CashRegisterAvailability[]>([]);
  readonly currentShift = signal<CurrentCashShiftResponse | null>(null);
  readonly openedShift = signal<CashShiftResponse | null>(null);
  readonly closedShift = signal<CloseCashShiftResponse | null>(null);

  readonly loadingCurrentShift = signal(false);
  readonly loadingRegisters = signal(false);
  readonly submitting = signal(false);
  readonly closing = signal(false);

  readonly submitted = signal(false);
  readonly closeSubmitted = signal(false);

  readonly loadError = signal<string | null>(null);
  readonly errorMessage = signal<string | null>(null);
  readonly closeError = signal<string | null>(null);

  readonly form = this.formBuilder.group({
    cajaId: this.formBuilder.control<number | null>(
      null,
      Validators.required,
    ),
    montoInicialEfectivo: this.formBuilder.control<number | null>(
      null,
      [
        Validators.required,
        Validators.min(0),
      ],
    ),
    observaciones: this.formBuilder.nonNullable.control(''),
  });

  readonly closeForm = this.formBuilder.group({
    efectivoReal: this.formBuilder.control<number | null>(
      null,
      [
        Validators.required,
        Validators.min(0),
      ],
    ),
    observaciones: this.formBuilder.nonNullable.control(''),
  });

  ngOnInit(): void {
    this.loadCurrentShift();
  }

  hasAvailableRegisters(): boolean {
    return this.cashRegisters().some(
      (cashRegister) => cashRegister.disponible,
    );
  }

  submit(): void {
    this.submitted.set(true);
    this.errorMessage.set(null);

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();

    if (
      value.cajaId === null ||
      value.montoInicialEfectivo === null
    ) {
      return;
    }

    const selectedRegister = this.cashRegisters().find(
      (cashRegister) => cashRegister.cajaId === value.cajaId,
    );

    if (!selectedRegister?.disponible) {
      this.errorMessage.set(
        'La caja seleccionada ya no se encuentra disponible.',
      );
      return;
    }

    this.submitting.set(true);

    this.cashShiftService
      .openShift({
        cajaId: value.cajaId,
        montoInicialEfectivo: value.montoInicialEfectivo,
        observaciones:
          value.observaciones.trim() || null,
      })
      .pipe(
        finalize(() => this.submitting.set(false)),
      )
      .subscribe({
        next: (response) => {
          this.openedShift.set(response);

          this.cashRegisters.update((registers) =>
            registers.map((cashRegister) =>
              cashRegister.cajaId === response.cajaId
                ? {
                  ...cashRegister,
                  disponible: false,
                }
                : cashRegister,
            ),
          );

          this.form.disable();
        },
        error: (error: unknown) => {
          this.errorMessage.set(
            this.errors.getMessage(error),
          );
        },
      });
  }

  submitClose(): void {
    this.closeSubmitted.set(true);
    this.closeError.set(null);

    if (this.closeForm.invalid) {
      this.closeForm.markAllAsTouched();
      return;
    }

    const shift = this.currentShift();
    const value = this.closeForm.getRawValue();

    if (
      shift === null ||
      value.efectivoReal === null
    ) {
      return;
    }

    this.closing.set(true);

    this.cashShiftService
      .closeShift(
        shift.id,
        {
          efectivoReal: value.efectivoReal,
          observaciones:
            value.observaciones.trim() || null,
        },
      )
      .pipe(
        finalize(() => this.closing.set(false)),
      )
      .subscribe({
        next: (response) => {
          this.closedShift.set(response);
          this.currentShift.set(null);
          this.closeForm.disable();
        },
        error: (error: unknown) => {
          this.closeError.set(
            this.errors.getMessage(error),
          );
        },
      });
  }

  private loadCurrentShift(): void {
    this.loadingCurrentShift.set(true);
    this.loadError.set(null);

    this.cashShiftService
      .getCurrentShift()
      .pipe(
        finalize(() =>
          this.loadingCurrentShift.set(false),
        ),
      )
      .subscribe({
        next: (shift) => {
          this.currentShift.set(shift);
          this.cashRegisters.set([]);
        },
        error: (error: unknown) => {
          const problem = this.errors.getProblem(error);

          if (problem.code === 'open_cash_shift_required') {
            this.currentShift.set(null);
            this.loadCashRegisters();
            return;
          }

          this.loadError.set(
            this.errors.getMessage(error),
          );
        },
      });
  }

  private loadCashRegisters(): void {
    this.loadingRegisters.set(true);
    this.loadError.set(null);

    this.cashShiftService
      .getCashRegisters()
      .pipe(
        finalize(() =>
          this.loadingRegisters.set(false),
        ),
      )
      .subscribe({
        next: (cashRegisters) => {
          this.cashRegisters.set(cashRegisters);

          if (
            cashRegisters.length === 1 &&
            cashRegisters[0].disponible
          ) {
            this.form.controls.cajaId.setValue(
              cashRegisters[0].cajaId,
            );
          }
        },
        error: (error: unknown) => {
          this.loadError.set(
            this.errors.getMessage(error),
          );
        },
      });
  }
}
