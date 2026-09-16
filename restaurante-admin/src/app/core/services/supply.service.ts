import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  ConfigureStockLimitsRequest,
  CreateSupplyEntryRequest,
  CreateSupplyRequest,
  MeasurementUnit,
  SingleSupplyAlertStatusResponse,
  Supply,
  SupplyAlert,
  SupplyAlertSummary,
  SupplyCategory,
  SupplyEntryRegistrationResponse,
  SupplyRegistrationResponse,
  SupplyStockLimitsResponse,
  SupplyUpdateResponse,
  UpdateSupplyRequest,
} from '../models/supply.models';

@Injectable({
  providedIn: 'root',
})
export class SupplyService {
  private readonly http = inject(HttpClient);
  private readonly suppliesUrl = `${environment.apiBaseUrl}/admin/supplies`;

  listSupplies(categoryId?: number | null, search?: string | null): Observable<Supply[]> {
    let params = new HttpParams();

    if (categoryId != null) {
      params = params.set('categoryId', categoryId);
    }

    if (search && search.trim().length > 0) {
      params = params.set('search', search.trim());
    }

    return this.http.get<Supply[]>(this.suppliesUrl, { params });
  }

  getSupply(id: number): Observable<Supply> {
    return this.http.get<Supply>(`${this.suppliesUrl}/${id}`);
  }

  registerSupply(payload: CreateSupplyRequest): Observable<SupplyRegistrationResponse> {
    return this.http.post<SupplyRegistrationResponse>(this.suppliesUrl, payload);
  }

  updateSupply(id: number, payload: UpdateSupplyRequest): Observable<SupplyUpdateResponse> {
    return this.http.put<SupplyUpdateResponse>(`${this.suppliesUrl}/${id}`, payload);
  }

  configureStockLimits(
    id: number,
    payload: ConfigureStockLimitsRequest,
  ): Observable<SupplyStockLimitsResponse> {
    return this.http.put<SupplyStockLimitsResponse>(
      `${this.suppliesUrl}/${id}/stock-limits`,
      payload,
    );
  }

  listCategories(): Observable<SupplyCategory[]> {
    return this.http.get<SupplyCategory[]>(`${this.suppliesUrl}/categories`);
  }

  listMeasurementUnits(): Observable<MeasurementUnit[]> {
    return this.http.get<MeasurementUnit[]>(`${this.suppliesUrl}/measurement-units`);
  }

  listAlerts(categoryId?: number | null, level?: string | null): Observable<SupplyAlert[]> {
    let params = new HttpParams();

    if (categoryId != null) {
      params = params.set('categoryId', categoryId);
    }

    if (level) {
      params = params.set('level', level);
    }

    return this.http.get<SupplyAlert[]>(`${this.suppliesUrl}/alerts`, { params });
  }

  listAlertsSummary(): Observable<SupplyAlertSummary> {
    return this.http.get<SupplyAlertSummary>(`${this.suppliesUrl}/alerts/summary`);
  }

  getSupplyAlert(id: number): Observable<SingleSupplyAlertStatusResponse> {
    return this.http.get<SingleSupplyAlertStatusResponse>(`${this.suppliesUrl}/${id}/alert`);
  }

  registerSupplyEntry(
    payload: CreateSupplyEntryRequest,
  ): Observable<SupplyEntryRegistrationResponse> {
    return this.http.post<SupplyEntryRegistrationResponse>(`${this.suppliesUrl}/entries`, payload);
  }
}
