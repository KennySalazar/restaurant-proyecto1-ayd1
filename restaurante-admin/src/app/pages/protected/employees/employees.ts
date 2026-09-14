import {
  Component,
  computed,
  inject,
  OnInit,
  signal,
} from '@angular/core';
import { DatePipe } from '@angular/common';
import { TranslocoPipe } from '@jsverse/transloco';
import { finalize } from 'rxjs';

import {
  Employee,
  OperationalRole,
} from '../../../core/models/employee.models';
import { ApiErrorService } from '../../../core/services/api-error.service';
import { EmployeeService } from '../../../core/services/employee.service';
import { FormFeedbackComponent } from '../../../shared/components/form-feedback/form-feedback';
import { PageHeadingComponent } from '../../../shared/components/page-heading/page-heading';

@Component({
  selector: 'app-employees-page',
  imports: [
    DatePipe,
    FormFeedbackComponent,
    PageHeadingComponent,
    TranslocoPipe,
  ],
  templateUrl: './employees.html',
  styleUrl: './employees.scss',
})
export class EmployeesPageComponent implements OnInit {
  private readonly employeeService = inject(EmployeeService);
  private readonly errors = inject(ApiErrorService);

  readonly employees = signal<Employee[]>([]);
  readonly selectedEmployee = signal<Employee | null>(null);
  readonly isLoading = signal(true);
  readonly isDetailLoading = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly totalEmployees = computed(() => this.employees().length);

  readonly activeEmployees = computed(
    () => this.employees().filter((employee) => employee.habilitado).length,
  );

  readonly disabledEmployees = computed(
    () => this.employees().filter((employee) => !employee.habilitado).length,
  );

  ngOnInit(): void {
    this.loadEmployees();
  }

  loadEmployees(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.employeeService
      .getEmployees()
      .pipe(finalize(() => this.isLoading.set(false)))
      .subscribe({
        next: (employees) => this.employees.set(employees),
        error: (error: unknown) => {
          this.errorMessage.set(this.errors.getMessage(error));
        },
      });
  }

  viewEmployee(id: number): void {
    this.isDetailLoading.set(true);

    this.employeeService
      .getEmployee(id)
      .pipe(finalize(() => this.isDetailLoading.set(false)))
      .subscribe({
        next: (employee) => this.selectedEmployee.set(employee),
        error: (error: unknown) => this.errors.present(error),
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
}