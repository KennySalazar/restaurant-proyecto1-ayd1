import { TableStatus } from './table.models';
import { WaitlistStatus } from './waitlist.models';

export interface SeatReservationRequest {
  reservaId?: number | null;
  codigoReserva?: string | null;
  nombreCliente?: string | null;
  cantidadPersonas?: number | null;
  notas?: string | null;
}

export interface SeatReservationResult {
  mensaje: string;
  mesaId: number;
  numeroMesa: string;
  estadoMesa: TableStatus;
  reservaId: number;
  codigoReserva: string;
  nombreCliente: string;
  cantidadPersonas: number;
  cuentaId: number;
  numeroCuenta: string;
  sentadoEn: string;
}

export interface SeatWaitlistRequest {
  listaEsperaId?: number | null;
  cantidadPersonas?: number | null;
  notas?: string | null;
}

export interface SeatWaitlistResult {
  mensaje: string;
  mesaId: number;
  numeroMesa: string;
  estadoMesa: TableStatus;
  listaEsperaId: number;
  nombreCliente: string;
  telefonoCliente: string | null;
  cantidadPersonas: number;
  estadoListaEspera: WaitlistStatus;
  cuentaId: number;
  numeroCuenta: string;
  sentadoEn: string;
}
