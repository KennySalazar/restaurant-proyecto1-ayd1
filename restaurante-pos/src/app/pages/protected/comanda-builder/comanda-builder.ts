import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';
import { MessageService } from 'primeng/api';
import { Dialog } from 'primeng/dialog';
import { catchError, EMPTY, finalize, interval, of, startWith, switchMap } from 'rxjs';
import { Account } from '../../../core/models/account.models';
import {
  AccountRoundResponse,
  ComandaItemResponse,
  RegisterDishCancellationRequest,
} from '../../../core/models/comanda.models';
import { MenuDish } from '../../../core/models/menu.models';
import { RestaurantTable } from '../../../core/models/table.models';
import {
  SplitByItemsRequest,
  SubaccountItemDefinitionRequest,
  SubaccountResponse,
} from '../../../core/models/subaccount.models';
import { AccountService } from '../../../core/services/account.service';
import { ApiErrorService } from '../../../core/services/api-error.service';
import { ComandaService } from '../../../core/services/comanda.service';
import { MenuService } from '../../../core/services/menu.service';
import { TableService } from '../../../core/services/table.service';
import { FormFeedbackComponent } from '../../../shared/components/form-feedback/form-feedback';
import { PageHeadingComponent } from '../../../shared/components/page-heading/page-heading';

const POLL_INTERVAL_MS = 5000;
const CANCELLATION_TYPES = ['CLIENTE', 'ERROR_MESERO', 'NO_DISPONIBLE_COCINA', 'OTRO'] as const;

@Component({
  selector: 'app-comanda-builder-page',
  imports: [
    Dialog,
    FormFeedbackComponent,
    FormsModule,
    PageHeadingComponent,
    RouterLink,
    TranslocoPipe,
  ],
  templateUrl: './comanda-builder.html',
  styleUrl: './comanda-builder.scss',
})
export class ComandaBuilderPageComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private readonly tableService = inject(TableService);
  private readonly accountService = inject(AccountService);
  private readonly menuService = inject(MenuService);
  private readonly comandaService = inject(ComandaService);
  private readonly errors = inject(ApiErrorService);
  private readonly messages = inject(MessageService);
  private readonly transloco = inject(TranslocoService);

  readonly cancellationTypes = CANCELLATION_TYPES;

  readonly loading = signal(false);
  readonly loadError = signal<string | null>(null);

  readonly table = signal<RestaurantTable | null>(null);
  readonly account = signal<Account | null>(null);
  readonly menu = signal<MenuDish[]>([]);
  readonly rounds = signal<AccountRoundResponse[]>([]);

  readonly draftRound = computed(() => this.rounds().find((round) => round.status === 'BORRADOR') ?? null);
  readonly sentRounds = computed(() =>
    this.rounds()
      .filter((round) => round.status !== 'BORRADOR')
      .sort((a, b) => b.roundNumber - a.roundNumber),
  );

  readonly addDialogOpen = signal(false);
  readonly selectedDish = signal<MenuDish | null>(null);
  readonly addQuantity = signal(1);
  readonly addNotes = signal('');
  readonly selectedModifierIds = signal<Set<number>>(new Set());
  readonly addSubmitting = signal(false);
  readonly addError = signal<string | null>(null);

  readonly cancelDialogOpen = signal(false);
  readonly cancelTargetItem = signal<ComandaItemResponse | null>(null);
  readonly cancelMotivo = signal('');
  readonly cancelTipo = signal<(typeof CANCELLATION_TYPES)[number]>('CLIENTE');
  readonly cancelSubmitting = signal(false);
  readonly cancelError = signal<string | null>(null);

  readonly sendingComanda = signal(false);
  readonly rowActionPending = signal<number | null>(null);

  readonly subaccounts = signal<SubaccountResponse[]>([]);

  readonly splitDialogOpen = signal(false);
  readonly splitMode = signal<'personas' | 'items'>('personas');
  readonly splitPersonCount = signal(2);
  readonly splitBucketCount = signal(2);
  readonly itemBucketAssignments = signal<Map<number, number>>(new Map());
  readonly splitSubmitting = signal(false);
  readonly splitError = signal<string | null>(null);

  readonly assignableItems = computed(() =>
    this.rounds()
      .flatMap((round) => round.items)
      .filter((item) => item.status !== 'CANCELADO'),
  );

  readonly splitBuckets = computed(() => Array.from({ length: this.splitBucketCount() }, (_, i) => i));

  private tableId!: number;

  ngOnInit(): void {
    this.tableId = Number(this.route.snapshot.paramMap.get('tableId'));
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.loadError.set(null);

    this.tableService.getTable(this.tableId).subscribe({
      next: (table) => this.table.set(table),
      error: () => {
        /* la card de mesa es informativa; si falla no bloquea el resto */
      },
    });

    this.accountService.getActiveAccount(this.tableId).subscribe({
      next: (account) => {
        this.account.set(account);
        this.startPolling(account.id);
        this.refreshSubaccounts();
      },
      error: (error: unknown) => {
        this.loading.set(false);
        this.loadError.set(this.errors.getMessage(error));
      },
    });

    this.menuService.getMenu().subscribe({
      next: (catalog) => this.menu.set(catalog.dishes),
      error: (error: unknown) => {
        this.loadError.set(this.errors.getMessage(error));
      },
    });
  }

  private startPolling(accountId: number): void {
    interval(POLL_INTERVAL_MS)
      .pipe(
        startWith(0),
        switchMap(() =>
          this.comandaService.getAccountRounds(accountId).pipe(
            catchError((error: unknown) => {
              this.loadError.set(this.errors.getMessage(error));
              return EMPTY;
            }),
          ),
        ),
        finalize(() => this.loading.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((rounds) => {
        this.rounds.set(rounds);
        this.loadError.set(null);
        this.loading.set(false);
      });
  }

  goBack(): void {
    void this.router.navigate(['/app/mesas']);
  }

  dishUnavailableReasonKey(dish: MenuDish): string {
    return dish.unavailabilityReason ? `comanda.unavailability.${dish.unavailabilityReason}` : '';
  }

  openAddDialog(dish: MenuDish): void {
    this.selectedDish.set(dish);
    this.addQuantity.set(1);
    this.addNotes.set('');
    this.selectedModifierIds.set(new Set());
    this.addError.set(null);
    this.addDialogOpen.set(true);
  }

  closeAddDialog(): void {
    this.addDialogOpen.set(false);
    this.selectedDish.set(null);
  }

  toggleModifier(modifierId: number): void {
    const current = new Set(this.selectedModifierIds());
    if (current.has(modifierId)) {
      current.delete(modifierId);
    } else {
      current.add(modifierId);
    }
    this.selectedModifierIds.set(current);
  }

  submitAddDish(): void {
    const dish = this.selectedDish();
    const account = this.account();

    if (!dish || !account || this.addQuantity() < 1) {
      return;
    }

    this.addSubmitting.set(true);
    this.addError.set(null);

    this.comandaService
      .addDishesToAccount(account.id, {
        items: [
          {
            platilloId: dish.id,
            cantidad: this.addQuantity(),
            notas: this.addNotes().trim() || null,
            modificadoresIds: [...this.selectedModifierIds()],
          },
        ],
      })
      .pipe(finalize(() => this.addSubmitting.set(false)))
      .subscribe({
        next: () => {
          this.messages.add({
            severity: 'success',
            summary: this.translateSuccess('comanda.addDialog.success'),
            life: 4000,
          });
          this.closeAddDialog();
          this.refreshRounds();
        },
        error: (error: unknown) => {
          this.addError.set(this.errors.getMessage(error));
        },
      });
  }

  removeDraftItem(item: ComandaItemResponse): void {
    this.rowActionPending.set(item.id);

    this.comandaService
      .cancelUnsentDish(item.id)
      .pipe(finalize(() => this.rowActionPending.set(null)))
      .subscribe({
        next: () => this.refreshRounds(),
        error: (error: unknown) => {
          this.loadError.set(this.errors.getMessage(error));
        },
      });
  }

  sendComanda(): void {
    const account = this.account();
    if (!account) {
      return;
    }

    this.sendingComanda.set(true);

    this.comandaService
      .sendComandaByAccount(account.id)
      .pipe(finalize(() => this.sendingComanda.set(false)))
      .subscribe({
        next: (response) => {
          if (response.rejectedDishes.length > 0) {
            this.messages.add({
              severity: 'warn',
              summary: this.translateSuccess('comanda.send.partialTitle'),
              detail: response.rejectedDishes.map((rejected) => rejected.dishName).join(', '),
              life: 8000,
            });
          } else {
            this.messages.add({
              severity: 'success',
              summary: this.translateSuccess('comanda.send.successTitle'),
              life: 4000,
            });
          }
          this.refreshRounds();
        },
        error: (error: unknown) => {
          this.loadError.set(this.errors.getMessage(error));
        },
      });
  }

  deliverItem(item: ComandaItemResponse): void {
    this.rowActionPending.set(item.id);

    this.comandaService
      .markDishAsDelivered(item.id)
      .pipe(finalize(() => this.rowActionPending.set(null)))
      .subscribe({
        next: () => this.refreshRounds(),
        error: (error: unknown) => {
          this.loadError.set(this.errors.getMessage(error));
        },
      });
  }

  openCancelDialog(item: ComandaItemResponse): void {
    this.cancelTargetItem.set(item);
    this.cancelMotivo.set('');
    this.cancelTipo.set('CLIENTE');
    this.cancelError.set(null);
    this.cancelDialogOpen.set(true);
  }

  closeCancelDialog(): void {
    this.cancelDialogOpen.set(false);
    this.cancelTargetItem.set(null);
  }

  submitCancelException(): void {
    const item = this.cancelTargetItem();
    if (!item || !this.cancelMotivo().trim()) {
      return;
    }

    this.cancelSubmitting.set(true);
    this.cancelError.set(null);

    const request: RegisterDishCancellationRequest = {
      motivo: this.cancelMotivo().trim(),
      tipo: this.cancelTipo(),
    };

    this.comandaService
      .registerDishCancellationException(item.id, request)
      .pipe(finalize(() => this.cancelSubmitting.set(false)))
      .subscribe({
        next: () => {
          this.messages.add({
            severity: 'success',
            summary: this.translateSuccess('comanda.cancelException.success'),
            life: 4000,
          });
          this.closeCancelDialog();
          this.refreshRounds();
        },
        error: (error: unknown) => {
          this.cancelError.set(this.errors.getMessage(error));
        },
      });
  }

  statusBadgeClass(status: string): string {
    return `status-badge status-badge--${status.toLowerCase()}`;
  }

  openSplitDialog(): void {
    this.splitMode.set('personas');
    this.splitPersonCount.set(2);
    this.splitBucketCount.set(2);
    this.itemBucketAssignments.set(new Map());
    this.splitError.set(null);
    this.splitDialogOpen.set(true);
  }

  closeSplitDialog(): void {
    this.splitDialogOpen.set(false);
  }

  setSplitMode(mode: 'personas' | 'items'): void {
    this.splitMode.set(mode);
    this.splitError.set(null);
  }

  setSplitBucketCount(count: number): void {
    const safeCount = Math.max(2, count || 2);
    this.splitBucketCount.set(safeCount);

    const current = new Map(this.itemBucketAssignments());
    for (const [itemId, bucket] of current) {
      if (bucket >= safeCount) {
        current.delete(itemId);
      }
    }
    this.itemBucketAssignments.set(current);
  }

  assignItemToBucket(itemId: number, bucketIndex: number | string): void {
    const bucket = Number(bucketIndex);
    const current = new Map(this.itemBucketAssignments());

    if (Number.isNaN(bucket) || bucket < 0) {
      current.delete(itemId);
    } else {
      current.set(itemId, bucket);
    }

    this.itemBucketAssignments.set(current);
  }

  bucketForItem(itemId: number): number | null {
    return this.itemBucketAssignments().get(itemId) ?? null;
  }

  submitSplitByPeople(): void {
    const account = this.account();
    if (!account || this.splitPersonCount() < 2) {
      return;
    }

    this.splitSubmitting.set(true);
    this.splitError.set(null);

    this.comandaService
      .splitByPeople(account.id, { numeroPersonas: this.splitPersonCount() })
      .pipe(finalize(() => this.splitSubmitting.set(false)))
      .subscribe({
        next: (response) => {
          this.messages.add({
            severity: 'success',
            summary: this.translateSuccess('comanda.split.success'),
            detail: `${response.totalSubcuentas}`,
            life: 4000,
          });
          this.subaccounts.set(response.subcuentas);
          this.closeSplitDialog();
        },
        error: (error: unknown) => {
          this.splitError.set(this.errors.getMessage(error));
        },
      });
  }

  submitSplitByItems(): void {
    const account = this.account();
    if (!account) {
      return;
    }

    const assignments = this.itemBucketAssignments();
    const buckets = new Map<number, SubaccountItemDefinitionRequest>();

    for (const bucketIndex of this.splitBuckets()) {
      buckets.set(bucketIndex, {
        nombre: this.transloco.translate('comanda.split.subaccountFallback', {
          numero: bucketIndex + 1,
        }),
        items: [],
      });
    }

    for (const item of this.assignableItems()) {
      const bucketIndex = assignments.get(item.id);
      if (bucketIndex === undefined) {
        continue;
      }
      buckets.get(bucketIndex)?.items.push({ comandaDetalleId: item.id });
    }

    const subcuentas = [...buckets.values()].filter((bucket) => bucket.items.length > 0);

    if (subcuentas.length < 2) {
      this.splitError.set(this.transloco.translate('comanda.split.validation.minTwoBuckets'));
      return;
    }

    const request: SplitByItemsRequest = { subcuentas };

    this.splitSubmitting.set(true);
    this.splitError.set(null);

    this.comandaService
      .splitByItems(account.id, request)
      .pipe(finalize(() => this.splitSubmitting.set(false)))
      .subscribe({
        next: (response) => {
          this.messages.add({
            severity: 'success',
            summary: this.translateSuccess('comanda.split.success'),
            detail: `${response.totalSubcuentas}`,
            life: 4000,
          });
          this.subaccounts.set(response.subcuentas);
          this.closeSplitDialog();
        },
        error: (error: unknown) => {
          this.splitError.set(this.errors.getMessage(error));
        },
      });
  }

  private refreshSubaccounts(): void {
    const account = this.account();
    if (!account) {
      return;
    }

    this.comandaService
      .getSubaccounts(account.id)
      .pipe(catchError(() => of([])))
      .subscribe((subaccounts) => this.subaccounts.set(subaccounts));
  }

  private refreshRounds(): void {
    const account = this.account();
    if (!account) {
      return;
    }

    this.comandaService
      .getAccountRounds(account.id)
      .pipe(catchError(() => of([])))
      .subscribe((rounds) => this.rounds.set(rounds));
  }

  private translateSuccess(key: string): string {
    return this.transloco.translate(key);
  }
}
