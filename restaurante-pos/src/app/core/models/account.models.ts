import { TableStatus } from './table.models';

export interface OpenAccountRequest {
  cantidadPersonas?: number | null;
  clienteId?: number | null;
  observaciones?: string | null;
}

export interface Account {
  id: number;
  restauranteId: number;
  mesaId: number;
  numeroMesa: string;
  meseroId: number;
  nombreMesero: string;
  clienteId: number | null;
  reservaId: number | null;
  listaEsperaId: number | null;
  numeroCuenta: string;
  cantidadPersonas: number;
  estadoCuenta: string;
  abiertaEn: string;
  observaciones: string | null;
  estadoMesa: TableStatus;
}
