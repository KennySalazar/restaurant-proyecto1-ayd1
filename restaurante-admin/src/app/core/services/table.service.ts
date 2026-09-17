import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  CreateTableRequest,
  RestaurantTable,
  TableZone,
  UpdateTableRequest,
} from '../models/table.models';

@Injectable({
  providedIn: 'root',
})
export class TableService {
  private readonly http = inject(HttpClient);
  private readonly tablesUrl = `${environment.apiBaseUrl}/admin/mesas`;

  getTables(): Observable<RestaurantTable[]> {
    return this.http.get<RestaurantTable[]>(this.tablesUrl);
  }

  getTable(id: number): Observable<RestaurantTable> {
    return this.http.get<RestaurantTable>(`${this.tablesUrl}/${id}`);
  }

  getZones(): Observable<TableZone[]> {
    return this.http.get<TableZone[]>(`${this.tablesUrl}/zonas`);
  }

  createTable(payload: CreateTableRequest): Observable<RestaurantTable> {
    return this.http.post<RestaurantTable>(this.tablesUrl, payload);
  }

  updateTable(
  id: number,
  payload: UpdateTableRequest,
): Observable<RestaurantTable> {
  return this.http.put<RestaurantTable>(
    `${this.tablesUrl}/${id}`,
    payload,
  );
}

  retireTable(id: number): Observable<RestaurantTable> {
    return this.http.delete<RestaurantTable>(`${this.tablesUrl}/${id}`);
  }
}