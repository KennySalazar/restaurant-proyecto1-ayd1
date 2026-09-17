export type WaitlistStatus =
  | 'ESPERANDO'
  | 'SUGERIDA'
  | 'NOTIFICADA'
  | 'SENTADA'
  | 'RETIRADA';

export interface WaitlistQueueEntry {
  id: number;
  posicion: number;
  nombreCliente: string;
  telefonoCliente: string;
  cantidadPersonas: number;
  horaLlegada: string;
  estado: WaitlistStatus;
  notas: string | null;
}

export interface CreateWaitlistEntryRequest {
  nombreCliente: string;
  telefonoCliente: string;
  cantidadPersonas: number;
  notas: string | null;
}