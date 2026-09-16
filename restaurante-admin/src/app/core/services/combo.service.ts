import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  ComboRegistrationResponse,
  ComboRetirementResponse,
  ComboSummary,
  ComboUpdateResponse,
  CreateComboRequest,
  UpdateComboRequest,
} from '../models/combo.models';

@Injectable({
  providedIn: 'root',
})
export class ComboService {
  private readonly http = inject(HttpClient);
  private readonly combosUrl = `${environment.apiBaseUrl}/admin/combos`;

  listCombos(search?: string | null, active?: boolean | null): Observable<ComboSummary[]> {
    let params = new HttpParams();

    if (search && search.trim().length > 0) {
      params = params.set('search', search.trim());
    }

    if (active != null) {
      params = params.set('active', active);
    }

    return this.http.get<ComboSummary[]>(this.combosUrl, { params });
  }

  getCombo(id: number): Observable<ComboSummary> {
    return this.http.get<ComboSummary>(`${this.combosUrl}/${id}`);
  }

  createCombo(payload: CreateComboRequest): Observable<ComboRegistrationResponse> {
    return this.http.post<ComboRegistrationResponse>(this.combosUrl, payload);
  }

  updateCombo(id: number, payload: UpdateComboRequest): Observable<ComboUpdateResponse> {
    return this.http.put<ComboUpdateResponse>(`${this.combosUrl}/${id}`, payload);
  }

  retireCombo(id: number): Observable<ComboRetirementResponse> {
    return this.http.delete<ComboRetirementResponse>(`${this.combosUrl}/${id}`);
  }
}
