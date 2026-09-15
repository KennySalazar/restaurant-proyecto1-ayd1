import {
  Component,
  computed,
  inject,
  OnInit,
  signal,
} from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import {
  TranslocoPipe,
  TranslocoService,
} from '@jsverse/transloco';
import { MessageService } from 'primeng/api';
import { finalize } from 'rxjs';

import { RestaurantTable } from '../../../core/models/table.models';
import { ApiErrorService } from '../../../core/services/api-error.service';
import { ReservationService } from '../../../core/services/reservation.service';
import { TableService } from '../../../core/services/table.service';
import { FormFeedbackComponent } from '../../../shared/components/form-feedback/form-feedback';
import { PageHeadingComponent } from '../../../shared/components/page-heading/page-heading';

@Component({
  selector: 'app-reservations-page',
  imports: [
    FormFeedbackComponent,
    PageHeadingComponent,
    ReactiveFormsModule,
    TranslocoPipe,
  ],
  templateUrl: './reservations.html',
  styleUrl: './reservations.scss',
})
export class ReservationsPageComponent implements OnInit {
  private readonly formBuilder = inject(FormBuilder);
  private readonly reservationService = inject(ReservationService);
  private readonly tableService = inject(TableService);
  private readonly errors = inject(ApiErrorService);
  private readonly messages = inject(MessageService);
  private readonly transloco = inject(TranslocoService);

  readonly tables = signal<RestaurantTable[]>([]);
  readonly isLoadingTables = signal(true);
  readonly isSaving = signal(false);
  readonly submitted = signal(false);
  readonly serverMessage = signal<string | null>(null);

  readonly activeTables = computed(() =>
    this.tables().filter((table) => table.activo),
  );

  readonly reservationForm = this.formBuilder.nonNullable.group({
    nombreCliente: [
      '',
      [
        Validators.required,
        Validators.maxLength(150),
      ],
    ],
    telefonoCliente: [
        '',
        [
            Validators.required,
            Validators.pattern(/^[0-9]{8}$/),
        ],
        ],
    cantidadPersonas: [
      1,
      [
        Validators.required,
        Validators.min(1),
      ],
    ],
    fechaHoraInicio: [
      '',
      [
        Validators.required,
        this.futureDateTimeValidator,
      ],
    ],
    mesaId: [
      0,
      [
        Validators.required,
        Validators.min(1),
      ],
    ],
    notas: [
      '',
      [
        Validators.maxLength(500),
      ],
    ],
  });

  selectedTable(): RestaurantTable | null {
  const tableId = Number(
    this.reservationForm.controls.mesaId.value,
  );

  return (
    this.activeTables().find(
      (table) => table.id === tableId,
    ) ?? null
  );
}

  ngOnInit(): void {
    this.loadTables();
  }

  loadTables(): void {
    this.isLoadingTables.set(true);
    this.serverMessage.set(null);

    this.tableService
      .getTables()
      .pipe(
        finalize(() =>
          this.isLoadingTables.set(false),
        ),
      )
      .subscribe({
        next: (tables) => {
          this.tables.set(tables);
        },
        error: (error: unknown) => {
          this.serverMessage.set(
            this.errors.getMessage(error),
          );
        },
      });
  }

  submit(): void {
    this.submitted.set(true);
    this.serverMessage.set(null);

    if (this.reservationForm.invalid) {
      this.reservationForm.markAllAsTouched();
      return;
    }

    const raw = this.reservationForm.getRawValue();
    const table = this.selectedTable();

    if (!table) {
      this.serverMessage.set(
        this.transloco.translate(
          'reservations.validation.tableRequired',
        ),
      );
      return;
    }

    if (raw.cantidadPersonas > table.capacidad) {
      this.serverMessage.set(
        this.transloco.translate(
          'reservations.validation.capacity',
        ),
      );
      return;
    }

    this.isSaving.set(true);

    this.reservationService
      .createReservation({
        mesaId: Number(raw.mesaId),
        nombreCliente: raw.nombreCliente.trim(),
        telefonoCliente: raw.telefonoCliente.trim(),
        cantidadPersonas: Number(raw.cantidadPersonas),
        fechaHoraInicio:
          this.toGuatemalaOffset(raw.fechaHoraInicio),
        notas: raw.notas.trim() || null,
      })
      .pipe(
        finalize(() => this.isSaving.set(false)),
      )
      .subscribe({
        next: (reservation) => {
          this.messages.add({
            severity: 'success',
            summary: this.transloco.translate(
              'reservations.success.title',
            ),
            detail: this.transloco.translate(
              'reservations.success.message',
              {
                code: reservation.codigoReserva,
                table: reservation.mesa.numero,
              },
            ),
            life: 6000,
          });

          this.submitted.set(false);
          this.serverMessage.set(null);

          this.reservationForm.reset({
            nombreCliente: '',
            telefonoCliente: '',
            cantidadPersonas: 1,
            fechaHoraInicio: '',
            mesaId: 0,
            notas: '',
          });
        },
        error: (error: unknown) => {
          this.serverMessage.set(
            this.errors.getMessage(error),
          );
        },
      });
  }

  invalid(controlName: keyof typeof this.reservationForm.controls): boolean {
    const control =
      this.reservationForm.controls[controlName];

    return (
      control.invalid &&
      (control.touched || this.submitted())
    );
  }

  capacityInvalid(): boolean {
    const table = this.selectedTable();

    if (!table) {
      return false;
    }

    return (
      Number(
        this.reservationForm.controls
          .cantidadPersonas.value,
      ) > table.capacidad
    );
  }

  private futureDateTimeValidator(
    control: AbstractControl,
  ): ValidationErrors | null {
    const value = String(control.value ?? '');

    if (!value) {
      return null;
    }

    const normalized =
      value.length === 16
        ? `${value}:00`
        : value;

    const reservationDate =
      new Date(`${normalized}-06:00`);

    if (Number.isNaN(reservationDate.getTime())) {
      return { invalidDateTime: true };
    }

    return reservationDate.getTime() > Date.now()
      ? null
      : { reservationMustBeFuture: true };
  }

  private toGuatemalaOffset(
    value: string,
  ): string {
    const normalized =
      value.length === 16
        ? `${value}:00`
        : value;

    return `${normalized}-06:00`;
  }

  onPhoneInput(event: Event): void {
  const input = event.target as HTMLInputElement;

  const sanitized = input.value
    .replace(/\D/g, '')
    .slice(0, 8);

  input.value = sanitized;

  this.reservationForm.controls.telefonoCliente.setValue(
    sanitized,
    { emitEvent: false },
  );
}
}