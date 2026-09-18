import {
  HttpClient,
  HttpParams,
  HttpResponse,
} from '@angular/common/http';
import {
  inject,
  Injectable,
} from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  ReportExportRequest,
} from '../models/report-export.models';

@Injectable({
  providedIn: 'root',
})
export class ReportExportService {
  private readonly http = inject(HttpClient);

  private readonly exportUrl =
    `${environment.apiBaseUrl}/admin/reportes/exportar`;

  exportReport(
    request: ReportExportRequest,
  ): Observable<HttpResponse<Blob>> {
    let params = new HttpParams()
      .set('tipo', request.tipo)
      .set('formato', request.formato);

    if (request.fechaInicio) {
      params = params.set(
        'fechaInicio',
        request.fechaInicio,
      );
    }

    if (request.fechaFin) {
      params = params.set(
        'fechaFin',
        request.fechaFin,
      );
    }

    return this.http.get(
      this.exportUrl,
      {
        params,
        observe: 'response',
        responseType: 'blob',
      },
    );
  }

  download(
    response: HttpResponse<Blob>,
    request: ReportExportRequest,
  ): void {
    const content = response.body;

    if (!content || content.size === 0) {
      throw new Error(
        'El archivo generado esta vacio',
      );
    }

    const fileName =
      this.resolveFileName(
        response,
        request,
      );

    const objectUrl =
      URL.createObjectURL(content);

    const anchor =
      document.createElement('a');

    anchor.href = objectUrl;
    anchor.download = fileName;
    anchor.style.display = 'none';

    document.body.appendChild(anchor);

    anchor.click();
    anchor.remove();

    setTimeout(
      () =>
        URL.revokeObjectURL(objectUrl),
      0,
    );
  }

  private resolveFileName(
    response: HttpResponse<Blob>,
    request: ReportExportRequest,
  ): string {
    const disposition =
      response.headers.get(
        'content-disposition',
      );

    if (disposition) {
      const encodedMatch =
        /filename\*=UTF-8''([^;]+)/i.exec(
          disposition,
        );

      if (encodedMatch?.[1]) {
        return decodeURIComponent(
          encodedMatch[1],
        );
      }

      const regularMatch =
        /filename="?([^";]+)"?/i.exec(
          disposition,
        );

      if (regularMatch?.[1]) {
        return regularMatch[1];
      }
    }

    const extension =
      request.formato === 'PDF'
        ? 'pdf'
        : 'xlsx';

    let fileName =
      `reporte-${request.tipo
        .toLowerCase()
        .replaceAll('_', '-')}`;

    if (
      request.fechaInicio &&
      request.fechaFin
    ) {
      fileName +=
        `-${request.fechaInicio}` +
        `-${request.fechaFin}`;
    }

    return `${fileName}.${extension}`;
  }
}