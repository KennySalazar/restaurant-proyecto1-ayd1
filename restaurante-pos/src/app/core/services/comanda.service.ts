import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AccountRoundResponse,
  AddDishResponse,
  AddDishToAccountRequest,
  CancelUnsentDishResponse,
  ComandaDetailStatus,
  ComandaDishProgressResponse,
  ComandaInventoryProcessResponse,
  DeliverDishRequest,
  DishCancellationExceptionResponse,
  RegisterDishCancellationRequest,
} from '../models/comanda.models';
import {
  SplitAccountResponse,
  SplitByItemsRequest,
  SplitByPeopleRequest,
  SubaccountResponse,
} from '../models/subaccount.models';

export interface DishProgressFilters {
  mesaId?: number;
  cuentaId?: number;
  comandaId?: number;
  estado?: ComandaDetailStatus;
  meseroId?: number;
  soloRetrasados?: boolean;
}

@Injectable({ providedIn: 'root' })
export class ComandaService {
  private readonly accountsUrl = `${environment.apiBaseUrl}/operacion/cuentas`;
  private readonly comandasUrl = `${environment.apiBaseUrl}/operacion/comandas`;

  constructor(private readonly http: HttpClient) {}

  addDishesToAccount(accountId: number, request: AddDishToAccountRequest): Observable<AddDishResponse> {
    return this.http.post<AddDishResponse>(`${this.accountsUrl}/${accountId}/platillos`, request);
  }

  sendComandaByAccount(accountId: number): Observable<ComandaInventoryProcessResponse> {
    return this.http.post<ComandaInventoryProcessResponse>(
      `${this.accountsUrl}/${accountId}/enviar-comanda`,
      {},
    );
  }

  getAccountRounds(accountId: number): Observable<AccountRoundResponse[]> {
    return this.http.get<AccountRoundResponse[]>(`${this.accountsUrl}/${accountId}/rondas`);
  }

  getDishProgress(filters: DishProgressFilters): Observable<ComandaDishProgressResponse[]> {
    let params = new HttpParams();
    for (const [key, value] of Object.entries(filters)) {
      if (value !== undefined && value !== null) {
        params = params.set(key, String(value));
      }
    }

    return this.http.get<ComandaDishProgressResponse[]>(`${this.comandasUrl}/platillos/avance`, {
      params,
    });
  }

  getDelayedDishAlerts(meseroId?: number): Observable<ComandaDishProgressResponse[]> {
    let params = new HttpParams();
    if (meseroId !== undefined) {
      params = params.set('meseroId', String(meseroId));
    }

    return this.http.get<ComandaDishProgressResponse[]>(`${this.comandasUrl}/alertas/tiempo-excedido`, {
      params,
    });
  }

  markDishAsDelivered(detailId: number, request?: DeliverDishRequest): Observable<ComandaDishProgressResponse> {
    return this.http.post<ComandaDishProgressResponse>(
      `${this.comandasUrl}/platillos/${detailId}/entregar`,
      request ?? {},
    );
  }

  cancelUnsentDish(detailId: number): Observable<CancelUnsentDishResponse> {
    return this.http.delete<CancelUnsentDishResponse>(`${this.comandasUrl}/platillos/${detailId}`);
  }

  registerDishCancellationException(
    detailId: number,
    request: RegisterDishCancellationRequest,
  ): Observable<DishCancellationExceptionResponse> {
    return this.http.post<DishCancellationExceptionResponse>(
      `${this.comandasUrl}/platillos/${detailId}/cancelar`,
      request,
    );
  }

  splitByPeople(accountId: number, request: SplitByPeopleRequest): Observable<SplitAccountResponse> {
    return this.http.post<SplitAccountResponse>(
      `${this.accountsUrl}/${accountId}/dividir/personas`,
      request,
    );
  }

  splitByItems(accountId: number, request: SplitByItemsRequest): Observable<SplitAccountResponse> {
    return this.http.post<SplitAccountResponse>(
      `${this.accountsUrl}/${accountId}/dividir/items`,
      request,
    );
  }

  getSubaccounts(accountId: number): Observable<SubaccountResponse[]> {
    return this.http.get<SubaccountResponse[]>(`${this.accountsUrl}/${accountId}/subcuentas`);
  }
}
