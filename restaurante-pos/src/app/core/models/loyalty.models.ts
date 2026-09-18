export interface CustomerPoints {
  clienteId: number;
  nombres: string;
  apellidos: string | null;
  saldoPuntos: number;
  valorMonetarioPunto: number;
  valorMonetarioDisponible: number;
}

export interface CustomerRecord {
  clienteId: number;
  nombres: string;
  apellidos: string | null;
  telefono: string;
  correo: string | null;
  saldoPuntos: number;
  totalVisitas: number;
}

export interface RegisterCustomerRequest {
  nombres: string;
  apellidos: string | null;
  telefono: string;
  correo: string | null;
}
