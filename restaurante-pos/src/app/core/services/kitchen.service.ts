import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  DishPreparationStatusResponse,
  DishUnavailableResponse,
  KitchenComandaResponse,
  MarkDishUnavailableRequest,
  UpdateDishPreparationStatusRequest,
} from '../models/kitchen.models';

@Injectable({ providedIn: 'root' })
export class KitchenService {
  private readonly kitchenUrl = `${environment.apiBaseUrl}/operacion/cocina`;

  constructor(private readonly http: HttpClient) {}

  getActiveComandas(soloRetrasadas?: boolean): Observable<KitchenComandaResponse[]> {
    let params = new HttpParams();
    if (soloRetrasadas) {
      params = params.set('soloRetrasadas', 'true');
    }

    return this.http.get<KitchenComandaResponse[]>(`${this.kitchenUrl}/comandas`, { params });
  }

  updateDishPreparationStatus(
    detailId: number,
    request: UpdateDishPreparationStatusRequest,
  ): Observable<DishPreparationStatusResponse> {
    return this.http.put<DishPreparationStatusResponse>(
      `${this.kitchenUrl}/platillos/${detailId}/estado`,
      request,
    );
  }

  markDishAsUnavailable(
    detailId: number,
    request: MarkDishUnavailableRequest,
  ): Observable<DishUnavailableResponse> {
    return this.http.post<DishUnavailableResponse>(
      `${this.kitchenUrl}/platillos/${detailId}/no-disponible`,
      request,
    );
  }
}
