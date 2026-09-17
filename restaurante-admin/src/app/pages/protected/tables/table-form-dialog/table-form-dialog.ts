import {
  Component,
  EventEmitter,
  inject,
  Input,
  Output,
  signal,
} from '@angular/core';
import {
  FormBuilder,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';
import { MessageService } from 'primeng/api';
import { finalize } from 'rxjs';

import {
  CreateTableRequest,
  RestaurantTable,
  TableZone,
  UpdateTableRequest,
} from '../../../../core/models/table.models';
import { ApiErrorService } from '../../../../core/services/api-error.service';
import { TableService } from '../../../../core/services/table.service';
import { FormFeedbackComponent } from '../../../../shared/components/form-feedback/form-feedback';

@Component({
  selector: 'app-table-form-dialog',
  imports: [
    FormFeedbackComponent,
    ReactiveFormsModule,
    TranslocoPipe,
  ],
  templateUrl: './table-form-dialog.html',
  styleUrl: './table-form-dialog.scss',
})
export class TableFormDialogComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly tableService = inject(TableService);
  private readonly errors = inject(ApiErrorService);
  private readonly messages = inject(MessageService);
  private readonly transloco = inject(TranslocoService);

  @Output() readonly saved = new EventEmitter<RestaurantTable>();
  @Output() readonly dismissed = new EventEmitter<void>();

  @Input() table: RestaurantTable | null = null;

  private isOpen = false;

  @Input()
  set open(value: boolean) {
    this.isOpen = value;

    if (value) {
      this.prepareForm();
      this.loadZones();
    }
  }

  get open(): boolean {
    return this.isOpen;
  }

  readonly zones = signal<TableZone[]>([]);
  readonly isLoadingZones = signal(false);
  readonly isSaving = signal(false);
  readonly submitted = signal(false);
  readonly serverMessage = signal<string | null>(null);

  readonly registrationForm = this.formBuilder.nonNullable.group({
    numero: [
      '',
      [
        Validators.required,
        Validators.maxLength(20),
      ],
    ],
    capacidad: [
      1,
      [
        Validators.required,
        Validators.min(1),
      ],
    ],
    zonaId: [
      0,
      [
        Validators.required,
        Validators.min(1),
      ],
    ],
  });

  close(): void {
    if (this.isSaving()) {
      return;
    }

    this.dismissed.emit();
  }

  submit(): void {
  this.submitted.set(true);
  this.serverMessage.set(null);

  if (this.registrationForm.invalid) {
    this.registrationForm.markAllAsTouched();
    return;
  }

  const editing = this.table;

  this.isSaving.set(true);

  const request = editing
    ? this.tableService.updateTable(
        editing.id,
        this.buildUpdatePayload(),
      )
    : this.tableService.createTable(
        this.buildCreatePayload(),
      );

  request
    .pipe(finalize(() => this.isSaving.set(false)))
    .subscribe({
      next: (table) => {
        const key = editing
          ? 'tables.update'
          : 'tables.registration';

        this.messages.add({
          severity: 'success',
          summary: this.transloco.translate(
            `${key}.successTitle`,
          ),
          detail: this.transloco.translate(
            `${key}.successMessage`,
            {
              number: table.numero,
            },
          ),
          life: 5000,
        });

        this.saved.emit(table);
      },
      error: (error: unknown) => {
        this.serverMessage.set(
          this.errors.getMessage(error),
        );
      },
    });
}

  invalid(
    controlName: keyof typeof this.registrationForm.controls,
  ): boolean {
    const control = this.registrationForm.controls[controlName];

    return (
      control.invalid &&
      (control.touched || this.submitted())
    );
  }

  private prepareForm(): void {
  this.submitted.set(false);
  this.serverMessage.set(null);

  if (this.table) {
    this.registrationForm.reset({
      numero: this.table.numero,
      capacidad: this.table.capacidad,
      zonaId: this.table.zona.id,
    });

    return;
  }

  this.registrationForm.reset({
    numero: '',
    capacidad: 1,
    zonaId: 0,
  });
}

  private loadZones(): void {
    if (this.zones().length > 0 || this.isLoadingZones()) {
      return;
    }

    this.isLoadingZones.set(true);

    this.tableService
      .getZones()
      .pipe(finalize(() => this.isLoadingZones.set(false)))
      .subscribe({
        next: (zones) => this.zones.set(zones),
        error: (error: unknown) => {
          this.serverMessage.set(
            this.errors.getMessage(error),
          );
        },
      });
  }

  private buildUpdatePayload(): UpdateTableRequest {
  const raw = this.registrationForm.getRawValue();

  return {
    numero: raw.numero.trim(),
    capacidad: raw.capacidad,
    zonaId: raw.zonaId,
  };
}

private buildCreatePayload(): CreateTableRequest {
  return {
    ...this.buildUpdatePayload(),
    estadoInicial: 'LIBRE',
  };
}
}
