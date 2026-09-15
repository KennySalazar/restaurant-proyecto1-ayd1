import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { WaitlistQueueEntry } from '../models/waitlist.models';

@Injectable({
  providedIn: 'root',
})
export class WaitlistService {
  private readonly http = inject(HttpClient);
  private readonly waitlistUrl =
    `${environment.apiBaseUrl}/admin/lista-espera`;

  getWaitlist(): Observable<WaitlistQueueEntry[]> {
    return this.http.get<WaitlistQueueEntry[]>(this.waitlistUrl);
  }

  getWaitlistEntry(id: number): Observable<WaitlistQueueEntry> {
    return this.http.get<WaitlistQueueEntry>(
      `${this.waitlistUrl}/${id}`,
    );
  }
}