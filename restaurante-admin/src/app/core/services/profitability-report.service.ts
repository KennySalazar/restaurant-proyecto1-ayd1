import {
  HttpClient,
  HttpParams,
} from '@angular/common/http';
import {
  inject,
  Injectable,
} from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  CurrentDishProfitability,
  HistoricalProfitabilityReport,
} from '../models/profitability-report.models';

@Injectable({
  providedIn: 'root',
})
export class ProfitabilityReportService {
  private readonly http = inject(HttpClient);

  private readonly profitabilityUrl =
    `${environment.apiBaseUrl}/admin/reportes/rentabilidad`;

  getCurrentProfitability():
    Observable<CurrentDishProfitability[]> {
    return this.http.get<CurrentDishProfitability[]>(
      `${this.profitabilityUrl}/actual`,
    );
  }

  getHistoricalProfitability(
    startDate: string,
    endDate: string,
  ): Observable<HistoricalProfitabilityReport> {
    const params = new HttpParams()
      .set('fechaInicio', startDate)
      .set('fechaFin', endDate);

    return this.http.get<HistoricalProfitabilityReport>(
      `${this.profitabilityUrl}/historico`,
      {
        params,
      },
    );
  }
}