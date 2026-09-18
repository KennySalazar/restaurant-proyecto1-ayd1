export interface LoyaltyFrequentCustomer {
  clienteId: number;
  nombres: string;
  apellidos: string;
  cantidadVisitas: number;
}

export interface LoyaltyReport {
  fechaInicio: string;
  fechaFin: string;
  puntosOtorgados: number;
  puntosRedimidos: number;
  clientesFrecuentes:
    LoyaltyFrequentCustomer[];
}