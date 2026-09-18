import {
  Component,
  computed,
  inject,
  OnInit,
  signal,
} from '@angular/core';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';
import { finalize } from 'rxjs';
import { MessageService } from 'primeng/api';

import {
  Employee,
  OperationalRole,
} from '../../../core/models/employee.models';
import { ApiErrorService } from '../../../core/services/api-error.service';
import { EmployeeService } from '../../../core/services/employee.service';
import { FormFeedbackComponent } from '../../../shared/components/form-feedback/form-feedback';
import { PageHeadingComponent } from '../../../shared/components/page-heading/page-heading';
import { EmployeeFormDialogComponent } from './employee-form-dialog/employee-form-dialog';

@Component({
  selector: 'app-employees-page',
  imports: [
    FormFeedbackComponent,
    PageHeadingComponent,
    TranslocoPipe,
    EmployeeFormDialogComponent,
  ],
  templateUrl: './employees.html',
  styleUrl: './employees.scss',
})
export class EmployeesPageComponent implements OnInit {
  private readonly employeeService = inject(EmployeeService);
  private readonly errors = inject(ApiErrorService);
  private readonly messages = inject(MessageService);
  private readonly transloco = inject(TranslocoService);

  readonly employees = signal<Employee[]>([]);
  readonly selectedEmployee = signal<Employee | null>(null);

  readonly isLoading = signal(true);
  readonly isDetailLoading = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly registrationDialogOpen = signal(false);
  readonly editingEmployee = signal<Employee | null>(null);

  readonly deactivationCandidate = signal<Employee | null>(null);
  readonly isDeactivating = signal(false);

  readonly totalEmployees = computed(
    () => this.employees().length,
  );

  readonly activeEmployees = computed(
    () =>
      this.employees().filter(
        (employee) => employee.habilitado,
      ).length,
  );

  readonly disabledEmployees = computed(
    () =>
      this.employees().filter(
        (employee) => !employee.habilitado,
      ).length,
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
    }).format(date);
  }

  ngOnInit(): void {
    this.loadEmployees();
  }

  loadEmployees(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.employeeService
      .getEmployees()
      .pipe(
        finalize(() => this.isLoading.set(false)),
      )
      .subscribe({
        next: (employees) => {
          this.employees.set(employees);
        },
        error: (error: unknown) => {
          this.errorMessage.set(
            this.errors.getMessage(error),
          );
        },
      });
  }

  viewEmployee(id: number): void {
    this.isDetailLoading.set(true);

    this.employeeService
      .getEmployee(id)
      .pipe(
        finalize(() => this.isDetailLoading.set(false)),
      )
      .subscribe({
        next: (employee) => {
          this.selectedEmployee.set(employee);
        },
        error: (error: unknown) => {
          this.errors.present(error);
        },
      });
  }

  closeDetails(): void {
    this.selectedEmployee.set(null);
  }

  roleKey(role: OperationalRole): string {
    return `employees.roles.${role}`;
  }

  roleClass(role: OperationalRole): string {
    return `role-badge--${role.toLowerCase()}`;
  }

  openRegistration(): void {
    this.editingEmployee.set(null);
    this.registrationDialogOpen.set(true);
  }

  openEdition(employee: Employee): void {
    this.editingEmployee.set(employee);
    this.registrationDialogOpen.set(true);
  }

  closeRegistration(): void {
    this.registrationDialogOpen.set(false);
    this.editingEmployee.set(null);
  }

  handleEmployeeSaved(employee: Employee): void {
    const editing = this.editingEmployee();

    if (editing) {
      this.employees.update((employees) =>
        employees.map((current) =>
          current.id === employee.id
            ? employee
            : current,
        ),
      );

      if (
        this.selectedEmployee()?.id === employee.id
      ) {
        this.selectedEmployee.set(employee);
      }
    } else {
      this.employees.update((employees) => [
        employee,
        ...employees,
      ]);
    }

    this.registrationDialogOpen.set(false);
    this.editingEmployee.set(null);
  }

  requestDeactivation(employee: Employee): void {
    if (!employee.habilitado) {
      this.messages.add({
        severity: 'warn',
        summary: this.transloco.translate(
          'employees.deactivation.alreadyDisabledTitle',
        ),
        detail: this.transloco.translate(
          'employees.deactivation.alreadyDisabledMessage',
        ),
        life: 5000,
      });

      return;
    }

    this.deactivationCandidate.set(employee);
  }

  cancelDeactivation(): void {
    if (this.isDeactivating()) {
      return;
    }

    this.deactivationCandidate.set(null);
  }

  confirmDeactivation(): void {
    const employee = this.deactivationCandidate();

    if (!employee || this.isDeactivating()) {
      return;
    }

    this.isDeactivating.set(true);

    this.employeeService
      .deactivateEmployee(employee.id)
      .pipe(
        finalize(() => this.isDeactivating.set(false)),
      )
      .subscribe({
        next: (disabledEmployee) => {
          this.employees.update((employees) =>
            employees.map((current) =>
              current.id === disabledEmployee.id
                ? disabledEmployee
                : current,
            ),
          );

          if (
            this.selectedEmployee()?.id ===
            disabledEmployee.id
          ) {
            this.selectedEmployee.set(
              disabledEmployee,
            );
          }

          this.deactivationCandidate.set(null);

          this.messages.add({
            severity: 'success',
            summary: this.transloco.translate(
              'employees.deactivation.successTitle',
            ),
            detail: this.transloco.translate(
              'employees.deactivation.successMessage',
              {
                name:
                  `${disabledEmployee.nombres} ` +
                  `${disabledEmployee.apellidos}`,
              },
            ),
            life: 5000,
          });
        },
        error: (error: unknown) => {
          this.deactivationCandidate.set(null);
          this.errors.present(error);
        },
      });
  }
}
