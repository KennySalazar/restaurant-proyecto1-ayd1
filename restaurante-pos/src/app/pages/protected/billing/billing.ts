import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';
import { finalize } from 'rxjs';
import {
  BillingAccount,
  BillingCalculation,
  BillingSubaccount,
} from '../../../core/models/billing.models';
import { CurrentCashShiftResponse } from '../../../core/models/cash-shift.models';
import { ApiErrorService } from '../../../core/services/api-error.service';
import { BillingService } from '../../../core/services/billing.service';
import { CashShiftService } from '../../../core/services/cash-shift.service';
import { FormFeedbackComponent } from '../../../shared/components/form-feedback/form-feedback';
import { PageHeadingComponent } from '../../../shared/components/page-heading/page-heading';

@Component({
  selector: 'app-billing-page',
  imports: [
    DatePipe,
    DecimalPipe,
    FormFeedbackComponent,
    PageHeadingComponent,
    RouterLink,
    TranslocoPipe,
  ],
  templateUrl: './billing.html',
  styleUrl: './billing.scss',
})
export class BillingPageComponent implements OnInit {
  private readonly cashShiftService = inject(CashShiftService);
  private readonly billingService = inject(BillingService);
  private readonly errors = inject(ApiErrorService);

  readonly currentShift =
    signal<CurrentCashShiftResponse | null>(null);

  readonly accounts = signal<BillingAccount[]>([]);
  readonly selectedAccount = signal<BillingAccount | null>(null);

  readonly subaccounts = signal<BillingSubaccount[]>([]);
  readonly selectedSubaccount =
    signal<BillingSubaccount | null>(null);

  readonly calculation =
    signal<BillingCalculation | null>(null);

  readonly loadingShift = signal(false);
  readonly loadingAccounts = signal(false);
  readonly loadingSubaccounts = signal(false);
  readonly loadingCalculation = signal(false);
  readonly accountsLoaded = signal(false);

  readonly requiresCashShift = signal(false);
  readonly errorMessage = signal<string | null>(null);

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
    this.loadAccounts();
  }

  selectAccount(account: BillingAccount): void {
    this.selectedAccount.set(account);
    this.selectedSubaccount.set(null);
    this.subaccounts.set([]);
    this.calculation.set(null);
    this.errorMessage.set(null);

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
          }
        },
        error: (error: unknown) => {
          this.handleError(error);
        },
      });
  }

  selectSubaccount(subaccount: BillingSubaccount): void {
    const account = this.selectedAccount();

    if (account === null) {
      return;
    }

    this.selectedSubaccount.set(subaccount);
    this.calculation.set(null);
    this.errorMessage.set(null);
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
          const problem = this.errors.getProblem(error);

          if (problem.code === 'open_cash_shift_required') {
            this.currentShift.set(null);
            this.requiresCashShift.set(true);
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

  private handleError(error: unknown): void {
    const problem = this.errors.getProblem(error);

    if (problem.code === 'open_cash_shift_required') {
      this.currentShift.set(null);
      this.requiresCashShift.set(true);
      this.accounts.set([]);
      this.selectedAccount.set(null);
      this.selectedSubaccount.set(null);
      this.subaccounts.set([]);
      this.calculation.set(null);
      return;
    }

    this.errorMessage.set(
      this.errors.getMessage(error),
    );
  }
}
