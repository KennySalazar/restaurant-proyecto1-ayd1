export interface ServiceRatingRequest {
  calificacion: number;
  comentario: string | null;
}

export interface ServiceRatingResponse {
  calificacionId: number;
  facturaId: number;
  numeroDocumento: string;
  cuentaId: number;
  clienteId: number | null;
  meseroId: number;
  meseroNombres: string;
  meseroApellidos: string;
  calificacion: number;
  comentario: string | null;
  creadaEn: string;
}
