import { Component, computed, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';
import { MessageService } from 'primeng/api';
import { finalize, forkJoin } from 'rxjs';

import {
  DishSummary,
  Recipe,
  RecipeIngredientEdit,
  UpdateRecipeRequest,
} from '../../../core/models/recipe.models';
import { MeasurementUnit, Supply } from '../../../core/models/supply.models';
import { ApiErrorService } from '../../../core/services/api-error.service';
import { RecipeService } from '../../../core/services/recipe.service';
import { SupplyService } from '../../../core/services/supply.service';
import { FormFeedbackComponent } from '../../../shared/components/form-feedback/form-feedback';
import { IngredientEditorComponent } from '../../../shared/components/ingredient-editor/ingredient-editor';
import { PageHeadingComponent } from '../../../shared/components/page-heading/page-heading';

type RecipesTab = 'dishes' | 'modifiers' | 'costs' | 'history';

@Component({
  selector: 'app-recipes-page',
  imports: [
    FormFeedbackComponent,
    IngredientEditorComponent,
    PageHeadingComponent,
    ReactiveFormsModule,
    TranslocoPipe,
  ],
  templateUrl: './recipes.html',
  styleUrl: './recipes.scss',
})
export class RecipesPageComponent implements OnInit {
  private readonly formBuilder = inject(FormBuilder);
  private readonly recipeService = inject(RecipeService);
  private readonly supplyService = inject(SupplyService);
  private readonly errors = inject(ApiErrorService);
  private readonly messages = inject(MessageService);
  private readonly transloco = inject(TranslocoService);
  private readonly destroyRef = inject(DestroyRef);

  readonly activeTab = signal<RecipesTab>('dishes');

  readonly tabs: { id: RecipesTab; labelKey: string; icon: string }[] = [
    { id: 'dishes', labelKey: 'recipes.tabs.dishes', icon: 'pi-cutlery' },
    { id: 'modifiers', labelKey: 'recipes.tabs.modifiers', icon: 'pi-plus' },
    { id: 'costs', labelKey: 'recipes.tabs.costs', icon: 'pi-dollar' },
    { id: 'history', labelKey: 'recipes.tabs.history', icon: 'pi-history' },
  ];

  readonly dishes = signal<DishSummary[]>([]);
  readonly supplies = signal<Supply[]>([]);
  readonly measurementUnits = signal<MeasurementUnit[]>([]);
  readonly isInitialLoading = signal(true);
  readonly initialErrorMessage = signal<string | null>(null);

  readonly searchTerm = signal('');
  readonly selectedDishId = signal<number | null>(null);
  readonly selectedRecipe = signal<Recipe | null>(null);
  readonly isRecipeLoading = signal(false);

  readonly isDefineOpen = signal(false);
  readonly isSavingDefine = signal(false);
  readonly defineSubmitted = signal(false);
  readonly defineServerMessage = signal<string | null>(null);
  readonly defineDish = signal<DishSummary | null>(null);

  private readonly recipeIngredientsValidator = (
    control: AbstractControl,
  ): ValidationErrors | null => {
    const value = control.value as RecipeIngredientEdit[] | null;
    if (!value || value.length === 0) {
      return { ingredientsRequired: true };
    }

    const seen = new Set<number>();
    let hasCompleteEntry = false;
    for (const item of value) {
      if (item.supplyId == null || item.quantity == null || item.quantity <= 0) {
        continue;
      }
      hasCompleteEntry = true;
      if (seen.has(item.supplyId)) {
        return { duplicate: true };
      }
      seen.add(item.supplyId);
    }

    return hasCompleteEntry ? null : { incomplete: true };
  };

  readonly defineForm = this.formBuilder.group({
    ingredients: [[] as RecipeIngredientEdit[], [this.recipeIngredientsValidator]],
    changeReason: ['', [Validators.maxLength(500)]],
  });

  readonly selectedDish = computed(
    () => this.dishes().find((dish) => dish.id === this.selectedDishId()) ?? null,
  );

  readonly filteredDishes = computed(() => {
    const term = this.searchTerm().trim().toLowerCase();
    if (!term) {
      return this.dishes();
    }
    return this.dishes().filter(
      (dish) =>
        dish.name.toLowerCase().includes(term) || (dish.code ?? '').toLowerCase().includes(term),
    );
  });

  readonly dishTotal = computed(() => this.dishes().length);

  readonly dishesWithRecipe = computed(
    () => this.dishes().filter((dish) => dishHasRecipe(dish)).length,
  );

  readonly dishesWithoutRecipe = computed(
    () => this.dishes().filter((dish) => !dishHasRecipe(dish)).length,
  );

  readonly supplyById = computed(() => {
    const map = new Map<number, Supply>();
    for (const supply of this.supplies()) {
      map.set(supply.id, supply);
    }
    return map;
  });

  defineValidationErrorKey(): string | null {
    const ingredientsControl = this.defineForm.controls.ingredients;
    if (ingredientsControl.hasError('ingredientsRequired')) {
      return 'recipes.define.validation.ingredientsRequired';
    }
    if (ingredientsControl.hasError('duplicate')) {
      return 'recipes.define.validation.duplicate';
    }
    if (ingredientsControl.hasError('incomplete')) {
      return 'recipes.define.validation.incomplete';
    }
    return null;
  }

  defineCostPreview(): { total: number; hasConverted: boolean } {
    const suppliesById = this.supplyById();
    let total = 0;
    let hasConverted = false;
    const rows = this.defineForm.controls.ingredients.value ?? [];
    for (const row of rows) {
      const supply = row.supplyId != null ? suppliesById.get(row.supplyId) : undefined;
      if (!supply || row.quantity == null || row.quantity <= 0) {
        continue;
      }
      const sameUnit = row.measurementUnitId == null || row.measurementUnitId === supply.unitId;
      if (sameUnit) {
        total += supply.unitCost * row.quantity;
      } else {
        hasConverted = true;
      }
    }
    return { total, hasConverted };
  }

  ngOnInit(): void {
    forkJoin({
      dishes: this.recipeService.listDishes(null, null, true),
      supplies: this.supplyService.listSupplies(),
      units: this.supplyService.listMeasurementUnits(),
    })
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isInitialLoading.set(false)),
      )
      .subscribe({
        next: ({ dishes, supplies, units }) => {
          this.dishes.set(dishes);
          this.supplies.set(supplies);
          this.measurementUnits.set(units);
          const first = dishes[0];
          if (first) {
            this.selectDish(first.id);
          }
        },
        error: (error: unknown) => {
          this.initialErrorMessage.set(this.errors.getMessage(error));
        },
      });
  }

  setTab(tab: RecipesTab): void {
    this.activeTab.set(tab);
  }

  updateSearch(value: string): void {
    this.searchTerm.set(value);
  }

  clearSearch(): void {
    this.searchTerm.set('');
  }

  dishHasRecipe(dish: DishSummary | null): boolean {
    return dishHasRecipe(dish);
  }

  selectDish(dishId: number): void {
    this.selectedDishId.set(dishId);
    this.selectedRecipe.set(null);
    this.isRecipeLoading.set(true);
    this.recipeService
      .getDishRecipe(dishId)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isRecipeLoading.set(false)),
      )
      .subscribe((recipe) => this.selectedRecipe.set(recipe));
  }

  readonly isUpdatingRecipe = computed(() => {
    const dish = this.defineDish();
    return dish != null && dishHasRecipe(dish);
  });

  openDefine(dish: DishSummary): void {
    if (dishHasRecipe(dish)) {
      const title = this.transloco.translate('recipes.define.warning.title');
      const detail = this.transloco.translate('recipes.define.warning.alreadyDefined', {
        dish: dish.name,
      });
      this.messages.add({ severity: 'warn', summary: title, detail });
      return;
    }

    this.defineDish.set(dish);
    this.prepareDefineForm([
      { supplyId: null, quantity: null, measurementUnitId: null, notes: null },
    ]);
  }

  openUpdate(dish: DishSummary): void {
    const recipe = this.selectedRecipe();
    if (!dishHasRecipe(dish) || !recipe) {
      return;
    }

    this.defineDish.set(dish);
    this.prepareDefineForm(
      recipe.ingredients.map((ing) => ({
        supplyId: ing.supplyId,
        quantity: ing.quantity,
        measurementUnitId: ing.measurementUnitId,
        notes: ing.notes,
      })),
    );
  }

  private prepareDefineForm(ingredients: RecipeIngredientEdit[]): void {
    this.defineServerMessage.set(null);
    this.defineSubmitted.set(false);
    this.defineForm.reset();
    this.defineForm.controls.ingredients.setValue(ingredients);
    this.isDefineOpen.set(true);
  }

  closeDefine(): void {
    if (this.isSavingDefine()) {
      return;
    }
    this.isDefineOpen.set(false);
  }

  submitDefine(): void {
    if (this.isSavingDefine()) {
      return;
    }

    this.defineSubmitted.set(true);
    this.defineServerMessage.set(null);

    const ingredientsControl = this.defineForm.controls.ingredients;
    if (ingredientsControl.invalid || this.defineForm.invalid) {
      return;
    }

    const dish = this.defineDish();
    if (!dish) {
      return;
    }

    this.isSavingDefine.set(true);
    const updating = this.isUpdatingRecipe();
    const request: UpdateRecipeRequest = {
      ingredients: ingredientsControl.value ?? [],
      changeReason: this.defineForm.controls.changeReason.value?.trim() || null,
    };

    const submission = updating
      ? this.recipeService.updateDishRecipe(dish.id, request)
      : this.recipeService.defineDishRecipe(dish.id, request);

    submission
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isSavingDefine.set(false)),
      )
      .subscribe({
        next: (response) => {
          this.isDefineOpen.set(false);
          const summary = this.transloco.translate(
            updating ? 'recipes.update.success.title' : 'recipes.define.success.title',
          );
          const detail = this.transloco.translate(
            updating ? 'recipes.update.success.message' : 'recipes.define.success.message',
            {
              dish: dish.name,
              version: response.recipe.versionNumber,
            },
          );
          this.messages.add({ severity: 'success', summary, detail });
          this.selectDish(dish.id);
          this.reloadDishes();
        },
        error: (error: unknown) => {
          this.defineServerMessage.set(this.errors.getMessage(error));
        },
      });
  }

  hasIngredientsError(): boolean {
    return this.defineForm.controls.ingredients.invalid;
  }

  formatCost(value: number): string {
    return new Intl.NumberFormat('es-GT', {
      maximumFractionDigits: 2,
      minimumFractionDigits: 2,
      style: 'currency',
      currency: 'GTQ',
    }).format(value);
  }

  formatQuantity(value: number): string {
    return new Intl.NumberFormat('es-GT', {
      maximumFractionDigits: 4,
    }).format(value);
  }

  formatDate(iso: string): string {
    const date = new Date(iso);
    if (Number.isNaN(date.getTime())) {
      return iso;
    }
    return new Intl.DateTimeFormat('es-GT', {
      dateStyle: 'medium',
      timeStyle: 'short',
    }).format(date);
  }

  private reloadDishes(): void {
    this.recipeService
      .listDishes(null, null, true)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (dishes) => this.dishes.set(dishes),
        error: () => undefined,
      });
  }
}

function dishHasRecipe(dish: DishSummary | null | undefined): boolean {
  if (!dish || !dish.active) {
    return false;
  }
  return dish.unavailabilityReason !== 'SIN_RECETA';
}
