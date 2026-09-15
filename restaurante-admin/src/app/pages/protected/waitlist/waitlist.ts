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

import {
  WaitlistQueueEntry,
  WaitlistStatus,
} from '../../../core/models/waitlist.models';
import { WaitlistService } from '../../../core/services/waitlist.service';

@Component({
  selector: 'app-waitlist-page',
  imports: [TranslocoPipe],
  templateUrl: './waitlist.html',
  styleUrl: './waitlist.scss',
})
export class WaitlistPageComponent implements OnInit, OnDestroy {
  private readonly waitlistService = inject(WaitlistService);

  private refreshInterval: ReturnType<typeof setInterval> | null = null;

  readonly entries = signal<WaitlistQueueEntry[]>([]);
  readonly selectedEntry = signal<WaitlistQueueEntry | null>(null);

  readonly loading = signal(true);
  readonly refreshing = signal(false);
  readonly error = signal(false);

  readonly detailLoading = signal(false);
  readonly detailError = signal(false);

  readonly lastUpdated = signal<Date | null>(null);

  readonly waitingCount = computed(
    () =>
      this.entries().filter(
        (entry) => entry.estado === 'ESPERANDO',
      ).length,
  );

  readonly suggestedCount = computed(
    () =>
      this.entries().filter(
        (entry) => entry.estado === 'SUGERIDA',
      ).length,
  );

  readonly notifiedCount = computed(
    () =>
      this.entries().filter(
        (entry) => entry.estado === 'NOTIFICADA',
      ).length,
  );

  ngOnInit(): void {
    this.loadWaitlist();

    this.refreshInterval = setInterval(() => {
      this.loadWaitlist(true);
    }, 5000);
  }

  ngOnDestroy(): void {
    if (this.refreshInterval !== null) {
      clearInterval(this.refreshInterval);
    }
  }

  loadWaitlist(background = false): void {
    if (this.refreshing()) {
      return;
    }

    if (!background && this.entries().length === 0) {
      this.loading.set(true);
    }

    this.refreshing.set(true);

    this.waitlistService
      .getWaitlist()
      .pipe(
        finalize(() => {
          this.loading.set(false);
          this.refreshing.set(false);
        }),
      )
      .subscribe({
        next: (entries) => {
          this.entries.set(entries);
          this.error.set(false);
          this.lastUpdated.set(new Date());

          const selected = this.selectedEntry();

          if (selected) {
            const updated = entries.find(
              (entry) => entry.id === selected.id,
            );

            if (updated) {
              this.selectedEntry.set(updated);
            } else {
              this.selectedEntry.set(null);
            }
          }
        },
        error: () => {
          this.error.set(true);
        },
      });
  }

  viewDetail(id: number): void {
    this.detailLoading.set(true);
    this.detailError.set(false);

    this.waitlistService
      .getWaitlistEntry(id)
      .pipe(
        finalize(() => {
          this.detailLoading.set(false);
        }),
      )
      .subscribe({
        next: (entry) => {
          this.selectedEntry.set(entry);
        },
        error: () => {
          this.detailError.set(true);
        },
      });
  }

  closeDetail(): void {
    this.selectedEntry.set(null);
    this.detailError.set(false);
  }

  statusKey(status: WaitlistStatus): string {
    return `waitlist.status.${status}`;
  }

  statusIcon(status: WaitlistStatus): string {
    switch (status) {
      case 'ESPERANDO':
        return 'pi-clock';
      case 'SUGERIDA':
        return 'pi-lightbulb';
      case 'NOTIFICADA':
        return 'pi-bell';
      case 'SENTADA':
        return 'pi-check-circle';
      case 'RETIRADA':
        return 'pi-times-circle';
    }
  }

  formatArrival(value: string): string {
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