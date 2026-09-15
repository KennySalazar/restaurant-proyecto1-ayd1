import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  CreateReservationRequest,
  ReservationResponse,
} from '../models/reservation.models';

@Injectable({
  providedIn: 'root',
})
export class ReservationService {
  private readonly http = inject(HttpClient);

  private readonly reservationsUrl =
    `${environment.apiBaseUrl}/admin/reservas`;

  createReservation(
    request: CreateReservationRequest,
  ): Observable<ReservationResponse> {
    return this.http.post<ReservationResponse>(
      this.reservationsUrl,
      request,
    );
  }
}