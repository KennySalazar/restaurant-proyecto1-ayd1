import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  CustomerPoints,
  CustomerRecord,
  RegisterCustomerRequest,
} from '../models/loyalty.models';

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

  findCustomerByPhone(
    phone: string,
  ): Observable<CustomerRecord> {
    const params = new HttpParams().set(
      'telefono',
      phone.trim(),
    );

    return this.http.get<CustomerRecord>(
      `${this.cashCustomersUrl}/buscar`,
      { params },
    );
  }

  registerAndAssociateCustomer(
    accountId: number,
    request: RegisterCustomerRequest,
  ): Observable<CustomerRecord> {
    return this.http.post<CustomerRecord>(
      `${this.cashCustomersUrl}/cuentas/${accountId}`,
      request,
    );
  }

  associateCustomer(
    accountId: number,
    customerId: number,
  ): Observable<CustomerRecord> {
    return this.http.put<CustomerRecord>(
      `${this.cashCustomersUrl}/${customerId}/cuentas/${accountId}`,
      {},
    );
  }
}
