import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { Notification, NotificationCount } from '../models/notification.models';

@Injectable({
  providedIn: 'root',
})
export class NotificationService {
  private readonly http = inject(HttpClient);
  private readonly notificationsUrl = `${environment.apiBaseUrl}/admin/notifications`;

  list(unreadOnly = false): Observable<Notification[]> {
    return this.http.get<Notification[]>(this.notificationsUrl, {
      params: new HttpParams().set('unreadOnly', unreadOnly),
    });
  }

  unreadCount(): Observable<NotificationCount> {
    return this.http.get<NotificationCount>(`${this.notificationsUrl}/count`);
  }

  markAsRead(id: number): Observable<Notification> {
    return this.http.put<Notification>(`${this.notificationsUrl}/${id}/read`, null);
  }

  markAllAsRead(): Observable<void> {
    return this.http.put<void>(`${this.notificationsUrl}/read-all`, null);
  }
}
