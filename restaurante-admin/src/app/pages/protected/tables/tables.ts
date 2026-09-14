import {
  Component,
  computed,
  inject,
  OnInit,
  signal,
} from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';
import { finalize } from 'rxjs';

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

  readonly tables = signal<RestaurantTable[]>([]);
  readonly selectedTable = signal<RestaurantTable | null>(null);
  readonly isLoading = signal(true);
  readonly isDetailLoading = signal(false);
  readonly errorMessage = signal<string | null>(null);

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
}