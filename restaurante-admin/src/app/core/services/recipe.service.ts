import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

import { environment } from '../../../environments/environment';
import {
  DefineModifierRecipeRequest,
  DefineRecipeRequest,
  DishCostSummary,
  DishProductionCost,
  DishSummary,
  ModifierProductionCost,
  ModifierRecipe,
  ModifierRecipeHistory,
  ModifierRecipeRegistrationResponse,
  ModifierRecipeVersionChangeDetail,
  Recipe,
  RecipeHistory,
  RecipeRegistrationResponse,
  RecipeUpdateResponse,
  RecipeVersionChangeDetail,
  UpdateRecipeRequest,
} from '../models/recipe.models';

@Injectable({
  providedIn: 'root',
})
export class RecipeService {
  private readonly http = inject(HttpClient);
  private readonly dishesUrl = `${environment.apiBaseUrl}/admin/dishes`;
  private readonly modifiersUrl = `${environment.apiBaseUrl}/admin/modifiers`;

  listDishes(
    categoryId?: number | null,
    search?: string | null,
    active?: boolean | null,
  ): Observable<DishSummary[]> {
    let params = new HttpParams();

    if (categoryId != null) {
      params = params.set('categoryId', categoryId);
    }

    if (search && search.trim().length > 0) {
      params = params.set('search', search.trim());
    }

    if (active != null) {
      params = params.set('active', active);
    }

    return this.http.get<DishSummary[]>(this.dishesUrl, { params });
  }

  getDishRecipe(dishId: number): Observable<Recipe | null> {
    return this.http
      .get<Recipe>(`${this.dishesUrl}/${dishId}/recipe`)
      .pipe(catchError(() => of(null)));
  }

  defineDishRecipe(
    dishId: number,
    request: DefineRecipeRequest,
  ): Observable<RecipeRegistrationResponse> {
    return this.http.post<RecipeRegistrationResponse>(
      `${this.dishesUrl}/${dishId}/recipe`,
      request,
    );
  }

  updateDishRecipe(dishId: number, request: UpdateRecipeRequest): Observable<RecipeUpdateResponse> {
    return this.http.put<RecipeUpdateResponse>(`${this.dishesUrl}/${dishId}/recipe`, request);
  }

  getModifierRecipe(modifierId: number): Observable<ModifierRecipe | null> {
    return this.http
      .get<ModifierRecipe>(`${this.modifiersUrl}/${modifierId}/recipe`)
      .pipe(catchError(() => of(null)));
  }

  defineModifierRecipe(
    modifierId: number,
    request: DefineModifierRecipeRequest,
  ): Observable<ModifierRecipeRegistrationResponse> {
    return this.http.post<ModifierRecipeRegistrationResponse>(
      `${this.modifiersUrl}/${modifierId}/recipe`,
      request,
    );
  }

  listDishCosts(): Observable<DishCostSummary[]> {
    return this.http.get<DishCostSummary[]>(`${this.dishesUrl}/costs`);
  }

  getDishProductionCost(dishId: number): Observable<DishProductionCost> {
    return this.http.get<DishProductionCost>(`${this.dishesUrl}/${dishId}/cost`);
  }

  getModifierProductionCost(modifierId: number): Observable<ModifierProductionCost> {
    return this.http.get<ModifierProductionCost>(`${this.modifiersUrl}/${modifierId}/cost`);
  }

  getDishRecipeHistory(dishId: number): Observable<RecipeHistory> {
    return this.http.get<RecipeHistory>(`${this.dishesUrl}/${dishId}/recipe/history`);
  }

  getDishRecipeVersionDetail(
    dishId: number,
    versionNumber: number,
  ): Observable<RecipeVersionChangeDetail> {
    return this.http.get<RecipeVersionChangeDetail>(
      `${this.dishesUrl}/${dishId}/recipe/versions/${versionNumber}`,
    );
  }

  getModifierRecipeHistory(modifierId: number): Observable<ModifierRecipeHistory> {
    return this.http.get<ModifierRecipeHistory>(
      `${this.modifiersUrl}/${modifierId}/recipe/history`,
    );
  }

  getModifierRecipeVersionDetail(
    modifierId: number,
    versionNumber: number,
  ): Observable<ModifierRecipeVersionChangeDetail> {
    return this.http.get<ModifierRecipeVersionChangeDetail>(
      `${this.modifiersUrl}/${modifierId}/recipe/versions/${versionNumber}`,
    );
  }
}
