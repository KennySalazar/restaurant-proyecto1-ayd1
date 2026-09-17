export type RestaurantConfigurationStatus =
  | 'VIGENTE'
  | 'HISTORICA';

export interface TipConfiguration {
  id: number;
  porcentaje: number;
  estado: RestaurantConfigurationStatus;
  vigenteDesde: string;
  vigenteHasta: string | null;
}

export interface UpdateTipConfigurationRequest {
  porcentaje: number;
}

export interface PointsAccumulationConfiguration {
  id: number;
  puntosPorMoneda: number;
  estado: RestaurantConfigurationStatus;
  vigenteDesde: string;
  vigenteHasta: string | null;
}

export interface UpdatePointsAccumulationConfigurationRequest {
  puntosPorMoneda: number;
}