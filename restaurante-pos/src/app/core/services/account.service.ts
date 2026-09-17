import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Account, OpenAccountRequest } from '../models/account.models';

@Injectable({ providedIn: 'root' })
export class AccountService {
  private readonly tablesUrl = `${environment.apiBaseUrl}/operacion/mesas`;

  constructor(private readonly http: HttpClient) {}

  openAccount(tableId: number, request: OpenAccountRequest): Observable<Account> {
    return this.http.post<Account>(`${this.tablesUrl}/${tableId}/abrir-cuenta`, request);
  }

  getActiveAccount(tableId: number): Observable<Account> {
    return this.http.get<Account>(`${this.tablesUrl}/${tableId}/cuenta`);
  }
}
