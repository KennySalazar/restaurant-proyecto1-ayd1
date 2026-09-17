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

export type RecipeIngredientChangeType = 'AGREGADO' | 'RETIRADO' | 'MODIFICADO' | 'SIN_CAMBIOS';

export interface RecipeIngredientChange {
  supplyId: number;
  supplyCode: string;
  supplyName: string;
  changeType: RecipeIngredientChangeType;
  previousQuantity: number | null;
  previousUnitName: string | null;
  newQuantity: number | null;
  newUnitName: string | null;
  quantityDifference: number | null;
  costDifference: number;
  notes: string | null;
}

export interface RecipeVersionHistoryItem {
  versionId: number;
  versionNumber: number;
  status: string;
  changeReason: string | null;
  effectiveFrom: string;
  effectiveTo: string | null;
  totalCost: number;
  salePrice: number | null;
  grossMargin: number | null;
  marginPercentage: number | null;
  ingredientCount: number;
  ingredients: RecipeIngredient[];
  changes: RecipeIngredientChange[];
  createdById: number | null;
  createdAt: string;
}

export interface RecipeHistory {
  dishId: number;
  dishCode: string;
  dishName: string;
  currentVersionNumber: number | null;
  totalVersions: number;
  hasSubsequentChanges: boolean;
  message: string;
  versions: RecipeVersionHistoryItem[];
}

export interface RecipeVersionChangeDetail {
  dishId: number;
  dishCode: string;
  dishName: string;
  versionId: number;
  versionNumber: number;
  status: string;
  changeReason: string | null;
  effectiveFrom: string;
  effectiveTo: string | null;
  totalCost: number;
  previousVersionNumber: number | null;
  previousTotalCost: number | null;
  totalCostDifference: number | null;
  previousComposition: RecipeIngredient[];
  newComposition: RecipeIngredient[];
  changes: RecipeIngredientChange[];
  createdById: number | null;
  createdAt: string;
}

export interface ModifierIngredientChange {
  supplyId: number;
  supplyCode: string;
  supplyName: string;
  changeType: RecipeIngredientChangeType;
  previousAdjustmentType: string | null;
  previousQuantity: number | null;
  previousUnitName: string | null;
  newAdjustmentType: string | null;
  newQuantity: number | null;
  newUnitName: string | null;
  quantityDifference: number | null;
  costDifference: number;
  notes: string | null;
}

export interface ModifierRecipeVersionHistoryItem {
  versionId: number;
  versionNumber: number;
  status: string;
  changeReason: string | null;
  effectiveFrom: string;
  effectiveTo: string | null;
  totalCost: number;
  ingredientCount: number;
  ingredients: ModifierIngredient[];
  changes: ModifierIngredientChange[];
  createdById: number | null;
  createdAt: string;
}

export interface ModifierRecipeHistory {
  modifierId: number;
  modifierCode: string;
  modifierName: string;
  additionalPrice: number;
  currentVersionNumber: number | null;
  totalVersions: number;
  hasSubsequentChanges: boolean;
  message: string;
  versions: ModifierRecipeVersionHistoryItem[];
}

export interface ModifierRecipeVersionChangeDetail {
  modifierId: number;
  modifierCode: string;
  modifierName: string;
  additionalPrice: number;
  versionId: number;
  versionNumber: number;
  status: string;
  changeReason: string | null;
  effectiveFrom: string;
  effectiveTo: string | null;
  totalCost: number;
  previousVersionNumber: number | null;
  previousTotalCost: number | null;
  totalCostDifference: number | null;
  previousComposition: ModifierIngredient[];
  newComposition: ModifierIngredient[];
  changes: ModifierIngredientChange[];
  createdById: number | null;
  createdAt: string;
}
