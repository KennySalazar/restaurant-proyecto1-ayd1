import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  BillingAccount,
  BillingCalculation,
  BillingSubaccount,
} from '../models/billing.models';

@Injectable({ providedIn: 'root' })
export class BillingService {
  private readonly cashAccountsUrl =
    `${environment.apiBaseUrl}/caja/cuentas`;

  constructor(private readonly http: HttpClient) {}

  getAccounts(): Observable<BillingAccount[]> {
    return this.http.get<BillingAccount[]>(
      this.cashAccountsUrl,
    );
  }

  getPendingSubaccounts(
    accountId: number,
  ): Observable<BillingSubaccount[]> {
    return this.http.get<BillingSubaccount[]>(
      `${this.cashAccountsUrl}/${accountId}/subcuentas`,
    );
  }

  calculateAccount(
    accountId: number,
  ): Observable<BillingCalculation> {
    return this.http.get<BillingCalculation>(
      `${this.cashAccountsUrl}/${accountId}/calculo`,
    );
  }

  calculateSubaccount(
    accountId: number,
    subaccountId: number,
  ): Observable<BillingCalculation> {
    return this.http.get<BillingCalculation>(
      `${this.cashAccountsUrl}/${accountId}/subcuentas/${subaccountId}/calculo`,
    );
  }
}
