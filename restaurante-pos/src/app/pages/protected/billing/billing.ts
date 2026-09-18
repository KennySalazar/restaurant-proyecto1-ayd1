import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import {
  FormControl,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { RouterLink } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';
import { finalize } from 'rxjs';
import {
  BillingAccount,
  BillingCalculation,
  BillingSubaccount,
} from '../../../core/models/billing.models';
import { CurrentCashShiftResponse } from '../../../core/models/cash-shift.models';
import { CustomerPoints } from '../../../core/models/loyalty.models';
import { ApiErrorService } from '../../../core/services/api-error.service';
import { BillingService } from '../../../core/services/billing.service';
import { CashShiftService } from '../../../core/services/cash-shift.service';
import { LoyaltyService } from '../../../core/services/loyalty.service';
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
    this.loadAccounts();
  }

  selectAccount(account: BillingAccount): void {
    this.selectedAccount.set(account);
    this.selectedSubaccount.set(null);
    this.subaccounts.set([]);
    this.calculation.set(null);
    this.errorMessage.set(null);
    this.resetLoyalty();

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

          this.pointsToRedeemControl.setValidators([
            Validators.required,
            Validators.min(1),
            Validators.max(loyalty.saldoPuntos),
            Validators.pattern(/^\d+$/),
          ]);

          this.pointsToRedeemControl
            .updateValueAndValidity({
              emitEvent: false,
            });
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
