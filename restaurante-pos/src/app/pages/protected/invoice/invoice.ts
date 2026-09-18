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
import {
  ActivatedRoute,
  RouterLink,
} from '@angular/router';
import {
  TranslocoPipe,
  TranslocoService,
} from '@jsverse/transloco';
import { finalize } from 'rxjs';
import { Invoice } from '../../../core/models/invoice.models';
import { ApiErrorService } from '../../../core/services/api-error.service';
import { InvoiceService } from '../../../core/services/invoice.service';
import { FormFeedbackComponent } from '../../../shared/components/form-feedback/form-feedback';
import { PageHeadingComponent } from '../../../shared/components/page-heading/page-heading';

@Component({
  selector: 'app-invoice-page',
  imports: [
    DatePipe,
    DecimalPipe,
    FormFeedbackComponent,
    PageHeadingComponent,
    RouterLink,
    TranslocoPipe,
  ],
  templateUrl: './invoice.html',
  styleUrl: './invoice.scss',
})
export class InvoicePageComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly invoiceService = inject(InvoiceService);
  private readonly errors = inject(ApiErrorService);
  private readonly transloco = inject(TranslocoService);

  readonly invoice = signal<Invoice | null>(null);

  readonly loading = signal(false);
  readonly downloading = signal(false);
  readonly printing = signal(false);

  readonly errorMessage = signal<string | null>(null);

  ngOnInit(): void {
    const invoiceId = Number(
      this.route.snapshot.paramMap.get('facturaId'),
    );

    if (
      !Number.isInteger(invoiceId) ||
      invoiceId <= 0
    ) {
      this.errorMessage.set(
        this.transloco.translate(
          'invoice.errors.invalidId',
        ),
      );
      return;
    }

    this.loadInvoice(invoiceId);
  }

  downloadPdf(): void {
    const invoice = this.invoice();

    if (invoice === null || this.downloading()) {
      return;
    }

    this.downloading.set(true);
    this.errorMessage.set(null);

    this.invoiceService
      .getInvoicePdf(invoice.facturaId)
      .pipe(
        finalize(() =>
          this.downloading.set(false),
        ),
      )
      .subscribe({
        next: (blob) => {
          const url = URL.createObjectURL(blob);

          const anchor = document.createElement('a');
          anchor.href = url;
          anchor.download =
            `${invoice.numeroDocumento}.pdf`;

          document.body.appendChild(anchor);
          anchor.click();
          anchor.remove();

          URL.revokeObjectURL(url);
        },
        error: (error: unknown) => {
          this.errorMessage.set(
            this.errors.getMessage(error),
          );
        },
      });
  }

  printPdf(): void {
    const invoice = this.invoice();

    if (invoice === null || this.printing()) {
      return;
    }

    this.printing.set(true);
    this.errorMessage.set(null);

    const printWindow = window.open(
      '',
      '_blank',
    );

    if (printWindow === null) {
      this.printing.set(false);
      this.errorMessage.set(
        this.transloco.translate(
          'invoice.errors.popupBlocked',
        ),
      );
      return;
    }

    printWindow.document.write(
      `<p>${this.transloco.translate(
        'invoice.openingPrint',
      )}</p>`,
    );

    this.invoiceService
      .getInvoicePdf(invoice.facturaId)
      .pipe(
        finalize(() =>
          this.printing.set(false),
        ),
      )
      .subscribe({
        next: (blob) => {
          const url = URL.createObjectURL(blob);

          printWindow.location.href = url;

          window.setTimeout(() => {
            try {
              printWindow.focus();
              printWindow.print();
            } finally {
              window.setTimeout(
                () => URL.revokeObjectURL(url),
                60000,
              );
            }
          }, 1200);
        },
        error: (error: unknown) => {
          printWindow.close();

          this.errorMessage.set(
            this.errors.getMessage(error),
          );
        },
      });
  }

  hasCustomer(invoice: Invoice): boolean {
    return Boolean(
      invoice.clienteNombre ||
      invoice.clienteApellido,
    );
  }

  customerName(invoice: Invoice): string {
    return [
      invoice.clienteNombre,
      invoice.clienteApellido,
    ]
      .filter(Boolean)
      .join(' ');
  }

  private loadInvoice(invoiceId: number): void {
    this.loading.set(true);
    this.errorMessage.set(null);

    this.invoiceService
      .getInvoice(invoiceId)
      .pipe(
        finalize(() =>
          this.loading.set(false),
        ),
      )
      .subscribe({
        next: (invoice) => {
          this.invoice.set(invoice);
        },
        error: (error: unknown) => {
          this.invoice.set(null);

          this.errorMessage.set(
            this.errors.getMessage(error),
          );
        },
      });
  }
}
