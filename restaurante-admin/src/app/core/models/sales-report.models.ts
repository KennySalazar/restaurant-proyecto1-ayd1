export interface SalesByCategory {
  categoriaId: number | null;
  categoria: string;
  cantidadVendida: number;
  montoVendido: number;
}

export interface SalesByWaiter {
  meseroId: number;
  mesero: string;
  cantidadVentas: number;
  montoVendido: number;
}

export interface SalesReport {
  fechaInicio: string;
  fechaFin: string;
  cantidadVentas: number;
  montoTotalVendido: number;
  ventasPorCategoria: SalesByCategory[];
  ventasPorMesero: SalesByWaiter[];
}