import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { TranslocoPipe } from '@jsverse/transloco';
import { Dialog } from 'primeng/dialog';
import { catchError, EMPTY, finalize, interval, startWith, switchMap } from 'rxjs';
import {
  DishPreparationTransition,
  KitchenComandaItemResponse,
  KitchenComandaResponse,
} from '../../../core/models/kitchen.models';
import { ApiErrorService } from '../../../core/services/api-error.service';
import { KitchenService } from '../../../core/services/kitchen.service';
import { FormFeedbackComponent } from '../../../shared/components/form-feedback/form-feedback';
import { PageHeadingComponent } from '../../../shared/components/page-heading/page-heading';

const POLL_INTERVAL_MS = 5000;

const NEXT_STATUS: Partial<Record<string, DishPreparationTransition>> = {
  RECIBIDO: 'EN_PREPARACION',
  EN_PREPARACION: 'LISTO',
};

@Component({
  selector: 'app-kitchen-page',
  imports: [Dialog, FormFeedbackComponent, FormsModule, PageHeadingComponent, TranslocoPipe],
  templateUrl: './kitchen.html',
  styleUrl: './kitchen.scss',
})
export class KitchenPageComponent implements OnInit {
  private readonly destroyRef = inject(DestroyRef);
  private readonly kitchenService = inject(KitchenService);
  private readonly errors = inject(ApiErrorService);

  readonly loading = signal(false);
  readonly loadError = signal<string | null>(null);
  readonly comandas = signal<KitchenComandaResponse[]>([]);
  readonly onlyDelayed = signal(false);
  readonly itemActionPending = signal<number | null>(null);

  readonly unavailableDialogOpen = signal(false);
  readonly unavailableTargetItem = signal<KitchenComandaItemResponse | null>(null);
  readonly unavailableReason = signal('');
  readonly unavailableSubmitting = signal(false);
  readonly unavailableError = signal<string | null>(null);

  readonly orderedComandas = computed(() =>
    [...this.comandas()].sort((a, b) => {
      const first = a.sentAt ?? a.createdAt;
      const second = b.sentAt ?? b.createdAt;
      return new Date(first).getTime() - new Date(second).getTime();
    }),
  );

  ngOnInit(): void {
    this.startPolling();
  }

  private startPolling(): void {
    this.loading.set(true);

    interval(POLL_INTERVAL_MS)
      .pipe(
        startWith(0),
        switchMap(() =>
          this.kitchenService.getActiveComandas().pipe(
            catchError((error: unknown) => {
              this.loadError.set(this.errors.getMessage(error));
              return EMPTY;
            }),
          ),
        ),
        finalize(() => this.loading.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((comandas) => {
        this.comandas.set(comandas);
        this.loadError.set(null);
        this.loading.set(false);
      });
  }

  filteredItems(comanda: KitchenComandaResponse): KitchenComandaItemResponse[] {
    if (!this.onlyDelayed()) {
      return comanda.items;
    }
    return comanda.items.filter((item) => item.timeExceeded);
  }

  visibleComandas(): KitchenComandaResponse[] {
    if (!this.onlyDelayed()) {
      return this.orderedComandas();
    }
    return this.orderedComandas().filter((comanda) => comanda.timeExceeded);
  }

  nextStatusLabelKey(status: string): string {
    const next = NEXT_STATUS[status];
    return next ? `kitchen.actions.advanceTo.${next}` : '';
  }

  canAdvance(status: string): boolean {
    return !!NEXT_STATUS[status];
  }

  canMarkUnavailable(status: string): boolean {
    return status === 'RECIBIDO' || status === 'EN_PREPARACION';
  }

  advance(item: KitchenComandaItemResponse): void {
    const next = NEXT_STATUS[item.status];
    if (!next) {
      return;
    }

    this.itemActionPending.set(item.id);

    this.kitchenService
      .updateDishPreparationStatus(item.id, { status: next })
      .pipe(finalize(() => this.itemActionPending.set(null)))
      .subscribe({
        next: () => this.refresh(),
        error: (error: unknown) => {
          this.loadError.set(this.errors.getMessage(error));
        },
      });
  }

  openUnavailableDialog(item: KitchenComandaItemResponse): void {
    this.unavailableTargetItem.set(item);
    this.unavailableReason.set('');
    this.unavailableError.set(null);
    this.unavailableDialogOpen.set(true);
  }

  closeUnavailableDialog(): void {
    this.unavailableDialogOpen.set(false);
    this.unavailableTargetItem.set(null);
  }

  submitUnavailable(): void {
    const item = this.unavailableTargetItem();
    if (!item) {
      return;
    }

    this.unavailableSubmitting.set(true);
    this.unavailableError.set(null);

    this.kitchenService
      .markDishAsUnavailable(item.id, {
        reason: this.unavailableReason().trim() || null,
        disableInMenu: true,
      })
      .pipe(finalize(() => this.unavailableSubmitting.set(false)))
      .subscribe({
        next: () => {
          this.closeUnavailableDialog();
          this.refresh();
        },
        error: (error: unknown) => {
          this.unavailableError.set(this.errors.getMessage(error));
        },
      });
  }

  statusBadgeClass(status: string): string {
    return `status-badge status-badge--${status.toLowerCase()}`;
  }

  private refresh(): void {
    this.kitchenService.getActiveComandas().subscribe({
      next: (comandas) => this.comandas.set(comandas),
      error: (error: unknown) => {
        this.loadError.set(this.errors.getMessage(error));
      },
    });
  }
}
