import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';

@Component({
  selector: 'app-unauthorized-page',
  imports: [RouterLink, TranslocoPipe],
  templateUrl: './unauthorized.html',
  styleUrl: './unauthorized.scss',
})
export class UnauthorizedPageComponent {}
