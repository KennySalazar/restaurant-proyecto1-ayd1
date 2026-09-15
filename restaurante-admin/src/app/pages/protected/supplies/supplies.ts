import { Component, computed, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { TranslocoPipe } from '@jsverse/transloco';
import { debounceTime, distinctUntilChanged, finalize, forkJoin, Subject } from 'rxjs';

import { Supply, SupplyCategory } from '../../../core/models/supply.models';
import { ApiErrorService } from '../../../core/services/api-error.service';
import { SupplyService } from '../../../core/services/supply.service';
import { FormFeedbackComponent } from '../../../shared/components/form-feedback/form-feedback';
import { PageHeadingComponent } from '../../../shared/components/page-heading/page-heading';

@Component({
  selector: 'app-supplies-page',
  imports: [FormFeedbackComponent, PageHeadingComponent, TranslocoPipe],
  templateUrl: './supplies.html',
  styleUrl: './supplies.scss',
})
export class SuppliesPageComponent implements OnInit {
  private readonly supplyService = inject(SupplyService);
  private readonly errors = inject(ApiErrorService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly searchSubject = new Subject<string>();

  readonly supplies = signal<Supply[]>([]);
  readonly categories = signal<SupplyCategory[]>([]);
  readonly isLoading = signal(true);
  readonly isFiltering = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly searchTerm = signal('');
  readonly selectedCategory = signal<number | null>(null);

  readonly totalSupplies = computed(() => this.supplies().length);

  readonly activeSupplies = computed(
    () => this.supplies().filter((supply) => supply.active).length,
  );

  readonly outOfStockSupplies = computed(
    () => this.supplies().filter((supply) => supply.currentStock <= 0).length,
  );

  readonly hasActiveFilters = computed(
    () => !!this.searchTerm() || this.selectedCategory() != null,
  );

  ngOnInit(): void {
    this.searchSubject
      .pipe(debounceTime(300), distinctUntilChanged(), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.loadSupplies());

    forkJoin({
      supplies: this.supplyService.listSupplies(),
      categories: this.supplyService.listCategories(),
    })
      .pipe(finalize(() => this.isLoading.set(false)))
      .subscribe({
        next: ({ supplies, categories }) => {
          this.supplies.set(supplies);
          this.categories.set(categories);
        },
        error: (error: unknown) => {
          this.errorMessage.set(this.errors.getMessage(error));
        },
      });
  }

  loadSupplies(): void {
    this.isFiltering.set(true);
    this.errorMessage.set(null);

    this.supplyService
      .listSupplies(this.selectedCategory(), this.searchTerm())
      .pipe(finalize(() => this.isFiltering.set(false)))
      .subscribe({
        next: (supplies) => this.supplies.set(supplies),
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
    this.loadSupplies();
  }

  clearFilters(): void {
    this.searchTerm.set('');
    this.selectedCategory.set(null);
    this.loadSupplies();
  }

  formatCost(value: number): string {
    return new Intl.NumberFormat('es-GT', {
      style: 'currency',
      currency: 'GTQ',
    }).format(value);
  }

  formatStock(value: number): string {
    return new Intl.NumberFormat('es-GT', {
      maximumFractionDigits: 2,
    }).format(value);
  }
}
