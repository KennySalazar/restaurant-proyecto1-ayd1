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

export interface ModifierIngredient {
  id: number;
  supplyId: number;
  supplyCode: string;
  supplyName: string;
  quantity: number;
  measurementUnitId: number | null;
  measurementUnitName: string;
  measurementUnitAbbreviation: string;
  adjustmentType: string;
  unitCost: number;
  subtotalCost: number;
  notes: string | null;
}

export interface ModifierRecipe {
  id: number;
  modifierId: number;
  modifierCode: string;
  modifierName: string;
  additionalPrice: number;
  versionNumber: number;
  status: string;
  changeReason: string | null;
  effectiveFrom: string;
  totalCost: number;
  ingredients: ModifierIngredient[];
  createdById: number | null;
  createdAt: string;
}

export interface ModifierIngredientEdit {
  supplyId: number | null;
  quantity: number | null;
  measurementUnitId: number | null;
  notes: string | null;
}

export interface DefineModifierRecipeRequest {
  ingredients: ModifierIngredientEdit[];
  changeReason?: string | null;
}

export interface ModifierRecipeRegistrationResponse {
  message: string;
  recipe: ModifierRecipe;
}

export interface ProductionCostIngredient {
  supplyId: number;
  supplyCode: string;
  supplyName: string;
  quantity: number;
  recipeUnitId: number | null;
  recipeUnitName: string;
  recipeUnitAbbreviation: string;
  stockUnitName: string;
  unitCost: number;
  proportionalQuantity: number;
  subtotalCost: number;
  notes: string | null;
}

export interface DishCostSummary {
  dishId: number;
  dishCode: string;
  dishName: string;
  categoryName: string;
  salePrice: number | null;
  recipeVersionId: number;
  recipeVersionNumber: number;
  totalProductionCost: number;
  grossMargin: number;
  marginPercentage: number;
}

export interface DishProductionCost {
  dishId: number;
  dishCode: string;
  dishName: string;
  categoryName: string;
  salePrice: number | null;
  recipeVersionId: number;
  recipeVersionNumber: number;
  totalProductionCost: number;
  grossMargin: number;
  marginPercentage: number;
  ingredients: ProductionCostIngredient[];
  calculatedAt: string;
}

export interface ModifierProductionCost {
  modifierId: number;
  modifierCode: string;
  modifierName: string;
  additionalPrice: number;
  recipeVersionId: number;
  recipeVersionNumber: number;
  totalProductionCost: number;
  grossMargin: number;
  marginPercentage: number;
  ingredients: ProductionCostIngredient[];
  calculatedAt: string;
}
