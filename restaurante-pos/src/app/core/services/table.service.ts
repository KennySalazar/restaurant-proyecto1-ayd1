import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  SeatReservationRequest,
  SeatReservationResult,
  SeatWaitlistRequest,
  SeatWaitlistResult,
} from '../models/seating.models';
import { RestaurantTable } from '../models/table.models';

@Injectable({ providedIn: 'root' })
export class TableService {
  private readonly tablesUrl = `${environment.apiBaseUrl}/operacion/mesas`;

  constructor(private readonly http: HttpClient) {}

  getTables(): Observable<RestaurantTable[]> {
    return this.http.get<RestaurantTable[]>(this.tablesUrl);
  }

  getTable(tableId: number): Observable<RestaurantTable> {
    return this.http.get<RestaurantTable>(`${this.tablesUrl}/${tableId}`);
  }

  seatReservation(
    tableId: number,
    request: SeatReservationRequest,
  ): Observable<SeatReservationResult> {
    return this.http.post<SeatReservationResult>(
      `${this.tablesUrl}/${tableId}/sentar-reserva`,
      request,
    );
  }

  seatWaitlist(tableId: number, request: SeatWaitlistRequest): Observable<SeatWaitlistResult> {
    return this.http.post<SeatWaitlistResult>(
      `${this.tablesUrl}/${tableId}/sentar-espera`,
      request,
    );
  }
}
