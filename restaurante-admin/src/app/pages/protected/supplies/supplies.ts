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
import { debounceTime, distinctUntilChanged, finalize, forkJoin, Subject } from 'rxjs';

import {
  CreateSupplyRequest,
  KardexMovementNature,
  KardexRecord,
  KARDEX_MOVEMENT_NATURES,
  MeasurementUnit,
  Supply,
  SupplyCategory,
  WASTE_REASON_TYPES,
  WasteReasonType,
} from '../../../core/models/supply.models';
import { ApiErrorService } from '../../../core/services/api-error.service';
import { SupplyService } from '../../../core/services/supply.service';
import { FormFeedbackComponent } from '../../../shared/components/form-feedback/form-feedback';
import { PageHeadingComponent } from '../../../shared/components/page-heading/page-heading';

type SupplyTab = 'supplies' | 'kardex';

@Component({
  selector: 'app-supplies-page',
  imports: [FormFeedbackComponent, PageHeadingComponent, ReactiveFormsModule, TranslocoPipe],
  templateUrl: './supplies.html',
  styleUrls: ['./supplies.scss', './supplies-kardex.scss'],
})
export class SuppliesPageComponent implements OnInit {
  private readonly formBuilder = inject(FormBuilder);
  private readonly supplyService = inject(SupplyService);
  private readonly errors = inject(ApiErrorService);
  private readonly messages = inject(MessageService);
  private readonly transloco = inject(TranslocoService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly searchSubject = new Subject<string>();

  readonly supplies = signal<Supply[]>([]);
  readonly categories = signal<SupplyCategory[]>([]);
  readonly measurementUnits = signal<MeasurementUnit[]>([]);
  readonly isLoading = signal(true);
  readonly isFiltering = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly searchTerm = signal('');
  readonly selectedCategory = signal<number | null>(null);

  readonly tabs: { id: SupplyTab; labelKey: string; icon: string }[] = [
    { id: 'supplies', labelKey: 'supplies.tabs.supplies', icon: 'pi-box' },
    { id: 'kardex', labelKey: 'supplies.tabs.kardex', icon: 'pi-history' },
  ];

  readonly kardexNatures = KARDEX_MOVEMENT_NATURES;

  readonly activeTab = signal<SupplyTab>('supplies');
  readonly kardexLoaded = signal(false);
  readonly isKardexLoading = signal(false);
  readonly kardexErrorMessage = signal<string | null>(null);
  readonly kardex = signal<KardexRecord[]>([]);
  readonly kardexSelectedSupplyId = signal<number | null>(null);
  readonly kardexNature = signal<KardexMovementNature | null>(null);

  readonly isRegistrationOpen = signal(false);
  readonly isSaving = signal(false);
  readonly submitted = signal(false);
  readonly serverMessage = signal<string | null>(null);
  readonly editingSupply = signal<Supply | null>(null);

  readonly isEditing = computed(() => this.editingSupply() != null);

  readonly stockLimitsCandidate = signal<Supply | null>(null);
  readonly isSavingLimits = signal(false);
  readonly submittedLimits = signal(false);
  readonly limitsServerMessage = signal<string | null>(null);

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

  readonly kardexSupplies = computed(() => {
    const ids = new Set(this.kardex().map((record) => record.supplyId));
    return this.supplies().filter((supply) => ids.has(supply.id));
  });

  readonly kardexFiltered = computed(() => {
    const records = this.kardex();
    const supplyId = this.kardexSelectedSupplyId();
    const nature = this.kardexNature();

    return records
      .filter(
        (record) =>
          (supplyId == null || record.supplyId === supplyId) &&
          (nature == null || record.movementNature === nature),
      )
      .sort((a, b) => b.createdAt.localeCompare(a.createdAt));
  });

  readonly kardexSummary = computed(() => {
    const records = this.kardexFiltered();
    return {
      total: records.length,
      entries: records.filter((record) => record.movementNature === 'ENTRADA').length,
      sales: records.filter((record) => record.type === 'SALIDA_VENTA').length,
      wastes: records.filter((record) => record.type === 'SALIDA_MERMA').length,
      adjustments: records.filter((record) => record.isAdjustment).length,
    };
  });

  readonly hasKardexFilters = computed(
    () => this.kardexSelectedSupplyId() != null || this.kardexNature() != null,
  );

  readonly registrationForm = this.formBuilder.group({
    code: ['', [Validators.maxLength(30)]],
    name: ['', [Validators.required, Validators.maxLength(120)]],
    description: ['', [Validators.maxLength(255)]],
    categoryId: [0 as number, [Validators.required, Validators.min(1)]],
    unitId: [0 as number, [Validators.required, Validators.min(1)]],
    unitCost: [null as number | null, [Validators.required, Validators.min(0)]],
    minimumStock: [null as number | null, [Validators.min(0)]],
    maximumStock: [null as number | null, [Validators.min(0)]],
  });

  readonly stockLimitsForm = this.formBuilder.group(
    {
      minimumStock: [null as number | null, [Validators.required, Validators.min(0)]],
      maximumStock: [null as number | null, [Validators.min(0)]],
    },
    { validators: [this.stockLimitsValidator] },
  );

  readonly entryCandidate = signal<Supply | null>(null);
  readonly isEntryOpen = signal(false);
  readonly isSavingEntry = signal(false);
  readonly submittedEntry = signal(false);
  readonly entryServerMessage = signal<string | null>(null);
  readonly entryDetailsOpen = signal(false);

  private readonly pastOrPresentDate = (control: AbstractControl): ValidationErrors | null => {
    const value: string | null = control.value;

    if (!value) {
      return null;
    }

    return value > this.todayIso() ? { futureDate: true } : null;
  };

  private readonly entryDateConsistency = (control: AbstractControl): ValidationErrors | null => {
    const date: string | null = control.get('date')?.value;
    const expirationDate: string | null = control.get('expirationDate')?.value;

    if (date && expirationDate && expirationDate < date) {
      return { expirationBeforeDate: true };
    }

    return null;
  };

  private readonly notInteger = (control: AbstractControl): ValidationErrors | null => {
    const value: number | null = control.value;
    const numeric = Number(value);

    if (value == null || Number.isNaN(numeric)) {
      return null;
    }

    return Number.isInteger(numeric) ? null : { notInteger: true };
  };

  readonly entryForm = this.formBuilder.group(
    {
      supplyId: [0 as number, [Validators.required, Validators.min(1)]],
      quantity: [null as number | null, [Validators.required, Validators.min(0.0001)]],
      date: [this.todayIso(), [Validators.required, this.pastOrPresentDate]],
      unitCost: [null as number | null, [Validators.required, Validators.min(0)]],
      supplierName: ['', [Validators.maxLength(150)]],
      purchaseReference: ['', [Validators.maxLength(100)]],
      batchNumber: ['', [Validators.maxLength(80)]],
      expirationDate: [''],
      notes: ['', [Validators.maxLength(500)]],
    },
    { validators: [this.entryDateConsistency] },
  );

  readonly wasteCandidate = signal<Supply | null>(null);
  readonly isWasteOpen = signal(false);
  readonly isSavingWaste = signal(false);
  readonly submittedWaste = signal(false);
  readonly wasteServerMessage = signal<string | null>(null);
  readonly wasteDetailsOpen = signal(false);

  readonly wasteForm = this.formBuilder.group({
    supplyId: [0 as number, [Validators.required, Validators.min(1)]],
    quantity: [null as number | null, [Validators.required, Validators.min(0.0001)]],
    reasonType: ['' as WasteReasonType | '', [Validators.required]],
    reason: ['', [Validators.maxLength(500)]],
    batchNumber: ['', [Validators.maxLength(80)]],
    notes: ['', [Validators.maxLength(255)]],
    wasteDate: [this.todayIso()],
  });

  ngOnInit(): void {
    this.searchSubject
      .pipe(debounceTime(300), distinctUntilChanged(), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.loadSupplies());

    this.registrationForm.controls.unitId.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((unitId) => this.applyStockUnitValidators(unitId));

    forkJoin({
      supplies: this.supplyService.listSupplies(),
      categories: this.supplyService.listCategories(),
      measurementUnits: this.supplyService.listMeasurementUnits(),
    })
      .pipe(finalize(() => this.isLoading.set(false)))
      .subscribe({
        next: ({ supplies, categories, measurementUnits }) => {
          this.supplies.set(supplies);
          this.categories.set(categories);
          this.measurementUnits.set(measurementUnits);
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

  openRegistration(): void {
    this.submitted.set(false);
    this.serverMessage.set(null);
    this.editingSupply.set(null);
    this.registrationForm.reset({
      code: '',
      name: '',
      description: '',
      categoryId: 0,
      unitId: 0,
      unitCost: null,
      minimumStock: null,
      maximumStock: null,
    });
    this.isRegistrationOpen.set(true);
  }

  openEdit(supply: Supply): void {
    this.submitted.set(false);
    this.serverMessage.set(null);
    this.editingSupply.set(supply);

    this.registrationForm.setValue({
      code: supply.code ?? '',
      name: supply.name,
      description: supply.description ?? '',
      categoryId: supply.categoryId,
      unitId: supply.unitId,
      unitCost: supply.unitCost,
      minimumStock: supply.minimumStock,
      maximumStock: supply.maximumStock,
    });

    this.isRegistrationOpen.set(true);
  }

  closeRegistration(): void {
    if (this.isSaving()) {
      return;
    }

    this.isRegistrationOpen.set(false);
  }

  submitSupply(): void {
    this.submitted.set(true);
    this.serverMessage.set(null);

    if (this.registrationForm.invalid) {
      this.registrationForm.markAllAsTouched();
      return;
    }

    const editing = this.editingSupply();
    this.isSaving.set(true);

    const request = editing
      ? this.supplyService.updateSupply(editing.id, this.buildPayload())
      : this.supplyService.registerSupply(this.buildPayload());

    request.pipe(finalize(() => this.isSaving.set(false))).subscribe({
      next: (response) => {
        const successKey = editing ? 'supplies.update.success' : 'supplies.registration.success';

        this.messages.add({
          severity: 'success',
          summary: this.transloco.translate(`${successKey}.title`),
          detail: this.transloco.translate(`${successKey}.message`, {
            name: response.supply.name,
          }),
          life: 6000,
        });

        if (editing) {
          this.supplies.update((list) =>
            list.map((current) => (current.id === response.supply.id ? response.supply : current)),
          );
        } else {
          this.loadSupplies();
        }

        this.isRegistrationOpen.set(false);
        this.submitted.set(false);
        this.serverMessage.set(null);
        this.editingSupply.set(null);
        this.registrationForm.reset();
      },
      error: (error: unknown) => {
        this.serverMessage.set(this.errors.getMessage(error));
      },
    });
  }

  private buildPayload(): CreateSupplyRequest {
    const raw = this.registrationForm.getRawValue();

    return {
      code: raw.code?.trim() || null,
      name: raw.name!.trim(),
      description: raw.description?.trim() || null,
      categoryId: raw.categoryId!,
      unitId: raw.unitId!,
      unitCost: raw.unitCost,
      minimumStock: raw.minimumStock ?? null,
      maximumStock: raw.maximumStock ?? null,
    };
  }

  invalid(controlName: keyof typeof this.registrationForm.controls): boolean {
    const control = this.registrationForm.controls[controlName];

    return control.invalid && (control.touched || this.submitted());
  }

  openStockLimits(supply: Supply): void {
    this.submittedLimits.set(false);
    this.limitsServerMessage.set(null);
    this.stockLimitsCandidate.set(supply);

    this.stockLimitsForm.reset({
      minimumStock: supply.minimumStock,
      maximumStock: supply.maximumStock,
    });

    const integerValidator = this.isConteoUnit(supply.unitId) ? [this.notInteger] : [];
    this.stockLimitsForm.controls.minimumStock.setValidators([
      Validators.required,
      Validators.min(0),
      ...integerValidator,
    ]);
    this.stockLimitsForm.controls.maximumStock.setValidators([
      Validators.min(0),
      ...integerValidator,
    ]);
    this.stockLimitsForm.controls.minimumStock.updateValueAndValidity();
    this.stockLimitsForm.controls.maximumStock.updateValueAndValidity();
  }

  closeStockLimits(): void {
    if (this.isSavingLimits()) {
      return;
    }

    this.stockLimitsCandidate.set(null);
  }

  submitStockLimits(): void {
    this.submittedLimits.set(true);
    this.limitsServerMessage.set(null);

    if (this.stockLimitsForm.invalid) {
      this.stockLimitsForm.markAllAsTouched();
      return;
    }

    const supply = this.stockLimitsCandidate();

    if (!supply) {
      return;
    }

    const raw = this.stockLimitsForm.getRawValue();
    this.isSavingLimits.set(true);

    this.supplyService
      .configureStockLimits(supply.id, {
        minimumStock: raw.minimumStock!,
        maximumStock: raw.maximumStock ?? null,
      })
      .pipe(finalize(() => this.isSavingLimits.set(false)))
      .subscribe({
        next: (response) => {
          this.messages.add({
            severity: 'success',
            summary: this.transloco.translate('supplies.stockLimits.success.title'),
            detail: this.transloco.translate('supplies.stockLimits.success.message', {
              name: response.supply.name,
            }),
            life: 6000,
          });

          this.supplies.update((list) =>
            list.map((current) => (current.id === response.supply.id ? response.supply : current)),
          );

          this.stockLimitsCandidate.set(null);
          this.submittedLimits.set(false);
          this.limitsServerMessage.set(null);
          this.stockLimitsForm.reset();
        },
        error: (error: unknown) => {
          this.limitsServerMessage.set(this.errors.getMessage(error));
        },
      });
  }

  invalidLimits(controlName: 'minimumStock' | 'maximumStock'): boolean {
    const control = this.stockLimitsForm.controls[controlName];

    return control.invalid && (control.touched || this.submittedLimits());
  }

  maxBelowMin(): boolean {
    return this.stockLimitsForm.hasError('maxBelowMin') && this.submittedLimits();
  }

  private stockLimitsValidator(control: AbstractControl): ValidationErrors | null {
    const minimum = control.get('minimumStock')?.value;
    const maximum = control.get('maximumStock')?.value;

    if (maximum != null && minimum != null && Number(maximum) < Number(minimum)) {
      return { maxBelowMin: true };
    }

    return null;
  }

  openEntry(supply: Supply): void {
    this.submittedEntry.set(false);
    this.entryServerMessage.set(null);
    this.entryDetailsOpen.set(false);
    this.entryCandidate.set(supply);
    this.entryForm.controls.supplyId.setValue(supply.id);

    this.entryForm.patchValue({
      quantity: null,
      date: this.todayIso(),
      unitCost: supply.unitCost,
      supplierName: '',
      purchaseReference: '',
      batchNumber: '',
      expirationDate: '',
      notes: '',
    });

    const integerValidator = this.isConteoUnit(supply.unitId) ? [this.notInteger] : [];
    this.entryForm.controls.quantity.setValidators([
      Validators.required,
      Validators.min(0.0001),
      ...integerValidator,
    ]);
    this.entryForm.controls.quantity.updateValueAndValidity();

    this.isEntryOpen.set(true);
  }

  closeEntry(): void {
    if (this.isSavingEntry()) {
      return;
    }

    this.entryDetailsOpen.set(false);
    this.isEntryOpen.set(false);
  }

  toggleEntryDetails(): void {
    this.entryDetailsOpen.update((open) => !open);
  }

  submitEntry(): void {
    this.submittedEntry.set(true);
    this.entryServerMessage.set(null);

    if (this.entryForm.invalid) {
      this.entryForm.markAllAsTouched();
      return;
    }

    const raw = this.entryForm.getRawValue();
    this.isSavingEntry.set(true);

    this.supplyService
      .registerSupplyEntry({
        supplyId: raw.supplyId!,
        quantity: raw.quantity!,
        date: raw.date!,
        unitCost: raw.unitCost!,
        supplierName: raw.supplierName?.trim() || null,
        purchaseReference: raw.purchaseReference?.trim() || null,
        batchNumber: raw.batchNumber?.trim() || null,
        expirationDate: raw.expirationDate || null,
        notes: raw.notes?.trim() || null,
      })
      .pipe(finalize(() => this.isSavingEntry.set(false)))
      .subscribe({
        next: (response) => {
          this.messages.add({
            severity: 'success',
            summary: this.transloco.translate('supplyEntries.success.title'),
            detail: this.transloco.translate('supplyEntries.success.message', {
              quantity: this.formatStock(response.entry.quantity),
              unit: response.entry.unitAbbreviation,
              name: response.entry.supplyName,
              stock: this.formatStock(response.entry.currentStock),
            }),
            life: 7000,
          });

          this.loadSupplies();
          this.isEntryOpen.set(false);
          this.submittedEntry.set(false);
          this.entryServerMessage.set(null);
          this.entryDetailsOpen.set(false);
          this.entryCandidate.set(null);
          this.entryForm.reset();
        },
        error: (error: unknown) => {
          this.entryServerMessage.set(this.errors.getMessage(error));
        },
      });
  }

  invalidEntry(controlName: keyof typeof this.entryForm.controls): boolean {
    const control = this.entryForm.controls[controlName];

    return control.invalid && (control.touched || this.submittedEntry());
  }

  openWaste(supply: Supply): void {
    this.submittedWaste.set(false);
    this.wasteServerMessage.set(null);
    this.wasteDetailsOpen.set(false);
    this.wasteCandidate.set(supply);
    this.wasteForm.controls.supplyId.setValue(supply.id);

    this.wasteForm.patchValue({
      quantity: null,
      reasonType: '',
      reason: '',
      batchNumber: '',
      notes: '',
      wasteDate: this.todayIso(),
    });

    const integerValidator = this.isConteoUnit(supply.unitId) ? [this.notInteger] : [];
    this.wasteForm.controls.quantity.setValidators([
      Validators.required,
      Validators.min(0.0001),
      ...integerValidator,
    ]);
    this.wasteForm.controls.quantity.updateValueAndValidity();

    this.isWasteOpen.set(true);
  }

  closeWaste(): void {
    if (this.isSavingWaste()) {
      return;
    }

    this.wasteDetailsOpen.set(false);
    this.isWasteOpen.set(false);
  }

  toggleWasteDetails(): void {
    this.wasteDetailsOpen.update((open) => !open);
  }

  submitWaste(): void {
    this.submittedWaste.set(true);
    this.wasteServerMessage.set(null);

    if (this.wasteForm.invalid) {
      this.wasteForm.markAllAsTouched();
      return;
    }

    const supply = this.wasteCandidate();

    if (!supply) {
      return;
    }

    const raw = this.wasteForm.getRawValue();
    this.isSavingWaste.set(true);

    this.supplyService
      .registerSupplyWaste(supply.id, {
        quantity: raw.quantity!,
        reasonType: raw.reasonType ? (raw.reasonType as WasteReasonType) : null,
        reason: raw.reason?.trim() || null,
        batchNumber: raw.batchNumber?.trim() || null,
        notes: raw.notes?.trim() || null,
        date: raw.wasteDate || null,
      })
      .pipe(finalize(() => this.isSavingWaste.set(false)))
      .subscribe({
        next: (response) => {
          const waste = response.waste;
          const alertGenerated =
            supply.minimumStock != null && waste.resultingStock <= supply.minimumStock;

          this.messages.add({
            severity: 'success',
            summary: this.transloco.translate('supplyWastes.success.title'),
            detail:
              this.transloco.translate('supplyWastes.success.message', {
                quantity: this.formatStock(waste.quantity),
                unit: waste.measurementUnitAbbreviation,
                name: waste.supplyName,
                stock: this.formatStock(waste.resultingStock),
              }) +
              (alertGenerated
                ? ' ' + this.transloco.translate('supplyWastes.success.alertNote')
                : ''),
            life: 7000,
          });

          this.loadSupplies();
          this.isWasteOpen.set(false);
          this.submittedWaste.set(false);
          this.wasteServerMessage.set(null);
          this.wasteDetailsOpen.set(false);
          this.wasteCandidate.set(null);
          this.wasteForm.reset();
        },
        error: (error: unknown) => {
          this.wasteServerMessage.set(this.errors.getMessage(error));
        },
      });
  }

  invalidWaste(
    controlName: 'quantity' | 'reasonType' | 'reason' | 'batchNumber' | 'notes',
  ): boolean {
    const control = this.wasteForm.controls[controlName];

    return control.invalid && (control.touched || this.submittedWaste());
  }

  reasonTypeKey(reasonType: WasteReasonType): string {
    return `supplyWastes.fields.reasonTypes.${reasonType}`;
  }

  reasonTypes(): ReadonlyArray<WasteReasonType> {
    return WASTE_REASON_TYPES;
  }

  private isConteoUnit(unitId: number | null | undefined): boolean {
    if (!unitId) {
      return false;
    }

    return this.measurementUnits().some(
      (unit) => unit.id === unitId && unit.dimension === 'CONTEO',
    );
  }

  private applyStockUnitValidators(unitId: number | null): void {
    const integerValidator = this.isConteoUnit(unitId) ? [this.notInteger] : [];

    this.registrationForm.controls.minimumStock.setValidators([
      Validators.min(0),
      ...integerValidator,
    ]);
    this.registrationForm.controls.maximumStock.setValidators([
      Validators.min(0),
      ...integerValidator,
    ]);
    this.registrationForm.controls.minimumStock.updateValueAndValidity();
    this.registrationForm.controls.maximumStock.updateValueAndValidity();
  }

  registrationMinMaxStep(): string {
    return this.isConteoUnit(this.registrationForm.controls.unitId.value) ? '1' : '0.01';
  }

  stockLimitsStep(): string {
    return this.isConteoUnit(this.stockLimitsCandidate()?.unitId) ? '1' : '0.01';
  }

  entryQuantityStep(): string {
    return this.isConteoUnit(this.entryCandidate()?.unitId) ? '1' : '0.01';
  }

  wasteQuantityStep(): string {
    return this.isConteoUnit(this.wasteCandidate()?.unitId) ? '1' : '0.01';
  }

  private todayIso(): string {
    const now = new Date();
    const year = now.getFullYear();
    const month = String(now.getMonth() + 1).padStart(2, '0');
    const day = String(now.getDate()).padStart(2, '0');

    return `${year}-${month}-${day}`;
  }

  setTab(tab: SupplyTab): void {
    this.activeTab.set(tab);

    if (tab === 'kardex' && !this.kardexLoaded()) {
      this.loadKardex();
    }
  }

  loadKardex(): void {
    this.isKardexLoading.set(true);
    this.kardexErrorMessage.set(null);

    this.supplyService
      .getKardex()
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isKardexLoading.set(false)),
      )
      .subscribe({
        next: (records) => {
          this.kardex.set(records);
          this.kardexLoaded.set(true);
        },
        error: (error: unknown) => this.kardexErrorMessage.set(this.errors.getMessage(error)),
      });
  }

  selectKardexSupply(supplyId: number | null): void {
    this.kardexSelectedSupplyId.set(supplyId);
  }

  parseKardexSupplyId(raw: string): number | null {
    if (!raw) {
      return null;
    }

    const value = Number(raw);
    return Number.isNaN(value) ? null : value;
  }

  setKardexNature(nature: KardexMovementNature | null): void {
    this.kardexNature.set(nature);
  }

  clearKardexFilters(): void {
    this.kardexSelectedSupplyId.set(null);
    this.kardexNature.set(null);
  }

  kardexTypeChipClass(type: string): string {
    return `kardex-type-chip--${type.toLowerCase()}`;
  }

  kardexReference(
    record: KardexRecord,
  ): { kind: 'comanda' | 'entry' | 'waste' | 'reason'; text: string } | null {
    if (record.comandaId != null) {
      return { kind: 'comanda', text: String(record.comandaId) };
    }

    if (record.entryDocumentNumber) {
      return { kind: 'entry', text: record.entryDocumentNumber };
    }

    if (record.wasteDocumentNumber) {
      return { kind: 'waste', text: record.wasteDocumentNumber };
    }

    if (record.reason) {
      return { kind: 'reason', text: record.reason };
    }

    return null;
  }

  formatKardexQuantity(value: number): string {
    return new Intl.NumberFormat('es-GT', {
      maximumFractionDigits: 4,
    }).format(value);
  }

  formatDateTime(iso: string): string {
    const date = new Date(iso);

    if (Number.isNaN(date.getTime())) {
      return iso;
    }

    return new Intl.DateTimeFormat('es-GT', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    }).format(date);
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

  estimatedTotalCost(): string | null {
    const raw = this.entryForm.getRawValue();
    const quantity = raw.quantity;
    const unitCost = raw.unitCost;

    if (!quantity || unitCost == null || unitCost < 0) {
      return null;
    }

    return this.formatCost(quantity * unitCost);
  }

  estimatedWasteLoss(): string | null {
    const quantity = this.wasteForm.getRawValue().quantity;
    const supply = this.wasteCandidate();

    if (!quantity || quantity <= 0 || !supply) {
      return null;
    }

    return this.formatCost(quantity * supply.unitCost);
  }

  wasteStockPreview(): { remaining: number; overStock: boolean } | null {
    const quantity = this.wasteForm.getRawValue().quantity;
    const supply = this.wasteCandidate();

    if (!quantity || quantity <= 0 || !supply) {
      return null;
    }

    const remaining = supply.currentStock - quantity;

    return {
      remaining: Math.max(0, remaining),
      overStock: remaining < 0,
    };
  }
}
