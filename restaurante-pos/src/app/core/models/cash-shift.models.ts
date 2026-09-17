export interface CashRegisterAvailability {
  cajaId: number;
  codigo: string;
  nombre: string;
  ubicacion: string | null;
  activa: boolean;
  disponible: boolean;
}

export interface OpenCashShiftRequest {
  cajaId: number;
  montoInicialEfectivo: number;
  observaciones?: string | null;
}

export interface CashShiftResponse {
  id: number;
  cajaId: number;
  codigoCaja: string;
  nombreCaja: string;
  cajeroId: number;
  estado: string;
  montoInicialEfectivo: number;
  abiertaEn: string;
  observaciones?: string | null;
}
