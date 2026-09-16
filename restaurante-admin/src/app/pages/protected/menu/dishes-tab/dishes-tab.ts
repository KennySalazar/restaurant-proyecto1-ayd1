import { Component, computed, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';
import { debounceTime, distinctUntilChanged, finalize, forkJoin, Subject } from 'rxjs';

import { DishCategory, DishSummary } from '../../../../core/models/dish.models';
import { ApiErrorService } from '../../../../core/services/api-error.service';
import { DishService } from '../../../../core/services/dish.service';
import { FormFeedbackComponent } from '../../../../shared/components/form-feedback/form-feedback';

@Component({
  selector: 'app-menu-dishes-tab',
  imports: [FormFeedbackComponent, TranslocoPipe],
  templateUrl: './dishes-tab.html',
  styleUrl: './dishes-tab.scss',
})
export class DishesTabComponent implements OnInit {
  private readonly dishService = inject(DishService);
  private readonly errors = inject(ApiErrorService);
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

  readonly totalDishes = computed(() => this.dishes().length);

  readonly availableDishes = computed(() => this.dishes().filter((dish) => dish.available).length);

  readonly unavailableDishes = computed(
    () => this.dishes().filter((dish) => !dish.available).length,
  );

  readonly hasActiveFilters = computed(
    () => !!this.searchTerm() || this.selectedCategory() != null,
  );

  ngOnInit(): void {
    this.searchSubject
      .pipe(debounceTime(300), distinctUntilChanged(), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.loadDishes());

    forkJoin({
      dishes: this.dishService.listDishes(),
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

    this.dishService
      .listDishes(this.selectedCategory(), this.searchTerm())
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

  clearFilters(): void {
    this.searchTerm.set('');
    this.selectedCategory.set(null);
    this.loadDishes();
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
