export type OperationalRole = 'WAITER' | 'KITCHEN' | 'CASHIER';

export interface Employee {
  id: number;
  codigoEmpleado: string;
  nombres: string;
  apellidos: string;
  email: string;
  fechaContratacion: string | null;
  rol: OperationalRole;
  habilitado: boolean;
}

export interface OperationalRoleOption {
  codigo: OperationalRole;
  nombre: string;
}

export interface CreateEmployeeRequest {
  codigoEmpleado: string;
  nombres: string;
  apellidos: string;
  email: string;
  password: string;
  fechaContratacion: string | null;
  rol: OperationalRole;
}