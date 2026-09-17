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

export interface TransferAccountRequest {
  mesaDestinoId: number;
  motivo?: string | null;
}

export interface TransferAccountResponse {
  mensaje: string;
  cuentaId: number;
  numeroCuenta: string;
  mesaOrigenId: number;
  numeroMesaOrigen: string;
  estadoMesaOrigen: TableStatus;
  mesaDestinoId: number;
  numeroMesaDestino: string;
  estadoMesaDestino: TableStatus;
  cantidadComandas: number;
  transferidoPorId: number;
  motivo: string | null;
  transferidaEn: string;
}

export interface MergeAccountsRequest {
  mesaOrigenId?: number | null;
  mesaDestinoId?: number | null;
  cuentaOrigenId?: number | null;
  cuentaDestinoId?: number | null;
  motivo?: string | null;
}

export interface MergeAccountsResponse {
  mensaje: string;
  fusionId: number;
  cuentaOrigenId: number;
  numeroCuentaOrigen: string;
  estadoCuentaOrigen: string;
  mesaOrigenId: number;
  numeroMesaOrigen: string;
  estadoMesaOrigen: TableStatus;
  cuentaDestinoId: number;
  numeroCuentaDestino: string;
  estadoCuentaDestino: string;
  mesaDestinoId: number;
  numeroMesaDestino: string;
  estadoMesaDestino: TableStatus;
  comandasTransferidas: number;
  totalComandasDestino: number;
  totalPersonas: number;
  realizadoPorId: number;
  motivo: string | null;
  fusionadaEn: string;
}

export interface ActiveFusionResponse {
  mesaOrigenId: number;
  numeroMesaOrigen: string;
  mesaDestinoId: number;
  numeroMesaDestino: string;
  cuentaDestinoId: number;
  totalPersonas: number;
}
