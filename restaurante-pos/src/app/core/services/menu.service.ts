import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { MenuCatalog } from '../models/menu.models';

@Injectable({ providedIn: 'root' })
export class MenuService {
  private readonly menuUrl = `${environment.apiBaseUrl}/operacion/menu`;

  constructor(private readonly http: HttpClient) {}

  getMenu(): Observable<MenuCatalog> {
    return this.http.get<MenuCatalog>(this.menuUrl);
  }
}
