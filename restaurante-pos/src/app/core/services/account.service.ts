import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  Account,
  ActiveFusionResponse,
  MergeAccountsRequest,
  MergeAccountsResponse,
  OpenAccountRequest,
  TransferAccountRequest,
  TransferAccountResponse,
} from '../models/account.models';

@Injectable({ providedIn: 'root' })
export class AccountService {
  private readonly tablesUrl = `${environment.apiBaseUrl}/operacion/mesas`;
  private readonly accountsUrl = `${environment.apiBaseUrl}/operacion/cuentas`;

  constructor(private readonly http: HttpClient) {}

  openAccount(tableId: number, request: OpenAccountRequest): Observable<Account> {
    return this.http.post<Account>(`${this.tablesUrl}/${tableId}/abrir-cuenta`, request);
  }

  getActiveAccount(tableId: number): Observable<Account> {
    return this.http.get<Account>(`${this.tablesUrl}/${tableId}/cuenta`);
  }

  getActiveAccounts(): Observable<Account[]> {
    return this.http.get<Account[]>(this.accountsUrl);
  }

  transferAccount(
    accountId: number,
    request: TransferAccountRequest,
  ): Observable<TransferAccountResponse> {
    return this.http.post<TransferAccountResponse>(
      `${this.accountsUrl}/${accountId}/transferir`,
      request,
    );
  }

  mergeAccounts(request: MergeAccountsRequest): Observable<MergeAccountsResponse> {
    return this.http.post<MergeAccountsResponse>(`${this.accountsUrl}/fusionar`, request);
  }

  getActiveFusions(): Observable<ActiveFusionResponse[]> {
    return this.http.get<ActiveFusionResponse[]>(`${this.accountsUrl}/fusiones-activas`);
  }
}
