import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  CashRegisterAvailability,
  CashShiftResponse,
  OpenCashShiftRequest,
} from '../models/cash-shift.models';

@Injectable({ providedIn: 'root' })
export class CashShiftService {
  private readonly cashUrl = `${environment.apiBaseUrl}/caja`;

  constructor(private readonly http: HttpClient) {}

  getCashRegisters(): Observable<CashRegisterAvailability[]> {
    return this.http.get<CashRegisterAvailability[]>(`${this.cashUrl}/cajas`);
  }

  openShift(request: OpenCashShiftRequest): Observable<CashShiftResponse> {
    return this.http.post<CashShiftResponse>(`${this.cashUrl}/turnos/abrir`, request);
  }
}
