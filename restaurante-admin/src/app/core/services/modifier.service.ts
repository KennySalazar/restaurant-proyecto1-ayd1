import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  CreateModifierRequest,
  ModifierDeactivationResponse,
  ModifierRegistrationResponse,
  ModifierSummary,
  ModifierUpdateResponse,
  UpdateModifierRequest,
} from '../models/modifier.models';

@Injectable({
  providedIn: 'root',
})
export class ModifierService {
  private readonly http = inject(HttpClient);
  private readonly modifiersUrl = `${environment.apiBaseUrl}/admin/modifiers`;

  listModifiers(dishId?: number | null, search?: string | null): Observable<ModifierSummary[]> {
    let params = new HttpParams();

    if (dishId != null) {
      params = params.set('dishId', dishId);
    }

    if (search && search.trim().length > 0) {
      params = params.set('search', search.trim());
    }

    return this.http.get<ModifierSummary[]>(this.modifiersUrl, { params });
  }

  getModifier(id: number): Observable<ModifierSummary> {
    return this.http.get<ModifierSummary>(`${this.modifiersUrl}/${id}`);
  }

  registerModifier(payload: CreateModifierRequest): Observable<ModifierRegistrationResponse> {
    return this.http.post<ModifierRegistrationResponse>(this.modifiersUrl, payload);
  }

  updateModifier(id: number, payload: UpdateModifierRequest): Observable<ModifierUpdateResponse> {
    return this.http.put<ModifierUpdateResponse>(`${this.modifiersUrl}/${id}`, payload);
  }

  deactivateModifier(id: number): Observable<ModifierDeactivationResponse> {
    return this.http.delete<ModifierDeactivationResponse>(`${this.modifiersUrl}/${id}`);
  }
}
