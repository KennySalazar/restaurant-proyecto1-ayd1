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
import { SalesReport } from '../models/sales-report.models';

@Injectable({
  providedIn: 'root',
})
export class SalesReportService {
  private readonly http = inject(HttpClient);

  private readonly salesReportUrl =
    `${environment.apiBaseUrl}/admin/reportes/ventas`;

  getReport(
    startDate: string,
    endDate: string,
  ): Observable<SalesReport> {
    const params = new HttpParams()
      .set('fechaInicio', startDate)
      .set('fechaFin', endDate);

    return this.http.get<SalesReport>(
      this.salesReportUrl,
      {
        params,
      },
    );
  }
}