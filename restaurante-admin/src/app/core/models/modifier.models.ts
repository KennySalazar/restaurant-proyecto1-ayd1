export interface ModifierDishItem {
  id: number;
  code: string;
  name: string;
  categoryName: string;
  required: boolean | null;
  maxSelections: number | null;
}

export interface ModifierSummary {
  id: number;
  code: string;
  name: string;
  description: string | null;
  additionalPrice: number;
  active: boolean;
  hasRecipe: boolean;
  associatedDishes: ModifierDishItem[];
  createdAt: string;
  updatedAt: string;
}

export interface CreateModifierRequest {
  code?: string | null;
  name: string;
  description?: string | null;
  additionalPrice?: number | null;
  dishIds: number[];
}

export interface UpdateModifierRequest {
  code?: string | null;
  name: string;
  description?: string | null;
  additionalPrice?: number | null;
  dishIds?: number[];
}

export interface ModifierRegistrationResponse {
  message: string;
  modifier: ModifierSummary;
}

export interface ModifierUpdateResponse {
  message: string;
  modifier: ModifierSummary;
}

export interface ModifierDeactivationResponse {
  message: string;
  modifier: ModifierSummary;
}
