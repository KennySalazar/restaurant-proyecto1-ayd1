export interface TableOccupancySlot {
  fecha: string;
  hora: number;
  mesasOcupadas: number;
}

export interface TableOccupancyReport {
  fechaInicio: string;
  fechaFin: string;
  huboOcupaciones: boolean;
  ocupacionPorHorario: TableOccupancySlot[];
}