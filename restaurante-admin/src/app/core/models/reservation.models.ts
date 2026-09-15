export interface CreateReservationRequest {
  mesaId: number;
  nombreCliente: string;
  telefonoCliente: string;
  cantidadPersonas: number;
  fechaHoraInicio: string;
  notas: string | null;
}

export interface ReservationTable {
  id: number;
  numero: string;
  capacidad: number;
  zona: string;
}

export interface ReservationResponse {
  id: number;
  codigoReserva: string;
  nombreCliente: string;
  telefonoCliente: string;
  cantidadPersonas: number;
  fechaHoraInicio: string;
  fechaHoraFin: string;
  estado: string;
  notas: string | null;
  mesa: ReservationTable;
}