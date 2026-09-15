import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  CreateSupplyRequest,
  MeasurementUnit,
  Supply,
  SupplyCategory,
  SupplyRegistrationResponse,
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

  listCategories(): Observable<SupplyCategory[]> {
    return this.http.get<SupplyCategory[]>(`${this.suppliesUrl}/categories`);
  }

  listMeasurementUnits(): Observable<MeasurementUnit[]> {
    return this.http.get<MeasurementUnit[]>(`${this.suppliesUrl}/measurement-units`);
  }
}
