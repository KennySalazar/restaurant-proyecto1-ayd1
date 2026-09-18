import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CustomerPoints } from '../models/loyalty.models';

@Injectable({ providedIn: 'root' })
export class LoyaltyService {
  private readonly cashCustomersUrl =
    `${environment.apiBaseUrl}/caja/clientes`;

  constructor(private readonly http: HttpClient) {}

  getCustomerPoints(
    customerId: number,
  ): Observable<CustomerPoints> {
    return this.http.get<CustomerPoints>(
      `${this.cashCustomersUrl}/${customerId}/puntos`,
    );
  }
}
