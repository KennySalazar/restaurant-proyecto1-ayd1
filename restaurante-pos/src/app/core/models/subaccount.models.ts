export interface AssignItemRequest {
  comandaDetalleId: number;
  cantidad?: number | null;
}

export interface SubaccountItemDefinitionRequest {
  nombre?: string | null;
  items: AssignItemRequest[];
}

export interface SplitByPeopleRequest {
  numeroPersonas: number;
  nombres?: string[] | null;
}

export interface SplitByItemsRequest {
  subcuentas: SubaccountItemDefinitionRequest[];
}

export interface SubaccountItemResponse {
  id: number;
  comandaDetalleId: number;
  nombrePlatillo: string;
  cantidadAsignada: number;
  precioUnitario: number;
  subtotal: number;
}

export interface SubaccountResponse {
  id: number;
  cuentaId: number;
  numeroSubcuenta: number;
  nombre: string | null;
  tipoDivision: string;
  porcentajeAsignado: number | null;
  estado: string;
  subtotal: number;
  creadoEn: string;
  items: SubaccountItemResponse[];
}

export interface SplitAccountResponse {
  mensaje: string;
  cuentaId: number;
  numeroCuenta: string;
  tipoDivision: string;
  subtotalCuenta: number;
  totalSubcuentas: number;
  subcuentas: SubaccountResponse[];
}
