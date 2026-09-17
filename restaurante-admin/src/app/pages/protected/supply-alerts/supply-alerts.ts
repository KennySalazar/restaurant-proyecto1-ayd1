import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';
import { finalize, forkJoin } from 'rxjs';

import {
  SupplyAlert,
  SupplyAlertLevel,
  SupplyAlertSummary,
  SupplyCategory,
} from '../../../core/models/supply.models';
import { ApiErrorService } from '../../../core/services/api-error.service';
import { SupplyService } from '../../../core/services/supply.service';
import { FormFeedbackComponent } from '../../../shared/components/form-feedback/form-feedback';
import { PageHeadingComponent } from '../../../shared/components/page-heading/page-heading';

@Component({
  selector: 'app-supply-alerts-page',
  imports: [FormFeedbackComponent, PageHeadingComponent, TranslocoPipe],
  templateUrl: './supply-alerts.html',
  styleUrl: './supply-alerts.scss',
})
export class SupplyAlertsPageComponent implements OnInit {
  private readonly supplyService = inject(SupplyService);
  private readonly errors = inject(ApiErrorService);

  readonly summary = signal<SupplyAlertSummary | null>(null);
  readonly categories = signal<SupplyCategory[]>([]);
  readonly isLoading = signal(true);
  readonly errorMessage = signal<string | null>(null);
  readonly selectedCategory = signal<number | null>(null);
  readonly selectedLevel = signal<SupplyAlertLevel | null>(null);

  readonly totalAlerts = computed(() => this.summary()?.totalAlerts ?? 0);
  readonly outOfStockCount = computed(() => this.summary()?.outOfStockCount ?? 0);
  readonly lowStockCount = computed(() => this.summary()?.lowStockCount ?? 0);

  readonly hasActiveFilters = computed(
    () => this.selectedCategory() != null || this.selectedLevel() != null,
  );

  readonly filteredAlerts = computed(() => {
    const alerts = this.summary()?.alerts ?? [];
    const categoryId = this.selectedCategory();
    const level = this.selectedLevel();

    return alerts.filter((alert) => {
      if (categoryId != null && alert.categoryId !== categoryId) {
        return false;
      }

      if (level && alert.alertLevel !== level) {
        return false;
      }

      return true;
    });
  });

  ngOnInit(): void {
    forkJoin({
      summary: this.supplyService.listAlertsSummary(),
      categories: this.supplyService.listCategories(),
    })
      .pipe(finalize(() => this.isLoading.set(false)))
      .subscribe({
        next: ({ summary, categories }) => {
          this.summary.set(summary);
          this.categories.set(categories);
        },
        error: (error: unknown) => {
          this.errorMessage.set(this.errors.getMessage(error));
        },
      });
  }

  loadAlerts(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.supplyService
      .listAlertsSummary()
      .pipe(finalize(() => this.isLoading.set(false)))
      .subscribe({
        next: (summary) => this.summary.set(summary),
        error: (error: unknown) => {
          this.errorMessage.set(this.errors.getMessage(error));
        },
      });
  }

  onCategoryChange(categoryId: string): void {
    this.selectedCategory.set(categoryId ? Number(categoryId) : null);
  }

  onLevelChange(level: string): void {
    this.selectedLevel.set((level as SupplyAlertLevel) || null);
  }

  clearFilters(): void {
    this.selectedCategory.set(null);
    this.selectedLevel.set(null);
  }

  levelKey(level: SupplyAlertLevel): string {
    return `supplyAlerts.card.level.${level}`;
  }

  levelClass(level: SupplyAlertLevel): string {
    return `alert-badge--${level.toLowerCase()}`;
  }

  priorityKey(priority: string): string {
    return `supplyAlerts.card.priority.${priority}`;
  }

  formatStock(value: number): string {
    return new Intl.NumberFormat('es-GT', {
      maximumFractionDigits: 2,
    }).format(value);
  }

  formatDateTime(value: string): string {
    const date = new Date(value);

    if (Number.isNaN(date.getTime())) {
      return value;
    }

    return new Intl.DateTimeFormat('es-GT', {
      dateStyle: 'medium',
      timeStyle: 'short',
    }).format(date);
  }
}
