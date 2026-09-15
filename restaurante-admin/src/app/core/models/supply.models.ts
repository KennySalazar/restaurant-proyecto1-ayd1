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

export interface CreateSupplyRequest {
  code?: string | null;
  name: string;
  description?: string | null;
  categoryId: number;
  unitId: number;
  unitCost: number | null;
  minimumStock?: number | null;
  maximumStock?: number | null;
}

export interface SupplyRegistrationResponse {
  message: string;
  supply: Supply;
}

export type UpdateSupplyRequest = CreateSupplyRequest;

export interface SupplyUpdateResponse {
  message: string;
  supply: Supply;
}
