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

export interface ConfigureStockLimitsRequest {
  minimumStock: number;
  maximumStock?: number | null;
}

export interface SupplyStockLimitsResponse {
  message: string;
  supply: Supply;
}

export type SupplyAlertLevel = 'BAJO' | 'AGOTADO';

export type AlertPriority = 'ALTA' | 'CRITICA';

export interface SupplyAlert {
  supplyId: number;
  supplyCode: string;
  supplyName: string;
  categoryId: number;
  categoryName: string;
  measurementUnitId: number;
  measurementUnitName: string;
  measurementUnitAbbreviation: string;
  currentStock: number;
  minimumStock: number;
  maximumStock: number | null;
  deficit: number;
  alertLevel: SupplyAlertLevel;
  priority: AlertPriority;
  message: string;
  timestamp: string;
}

export interface SupplyAlertSummary {
  totalAlerts: number;
  outOfStockCount: number;
  lowStockCount: number;
  alerts: SupplyAlert[];
}

export interface SingleSupplyAlertStatusResponse {
  hasAlert: boolean;
  alert: SupplyAlert | null;
}
