import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  CreateDishRequest,
  DishCategory,
  DishRegistrationResponse,
  DishSummary,
} from '../models/dish.models';

@Injectable({
  providedIn: 'root',
})
export class DishService {
  private readonly http = inject(HttpClient);
  private readonly dishesUrl = `${environment.apiBaseUrl}/admin/dishes`;

  listDishes(categoryId?: number | null, search?: string | null): Observable<DishSummary[]> {
    let params = new HttpParams();

    if (categoryId != null) {
      params = params.set('categoryId', categoryId);
    }

    if (search && search.trim().length > 0) {
      params = params.set('search', search.trim());
    }

    return this.http.get<DishSummary[]>(this.dishesUrl, { params });
  }

  listCategories(): Observable<DishCategory[]> {
    return this.http.get<DishCategory[]>(`${this.dishesUrl}/categories`);
  }

  registerDish(payload: CreateDishRequest): Observable<DishRegistrationResponse> {
    return this.http.post<DishRegistrationResponse>(this.dishesUrl, payload);
  }
}
