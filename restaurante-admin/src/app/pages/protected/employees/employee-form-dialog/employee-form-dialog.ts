import {
  Component,
  EventEmitter,
  inject,
  Input,
  OnChanges,
  Output,
  signal,
  SimpleChanges,
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
  CreateEmployeeRequest,
  Employee,
  OperationalRole,
  OperationalRoleOption,
  UpdateEmployeeRequest,
} from '../../../../core/models/employee.models';
import { ApiErrorService } from '../../../../core/services/api-error.service';
import { EmployeeService } from '../../../../core/services/employee.service';
import { FormFeedbackComponent } from '../../../../shared/components/form-feedback/form-feedback';

@Component({
  selector: 'app-employee-form-dialog',
  imports: [
    FormFeedbackComponent,
    ReactiveFormsModule,
    TranslocoPipe,
  ],
  templateUrl: './employee-form-dialog.html',
  styleUrl: './employee-form-dialog.scss',
})
export class EmployeeFormDialogComponent implements OnChanges {
  private readonly fb = inject(FormBuilder);
  private readonly employeeService = inject(EmployeeService);
  private readonly errors = inject(ApiErrorService);
  private readonly messages = inject(MessageService);
  private readonly transloco = inject(TranslocoService);

  @Input() open = false;
  @Input() employee: Employee | null = null;

  @Output() closed = new EventEmitter<void>();
  @Output() saved = new EventEmitter<Employee>();

  readonly roles = signal<OperationalRoleOption[]>([]);
  readonly isLoadingRoles = signal(false);
  readonly isSaving = signal(false);
  readonly submitted = signal(false);
  readonly serverMessage = signal<string | null>(null);

  readonly employeeForm = this.fb.nonNullable.group({
    codigoEmpleado: [
      '',
      [
        Validators.required,
        Validators.maxLength(30),
      ],
    ],
    nombres: [
      '',
      [
        Validators.required,
        Validators.maxLength(100),
      ],
    ],
    apellidos: [
      '',
      [
        Validators.required,
        Validators.maxLength(100),
      ],
    ],
    email: [
      '',
      [
        Validators.required,
        Validators.email,
        Validators.maxLength(320),
      ],
    ],
    password: [
      '',
      [
        Validators.required,
        Validators.minLength(8),
        Validators.maxLength(72),
        Validators.pattern(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,72}$/),
      ],
    ],
    fechaContratacion: [''],
    rol: this.fb.control<OperationalRole | ''>(
      '',
      Validators.required,
    ),
  });

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['open']?.currentValue === true) {
      this.prepareForm();
      this.loadRoles();
    }
  }

  invalid(controlName: string): boolean {
    const control = this.employeeForm.get(controlName);

    return Boolean(
      control &&
        control.invalid &&
        (control.touched || this.submitted()),
    );
  }

  close(): void {
    if (this.isSaving()) {
      return;
    }

    this.closed.emit();
  }

  submit(): void {
  this.submitted.set(true);
  this.serverMessage.set(null);

  if (this.employeeForm.invalid) {
    this.employeeForm.markAllAsTouched();
    return;
  }

  const raw = this.employeeForm.getRawValue();

  if (!raw.rol) {
    return;
  }

  const editing = this.employee;

  const request = editing
    ? this.employeeService.updateEmployee(
        editing.id,
        this.buildUpdatePayload(),
      )
    : this.employeeService.createEmployee(
        this.buildCreatePayload(),
      );

  this.isSaving.set(true);

  request
    .pipe(finalize(() => this.isSaving.set(false)))
    .subscribe({
      next: (employee) => {
        const key = editing
          ? 'employees.update'
          : 'employees.registration';

        this.messages.add({
          severity: 'success',
          summary: this.transloco.translate(
            `${key}.successTitle`,
          ),
          detail: this.transloco.translate(
            `${key}.successMessage`,
            {
              name: `${employee.nombres} ${employee.apellidos}`,
            },
          ),
          life: 5000,
        });

        this.saved.emit(employee);
      },
      error: (error: unknown) => {
        this.serverMessage.set(
          this.errors.getMessage(error),
        );
      },
    });
}

private buildUpdatePayload(): UpdateEmployeeRequest {
  const raw = this.employeeForm.getRawValue();

  if (!raw.rol) {
    throw new Error('Rol operativo requerido');
  }

  return {
    codigoEmpleado: raw.codigoEmpleado.trim(),
    nombres: raw.nombres.trim(),
    apellidos: raw.apellidos.trim(),
    email: raw.email.trim(),
    fechaContratacion:
      raw.fechaContratacion || null,
    rol: raw.rol,
  };
}

private buildCreatePayload(): CreateEmployeeRequest {
  const raw = this.employeeForm.getRawValue();

  return {
    ...this.buildUpdatePayload(),
    password: raw.password,
  };
}

  private prepareForm(): void {
  this.submitted.set(false);
  this.serverMessage.set(null);

  const passwordControl =
    this.employeeForm.controls.password;

  if (this.employee) {
    passwordControl.clearValidators();

    this.employeeForm.reset({
      codigoEmpleado: this.employee.codigoEmpleado,
      nombres: this.employee.nombres,
      apellidos: this.employee.apellidos,
      email: this.employee.email,
      password: '',
      fechaContratacion:
        this.employee.fechaContratacion ?? '',
      rol: this.employee.rol,
    });
  } else {
    passwordControl.setValidators([
      Validators.required,
      Validators.minLength(8),
      Validators.maxLength(72),
      Validators.pattern(
        /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,72}$/,
      ),
    ]);

    this.employeeForm.reset({
      codigoEmpleado: '',
      nombres: '',
      apellidos: '',
      email: '',
      password: '',
      fechaContratacion: '',
      rol: '',
    });
  }

  passwordControl.updateValueAndValidity();
}

  private loadRoles(): void {
    this.isLoadingRoles.set(true);

    this.employeeService
      .getOperationalRoles()
      .pipe(finalize(() => this.isLoadingRoles.set(false)))
      .subscribe({
        next: (roles) => this.roles.set(roles),
        error: (error: unknown) => {
          this.roles.set([]);
          this.serverMessage.set(this.errors.getMessage(error));
        },
      });
  }
}