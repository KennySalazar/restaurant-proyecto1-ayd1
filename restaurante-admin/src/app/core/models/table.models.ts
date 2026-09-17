export type TableStatus =
  | 'LIBRE'
  | 'RESERVADA'
  | 'OCUPADA'
  | 'CUENTA_SOLICITADA';

export interface TableZone {
  id: number;
  nombre: string;
}

export interface RestaurantTable {
  id: number;
  numero: string;
  capacidad: number;
  zona: TableZone;
  estado: TableStatus;
  activo: boolean;
}