import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  CreateDishRequest,
  DishAvailabilityResponse,
  DishCategory,
  DishRegistrationResponse,
  DishRetirementResponse,
  DishSummary,
  DishUpdateResponse,
  UpdateDishRequest,
} from '../models/dish.models';

@Injectable({
  providedIn: 'root',
})
export class DishService {
  private readonly http = inject(HttpClient);
  private readonly dishesUrl = `${environment.apiBaseUrl}/admin/dishes`;

  listDishes(
    categoryId?: number | null,
    search?: string | null,
    active?: boolean | null,
  ): Observable<DishSummary[]> {
    let params = new HttpParams();

    if (categoryId != null) {
      params = params.set('categoryId', categoryId);
    }

    if (search && search.trim().length > 0) {
      params = params.set('search', search.trim());
    }

    if (active != null) {
      params = params.set('active', active);
    }

    return this.http.get<DishSummary[]>(this.dishesUrl, { params });
  }

  listCategories(): Observable<DishCategory[]> {
    return this.http.get<DishCategory[]>(`${this.dishesUrl}/categories`);
  }

  registerDish(payload: CreateDishRequest): Observable<DishRegistrationResponse> {
    return this.http.post<DishRegistrationResponse>(this.dishesUrl, payload);
  }

  updateDish(id: number, payload: UpdateDishRequest): Observable<DishUpdateResponse> {
    return this.http.put<DishUpdateResponse>(`${this.dishesUrl}/${id}`, payload);
  }

  retireDish(id: number): Observable<DishRetirementResponse> {
    return this.http.delete<DishRetirementResponse>(`${this.dishesUrl}/${id}`);
  }

  updateDishAvailability(
    id: number,
    manualAvailable: boolean,
  ): Observable<DishAvailabilityResponse> {
    return this.http.put<DishAvailabilityResponse>(`${this.dishesUrl}/${id}/availability`, {
      manualAvailable,
    });
  }
}
