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
import { TableOccupancyReport } from '../models/table-occupancy-report.models';

@Injectable({
  providedIn: 'root',
})
export class TableOccupancyReportService {
  private readonly http = inject(HttpClient);

  private readonly reportUrl =
    `${environment.apiBaseUrl}/admin/reportes/ocupacion-mesas`;

  getReport(
    startDate: string,
    endDate: string,
  ): Observable<TableOccupancyReport> {
    const params = new HttpParams()
      .set('fechaInicio', startDate)
      .set('fechaFin', endDate);

    return this.http.get<TableOccupancyReport>(
      this.reportUrl,
      {
        params,
      },
    );
  }
}