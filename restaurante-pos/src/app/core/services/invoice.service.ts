import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  Invoice,
  InvoiceHistoryItem,
} from '../models/invoice.models';

@Injectable({ providedIn: 'root' })
export class InvoiceService {
  private readonly cashInvoicesUrl =
    `${environment.apiBaseUrl}/caja/facturas`;

  constructor(private readonly http: HttpClient) {}

  getInvoice(invoiceId: number): Observable<Invoice> {
    return this.http.get<Invoice>(
      `${this.cashInvoicesUrl}/${invoiceId}`,
    );
  }

  getInvoiceHistory(): Observable<InvoiceHistoryItem[]> {
    return this.http.get<InvoiceHistoryItem[]>(
      `${this.cashInvoicesUrl}/historial`,
    );
  }

  getInvoicePdf(invoiceId: number): Observable<Blob> {
    return this.http.get(
      `${this.cashInvoicesUrl}/${invoiceId}/pdf`,
      {
        responseType: 'blob',
      },
    );
  }
}
