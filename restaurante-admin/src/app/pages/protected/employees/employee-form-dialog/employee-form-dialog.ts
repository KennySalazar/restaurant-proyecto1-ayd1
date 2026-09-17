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

    const payload: CreateEmployeeRequest = {
      codigoEmpleado: raw.codigoEmpleado.trim(),
      nombres: raw.nombres.trim(),
      apellidos: raw.apellidos.trim(),
      email: raw.email.trim(),
      password: raw.password,
      fechaContratacion: raw.fechaContratacion || null,
      rol: raw.rol,
    };

    this.isSaving.set(true);

    this.employeeService
      .createEmployee(payload)
      .pipe(finalize(() => this.isSaving.set(false)))
      .subscribe({
        next: (employee) => {
          this.messages.add({
            severity: 'success',
            summary: this.transloco.translate(
              'employees.registration.successTitle',
            ),
            detail: this.transloco.translate(
              'employees.registration.successMessage',
              {
                name: `${employee.nombres} ${employee.apellidos}`,
              },
            ),
            life: 5000,
          });

          this.saved.emit(employee);
        },
        error: (error: unknown) => {
          this.serverMessage.set(this.errors.getMessage(error));
        },
      });
  }

  private prepareForm(): void {
    this.submitted.set(false);
    this.serverMessage.set(null);

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