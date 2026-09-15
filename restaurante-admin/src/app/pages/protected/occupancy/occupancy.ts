import {
  Component,
  OnDestroy,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';
import { finalize } from 'rxjs';

import { OccupancyPanelTable } from '../../../core/models/occupancy-panel.models';
import { OccupancyPanelService } from '../../../core/services/occupancy-panel.service';

@Component({
  selector: 'app-occupancy-page',
  imports: [TranslocoPipe],
  templateUrl: './occupancy.html',
  styleUrl: './occupancy.scss',
})
export class OccupancyPageComponent implements OnInit, OnDestroy {
  private readonly occupancyService = inject(OccupancyPanelService);

  private refreshInterval: ReturnType<typeof setInterval> | null = null;

  readonly tables = signal<OccupancyPanelTable[]>([]);
  readonly loading = signal(true);
  readonly refreshing = signal(false);
  readonly error = signal(false);
  readonly lastUpdated = signal<Date | null>(null);

  readonly freeCount = computed(
    () => this.tables().filter((table) => table.estado === 'LIBRE').length,
  );

  readonly reservedCount = computed(
    () => this.tables().filter((table) => table.estado === 'RESERVADA').length,
  );

  readonly occupiedCount = computed(
    () => this.tables().filter((table) => table.estado === 'OCUPADA').length,
  );

  readonly requestedCount = computed(
    () =>
      this.tables().filter(
        (table) => table.estado === 'CUENTA_SOLICITADA',
      ).length,
  );

  ngOnInit(): void {
    this.loadPanel();

    this.refreshInterval = setInterval(() => {
      this.loadPanel(true);
    }, 5000);
  }

  ngOnDestroy(): void {
    if (this.refreshInterval !== null) {
      clearInterval(this.refreshInterval);
    }
  }

  loadPanel(background = false): void {
    if (this.refreshing()) {
      return;
    }

    if (!background && this.tables().length === 0) {
      this.loading.set(true);
    }

    this.refreshing.set(true);

    this.occupancyService
      .getOccupancyPanel()
      .pipe(
        finalize(() => {
          this.loading.set(false);
          this.refreshing.set(false);
        }),
      )
      .subscribe({
        next: (tables) => {
          this.tables.set(tables);
          this.lastUpdated.set(new Date());
          this.error.set(false);
        },
        error: () => {
          this.error.set(true);
        },
      });
  }

  statusKey(status: string): string {
    return `occupancy.status.${status}`;
  }

  statusIcon(status: string): string {
    switch (status) {
      case 'LIBRE':
        return 'pi-check-circle';
      case 'RESERVADA':
        return 'pi-calendar';
      case 'OCUPADA':
        return 'pi-users';
      case 'CUENTA_SOLICITADA':
        return 'pi-receipt';
      default:
        return 'pi-circle';
    }
  }

  formatReservationUntil(value: string | null): string {
    if (!value) {
      return '';
    }

    return new Intl.DateTimeFormat('es-GT', {
      timeZone: 'America/Guatemala',
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      hour12: false,
    }).format(new Date(value));
  }

  formatLastUpdated(): string {
    const value = this.lastUpdated();

    if (!value) {
      return '';
    }

    return new Intl.DateTimeFormat('es-GT', {
      timeZone: 'America/Guatemala',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      hour12: false,
    }).format(value);
  }
}