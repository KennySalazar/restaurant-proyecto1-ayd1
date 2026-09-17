import { DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';
import { MessageService } from 'primeng/api';
import { Dialog } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { finalize, forkJoin } from 'rxjs';
import { Account, ActiveFusionResponse } from '../../../core/models/account.models';
import { Reservation } from '../../../core/models/reservation.models';
import {
  RestaurantTable,
  TABLE_STATUSES,
  TableStatus,
  TableZoneGroup,
} from '../../../core/models/table.models';
import { WaitlistQueueEntry, WaitlistSuggestion } from '../../../core/models/waitlist.models';
import { AccountService } from '../../../core/services/account.service';
import { ApiErrorService } from '../../../core/services/api-error.service';
import { ReservationService } from '../../../core/services/reservation.service';
import { TableService } from '../../../core/services/table.service';
import { WaitlistService } from '../../../core/services/waitlist.service';
import { FormFeedbackComponent } from '../../../shared/components/form-feedback/form-feedback';
import { PageHeadingComponent } from '../../../shared/components/page-heading/page-heading';

const STATUS_ICONS: Record<TableStatus, string> = {
  LIBRE: 'pi-check-circle',
  RESERVADA: 'pi-calendar',
  OCUPADA: 'pi-users',
  CUENTA_SOLICITADA: 'pi-dollar',
};

type DialogKind = 'reservation' | 'waitlist' | 'open-account' | 'transfer' | 'merge';

interface OperationSummary {
  titleKey: string;
  numeroMesa: string;
  numeroCuenta: string;
  cantidadPersonas: number;
  cliente: string | null;
}

interface TableFusionInfo {
  otherTableId: number;
  otherTableNumber: string;
  isOrigin: boolean;
  totalPersonas: number;
}

@Component({
  selector: 'app-tables-page',
  imports: [
    DatePipe,
    Dialog,
    FormFeedbackComponent,
    InputTextModule,
    PageHeadingComponent,
    ReactiveFormsModule,
    TranslocoPipe,
  ],
  templateUrl: './tables.html',
  styleUrl: './tables.scss',
})
export class TablesPageComponent implements OnInit {
  private readonly tableService = inject(TableService);
  private readonly accountService = inject(AccountService);
  private readonly reservationService = inject(ReservationService);
  private readonly waitlistService = inject(WaitlistService);
  private readonly errors = inject(ApiErrorService);
  private readonly messages = inject(MessageService);
  private readonly transloco = inject(TranslocoService);
  private readonly formBuilder = inject(FormBuilder);

  readonly tables = signal<RestaurantTable[]>([]);
  readonly loading = signal(false);
  readonly loadError = signal<string | null>(null);
  readonly lastUpdated = signal<Date | null>(null);

  readonly statuses = TABLE_STATUSES;

  readonly dialog = signal<DialogKind | null>(null);
  readonly selectedTable = signal<RestaurantTable | null>(null);
  readonly submitting = signal(false);
  readonly submitted = signal(false);
  readonly dialogError = signal<string | null>(null);

  readonly operationSummary = signal<OperationSummary | null>(null);

  readonly tableReservations = signal<Reservation[]>([]);
  readonly loadingReservations = signal(false);

  readonly waitlistSuggestion = signal<WaitlistSuggestion | null>(null);
  readonly waitlistQueue = signal<WaitlistQueueEntry[]>([]);
  readonly loadingWaitlist = signal(false);

  readonly accountsByTable = signal<Map<number, Account>>(new Map());
  readonly activeFusions = signal<ActiveFusionResponse[]>([]);
  readonly reservationsByTable = signal<Map<number, Reservation>>(new Map());

  readonly openAccountForm = this.formBuilder.group({
    cantidadPersonas: this.formBuilder.control<number | null>(1, [Validators.min(1)]),
    observaciones: this.formBuilder.nonNullable.control('', [Validators.maxLength(500)]),
  });

  readonly reservationForm = this.formBuilder.group({
    reservaId: this.formBuilder.control<number | null>(null, [Validators.required]),
    cantidadPersonas: this.formBuilder.control<number | null>(null, [Validators.min(1)]),
    notas: this.formBuilder.nonNullable.control('', [Validators.maxLength(500)]),
  });

  readonly waitlistForm = this.formBuilder.group({
    listaEsperaId: this.formBuilder.control<number | null>(null, [Validators.required]),
    cantidadPersonas: this.formBuilder.control<number | null>(null, [Validators.min(1)]),
    notas: this.formBuilder.nonNullable.control('', [Validators.maxLength(500)]),
  });

  readonly transferForm = this.formBuilder.group({
    mesaDestinoId: this.formBuilder.control<number | null>(null, [Validators.required]),
    motivo: this.formBuilder.nonNullable.control('', [Validators.maxLength(500)]),
  });

  readonly mergeForm = this.formBuilder.group({
    mesaDestinoId: this.formBuilder.control<number | null>(null, [Validators.required]),
    motivo: this.formBuilder.nonNullable.control('', [Validators.maxLength(500)]),
  });

  readonly activeTables = computed(() => this.tables().filter((table) => table.activo));

  readonly fusedTableIds = computed(() => {
    const ids = new Set<number>();

    for (const fusion of this.activeFusions()) {
      ids.add(fusion.mesaOrigenId);
      ids.add(fusion.mesaDestinoId);
    }

    return ids;
  });

  readonly counts = computed(() => {
    const base: Record<TableStatus, number> = {
      LIBRE: 0,
      RESERVADA: 0,
      OCUPADA: 0,
      CUENTA_SOLICITADA: 0,
    };

    for (const table of this.activeTables()) {
      base[table.estado] += 1;
    }

    return base;
  });

  readonly zoneGroups = computed<TableZoneGroup[]>(() => {
    const groups = new Map<string, TableZoneGroup>();

    for (const table of this.activeTables()) {
      const key = table.zona ? `zona-${table.zona.id}` : 'sin-zona';
      const nombre = table.zona?.nombre ?? '';

      let group = groups.get(key);
      if (!group) {
        group = { key, nombre, tables: [] };
        groups.set(key, group);
      }

      group.tables.push(table);
    }

    return [...groups.values()]
      .map((group) => ({
        ...group,
        tables: [...group.tables].sort((first, second) =>
          first.numero.localeCompare(second.numero, 'es', { numeric: true }),
        ),
      }))
      .sort((first, second) => first.nombre.localeCompare(second.nombre, 'es', { numeric: true }));
  });

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.loadError.set(null);

    forkJoin({
      tables: this.tableService.getTables(),
      accounts: this.accountService.getActiveAccounts(),
      fusions: this.accountService.getActiveFusions(),
      reservations: this.reservationService.getUpcomingReservations(),
    })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: ({ tables, accounts, fusions, reservations }) => {
          this.tables.set(tables);
          this.accountsByTable.set(new Map(accounts.map((account) => [account.mesaId, account])));
          this.activeFusions.set(fusions);
          this.reservationsByTable.set(this.buildReservationsByTable(reservations));
          this.lastUpdated.set(new Date());
        },
        error: (error: unknown) => {
          this.loadError.set(this.errors.getMessage(error));
        },
      });
  }

  getAccountForTable(tableId: number): Account | null {
    return this.accountsByTable().get(tableId) ?? null;
  }

  getFusionForTable(tableId: number): TableFusionInfo | null {
    const fusion = this.activeFusions().find(
      (candidate) => candidate.mesaOrigenId === tableId || candidate.mesaDestinoId === tableId,
    );

    if (!fusion) {
      return null;
    }

    const isOrigin = fusion.mesaOrigenId === tableId;

    return {
      otherTableId: isOrigin ? fusion.mesaDestinoId : fusion.mesaOrigenId,
      otherTableNumber: isOrigin ? fusion.numeroMesaDestino : fusion.numeroMesaOrigen,
      isOrigin,
      totalPersonas: fusion.totalPersonas,
    };
  }

  getReservationForTable(tableId: number): Reservation | null {
    return this.reservationsByTable().get(tableId) ?? null;
  }

  canMerge(table: RestaurantTable): boolean {
    const account = this.getAccountForTable(table.id);
    return !!account && account.estadoCuenta === 'ABIERTA' && !this.fusedTableIds().has(table.id);
  }

  statusCardClass(status: TableStatus): string {
    return `table-card table-card--${status.toLowerCase()}`;
  }

  statusLabelKey(status: TableStatus): string {
    return `tables.status.${status}`;
  }

  statusIcon(status: TableStatus): string {
    return STATUS_ICONS[status];
  }

  openAccountDialog(table: RestaurantTable): void {
    this.resetDialog();
    this.selectedTable.set(table);
    this.openAccountForm.reset({ cantidadPersonas: 1, observaciones: '' });
    this.dialog.set('open-account');
  }

  openReservationDialog(table: RestaurantTable): void {
    this.resetDialog();
    this.selectedTable.set(table);
    this.reservationForm.reset({
      reservaId: null,
      cantidadPersonas: null,
      notas: '',
    });
    this.dialog.set('reservation');
    this.loadTableReservations(table);
  }

  openWaitlistDialog(table: RestaurantTable): void {
    this.resetDialog();
    this.selectedTable.set(table);
    this.waitlistForm.reset({
      listaEsperaId: null,
      cantidadPersonas: null,
      notas: '',
    });
    this.dialog.set('waitlist');
    this.loadWaitlistData(table);
  }

  openTransferDialog(table: RestaurantTable): void {
    this.resetDialog();
    this.selectedTable.set(table);
    this.transferForm.reset({ mesaDestinoId: null, motivo: '' });
    this.dialog.set('transfer');
  }

  openMergeDialog(table: RestaurantTable): void {
    this.resetDialog();
    this.selectedTable.set(table);
    this.mergeForm.reset({ mesaDestinoId: null, motivo: '' });
    this.dialog.set('merge');
  }

  transferDestinationOptions(): RestaurantTable[] {
    const table = this.selectedTable();
    const account = table ? this.getAccountForTable(table.id) : null;

    if (!table || !account) {
      return [];
    }

    return this.activeTables().filter(
      (candidate) =>
        candidate.estado === 'LIBRE' && candidate.capacidad >= account.cantidadPersonas,
    );
  }

  mergeDestinationOptions(): RestaurantTable[] {
    const table = this.selectedTable();

    if (!table) {
      return [];
    }

    const fusedIds = this.fusedTableIds();

    return this.activeTables().filter((candidate) => {
      if (
        candidate.id === table.id ||
        candidate.estado !== 'OCUPADA' ||
        fusedIds.has(candidate.id)
      ) {
        return false;
      }

      const candidateAccount = this.getAccountForTable(candidate.id);
      return !!candidateAccount && candidateAccount.estadoCuenta === 'ABIERTA';
    });
  }

  closeDialog(): void {
    this.dialog.set(null);
    this.selectedTable.set(null);
    this.dialogError.set(null);
    this.submitted.set(false);
    this.submitting.set(false);
    this.tableReservations.set([]);
    this.waitlistSuggestion.set(null);
    this.waitlistQueue.set([]);
  }

  dismissSummary(): void {
    this.operationSummary.set(null);
  }

  onReservationChange(): void {
    const reservationId = this.reservationForm.controls.reservaId.value;
    const reservation = this.tableReservations().find((item) => item.id === reservationId);

    if (reservation) {
      this.reservationForm.controls.cantidadPersonas.setValue(reservation.cantidadPersonas);
    }
  }

  onWaitlistChange(): void {
    const entryId = this.waitlistForm.controls.listaEsperaId.value;
    const entry =
      this.waitlistSuggestion()?.id === entryId
        ? this.waitlistSuggestion()
        : this.waitlistQueue().find((item) => item.id === entryId);

    if (entry) {
      this.waitlistForm.controls.cantidadPersonas.setValue(entry.cantidadPersonas);
    }
  }

  isWaitlistCompatible(entry: WaitlistQueueEntry): boolean {
    const table = this.selectedTable();

    return !table || entry.cantidadPersonas <= table.capacidad;
  }

  compatibleWaitlist(): WaitlistQueueEntry[] {
    return this.waitlistQueue().filter((entry) => this.isWaitlistCompatible(entry));
  }

  submitOpenAccount(): void {
    const table = this.selectedTable();

    if (!table) {
      return;
    }

    this.submitted.set(true);
    this.dialogError.set(null);

    if (this.openAccountForm.invalid) {
      this.openAccountForm.markAllAsTouched();
      return;
    }

    const value = this.openAccountForm.getRawValue();
    this.submitting.set(true);

    this.accountService
      .openAccount(table.id, {
        cantidadPersonas: value.cantidadPersonas ?? null,
        observaciones: value.observaciones.trim() || null,
      })
      .pipe(finalize(() => this.submitting.set(false)))
      .subscribe({
        next: (account) => {
          this.handleSuccess('tables.success.accountTitle', {
            numeroMesa: account.numeroMesa,
            numeroCuenta: account.numeroCuenta,
            cantidadPersonas: account.cantidadPersonas,
            cliente: null,
          });
        },
        error: (error: unknown) => {
          this.dialogError.set(this.errors.getMessage(error));
        },
      });
  }

  submitReservation(): void {
    const table = this.selectedTable();

    if (!table) {
      return;
    }

    this.submitted.set(true);
    this.dialogError.set(null);

    if (this.reservationForm.invalid) {
      this.reservationForm.markAllAsTouched();
      return;
    }

    const value = this.reservationForm.getRawValue();
    this.submitting.set(true);

    this.tableService
      .seatReservation(table.id, {
        reservaId: value.reservaId,
        cantidadPersonas: value.cantidadPersonas ?? null,
        notas: value.notas.trim() || null,
      })
      .pipe(finalize(() => this.submitting.set(false)))
      .subscribe({
        next: (result) => {
          this.handleSuccess('tables.success.reservationTitle', {
            numeroMesa: result.numeroMesa,
            numeroCuenta: result.numeroCuenta,
            cantidadPersonas: result.cantidadPersonas,
            cliente: result.nombreCliente,
          });
        },
        error: (error: unknown) => {
          this.dialogError.set(this.errors.getMessage(error));
        },
      });
  }

  submitWaitlist(): void {
    const table = this.selectedTable();

    if (!table) {
      return;
    }

    this.submitted.set(true);
    this.dialogError.set(null);

    if (this.waitlistForm.invalid) {
      this.waitlistForm.markAllAsTouched();
      return;
    }

    const value = this.waitlistForm.getRawValue();
    this.submitting.set(true);

    this.tableService
      .seatWaitlist(table.id, {
        listaEsperaId: value.listaEsperaId,
        cantidadPersonas: value.cantidadPersonas ?? null,
        notas: value.notas.trim() || null,
      })
      .pipe(finalize(() => this.submitting.set(false)))
      .subscribe({
        next: (result) => {
          this.handleSuccess('tables.success.waitlistTitle', {
            numeroMesa: result.numeroMesa,
            numeroCuenta: result.numeroCuenta,
            cantidadPersonas: result.cantidadPersonas,
            cliente: result.nombreCliente,
          });
        },
        error: (error: unknown) => {
          this.dialogError.set(this.errors.getMessage(error));
        },
      });
  }

  submitTransfer(): void {
    const table = this.selectedTable();
    const account = table ? this.getAccountForTable(table.id) : null;

    if (!table || !account) {
      return;
    }

    this.submitted.set(true);
    this.dialogError.set(null);

    if (this.transferForm.invalid) {
      this.transferForm.markAllAsTouched();
      return;
    }

    const value = this.transferForm.getRawValue();
    this.submitting.set(true);

    this.accountService
      .transferAccount(account.id, {
        mesaDestinoId: value.mesaDestinoId!,
        motivo: value.motivo.trim() || null,
      })
      .pipe(finalize(() => this.submitting.set(false)))
      .subscribe({
        next: (result) => {
          this.handleSuccess('tables.success.transferTitle', {
            numeroMesa: result.numeroMesaDestino,
            numeroCuenta: result.numeroCuenta,
            cantidadPersonas: account.cantidadPersonas,
            cliente: null,
          });
        },
        error: (error: unknown) => {
          this.dialogError.set(this.errors.getMessage(error));
        },
      });
  }

  submitMerge(): void {
    const table = this.selectedTable();

    if (!table) {
      return;
    }

    this.submitted.set(true);
    this.dialogError.set(null);

    if (this.mergeForm.invalid) {
      this.mergeForm.markAllAsTouched();
      return;
    }

    const value = this.mergeForm.getRawValue();
    this.submitting.set(true);

    this.accountService
      .mergeAccounts({
        mesaOrigenId: table.id,
        mesaDestinoId: value.mesaDestinoId!,
        motivo: value.motivo.trim() || null,
      })
      .pipe(finalize(() => this.submitting.set(false)))
      .subscribe({
        next: (result) => {
          this.handleSuccess('tables.success.mergeTitle', {
            numeroMesa: result.numeroMesaDestino,
            numeroCuenta: result.numeroCuentaDestino,
            cantidadPersonas: result.totalPersonas,
            cliente: null,
          });
        },
        error: (error: unknown) => {
          this.dialogError.set(this.errors.getMessage(error));
        },
      });
  }

  private buildReservationsByTable(reservations: Reservation[]): Map<number, Reservation> {
    const map = new Map<number, Reservation>();

    for (const reservation of reservations) {
      const tableId = reservation.mesa?.id;

      if (!tableId) {
        continue;
      }

      const existing = map.get(tableId);
      if (!existing || reservation.fechaHoraInicio < existing.fechaHoraInicio) {
        map.set(tableId, reservation);
      }
    }

    return map;
  }

  private loadTableReservations(table: RestaurantTable): void {
    this.loadingReservations.set(true);

    this.reservationService
      .getUpcomingReservations()
      .pipe(finalize(() => this.loadingReservations.set(false)))
      .subscribe({
        next: (reservations) => {
          const forTable = reservations.filter((reservation) => reservation.mesa?.id === table.id);

          this.tableReservations.set(forTable);

          if (forTable.length === 1) {
            this.reservationForm.controls.reservaId.setValue(forTable[0].id);
            this.reservationForm.controls.cantidadPersonas.setValue(forTable[0].cantidadPersonas);
          }
        },
        error: (error: unknown) => {
          this.dialogError.set(this.errors.getMessage(error));
        },
      });
  }

  private loadWaitlistData(table: RestaurantTable): void {
    this.loadingWaitlist.set(true);

    forkJoin({
      suggestion: this.waitlistService.getSuggestionForTable(table.id),
      queue: this.waitlistService.getWaitlist(),
    })
      .pipe(finalize(() => this.loadingWaitlist.set(false)))
      .subscribe({
        next: ({ suggestion, queue }) => {
          this.waitlistSuggestion.set(suggestion);
          this.waitlistQueue.set(queue);

          const defaultEntry =
            suggestion ?? queue.find((entry) => entry.cantidadPersonas <= table.capacidad);

          if (defaultEntry) {
            this.waitlistForm.controls.listaEsperaId.setValue(defaultEntry.id);
            this.waitlistForm.controls.cantidadPersonas.setValue(defaultEntry.cantidadPersonas);
          }
        },
        error: (error: unknown) => {
          this.dialogError.set(this.errors.getMessage(error));
        },
      });
  }

  private handleSuccess(titleKey: string, summary: Omit<OperationSummary, 'titleKey'>): void {
    this.operationSummary.set({ titleKey, ...summary });

    this.messages.add({
      severity: 'success',
      summary: this.transloco.translate(titleKey),
      detail: `${summary.numeroMesa} · ${summary.numeroCuenta}`,
      life: 5000,
    });

    this.closeDialog();
    this.load();
  }

  private resetDialog(): void {
    this.dialogError.set(null);
    this.submitted.set(false);
    this.submitting.set(false);
    this.tableReservations.set([]);
    this.waitlistSuggestion.set(null);
    this.waitlistQueue.set([]);
  }
}
