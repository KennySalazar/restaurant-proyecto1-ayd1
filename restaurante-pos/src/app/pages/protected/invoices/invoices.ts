import {
  DatePipe,
  DecimalPipe,
} from '@angular/common';
import {
  Component,
  OnInit,
  inject,
  signal,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';
import { finalize } from 'rxjs';
import { InvoiceHistoryItem } from '../../../core/models/invoice.models';
import { ApiErrorService } from '../../../core/services/api-error.service';
import { InvoiceService } from '../../../core/services/invoice.service';
import { FormFeedbackComponent } from '../../../shared/components/form-feedback/form-feedback';
import { PageHeadingComponent } from '../../../shared/components/page-heading/page-heading';

@Component({
  selector: 'app-invoices-page',
  imports: [
    DatePipe,
    DecimalPipe,
    FormFeedbackComponent,
    PageHeadingComponent,
    RouterLink,
    TranslocoPipe,
  ],
  templateUrl: './invoices.html',
  styleUrl: './invoices.scss',
})
export class InvoicesPageComponent implements OnInit {
  private readonly invoiceService = inject(InvoiceService);
  private readonly errors = inject(ApiErrorService);

  readonly invoices = signal<InvoiceHistoryItem[]>([]);
  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);

  ngOnInit(): void {
    this.loadInvoices();
  }

  refresh(): void {
    this.loadInvoices();
  }

  customerName(invoice: InvoiceHistoryItem): string {
    const name = [
      invoice.clienteNombres,
      invoice.clienteApellidos,
    ]
      .filter(Boolean)
      .join(' ');

    return name || 'Consumidor final';
  }

  waiterName(invoice: InvoiceHistoryItem): string {
    return [
      invoice.meseroNombres,
      invoice.meseroApellidos,
    ]
      .filter(Boolean)
      .join(' ');
  }

  private loadInvoices(): void {
    this.loading.set(true);
    this.errorMessage.set(null);

    this.invoiceService
      .getInvoiceHistory()
      .pipe(
        finalize(() =>
          this.loading.set(false),
        ),
      )
      .subscribe({
        next: (invoices) => {
          this.invoices.set(invoices);
        },
        error: (error: unknown) => {
          this.invoices.set([]);

          this.errorMessage.set(
            this.errors.getMessage(error),
          );
        },
      });
  }
}
