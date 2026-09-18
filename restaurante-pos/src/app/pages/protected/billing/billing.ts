import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import {
  FormControl,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';
import { finalize } from 'rxjs';
import {
  BillingAccount,
  BillingCalculation,
  BillingSubaccount,
} from '../../../core/models/billing.models';
import { CurrentCashShiftResponse } from '../../../core/models/cash-shift.models';
import {
  CustomerPoints,
  CustomerRecord,
  RegisterCustomerRequest,
} from '../../../core/models/loyalty.models';
import {
  PAYMENT_METHOD_IDS,
  PaymentMode,
  RegisterPaymentRequest,
} from '../../../core/models/payment.models';
import { ApiErrorService } from '../../../core/services/api-error.service';
import { BillingService } from '../../../core/services/billing.service';
import { CashShiftService } from '../../../core/services/cash-shift.service';
import { LoyaltyService } from '../../../core/services/loyalty.service';
import { PaymentService } from '../../../core/services/payment.service';
import { FormFeedbackComponent } from '../../../shared/components/form-feedback/form-feedback';
import { PageHeadingComponent } from '../../../shared/components/page-heading/page-heading';

@Component({
  selector: 'app-billing-page',
  imports: [
    DatePipe,
    DecimalPipe,
    FormFeedbackComponent,
    PageHeadingComponent,
    ReactiveFormsModule,
    RouterLink,
    TranslocoPipe,
  ],
  templateUrl: './billing.html',
  styleUrl: './billing.scss',
})
export class BillingPageComponent implements OnInit {
  private readonly cashShiftService = inject(CashShiftService);
  private readonly billingService = inject(BillingService);
  private readonly loyaltyService = inject(LoyaltyService);
  private readonly paymentService = inject(PaymentService);
  private readonly router = inject(Router);
  private readonly errors = inject(ApiErrorService);

  readonly currentShift =
    signal<CurrentCashShiftResponse | null>(null);

  readonly accounts = signal<BillingAccount[]>([]);
  readonly selectedAccount =
    signal<BillingAccount | null>(null);

  readonly subaccounts = signal<BillingSubaccount[]>([]);
  readonly selectedSubaccount =
    signal<BillingSubaccount | null>(null);

  readonly calculation =
    signal<BillingCalculation | null>(null);

  readonly customerPoints =
    signal<CustomerPoints | null>(null);

  readonly pointsSelectedForRedemption = signal(0);
  readonly redemptionDiscountPreview = signal(0);
  readonly redemptionAttempted = signal(false);

  readonly paymentSubmitted = signal(false);
  readonly charging = signal(false);
  readonly paymentErrorMessage = signal<string | null>(null);

  readonly customerSearchResult = signal<CustomerRecord | null>(null);
  readonly searchingCustomer = signal(false);
  readonly registeringCustomer = signal(false);
  readonly associatingCustomer = signal(false);
  readonly customerEnrollmentErrorMessage = signal<string | null>(null);

  readonly loadingShift = signal(false);
  readonly loadingAccounts = signal(false);
  readonly loadingSubaccounts = signal(false);
  readonly loadingCalculation = signal(false);
  readonly loadingLoyalty = signal(false);
  readonly accountsLoaded = signal(false);

  readonly requiresCashShift = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly loyaltyErrorMessage =
    signal<string | null>(null);

  readonly pointsToRedeemControl =
    new FormControl<number | null>(
      null,
      {
        validators: [
          Validators.required,
          Validators.min(1),
          Validators.pattern(/^\d+$/),
        ],
      },
    );


  readonly customerSearchPhoneControl =
    new FormControl('', {
      nonNullable: true,
      validators: [
        Validators.required,
        Validators.maxLength(25),
      ],
    });

  readonly customerNamesControl =
    new FormControl('', {
      nonNullable: true,
      validators: [
        Validators.required,
        Validators.maxLength(120),
      ],
    });

  readonly customerLastNamesControl =
    new FormControl('', {
      nonNullable: true,
      validators: [Validators.maxLength(120)],
    });

  readonly customerPhoneControl =
    new FormControl('', {
      nonNullable: true,
      validators: [
        Validators.required,
        Validators.maxLength(25),
      ],
    });

  readonly customerEmailControl =
    new FormControl('', {
      nonNullable: true,
      validators: [
        Validators.email,
        Validators.maxLength(150),
      ],
    });

  readonly paymentModeControl =
    new FormControl<PaymentMode>('CASH', {
      nonNullable: true,
    });

  readonly cashReceivedControl =
    new FormControl<number | null>(null, {
      validators: [
        Validators.required,
        Validators.min(0.01),
      ],
    });

  readonly cardReferenceControl =
    new FormControl('', {
      nonNullable: true,
      validators: [
        Validators.required,
        Validators.maxLength(120),
      ],
    });

  readonly cardAuthorizationControl =
    new FormControl('', {
      nonNullable: true,
      validators: [Validators.maxLength(120)],
    });

  readonly maximumRedeemablePoints = computed(() => {
    const loyalty = this.customerPoints();

    if (loyalty === null) {
      return 0;
    }

    const calculation = this.calculation();

    if (
      calculation === null ||
      loyalty.valorMonetarioPunto <= 0
    ) {
      return loyalty.saldoPuntos;
    }

    const subtotalLimit = Math.floor(
      calculation.subtotal /
        loyalty.valorMonetarioPunto,
    );

    return Math.max(
      0,
      Math.min(
        loyalty.saldoPuntos,
        subtotalLimit,
      ),
    );
  });

  readonly chargeBreakdown = computed(() => {
    const calculation = this.calculation();

    if (calculation === null) {
      return null;
    }

    const pointsDiscount =
      this.pointsSelectedForRedemption() > 0
        ? this.redemptionDiscountPreview()
        : 0;

    if (pointsDiscount <= 0) {
      return {
        discount: 0,
        taxableBase: calculation.subtotal,
        taxAmount: calculation.montoImpuesto,
        tipAmount: calculation.montoPropina,
        total: calculation.total,
      };
    }

    const taxableBase = this.money(
      calculation.subtotal - pointsDiscount,
    );

    const taxAmount = this.money(
      taxableBase *
        (calculation.porcentajeImpuesto / 100),
    );

    const tipAmount = this.money(
      taxableBase *
        (calculation.porcentajePropina / 100),
    );

    return {
      discount: pointsDiscount,
      taxableBase,
      taxAmount,
      tipAmount,
      total: this.money(
        taxableBase + taxAmount + tipAmount,
      ),
    };
  });

  ngOnInit(): void {
    this.loadCurrentShift();
  }

  accountStatusKey(status: string): string {
    if (status === 'PARCIALMENTE_PAGADA') {
      return 'billing.status.partiallyPaid';
    }

    return 'billing.status.readyForPayment';
  }

  refreshAccounts(): void {
    this.selectedAccount.set(null);
    this.selectedSubaccount.set(null);
    this.subaccounts.set([]);
    this.calculation.set(null);
    this.resetLoyalty();
    this.resetPayment();
    this.resetCustomerEnrollment();
    this.loadAccounts();
  }

  selectAccount(account: BillingAccount): void {
    this.selectedAccount.set(account);
    this.selectedSubaccount.set(null);
    this.subaccounts.set([]);
    this.calculation.set(null);
    this.errorMessage.set(null);
    this.resetLoyalty();
    this.resetPayment();
    this.resetCustomerEnrollment();

    if (account.clienteId !== null) {
      this.loadCustomerPoints(account.clienteId);
    }

    this.loadingSubaccounts.set(true);

    this.billingService
      .getPendingSubaccounts(account.id)
      .pipe(
        finalize(() =>
          this.loadingSubaccounts.set(false),
        ),
      )
      .subscribe({
        next: (subaccounts) => {
          this.subaccounts.set(subaccounts);

          if (subaccounts.length === 0) {
            this.loadAccountCalculation(account.id);
            return;
          }

          this.clearPointsSelection();
        },
        error: (error: unknown) => {
          this.handleError(error);
        },
      });
  }

  selectSubaccount(
    subaccount: BillingSubaccount,
  ): void {
    const account = this.selectedAccount();

    if (account === null) {
      return;
    }

    this.selectedSubaccount.set(subaccount);
    this.calculation.set(null);
    this.errorMessage.set(null);
    this.clearPointsSelection();
    this.resetPayment();
    this.loadingCalculation.set(true);

    this.billingService
      .calculateSubaccount(
        account.id,
        subaccount.id,
      )
      .pipe(
        finalize(() =>
          this.loadingCalculation.set(false),
        ),
      )
      .subscribe({
        next: (calculation) => {
          this.calculation.set(calculation);
          this.syncPointsValidators();
        },
        error: (error: unknown) => {
          this.handleError(error);
        },
      });
  }

  applyPointsRedemption(): void {
    const loyalty = this.customerPoints();

    this.redemptionAttempted.set(true);
    this.pointsToRedeemControl.markAsTouched();
    this.pointsToRedeemControl.updateValueAndValidity();

    if (
      loyalty === null ||
      this.subaccounts().length > 0 ||
      this.pointsToRedeemControl.invalid
    ) {
      return;
    }

    const points =
      this.pointsToRedeemControl.value;

    if (
      points === null ||
      !Number.isInteger(points)
    ) {
      return;
    }

    this.pointsSelectedForRedemption.set(points);

    this.redemptionDiscountPreview.set(
      this.money(
        points * loyalty.valorMonetarioPunto,
      ),
    );
  }

  clearPointsRedemption(): void {
    this.clearPointsSelection();
  }

  onPointsToRedeemChange(): void {
    this.pointsSelectedForRedemption.set(0);
    this.redemptionDiscountPreview.set(0);
    this.redemptionAttempted.set(true);
  }

  pointsValidationKey(): string | null {
    if (!this.redemptionAttempted()) {
      return null;
    }

    if (
      this.pointsToRedeemControl.hasError('max')
    ) {
      return 'billing.loyalty.errors.exceedsBalance';
    }

    if (this.pointsToRedeemControl.invalid) {
      return 'billing.loyalty.errors.invalid';
    }

    return null;
  }


  searchCustomer(): void {
    this.customerSearchPhoneControl.markAsTouched();
    this.customerSearchPhoneControl.updateValueAndValidity();
    this.customerEnrollmentErrorMessage.set(null);
    this.customerSearchResult.set(null);

    if (this.customerSearchPhoneControl.invalid) {
      return;
    }

    const phone = this.customerSearchPhoneControl.value.trim();

    this.searchingCustomer.set(true);

    this.loyaltyService
      .findCustomerByPhone(phone)
      .pipe(
        finalize(() => this.searchingCustomer.set(false)),
      )
      .subscribe({
        next: (customer) => {
          this.customerSearchResult.set(customer);
        },
        error: (error: unknown) => {
          const problem = this.errors.getProblem(error);

          if (problem.code === 'customer_not_found') {
            this.customerPhoneControl.setValue(phone);
          }

          this.customerEnrollmentErrorMessage.set(
            this.errors.getMessage(error),
          );
        },
      });
  }

  associateExistingCustomer(): void {
    const account = this.selectedAccount();
    const customer = this.customerSearchResult();

    if (
      account === null ||
      customer === null ||
      this.associatingCustomer()
    ) {
      return;
    }

    this.customerEnrollmentErrorMessage.set(null);
    this.associatingCustomer.set(true);

    this.loyaltyService
      .associateCustomer(account.id, customer.clienteId)
      .pipe(
        finalize(() => this.associatingCustomer.set(false)),
      )
      .subscribe({
        next: (associatedCustomer) => {
          this.completeCustomerAssociation(associatedCustomer);
        },
        error: (error: unknown) => {
          this.customerEnrollmentErrorMessage.set(
            this.errors.getMessage(error),
          );
        },
      });
  }

  registerCustomer(): void {
    const account = this.selectedAccount();

    this.customerNamesControl.markAsTouched();
    this.customerLastNamesControl.markAsTouched();
    this.customerPhoneControl.markAsTouched();
    this.customerEmailControl.markAsTouched();

    this.customerNamesControl.updateValueAndValidity();
    this.customerLastNamesControl.updateValueAndValidity();
    this.customerPhoneControl.updateValueAndValidity();
    this.customerEmailControl.updateValueAndValidity();

    if (
      account === null ||
      this.customerNamesControl.invalid ||
      this.customerLastNamesControl.invalid ||
      this.customerPhoneControl.invalid ||
      this.customerEmailControl.invalid ||
      this.registeringCustomer()
    ) {
      return;
    }

    const lastNames = this.customerLastNamesControl.value.trim();
    const email = this.customerEmailControl.value.trim();

    const request: RegisterCustomerRequest = {
      nombres: this.customerNamesControl.value.trim(),
      apellidos: lastNames.length > 0 ? lastNames : null,
      telefono: this.customerPhoneControl.value.trim(),
      correo: email.length > 0 ? email : null,
    };

    this.customerEnrollmentErrorMessage.set(null);
    this.registeringCustomer.set(true);

    this.loyaltyService
      .registerAndAssociateCustomer(account.id, request)
      .pipe(
        finalize(() => this.registeringCustomer.set(false)),
      )
      .subscribe({
        next: (customer) => {
          this.completeCustomerAssociation(customer);
        },
        error: (error: unknown) => {
          this.customerEnrollmentErrorMessage.set(
            this.errors.getMessage(error),
          );
        },
      });
  }

  selectPaymentMode(mode: PaymentMode): void {
    this.paymentModeControl.setValue(mode);
    this.paymentSubmitted.set(false);
    this.paymentErrorMessage.set(null);

    if (mode === 'CASH') {
      this.cardReferenceControl.reset('');
      this.cardAuthorizationControl.reset('');
      return;
    }

    this.cashReceivedControl.reset(null);
  }

  cashChange(): number {
    const breakdown = this.chargeBreakdown();
    const received = this.cashReceivedControl.value;

    if (
      breakdown === null ||
      received === null ||
      received <= breakdown.total
    ) {
      return 0;
    }

    return this.money(
      received - breakdown.total,
    );
  }

  cashReceivedIsInsufficient(): boolean {
    if (!this.paymentSubmitted()) {
      return false;
    }

    const breakdown = this.chargeBreakdown();
    const received = this.cashReceivedControl.value;

    return (
      breakdown !== null &&
      received !== null &&
      received < breakdown.total
    );
  }

  canCharge(): boolean {
    const account = this.selectedAccount();
    const calculation = this.calculation();
    const breakdown = this.chargeBreakdown();

    if (
      this.charging() ||
      account === null ||
      calculation === null ||
      breakdown === null ||
      breakdown.total <= 0
    ) {
      return false;
    }

    if (calculation.subcuentaId !== null && this.selectedSubaccount() === null) {
      return false;
    }

    if (this.paymentModeControl.value === 'CASH') {
      const received = this.cashReceivedControl.value;

      return (
        this.cashReceivedControl.valid &&
        received !== null &&
        received >= breakdown.total
      );
    }

    return this.cardReferenceControl.valid;
  }

  charge(): void {
    const subaccountId = this.calculation()?.subcuentaId ?? null;

    if (subaccountId !== null) {
      this.chargeSubaccount(subaccountId);
      return;
    }

    this.chargeAccount();
  }

  chargeSubaccount(subaccountId: number): void {
    const account = this.selectedAccount();
    const calculation = this.calculation();
    const breakdown = this.chargeBreakdown();

    this.paymentSubmitted.set(true);
    this.paymentErrorMessage.set(null);

    if (
      account === null ||
      calculation === null ||
      breakdown === null ||
      calculation.subcuentaId !== subaccountId
    ) {
      return;
    }

    const payment = this.buildPaymentRequest(
      breakdown.total,
    );

    if (payment === null) {
      return;
    }

    this.charging.set(true);

    this.paymentService
      .chargeSubaccount(account.id, subaccountId, {
        puntosRedimidos:
          this.pointsSelectedForRedemption(),
        pagos: [payment],
      })
      .pipe(
        finalize(() => this.charging.set(false)),
      )
      .subscribe({
        next: (response) => {
          void this.router.navigate([
            '/app/caja/facturas',
            response.facturaId,
          ]);
        },
        error: (error: unknown) => {
          this.paymentErrorMessage.set(
            this.errors.getMessage(error),
          );
        },
      });
  }

  chargeAccount(): void {
    const account = this.selectedAccount();
    const calculation = this.calculation();
    const breakdown = this.chargeBreakdown();

    this.paymentSubmitted.set(true);
    this.paymentErrorMessage.set(null);

    if (
      account === null ||
      calculation === null ||
      breakdown === null ||
      calculation.subcuentaId !== null ||
      this.subaccounts().length > 0
    ) {
      return;
    }

    const payment = this.buildPaymentRequest(
      breakdown.total,
    );

    if (payment === null) {
      return;
    }

    this.charging.set(true);

    this.paymentService
      .chargeAccount(account.id, {
        puntosRedimidos:
          this.pointsSelectedForRedemption(),
        pagos: [payment],
      })
      .pipe(
        finalize(() => this.charging.set(false)),
      )
      .subscribe({
        next: (response) => {
          void this.router.navigate([
            '/app/caja/facturas',
            response.facturaId,
          ]);
        },
        error: (error: unknown) => {
          this.paymentErrorMessage.set(
            this.errors.getMessage(error),
          );
        },
      });
  }

  private loadCurrentShift(): void {
    this.loadingShift.set(true);
    this.requiresCashShift.set(false);
    this.errorMessage.set(null);

    this.cashShiftService
      .getCurrentShift()
      .pipe(
        finalize(() =>
          this.loadingShift.set(false),
        ),
      )
      .subscribe({
        next: (shift) => {
          this.currentShift.set(shift);
          this.loadAccounts();
        },
        error: (error: unknown) => {
          const problem =
            this.errors.getProblem(error);

          if (
            problem.code ===
            'open_cash_shift_required'
          ) {
            this.currentShift.set(null);
            this.requiresCashShift.set(true);
            this.resetLoyalty();
            return;
          }

          this.errorMessage.set(
            this.errors.getMessage(error),
          );
        },
      });
  }

  private loadAccounts(): void {
    this.loadingAccounts.set(true);
    this.accountsLoaded.set(false);
    this.errorMessage.set(null);

    this.billingService
      .getAccounts()
      .pipe(
        finalize(() =>
          this.loadingAccounts.set(false),
        ),
      )
      .subscribe({
        next: (accounts) => {
          this.accounts.set(accounts);
          this.accountsLoaded.set(true);
        },
        error: (error: unknown) => {
          this.handleError(error);
        },
      });
  }

  private loadAccountCalculation(
    accountId: number,
  ): void {
    this.loadingCalculation.set(true);

    this.billingService
      .calculateAccount(accountId)
      .pipe(
        finalize(() =>
          this.loadingCalculation.set(false),
        ),
      )
      .subscribe({
        next: (calculation) => {
          this.calculation.set(calculation);
          this.syncPointsValidators();
        },
        error: (error: unknown) => {
          this.handleError(error);
        },
      });
  }

  private loadCustomerPoints(
    customerId: number,
  ): void {
    this.loadingLoyalty.set(true);
    this.loyaltyErrorMessage.set(null);

    this.loyaltyService
      .getCustomerPoints(customerId)
      .pipe(
        finalize(() =>
          this.loadingLoyalty.set(false),
        ),
      )
      .subscribe({
        next: (loyalty) => {
          this.customerPoints.set(loyalty);

          this.syncPointsValidators();
        },
        error: (error: unknown) => {
          this.customerPoints.set(null);

          this.loyaltyErrorMessage.set(
            this.errors.getMessage(error),
          );
        },
      });
  }

  private clearPointsSelection(): void {
    this.pointsToRedeemControl.reset(null);
    this.pointsSelectedForRedemption.set(0);
    this.redemptionDiscountPreview.set(0);
    this.redemptionAttempted.set(false);
  }

  private resetLoyalty(): void {
    this.customerPoints.set(null);
    this.loyaltyErrorMessage.set(null);
    this.loadingLoyalty.set(false);

    this.pointsToRedeemControl.setValidators([
      Validators.required,
      Validators.min(1),
      Validators.pattern(/^\d+$/),
    ]);

    this.clearPointsSelection();
  }

  private syncPointsValidators(): void {
    const loyalty = this.customerPoints();

    if (loyalty === null) {
      return;
    }

    this.pointsToRedeemControl.setValidators([
      Validators.required,
      Validators.min(1),
      Validators.max(
        this.maximumRedeemablePoints(),
      ),
      Validators.pattern(/^\d+$/),
    ]);

    this.pointsToRedeemControl
      .updateValueAndValidity({
        emitEvent: false,
      });
  }

  private completeCustomerAssociation(
    customer: CustomerRecord,
  ): void {
    const account = this.selectedAccount();

    if (account === null) {
      return;
    }

    const updatedAccount: BillingAccount = {
      ...account,
      clienteId: customer.clienteId,
    };

    this.selectedAccount.set(updatedAccount);
    this.accounts.update((accounts) =>
      accounts.map((item) =>
        item.id === account.id
          ? updatedAccount
          : item,
      ),
    );

    this.resetLoyalty();
    this.resetCustomerEnrollment();
    this.loadCustomerPoints(customer.clienteId);
  }

  private resetCustomerEnrollment(): void {
    this.customerSearchResult.set(null);
    this.customerEnrollmentErrorMessage.set(null);
    this.searchingCustomer.set(false);
    this.registeringCustomer.set(false);
    this.associatingCustomer.set(false);
    this.customerSearchPhoneControl.reset('');
    this.customerNamesControl.reset('');
    this.customerLastNamesControl.reset('');
    this.customerPhoneControl.reset('');
    this.customerEmailControl.reset('');
  }

  private resetPayment(): void {
    this.paymentModeControl.setValue('CASH');
    this.cashReceivedControl.reset(null);
    this.cardReferenceControl.reset('');
    this.cardAuthorizationControl.reset('');
    this.paymentSubmitted.set(false);
    this.paymentErrorMessage.set(null);
    this.charging.set(false);
  }

  private buildPaymentRequest(
    amount: number,
  ): RegisterPaymentRequest | null {
    if (this.paymentModeControl.value === 'CASH') {
      this.cashReceivedControl.markAsTouched();
      this.cashReceivedControl.updateValueAndValidity();

      const received = this.cashReceivedControl.value;

      if (
        this.cashReceivedControl.invalid ||
        received === null ||
        received < amount
      ) {
        return null;
      }

      return {
        metodoPagoId: PAYMENT_METHOD_IDS.cash,
        monto: amount,
        montoRecibido: this.money(received),
        referencia: null,
        autorizacion: null,
      };
    }

    this.cardReferenceControl.markAsTouched();
    this.cardAuthorizationControl.markAsTouched();
    this.cardReferenceControl.updateValueAndValidity();
    this.cardAuthorizationControl.updateValueAndValidity();

    if (
      this.cardReferenceControl.invalid ||
      this.cardAuthorizationControl.invalid
    ) {
      return null;
    }

    const reference =
      this.cardReferenceControl.value.trim();
    const authorization =
      this.cardAuthorizationControl.value.trim();

    return {
      metodoPagoId: PAYMENT_METHOD_IDS.card,
      monto: amount,
      montoRecibido: null,
      referencia: reference,
      autorizacion:
        authorization.length > 0
          ? authorization
          : null,
    };
  }

  private handleError(error: unknown): void {
    const problem = this.errors.getProblem(error);

    if (
      problem.code ===
      'open_cash_shift_required'
    ) {
      this.currentShift.set(null);
      this.requiresCashShift.set(true);
      this.accounts.set([]);
      this.selectedAccount.set(null);
      this.selectedSubaccount.set(null);
      this.subaccounts.set([]);
      this.calculation.set(null);
      this.resetLoyalty();
      this.resetPayment();
      this.resetCustomerEnrollment();
      return;
    }

    this.errorMessage.set(
      this.errors.getMessage(error),
    );
  }

  private money(value: number): number {
    return Math.round(
      (value + Number.EPSILON) * 100,
    ) / 100;
  }
}
