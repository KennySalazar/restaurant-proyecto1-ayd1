import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { OccupancyPanelTable } from '../models/occupancy-panel.models';

@Injectable({
  providedIn: 'root',
})
export class OccupancyPanelService {
  private readonly http = inject(HttpClient);
  private readonly url =
    `${environment.apiBaseUrl}/admin/panel-ocupacion`;

  getOccupancyPanel(): Observable<OccupancyPanelTable[]> {
    return this.http.get<OccupancyPanelTable[]>(this.url);
  }
}