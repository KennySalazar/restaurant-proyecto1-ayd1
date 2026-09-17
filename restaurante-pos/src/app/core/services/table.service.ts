import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { RestaurantTable } from '../models/table.models';

@Injectable({ providedIn: 'root' })
export class TableService {
  private readonly tablesUrl = `${environment.apiBaseUrl}/operacion/mesas`;

  constructor(private readonly http: HttpClient) {}

  getTables(): Observable<RestaurantTable[]> {
    return this.http.get<RestaurantTable[]>(this.tablesUrl);
  }
}
