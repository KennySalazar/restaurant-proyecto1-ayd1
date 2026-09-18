export const WAITLIST_STATUSES = [
  'ESPERANDO',
  'SUGERIDA',
  'NOTIFICADA',
  'SENTADA',
  'RETIRADA',
] as const;

export type WaitlistStatus = (typeof WAITLIST_STATUSES)[number];

export interface WaitlistQueueEntry {
  id: number;
  posicion: number;
  nombreCliente: string;
  telefonoCliente: string | null;
  cantidadPersonas: number;
  horaLlegada: string;
  estado: WaitlistStatus;
  notas: string | null;
}

export interface WaitlistSuggestion {
  id: number;
  posicion: number;
  nombreCliente: string;
  telefonoCliente: string | null;
  cantidadPersonas: number;
  horaLlegada: string;
  estado: WaitlistStatus;
  mesaId: number;
  numeroMesa: string;
  capacidadMesa: number;
}
