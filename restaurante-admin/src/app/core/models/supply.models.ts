export interface Supply {
  id: number;
  code: string;
  name: string;
  description: string;
  categoryId: number;
  categoryName: string;
  unitId: number;
  unitName: string;
  unitAbbreviation: string;
  unitCost: number;
  currentStock: number;
  minimumStock: number;
  maximumStock: number;
  active: boolean;
  createdAt: string;
}

export interface SupplyCategory {
  id: number;
  name: string;
  description: string;
}

export interface MeasurementUnit {
  id: number;
  code: string;
  name: string;
  abbreviation: string;
  dimension: string;
}
