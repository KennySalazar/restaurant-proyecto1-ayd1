import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';

@Component({
  selector: 'app-brand-mark',
  imports: [RouterLink, TranslocoPipe],
  templateUrl: './brand-mark.html',
  styleUrl: './brand-mark.scss',
})
export class BrandMarkComponent {}
