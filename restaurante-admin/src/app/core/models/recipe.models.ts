export interface DishSummary {
  id: number;
  code: string;
  name: string;
  description: string;
  categoryId: number;
  categoryName: string;
  salePrice: number | null;
  imageUrl: string | null;
  preparationTimeMinutes: number | null;
  available: boolean;
  availablePortions: number | null;
  unavailabilityReason: string | null;
  unavailabilityReasonDescription: string | null;
  manualAvailable: boolean;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface RecipeIngredient {
  id: number;
  supplyId: number;
  supplyCode: string;
  supplyName: string;
  quantity: number;
  measurementUnitId: number | null;
  measurementUnitName: string;
  measurementUnitAbbreviation: string;
  unitCost: number;
  subtotalCost: number;
  notes: string | null;
}

export interface Recipe {
  id: number;
  dishId: number;
  dishCode: string;
  dishName: string;
  versionNumber: number;
  status: string;
  changeReason: string | null;
  effectiveFrom: string;
  totalCost: number;
  dishSalePrice: number | null;
  grossMargin: number | null;
  marginPercentage: number | null;
  ingredients: RecipeIngredient[];
  createdById: number | null;
  createdAt: string;
}

export interface RecipeIngredientEdit {
  supplyId: number | null;
  quantity: number | null;
  measurementUnitId: number | null;
  notes: string | null;
}

export interface DefineRecipeRequest {
  ingredients: RecipeIngredientEdit[];
  changeReason?: string | null;
}

export type UpdateRecipeRequest = DefineRecipeRequest;

export interface RecipeRegistrationResponse {
  message: string;
  recipe: Recipe;
}

export interface RecipeUpdateResponse {
  message: string;
  recipe: Recipe;
}
