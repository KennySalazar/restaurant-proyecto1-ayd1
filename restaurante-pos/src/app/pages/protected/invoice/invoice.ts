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
  FormControl,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
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
import { ServiceRatingResponse } from '../../../core/models/service-rating.models';
import { ApiErrorService } from '../../../core/services/api-error.service';
import { InvoiceService } from '../../../core/services/invoice.service';
import { ServiceRatingService } from '../../../core/services/service-rating.service';
import { FormFeedbackComponent } from '../../../shared/components/form-feedback/form-feedback';
import { PageHeadingComponent } from '../../../shared/components/page-heading/page-heading';

@Component({
  selector: 'app-invoice-page',
  imports: [
    DatePipe,
    DecimalPipe,
    FormFeedbackComponent,
    PageHeadingComponent,
    ReactiveFormsModule,
    RouterLink,
    TranslocoPipe,
  ],
  templateUrl: './invoice.html',
  styleUrl: './invoice.scss',
})
export class InvoicePageComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly invoiceService = inject(InvoiceService);
  private readonly serviceRatingService =
    inject(ServiceRatingService);
  private readonly errors = inject(ApiErrorService);
  private readonly transloco = inject(TranslocoService);

  readonly invoice = signal<Invoice | null>(null);

  readonly loading = signal(false);
  readonly downloading = signal(false);
  readonly printing = signal(false);
  readonly submittingRating = signal(false);

  readonly errorMessage = signal<string | null>(null);
  readonly ratingErrorMessage =
    signal<string | null>(null);

  readonly registeredRating =
    signal<ServiceRatingResponse | null>(null);

  readonly ratingOptions = [1, 2, 3, 4, 5] as const;

  readonly ratingControl =
    new FormControl<number | null>(
      null,
      {
        validators: [
          Validators.required,
          Validators.min(1),
          Validators.max(5),
        ],
      },
    );

  readonly ratingCommentControl =
    new FormControl<string>(
      '',
      {
        nonNullable: true,
        validators: [
          Validators.maxLength(500),
        ],
      },
    );

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

  selectRating(rating: number): void {
    if (this.registeredRating() !== null) {
      return;
    }

    this.ratingControl.setValue(rating);
    this.ratingControl.markAsTouched();
    this.ratingErrorMessage.set(null);
  }

  isRatingSelected(rating: number): boolean {
    const selected = this.ratingControl.value ?? 0;

    return rating <= selected;
  }

  submitRating(): void {
    const invoice = this.invoice();

    this.ratingControl.markAsTouched();
    this.ratingCommentControl.markAsTouched();

    this.ratingControl.updateValueAndValidity();
    this.ratingCommentControl.updateValueAndValidity();

    if (
      invoice === null ||
      this.ratingControl.invalid ||
      this.ratingCommentControl.invalid ||
      this.submittingRating() ||
      this.registeredRating() !== null
    ) {
      return;
    }

    const rating = this.ratingControl.value;

    if (rating === null) {
      return;
    }

    const comment =
      this.ratingCommentControl.value.trim();

    this.submittingRating.set(true);
    this.ratingErrorMessage.set(null);

    this.serviceRatingService
      .registerRating(
        invoice.facturaId,
        {
          calificacion: rating,
          comentario:
            comment.length > 0
              ? comment
              : null,
        },
      )
      .pipe(
        finalize(() =>
          this.submittingRating.set(false),
        ),
      )
      .subscribe({
        next: (response) => {
          this.registeredRating.set(response);

          this.ratingControl.disable({
            emitEvent: false,
          });

          this.ratingCommentControl.disable({
            emitEvent: false,
          });
        },
        error: (error: unknown) => {
          this.ratingErrorMessage.set(
            this.errors.getMessage(error),
          );
        },
      });
  }

  isAccountClosed(invoice: Invoice): boolean {
    return invoice.estadoCuenta === 'CERRADA';
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

  waiterName(
    rating: ServiceRatingResponse,
  ): string {
    return [
      rating.meseroNombres,
      rating.meseroApellidos,
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
