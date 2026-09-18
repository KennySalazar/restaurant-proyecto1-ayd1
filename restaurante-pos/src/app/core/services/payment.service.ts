import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  ChargeRequest,
  ChargeResponse,
} from '../models/payment.models';

@Injectable({ providedIn: 'root' })
export class PaymentService {
  private readonly cashAccountsUrl =
    `${environment.apiBaseUrl}/caja/cuentas`;

  constructor(private readonly http: HttpClient) {}

  chargeAccount(
    accountId: number,
    request: ChargeRequest,
  ): Observable<ChargeResponse> {
    return this.http.post<ChargeResponse>(
      `${this.cashAccountsUrl}/${accountId}/cobro`,
      request,
    );
  }

  chargeSubaccount(
    accountId: number,
    subaccountId: number,
    request: ChargeRequest,
  ): Observable<ChargeResponse> {
    return this.http.post<ChargeResponse>(
      `${this.cashAccountsUrl}/${accountId}/subcuentas/${subaccountId}/cobro`,
      request,
    );
  }
}
