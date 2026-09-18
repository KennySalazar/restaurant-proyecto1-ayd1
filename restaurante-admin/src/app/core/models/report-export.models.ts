export type ReportType =
  | 'VENTAS'
  | 'PLATILLOS_VENDIDOS'
  | 'RENTABILIDAD_ACTUAL'
  | 'RENTABILIDAD_HISTORICA'
  | 'OCUPACION_MESAS'
  | 'DESEMPENO_MESEROS'
  | 'FIDELIZACION'
  | 'INVENTARIO';

export type ReportExportFormat =
  | 'PDF'
  | 'EXCEL';

export interface ReportExportRequest {
  tipo: ReportType;
  formato: ReportExportFormat;
  fechaInicio?: string | null;
  fechaFin?: string | null;
}