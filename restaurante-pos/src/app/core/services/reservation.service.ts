import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Reservation } from '../models/reservation.models';

@Injectable({ providedIn: 'root' })
export class ReservationService {
  private readonly reservationsUrl = `${environment.apiBaseUrl}/operacion/reservas`;

  constructor(private readonly http: HttpClient) {}

  getUpcomingReservations(): Observable<Reservation[]> {
    return this.http.get<Reservation[]>(this.reservationsUrl);
  }
}
