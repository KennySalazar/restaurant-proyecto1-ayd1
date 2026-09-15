export interface OccupancyPanelTable {
  mesaId: number;
  numero: string;
  capacidad: number;
  zona: string;
  estado: string;

  cuentaActualId: number | null;
  numeroCuenta: string | null;
  principal: boolean | null;

  reservaActualId: number | null;
  clienteReserva: string | null;
  reservaHasta: string | null;

  listaEsperaActualId: number | null;
  clienteListaEspera: string | null;
}