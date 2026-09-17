import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  CreateEmployeeRequest,
  Employee,
  OperationalRoleOption,
  UpdateEmployeeRequest,
} from '../models/employee.models';

@Injectable({
  providedIn: 'root',
})
export class EmployeeService {
  private readonly http = inject(HttpClient);
  private readonly employeesUrl = `${environment.apiBaseUrl}/admin/empleados`;

  getEmployees(): Observable<Employee[]> {
    return this.http.get<Employee[]>(this.employeesUrl);
  }

  getEmployee(id: number): Observable<Employee> {
    return this.http.get<Employee>(`${this.employeesUrl}/${id}`);
  }

  getOperationalRoles(): Observable<OperationalRoleOption[]> {
    return this.http.get<OperationalRoleOption[]>(
      `${this.employeesUrl}/roles`,
    );
  }

  createEmployee(payload: CreateEmployeeRequest): Observable<Employee> {
    return this.http.post<Employee>(this.employeesUrl, payload);
  }

  updateEmployee(
  id: number,
  payload: UpdateEmployeeRequest,
): Observable<Employee> {
  return this.http.put<Employee>(
    `${this.employeesUrl}/${id}`,
    payload,
  );
}

  deactivateEmployee(id: number): Observable<Employee> {
    return this.http.delete<Employee>(`${this.employeesUrl}/${id}`);
  }
}