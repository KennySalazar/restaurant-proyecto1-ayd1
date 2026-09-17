export const TABLE_STATUSES = ['LIBRE', 'RESERVADA', 'OCUPADA', 'CUENTA_SOLICITADA'] as const;

export type TableStatus = (typeof TABLE_STATUSES)[number];

export interface TableZone {
  id: number;
  nombre: string;
}

export interface RestaurantTable {
  id: number;
  numero: string;
  capacidad: number;
  zona: TableZone | null;
  estado: TableStatus;
  activo: boolean;
}

export interface TableZoneGroup {
  key: string;
  nombre: string;
  tables: RestaurantTable[];
}
