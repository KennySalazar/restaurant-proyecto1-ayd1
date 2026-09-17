import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { WaitlistQueueEntry, WaitlistSuggestion } from '../models/waitlist.models';

@Injectable({ providedIn: 'root' })
export class WaitlistService {
  private readonly waitlistUrl = `${environment.apiBaseUrl}/operacion/lista-espera`;
  private readonly tablesUrl = `${environment.apiBaseUrl}/operacion/mesas`;

  constructor(private readonly http: HttpClient) {}

  getWaitlist(): Observable<WaitlistQueueEntry[]> {
    return this.http.get<WaitlistQueueEntry[]>(this.waitlistUrl);
  }

  getSuggestionForTable(tableId: number): Observable<WaitlistSuggestion | null> {
    return this.http.get<WaitlistSuggestion | null>(
      `${this.tablesUrl}/${tableId}/sugerencia-espera`,
    );
  }
}
