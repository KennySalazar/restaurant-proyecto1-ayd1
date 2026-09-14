import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  TipConfiguration,
  UpdateTipConfigurationRequest,
} from '../models/configuration.models';

@Injectable({
  providedIn: 'root',
})
export class RestaurantConfigurationService {
  private readonly http = inject(HttpClient);

  private readonly tipUrl =
    `${environment.apiBaseUrl}/admin/configuracion/propina`;

  getTipConfiguration(): Observable<TipConfiguration> {
    return this.http.get<TipConfiguration>(this.tipUrl);
  }

  updateTipConfiguration(
    request: UpdateTipConfigurationRequest,
  ): Observable<TipConfiguration> {
    return this.http.put<TipConfiguration>(
      this.tipUrl,
      request,
    );
  }
}