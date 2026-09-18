export interface InvoiceDetail {
  detalleId: number;
  nombre: string;
  cantidad: number;
  precioUnitario: number;
  modificadores: number;
  subtotalLinea: number;
}

export interface InvoicePayment {
  pagoId: number;
  metodoCodigo: string;
  metodoNombre: string;
  monto: number;
  montoRecibido: number | null;
  cambioEntregado: number | null;
  referencia: string | null;
  autorizacion: string | null;
}

export interface Invoice {
  facturaId: number;
  numeroDocumento: string;
  serie: string;
  numeroCorrelativo: number;
  tipoDocumento: string;
  estado: string;

  cuentaId: number;
  numeroCuenta: string;
  estadoCuenta: string;

  restauranteNombre: string;
  restauranteNombreComercial: string;
  identificacionFiscal: string;
  direccion: string;
  telefono: string;
  correo: string;
  moneda: string;

  clienteNombre: string | null;
  clienteApellido: string | null;

  subtotal: number;
  descuentoTotal: number;
  descuentoPuntos: number;
  porcentajeImpuesto: number;
  montoImpuesto: number;
  porcentajePropina: number;
  montoPropina: number;
  total: number;

  puntosRedimidos: number;
  puntosOtorgados: number;

  emitidaEn: string;

  detalles: InvoiceDetail[];
  pagos: InvoicePayment[];
}

export interface InvoiceHistoryItem {
  facturaId: number;
  numeroDocumento: string;
  emitidaEn: string;
  cuentaId: number;
  numeroCuenta: string;
  mesaId: number | null;
  numeroMesa: string | null;
  meseroId: number;
  meseroNombres: string;
  meseroApellidos: string;
  clienteNombres: string | null;
  clienteApellidos: string | null;
  subtotal: number;
  descuentoTotal: number;
  montoImpuesto: number;
  montoPropina: number;
  total: number;
  estado: string;
}
