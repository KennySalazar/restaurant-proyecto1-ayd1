export type PaymentMode = 'CASH' | 'CARD' | 'MIXED';

export const PAYMENT_METHOD_IDS = {
  cash: 1,
  card: 2,
} as const;

export interface RegisterPaymentRequest {
  metodoPagoId: number;
  monto: number;
  montoRecibido: number | null;
  referencia: string | null;
  autorizacion: string | null;
}

export interface ChargeRequest {
  puntosRedimidos: number;
  pagos: RegisterPaymentRequest[];
}

export interface PaymentResponse {
  pagoId: number;
  facturaId: number;
  cuentaId: number;
  subcuentaId: number | null;
  metodoPago: string;
  monto: number;
  montoRecibido: number | null;
  cambioEntregado: number | null;
  referencia: string | null;
  autorizacion: string | null;
  totalFactura: number;
  totalPagado: number;
  montoPendiente: number;
  estadoCuenta: string;
  pagadoEn: string;
}

export interface ChargeResponse {
  facturaId: number;
  numeroDocumento: string;
  cuentaId: number;
  subtotal: number;
  puntosRedimidos: number;
  descuentoPuntos: number;
  porcentajeImpuesto: number;
  montoImpuesto: number;
  porcentajePropina: number;
  montoPropina: number;
  totalFactura: number;
  puntosOtorgados: number;
  saldoPuntosResultante: number;
  totalPagado: number;
  montoPendiente: number;
  estadoCuenta: string;
  pagos: PaymentResponse[];
}
