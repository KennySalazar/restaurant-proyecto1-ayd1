import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  ServiceRatingRequest,
  ServiceRatingResponse,
} from '../models/service-rating.models';

@Injectable({ providedIn: 'root' })
export class ServiceRatingService {
  private readonly cashInvoicesUrl =
    `${environment.apiBaseUrl}/caja/facturas`;

  constructor(private readonly http: HttpClient) {}

  registerRating(
    invoiceId: number,
    request: ServiceRatingRequest,
  ): Observable<ServiceRatingResponse> {
    return this.http.post<ServiceRatingResponse>(
      `${this.cashInvoicesUrl}/${invoiceId}/calificacion`,
      request,
    );
  }
}
