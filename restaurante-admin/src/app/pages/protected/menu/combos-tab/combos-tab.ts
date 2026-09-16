import { Component, computed, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';
import { MessageService } from 'primeng/api';
import { debounceTime, distinctUntilChanged, finalize, forkJoin, Subject } from 'rxjs';

import { ComboSummary } from '../../../../core/models/combo.models';
import { DishSummary } from '../../../../core/models/dish.models';
import { ApiErrorService } from '../../../../core/services/api-error.service';
import { ComboService } from '../../../../core/services/combo.service';
import { DishService } from '../../../../core/services/dish.service';
import { ConfirmationDialogComponent } from '../../../../shared/components/confirmation-dialog/confirmation-dialog';
import { FormFeedbackComponent } from '../../../../shared/components/form-feedback/form-feedback';
import { ComboFormDialogComponent } from '../combo-form-dialog/combo-form-dialog';

@Component({
  selector: 'app-menu-combos-tab',
  imports: [
    ComboFormDialogComponent,
    ConfirmationDialogComponent,
    FormFeedbackComponent,
    TranslocoPipe,
  ],
  templateUrl: './combos-tab.html',
  styleUrl: './combos-tab.scss',
})
export class CombosTabComponent implements OnInit {
  private readonly comboService = inject(ComboService);
  private readonly dishService = inject(DishService);
  private readonly errors = inject(ApiErrorService);
  private readonly messages = inject(MessageService);
  private readonly transloco = inject(TranslocoService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly searchSubject = new Subject<string>();

  readonly combos = signal<ComboSummary[]>([]);
  readonly dishes = signal<DishSummary[]>([]);
  readonly isLoading = signal(true);
  readonly isFiltering = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly searchTerm = signal('');
  readonly selectedStatus = signal<'active' | 'retired' | 'all'>('all');

  readonly totalCombos = computed(() => this.combos().length);

  readonly activeCombos = computed(() => this.combos().filter((c) => c.active).length);

  readonly retiredCombos = computed(() => this.combos().filter((c) => !c.active).length);

  readonly hasActiveFilters = computed(
    () => !!this.searchTerm() || this.selectedStatus() !== 'all',
  );

  ngOnInit(): void {
    this.searchSubject
      .pipe(debounceTime(300), distinctUntilChanged(), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.loadCombos());

    forkJoin({
      combos: this.comboService.listCombos(null, null),
      dishes: this.dishService.listDishes(null, null, true),
    })
      .pipe(finalize(() => this.isLoading.set(false)))
      .subscribe({
        next: ({ combos, dishes }) => {
          this.combos.set(combos);
          this.dishes.set(dishes);
        },
        error: (error: unknown) => {
          this.errorMessage.set(this.errors.getMessage(error));
        },
      });
  }

  loadCombos(): void {
    this.isFiltering.set(true);
    this.errorMessage.set(null);

    const active = this.selectedStatus() === 'all' ? null : this.selectedStatus() === 'active';

    this.comboService
      .listCombos(this.searchTerm(), active)
      .pipe(finalize(() => this.isFiltering.set(false)))
      .subscribe({
        next: (combos) => this.combos.set(combos),
        error: (error: unknown) => {
          this.errorMessage.set(this.errors.getMessage(error));
        },
      });
  }

  onSearchChange(value: string): void {
    this.searchTerm.set(value);
    this.searchSubject.next(value);
  }

  onStatusChange(status: string): void {
    this.selectedStatus.set(status as 'active' | 'retired' | 'all');
    this.loadCombos();
  }

  clearFilters(): void {
    this.searchTerm.set('');
    this.selectedStatus.set('all');
    this.loadCombos();
  }

  readonly formOpen = signal(false);
  readonly editingCombo = signal<ComboSummary | null>(null);

  openRegistration(): void {
    this.editingCombo.set(null);
    this.formOpen.set(true);
  }

  openEdit(combo: ComboSummary): void {
    this.editingCombo.set(combo);
    this.formOpen.set(true);
  }

  closeForm(): void {
    this.formOpen.set(false);
    this.editingCombo.set(null);
  }

  onComboSaved(): void {
    this.formOpen.set(false);
    this.editingCombo.set(null);
    this.loadCombos();
  }

  readonly retirementCandidate = signal<ComboSummary | null>(null);
  readonly isRetiring = signal(false);
  readonly retirementMessage = signal<string | null>(null);

  openRetirement(combo: ComboSummary): void {
    this.retirementMessage.set(null);
    this.retirementCandidate.set(combo);
  }

  cancelRetirement(): void {
    if (this.isRetiring()) {
      return;
    }

    this.retirementCandidate.set(null);
    this.retirementMessage.set(null);
  }

  confirmRetirement(): void {
    const combo = this.retirementCandidate();

    if (!combo) {
      return;
    }

    this.isRetiring.set(true);
    this.retirementMessage.set(null);

    this.comboService
      .retireCombo(combo.id)
      .pipe(finalize(() => this.isRetiring.set(false)))
      .subscribe({
        next: (response) => {
          this.messages.add({
            severity: 'success',
            summary: this.transloco.translate('menu.combos.retirement.success.title'),
            detail: response.message,
            life: 6000,
          });

          this.retirementCandidate.set(null);
          this.loadCombos();
        },
        error: (error: unknown) => {
          this.retirementMessage.set(this.errors.getMessage(error));
        },
      });
  }

  formatPrice(value: number): string {
    return new Intl.NumberFormat('es-GT', {
      style: 'currency',
      currency: 'GTQ',
    }).format(value);
  }

  itemLabel(combo: ComboSummary): string {
    return combo.items
      .map((item) => (item.quantity > 1 ? `${item.dishName} ×${item.quantity}` : item.dishName))
      .join(', ');
  }
}
