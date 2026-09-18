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
  LoyaltyReport,
} from '../models/loyalty-report.models';

@Injectable({
  providedIn: 'root',
})
export class LoyaltyReportService {
  private readonly http =
    inject(HttpClient);

  private readonly reportUrl =
    `${environment.apiBaseUrl}/admin/reportes/fidelizacion`;

  getReport(
    startDate: string,
    endDate: string,
  ): Observable<LoyaltyReport> {
    const params = new HttpParams()
      .set('fechaInicio', startDate)
      .set('fechaFin', endDate);

    return this.http.get<LoyaltyReport>(
      this.reportUrl,
      {
        params,
      },
    );
  }
}