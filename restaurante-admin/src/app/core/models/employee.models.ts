export type OperationalRole = 'WAITER' | 'KITCHEN' | 'CASHIER';

export interface Employee {
  id: number;
  codigoEmpleado: string;
  nombres: string;
  apellidos: string;
  email: string;
  fechaContratacion: string;
  rol: OperationalRole;
  habilitado: boolean;
}