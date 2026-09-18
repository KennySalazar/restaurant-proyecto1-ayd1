export interface MenuModifier {
  id: number;
  code: string;
  name: string;
  additionalPrice: number;
  required: boolean;
  maxSelections: number;
}

export interface MenuDish {
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
  unavailabilityReason: string | null;
  modifiers: MenuModifier[];
}

export interface MenuCatalog {
  dishes: MenuDish[];
  combos: unknown[];
}
