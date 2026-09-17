import { DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';
import { finalize } from 'rxjs';
import {
  RestaurantTable,
  TABLE_STATUSES,
  TableStatus,
  TableZoneGroup,
} from '../../../core/models/table.models';
import { ApiErrorService } from '../../../core/services/api-error.service';
import { TableService } from '../../../core/services/table.service';
import { FormFeedbackComponent } from '../../../shared/components/form-feedback/form-feedback';
import { PageHeadingComponent } from '../../../shared/components/page-heading/page-heading';

const STATUS_ICONS: Record<TableStatus, string> = {
  LIBRE: 'pi-check-circle',
  RESERVADA: 'pi-calendar',
  OCUPADA: 'pi-users',
  CUENTA_SOLICITADA: 'pi-dollar',
};

@Component({
  selector: 'app-tables-page',
  imports: [DatePipe, FormFeedbackComponent, PageHeadingComponent, TranslocoPipe],
  templateUrl: './tables.html',
  styleUrl: './tables.scss',
})
export class TablesPageComponent implements OnInit {
  private readonly tableService = inject(TableService);
  private readonly errors = inject(ApiErrorService);

  readonly tables = signal<RestaurantTable[]>([]);
  readonly loading = signal(false);
  readonly loadError = signal<string | null>(null);
  readonly lastUpdated = signal<Date | null>(null);

  readonly statuses = TABLE_STATUSES;

  readonly activeTables = computed(() => this.tables().filter((table) => table.activo));

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

    this.tableService
      .getTables()
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (tables) => {
          this.tables.set(tables);
          this.lastUpdated.set(new Date());
        },
        error: (error: unknown) => {
          this.loadError.set(this.errors.getMessage(error));
        },
      });
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
}
