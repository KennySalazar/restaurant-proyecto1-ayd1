import {
  Component,
  computed,
  inject,
  OnInit,
  signal,
} from '@angular/core';

import { finalize } from 'rxjs';
import { MessageService } from 'primeng/api';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';

import {
  RestaurantTable,
  TableStatus,
} from '../../../core/models/table.models';
import { ApiErrorService } from '../../../core/services/api-error.service';
import { TableService } from '../../../core/services/table.service';
import { FormFeedbackComponent } from '../../../shared/components/form-feedback/form-feedback';
import { PageHeadingComponent } from '../../../shared/components/page-heading/page-heading';


@Component({
  selector: 'app-tables-page',
  imports: [
    FormFeedbackComponent,
    PageHeadingComponent,
    TranslocoPipe,
  ],
  templateUrl: './tables.html',
  styleUrl: './tables.scss',
})
export class TablesPageComponent implements OnInit {
  private readonly tableService = inject(TableService);
  private readonly errors = inject(ApiErrorService);
  private readonly messages = inject(MessageService);
  private readonly transloco = inject(TranslocoService);

  readonly tables = signal<RestaurantTable[]>([]);
  readonly selectedTable = signal<RestaurantTable | null>(null);
  readonly isLoading = signal(true);
  readonly isDetailLoading = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly retirementCandidate = signal<RestaurantTable | null>(null);
  readonly isRetiring = signal(false);

  readonly totalTables = computed(() => this.tables().length);

  readonly activeTables = computed(
    () => this.tables().filter((table) => table.activo).length,
  );

  readonly retiredTables = computed(
    () => this.tables().filter((table) => !table.activo).length,
  );

  ngOnInit(): void {
    this.loadTables();
  }

  loadTables(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.tableService
      .getTables()
      .pipe(finalize(() => this.isLoading.set(false)))
      .subscribe({
        next: (tables) => this.tables.set(tables),
        error: (error: unknown) => {
          this.errorMessage.set(this.errors.getMessage(error));
        },
      });
  }

  viewTable(id: number): void {
    this.isDetailLoading.set(true);

    this.tableService
      .getTable(id)
      .pipe(finalize(() => this.isDetailLoading.set(false)))
      .subscribe({
        next: (table) => this.selectedTable.set(table),
      });
  }

  closeDetails(): void {
    this.selectedTable.set(null);
  }

  statusKey(status: TableStatus): string {
    return `tables.states.${status}`;
  }

  statusClass(status: TableStatus): string {
    return `status-badge--${status.toLowerCase().replace(/_/g, '-')}`;
  }

  requestRetirement(table: RestaurantTable): void {
  if (!table.activo) {
    this.messages.add({
      severity: 'warn',
      summary: this.transloco.translate('tables.retirement.alreadyRetiredTitle'),
      detail: this.transloco.translate('tables.retirement.alreadyRetiredMessage'),
      life: 5000,
    });
    return;
  }

  this.retirementCandidate.set(table);
}

cancelRetirement(): void {
  if (this.isRetiring()) {
    return;
  }

  this.retirementCandidate.set(null);
}

confirmRetirement(): void {
  const table = this.retirementCandidate();

  if (!table || this.isRetiring()) {
    return;
  }

  this.isRetiring.set(true);

  this.tableService
    .retireTable(table.id)
    .pipe(finalize(() => this.isRetiring.set(false)))
    .subscribe({
      next: (retiredTable) => {
        this.tables.update((tables) =>
          tables.map((current) =>
            current.id === retiredTable.id ? retiredTable : current,
          ),
        );

        if (this.selectedTable()?.id === retiredTable.id) {
          this.selectedTable.set(retiredTable);
        }

        this.retirementCandidate.set(null);

        this.messages.add({
          severity: 'success',
          summary: this.transloco.translate('tables.retirement.successTitle'),
          detail: this.transloco.translate(
            'tables.retirement.successMessage',
            {
              number: retiredTable.numero,
            },
          ),
          life: 5000,
        });
      },
      error: (error: unknown) => {
        this.errors.present(error);
        this.retirementCandidate.set(null);
      },
    });
}
}