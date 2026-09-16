export type DishUnavailabilityReason = 'MANUAL' | 'FALTA_INSUMOS' | 'SIN_RECETA' | 'INACTIVO';

export interface DishCategory {
  id: number;
  code: string;
  name: string;
  visualOrder: number;
}

export interface DishSummary {
  id: number;
  code: string;
  name: string;
  description: string | null;
  categoryId: number;
  categoryName: string;
  salePrice: number;
  imageUrl: string | null;
  preparationTimeMinutes: number | null;
  available: boolean;
  availablePortions: number | null;
  unavailabilityReason: DishUnavailabilityReason | null;
  unavailabilityReasonDescription: string | null;
  manualAvailable: boolean;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateDishRequest {
  code?: string | null;
  name: string;
  description?: string | null;
  categoryId: number;
  salePrice: number;
  preparationTimeMinutes: number;
}

export interface DishRegistrationResponse {
  message: string;
  dish: DishSummary;
}

export interface UpdateDishRequest {
  code?: string | null;
  name: string;
  description?: string | null;
  categoryId: number;
  salePrice: number;
  preparationTimeMinutes: number;
}

export interface DishUpdateResponse {
  message: string;
  dish: DishSummary;
}
