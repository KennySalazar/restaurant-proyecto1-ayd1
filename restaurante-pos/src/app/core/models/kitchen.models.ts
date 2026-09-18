export interface KitchenComandaItemResponse {
  id: number;
  dishId: number | null;
  comboId: number | null;
  name: string;
  quantity: number;
  status: string;
  estimatedTimeMinutes: number;
  specialNotes: string | null;
  modifiers: string[];
  receivedAt: string | null;
  preparationStartedAt: string | null;
  deadline: string | null;
  elapsedMinutes: number;
  timeExceeded: boolean;
  delayMinutes: number;
  alertLevel: string;
}

export interface KitchenComandaResponse {
  id: number;
  accountId: number;
  accountNumber: string;
  tableId: number;
  tableNumber: string;
  roundNumber: number;
  waiterId: number;
  waiterName: string | null;
  status: string;
  sentAt: string | null;
  createdAt: string;
  elapsedMinutes: number;
  estimatedPreparationTimeMinutes: number;
  generalNotes: string | null;
  items: KitchenComandaItemResponse[];
  timeExceeded: boolean;
  delayedItemsCount: number;
  maxDelayMinutes: number;
  alertLevel: string;
}

export type DishPreparationTransition = 'EN_PREPARACION' | 'LISTO' | 'NO_DISPONIBLE';

export interface UpdateDishPreparationStatusRequest {
  status: DishPreparationTransition;
}

export interface DishPreparationStatusResponse {
  id: number;
  comandaId: number;
  accountId: number;
  tableId: number;
  tableNumber: string;
  dishName: string;
  quantity: number;
  previousStatus: string;
  currentStatus: string;
  preparationStartedAt: string | null;
  readyAt: string | null;
  comandaStatus: string;
  message: string;
}

export interface MarkDishUnavailableRequest {
  reason?: string | null;
  disableInMenu?: boolean | null;
}

export interface DishUnavailableResponse {
  id: number;
  comandaId: number;
  accountId: number;
  tableId: number;
  tableNumber: string;
  dishId: number | null;
  dishName: string;
  quantity: number;
  previousStatus: string;
  currentStatus: string;
  reason: string | null;
  menuDisabled: boolean;
  waiterId: number | null;
  waiterName: string | null;
  comandaStatus: string;
  markedAt: string;
  message: string;
}
