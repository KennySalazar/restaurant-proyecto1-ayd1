import { Component, computed, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';
import { MessageService } from 'primeng/api';
import { debounceTime, distinctUntilChanged, finalize, forkJoin, Subject } from 'rxjs';

import { DishSummary } from '../../../../core/models/dish.models';
import { ModifierSummary } from '../../../../core/models/modifier.models';
import { ApiErrorService } from '../../../../core/services/api-error.service';
import { DishService } from '../../../../core/services/dish.service';
import { ModifierService } from '../../../../core/services/modifier.service';
import { ConfirmationDialogComponent } from '../../../../shared/components/confirmation-dialog/confirmation-dialog';
import { FormFeedbackComponent } from '../../../../shared/components/form-feedback/form-feedback';
import { ModifierFormDialogComponent } from '../modifier-form-dialog/modifier-form-dialog';

@Component({
  selector: 'app-menu-modifiers-tab',
  imports: [
    ConfirmationDialogComponent,
    FormFeedbackComponent,
    ModifierFormDialogComponent,
    TranslocoPipe,
  ],
  templateUrl: './modifiers-tab.html',
  styleUrl: './modifiers-tab.scss',
})
export class ModifiersTabComponent implements OnInit {
  private readonly modifierService = inject(ModifierService);
  private readonly dishService = inject(DishService);
  private readonly errors = inject(ApiErrorService);
  private readonly messages = inject(MessageService);
  private readonly transloco = inject(TranslocoService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly searchSubject = new Subject<string>();

  readonly modifiers = signal<ModifierSummary[]>([]);
  readonly dishes = signal<DishSummary[]>([]);
  readonly isLoading = signal(true);
  readonly isFiltering = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly searchTerm = signal('');
  readonly selectedDishId = signal<number | null>(null);

  readonly totalModifiers = computed(() => this.modifiers().length);

  readonly activeModifiers = computed(() => this.modifiers().filter((m) => m.active).length);

  readonly inactiveModifiers = computed(() => this.modifiers().filter((m) => !m.active).length);

  readonly hasActiveFilters = computed(() => !!this.searchTerm() || this.selectedDishId() != null);

  ngOnInit(): void {
    this.searchSubject
      .pipe(debounceTime(300), distinctUntilChanged(), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.loadModifiers());

    forkJoin({
      modifiers: this.modifierService.listModifiers(null, null),
      dishes: this.dishService.listDishes(null, null, true),
    })
      .pipe(finalize(() => this.isLoading.set(false)))
      .subscribe({
        next: ({ modifiers, dishes }) => {
          this.modifiers.set(modifiers);
          this.dishes.set(dishes);
        },
        error: (error: unknown) => {
          this.errorMessage.set(this.errors.getMessage(error));
        },
      });
  }

  loadModifiers(): void {
    this.isFiltering.set(true);
    this.errorMessage.set(null);

    this.modifierService
      .listModifiers(this.selectedDishId(), this.searchTerm())
      .pipe(finalize(() => this.isFiltering.set(false)))
      .subscribe({
        next: (modifiers) => this.modifiers.set(modifiers),
        error: (error: unknown) => {
          this.errorMessage.set(this.errors.getMessage(error));
        },
      });
  }

  onSearchChange(value: string): void {
    this.searchTerm.set(value);
    this.searchSubject.next(value);
  }

  onDishChange(dishId: string): void {
    this.selectedDishId.set(dishId ? Number(dishId) : null);
    this.loadModifiers();
  }

  clearFilters(): void {
    this.searchTerm.set('');
    this.selectedDishId.set(null);
    this.loadModifiers();
  }

  readonly formOpen = signal(false);
  readonly editingModifier = signal<ModifierSummary | null>(null);

  openRegistration(): void {
    this.editingModifier.set(null);
    this.formOpen.set(true);
  }

  openEdit(modifier: ModifierSummary): void {
    this.editingModifier.set(modifier);
    this.formOpen.set(true);
  }

  closeForm(): void {
    this.formOpen.set(false);
    this.editingModifier.set(null);
  }

  onModifierSaved(): void {
    this.formOpen.set(false);
    this.editingModifier.set(null);
    this.loadModifiers();
  }

  readonly deactivationCandidate = signal<ModifierSummary | null>(null);
  readonly isDeactivating = signal(false);
  readonly deactivationMessage = signal<string | null>(null);

  openDeactivation(modifier: ModifierSummary): void {
    this.deactivationMessage.set(null);
    this.deactivationCandidate.set(modifier);
  }

  cancelDeactivation(): void {
    if (this.isDeactivating()) {
      return;
    }

    this.deactivationCandidate.set(null);
    this.deactivationMessage.set(null);
  }

  confirmDeactivation(): void {
    const modifier = this.deactivationCandidate();

    if (!modifier) {
      return;
    }

    this.isDeactivating.set(true);
    this.deactivationMessage.set(null);

    this.modifierService
      .deactivateModifier(modifier.id)
      .pipe(finalize(() => this.isDeactivating.set(false)))
      .subscribe({
        next: (response) => {
          this.messages.add({
            severity: 'success',
            summary: this.transloco.translate('menu.modifiers.deactivation.success.title'),
            detail: response.message,
            life: 6000,
          });

          this.deactivationCandidate.set(null);
          this.loadModifiers();
        },
        error: (error: unknown) => {
          this.deactivationMessage.set(this.errors.getMessage(error));
        },
      });
  }

  formatPrice(value: number): string {
    return new Intl.NumberFormat('es-GT', {
      style: 'currency',
      currency: 'GTQ',
    }).format(value);
  }

  dishNames(modifier: ModifierSummary): string {
    return modifier.associatedDishes.map((d) => d.name).join(', ');
  }
}
