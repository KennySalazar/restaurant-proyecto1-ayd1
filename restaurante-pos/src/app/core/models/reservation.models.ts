export const RESERVATION_STATUSES = [
  'PENDIENTE',
  'CONFIRMADA',
  'CLIENTE_PRESENTE',
  'ATENDIDA',
  'CANCELADA',
  'NO_ASISTIO',
] as const;

export type ReservationStatus = (typeof RESERVATION_STATUSES)[number];

export interface ReservationTable {
  id: number;
  numero: string;
  capacidad: number;
  zona: string | null;
}

export interface Reservation {
  id: number;
  codigoReserva: string;
  nombreCliente: string;
  telefonoCliente: string | null;
  cantidadPersonas: number;
  fechaHoraInicio: string;
  fechaHoraFin: string;
  estado: ReservationStatus;
  notas: string | null;
  mesa: ReservationTable | null;
}
