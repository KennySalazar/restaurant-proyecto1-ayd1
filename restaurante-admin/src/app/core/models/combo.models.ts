export interface ComboDetail {
  id: number;
  dishId: number;
  dishCode: string;
  dishName: string;
  dishRegularPrice: number;
  quantity: number;
  visualOrder: number;
}

export interface ComboSummary {
  id: number;
  code: string;
  name: string;
  description: string | null;
  salePrice: number;
  regularPriceSum: number;
  estimatedSavings: number;
  imageUrl: string | null;
  preparationTimeMinutes: number | null;
  manualAvailable: boolean;
  startDate: string | null;
  endDate: string | null;
  active: boolean;
  items: ComboDetail[];
  createdAt: string;
  updatedAt: string;
}

export interface ComboItemRequest {
  dishId: number;
  quantity: number;
}

export interface CreateComboRequest {
  code?: string | null;
  name: string;
  description?: string | null;
  salePrice: number;
  preparationTimeMinutes?: number | null;
  startDate?: string | null;
  endDate?: string | null;
  items: ComboItemRequest[];
}

export interface UpdateComboRequest {
  code?: string | null;
  name: string;
  description?: string | null;
  salePrice: number;
  preparationTimeMinutes?: number | null;
  startDate?: string | null;
  endDate?: string | null;
  items: ComboItemRequest[];
}

export interface ComboRegistrationResponse {
  message: string;
  combo: ComboSummary;
}

export interface ComboUpdateResponse {
  message: string;
  combo: ComboSummary;
}

export interface ComboRetirementResponse {
  message: string;
  combo: ComboSummary;
}
