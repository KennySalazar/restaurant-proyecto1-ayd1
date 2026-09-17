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

export interface CurrentCashShiftResponse {
  id: number;
  cajaId: number;
  codigoCaja: string;
  nombreCaja: string;
  estado: string;
  montoInicialEfectivo: number;
  efectivoEsperado: number;
  abiertaEn: string;
}

export interface CloseCashShiftRequest {
  efectivoReal: number;
  observaciones?: string | null;
}

export interface CloseCashShiftResponse {
  id: number;
  cajaId: number;
  cajeroId: number;
  estado: string;
  montoInicialEfectivo: number;
  efectivoEsperado: number;
  efectivoReal: number;
  diferencia: number;
  abiertaEn: string;
  cerradaEn: string;
  observaciones?: string | null;
}
