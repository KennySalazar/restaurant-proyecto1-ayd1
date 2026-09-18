export interface CurrentDishProfitability {
  platilloId: number;
  codigo: string;
  platillo: string;
  categoria: string;
  precioVenta: number;
  costoProduccion: number | null;
  gananciaUnitaria: number | null;
  margenRentabilidad: number | null;
  rentabilidadCalculable: boolean;
  motivoNoCalculable: string | null;
}

export interface HistoricalDishProfitability {
  platilloId: number;
  platillo: string;
  cantidadVendida: number;
  ingresoHistorico: number;
  costoHistorico: number;
  gananciaHistorica: number;
  margenRentabilidad: number | null;
}

export interface HistoricalProfitabilityReport {
  fechaInicio: string;
  fechaFin: string;
  huboVentas: boolean;
  platillos: HistoricalDishProfitability[];
}