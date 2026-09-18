export interface BillingCalculation {
  cuentaId: number;
  subcuentaId: number | null;
  numeroCuenta: string;
  subtotal: number;
  porcentajeImpuesto: number;
  montoImpuesto: number;
  porcentajePropina: number;
  montoPropina: number;
  total: number;
}

export interface BillingAccount {
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
  estadoMesa: string;
}

export interface BillingSubaccount {
  id: number;
  cuentaId: number;
  numeroSubcuenta: number;
  nombre: string;
  tipoDivision: string;
  porcentajeAsignado: number | null;
  estado: string;
  subtotal: number;
  creadoEn: string;
  items: unknown[];
}
