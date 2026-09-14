import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { RestaurantTable } from '../models/table.models';

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

  retireTable(id: number): Observable<RestaurantTable> {
  return this.http.delete<RestaurantTable>(`${this.tablesUrl}/${id}`);
}
}