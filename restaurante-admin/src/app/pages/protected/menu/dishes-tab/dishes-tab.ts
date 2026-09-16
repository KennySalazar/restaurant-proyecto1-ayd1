import { Component, computed, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';
import { MessageService } from 'primeng/api';
import { debounceTime, distinctUntilChanged, finalize, forkJoin, Subject } from 'rxjs';

import { DishCategory, DishSummary } from '../../../../core/models/dish.models';
import { ApiErrorService } from '../../../../core/services/api-error.service';
import { DishService } from '../../../../core/services/dish.service';
import { ConfirmationDialogComponent } from '../../../../shared/components/confirmation-dialog/confirmation-dialog';
import { FormFeedbackComponent } from '../../../../shared/components/form-feedback/form-feedback';
import { DishFormDialogComponent } from '../dish-form-dialog/dish-form-dialog';

@Component({
  selector: 'app-menu-dishes-tab',
  imports: [
    ConfirmationDialogComponent,
    DishFormDialogComponent,
    FormFeedbackComponent,
    TranslocoPipe,
  ],
  templateUrl: './dishes-tab.html',
  styleUrl: './dishes-tab.scss',
})
export class DishesTabComponent implements OnInit {
  private readonly dishService = inject(DishService);
  private readonly errors = inject(ApiErrorService);
  private readonly messages = inject(MessageService);
  private readonly transloco = inject(TranslocoService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly searchSubject = new Subject<string>();

  readonly dishes = signal<DishSummary[]>([]);
  readonly categories = signal<DishCategory[]>([]);
  readonly isLoading = signal(true);
  readonly isFiltering = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly searchTerm = signal('');
  readonly selectedCategory = signal<number | null>(null);
  readonly selectedStatus = signal<'active' | 'retired' | 'all'>('active');

  readonly totalDishes = computed(() => this.dishes().length);

  readonly availableDishes = computed(() => this.dishes().filter((dish) => dish.available).length);

  readonly unavailableDishes = computed(
    () => this.dishes().filter((dish) => !dish.available).length,
  );

  readonly hasActiveFilters = computed(
    () =>
      !!this.searchTerm() || this.selectedCategory() != null || this.selectedStatus() !== 'active',
  );

  ngOnInit(): void {
    this.searchSubject
      .pipe(debounceTime(300), distinctUntilChanged(), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.loadDishes());

    forkJoin({
      dishes: this.dishService.listDishes(null, null, true),
      categories: this.dishService.listCategories(),
    })
      .pipe(finalize(() => this.isLoading.set(false)))
      .subscribe({
        next: ({ dishes, categories }) => {
          this.dishes.set(dishes);
          this.categories.set(categories);
        },
        error: (error: unknown) => {
          this.errorMessage.set(this.errors.getMessage(error));
        },
      });
  }

  loadDishes(): void {
    this.isFiltering.set(true);
    this.errorMessage.set(null);

    const active =
      this.selectedStatus() === 'active'
        ? true
        : this.selectedStatus() === 'retired'
          ? false
          : null;

    this.dishService
      .listDishes(this.selectedCategory(), this.searchTerm(), active)
      .pipe(finalize(() => this.isFiltering.set(false)))
      .subscribe({
        next: (dishes) => this.dishes.set(dishes),
        error: (error: unknown) => {
          this.errorMessage.set(this.errors.getMessage(error));
        },
      });
  }

  onSearchChange(value: string): void {
    this.searchTerm.set(value);
    this.searchSubject.next(value);
  }

  onCategoryChange(categoryId: string): void {
    this.selectedCategory.set(categoryId ? Number(categoryId) : null);
    this.loadDishes();
  }

  onStatusChange(status: string): void {
    this.selectedStatus.set(status as 'active' | 'retired' | 'all');
    this.loadDishes();
  }

  clearFilters(): void {
    this.searchTerm.set('');
    this.selectedCategory.set(null);
    this.selectedStatus.set('active');
    this.loadDishes();
  }

  readonly registrationOpen = signal(false);
  readonly editingDish = signal<DishSummary | null>(null);

  openRegistration(): void {
    this.editingDish.set(null);
    this.registrationOpen.set(true);
  }

  openEdit(dish: DishSummary): void {
    this.editingDish.set(dish);
    this.registrationOpen.set(true);
  }

  closeRegistration(): void {
    this.registrationOpen.set(false);
    this.editingDish.set(null);
  }

  onDishSaved(): void {
    this.registrationOpen.set(false);
    this.editingDish.set(null);
    this.loadDishes();
  }

  readonly retirementCandidate = signal<DishSummary | null>(null);
  readonly isRetiring = signal(false);
  readonly retirementMessage = signal<string | null>(null);

  openRetirement(dish: DishSummary): void {
    this.retirementMessage.set(null);
    this.retirementCandidate.set(dish);
  }

  cancelRetirement(): void {
    if (this.isRetiring()) {
      return;
    }

    this.retirementCandidate.set(null);
    this.retirementMessage.set(null);
  }

  confirmRetirement(): void {
    const dish = this.retirementCandidate();

    if (!dish) {
      return;
    }

    this.isRetiring.set(true);
    this.retirementMessage.set(null);

    this.dishService
      .retireDish(dish.id)
      .pipe(finalize(() => this.isRetiring.set(false)))
      .subscribe({
        next: (response) => {
          this.messages.add({
            severity: 'success',
            summary: this.transloco.translate('menu.retirement.success.title'),
            detail: this.transloco.translate('menu.retirement.success.message', {
              name: response.dish.name,
            }),
            life: 6000,
          });

          this.retirementCandidate.set(null);
          this.loadDishes();
        },
        error: (error: unknown) => {
          this.retirementMessage.set(this.errors.getMessage(error));
        },
      });
  }

  readonly togglingDishId = signal<number | null>(null);

  toggleAvailability(dish: DishSummary): void {
    if (this.togglingDishId() === dish.id) {
      return;
    }

    this.togglingDishId.set(dish.id);

    this.dishService
      .updateDishAvailability(dish.id, !dish.manualAvailable)
      .pipe(finalize(() => this.togglingDishId.set(null)))
      .subscribe({
        next: (response) => {
          this.dishes.update((dishes) =>
            dishes.map((current) => (current.id === response.dish.id ? response.dish : current)),
          );

          this.messages.add({
            severity: 'success',
            summary: this.transloco.translate('menu.availability.success.title'),
            detail: response.message,
            life: 6000,
          });
        },
        error: (error: unknown) => {
          this.messages.add({
            severity: 'error',
            summary: this.transloco.translate('menu.availability.error.title'),
            detail: this.errors.getMessage(error),
            life: 7000,
          });
        },
      });
  }

  formatCost(value: number): string {
    return new Intl.NumberFormat('es-GT', {
      style: 'currency',
      currency: 'GTQ',
    }).format(value);
  }

  reasonText(dish: DishSummary): string {
    if (dish.unavailabilityReasonDescription) {
      return dish.unavailabilityReasonDescription;
    }

    if (dish.unavailabilityReason) {
      return this.transloco.translate(`menu.availability.reasons.${dish.unavailabilityReason}`);
    }

    return '';
  }
}
