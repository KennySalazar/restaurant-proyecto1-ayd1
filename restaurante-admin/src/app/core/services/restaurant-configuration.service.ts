import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  TipConfiguration,
  UpdateTipConfigurationRequest,
  PointsAccumulationConfiguration,
  UpdatePointsAccumulationConfigurationRequest,
} from '../models/configuration.models';

@Injectable({
  providedIn: 'root',
})
export class RestaurantConfigurationService {
  private readonly http = inject(HttpClient);

  private readonly tipUrl =
    `${environment.apiBaseUrl}/admin/configuracion/propina`;

    private readonly pointsAccumulationUrl =
  `${environment.apiBaseUrl}/admin/configuracion/fidelizacion/acumulacion-puntos`;

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

  getPointsAccumulationConfiguration():
  Observable<PointsAccumulationConfiguration> {
  return this.http.get<PointsAccumulationConfiguration>(
    this.pointsAccumulationUrl,
  );
}

updatePointsAccumulationConfiguration(
  request: UpdatePointsAccumulationConfigurationRequest,
): Observable<PointsAccumulationConfiguration> {
  return this.http.put<PointsAccumulationConfiguration>(
    this.pointsAccumulationUrl,
    request,
  );
}
}