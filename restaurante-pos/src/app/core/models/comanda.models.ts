export const COMANDA_DETAIL_STATUSES = [
  'BORRADOR',
  'RECIBIDO',
  'EN_PREPARACION',
  'LISTO',
  'ENTREGADO',
  'NO_DISPONIBLE',
  'CANCELADO',
] as const;

export type ComandaDetailStatus = (typeof COMANDA_DETAIL_STATUSES)[number];

export interface AddDishItemRequest {
  platilloId: number;
  cantidad: number;
  notas?: string | null;
  modificadoresIds?: number[] | null;
}

export interface AddDishToAccountRequest {
  items: AddDishItemRequest[];
}

export interface DishOrderDetailResponse {
  detalleId: number;
  platilloId: number;
  nombrePlatillo: string;
  cantidad: number;
  precioUnitario: number;
  subtotal: number;
  notas: string | null;
  modificadores: string[];
  estado: string;
}

export interface AddDishResponse {
  mensaje: string;
  cuentaId: number;
  numeroCuenta: string;
  mesaId: number;
  numeroMesa: string;
  comandaId: number;
  numeroRonda: number;
  platillos: DishOrderDetailResponse[];
  subtotalAgregado: number;
}

export interface ComandaItemResponse {
  id: number;
  dishId: number | null;
  comboId: number | null;
  name: string;
  quantity: number;
  unitPrice: number;
  status: string;
  specialNotes: string | null;
  modifiers: string[];
  estimatedTimeMinutes: number;
  elapsedMinutes: number;
  deadline: string | null;
  timeExceeded: boolean;
  delayMinutes: number;
  alertLevel: string;
}

export interface ComandaResponse {
  id: number;
  accountId: number;
  tableId: number;
  roundNumber: number;
  waiterId: number;
  status: string;
  generalNotes: string | null;
  createdAt: string;
  sentAt: string | null;
  items: ComandaItemResponse[];
}

export interface RejectedDishDetailResponse {
  detailId: number;
  dishName: string;
  reason: string;
}

export interface KardexMovementResponse {
  id: number;
  supplyId: number;
  supplyCode: string;
  supplyName: string;
  unit: string;
  type: string;
  quantity: number;
  previousStock: number;
  resultingStock: number;
  reason: string;
  responsibleUserId: number;
  createdAt: string;
}

export interface ComandaInventoryProcessResponse {
  message: string;
  comanda: ComandaResponse;
  movements: KardexMovementResponse[];
  rejectedDishes: RejectedDishDetailResponse[];
}

export interface CreateComandaItemRequest {
  dishId?: number | null;
  comboId?: number | null;
  quantity: number;
  specialNotes?: string | null;
  modifierIds?: number[] | null;
}

export interface CreateAccountRoundRequest {
  generalNotes?: string | null;
  sendImmediately?: boolean | null;
  items: CreateComandaItemRequest[];
}

export interface AccountRoundResponse {
  roundNumber: number;
  comandaId: number;
  accountId: number;
  tableId: number;
  tableNumber: string;
  status: string;
  generalNotes: string | null;
  waiterId: number;
  waiterName: string | null;
  createdAt: string;
  sentAt: string | null;
  finishedAt: string | null;
  items: ComandaItemResponse[];
  totalRoundsInAccount: number;
  message: string | null;
}

export interface ComandaDishProgressResponse {
  id: number;
  comandaId: number;
  accountId: number;
  tableId: number;
  tableNumber: string;
  roundNumber: number;
  dishId: number | null;
  comboId: number | null;
  name: string;
  quantity: number;
  unitPrice: number;
  status: string;
  previousStatus: string | null;
  specialNotes: string | null;
  modifiers: string[];
  receivedAt: string | null;
  preparationStartedAt: string | null;
  readyAt: string | null;
  deliveredAt: string | null;
  waiterId: number | null;
  waiterName: string | null;
  comandaStatus: string;
  message: string | null;
  estimatedTimeMinutes: number;
  elapsedMinutes: number;
  deadline: string | null;
  timeExceeded: boolean;
  delayMinutes: number;
  alertLevel: string;
}

export interface DeliverDishRequest {
  notes?: string | null;
}

export interface CancelUnsentDishResponse {
  detalleId: number;
  nombrePlatillo: string;
  cantidad: number;
  comandaId: number;
  cuentaId: number;
  numeroRonda: number;
  comandaEliminada: boolean;
  platillosRestantesEnComanda: number;
  mensaje: string;
}

export interface RegisterDishCancellationRequest {
  motivo: string;
  tipo?: string | null;
  accionInventario?: string | null;
  autorizadoPorId?: number | null;
}

export interface DishCancellationExceptionResponse {
  cancelacionId: number;
  detalleId: number;
  platilloNombre: string;
  cantidad: number;
  estadoAnterior: string;
  estadoNuevo: string;
  tipo: string | null;
  motivo: string;
  estadoSolicitud: string;
  accionInventario: string | null;
  solicitadaPorId: number;
  solicitadaPorNombre: string | null;
  autorizadaPorId: number | null;
  autorizadaPorNombre: string | null;
  registradaEn: string;
  comandaId: number;
  cuentaId: number;
  numeroMesa: string;
  mensaje: string;
}
