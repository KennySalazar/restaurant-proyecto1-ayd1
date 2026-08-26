import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';
import { BrandMarkComponent } from '../../shared/components/brand-mark/brand-mark';

@Component({
  selector: 'app-public-layout',
  imports: [BrandMarkComponent, RouterOutlet, TranslocoPipe],
  templateUrl: './public-layout.html',
  styleUrl: './public-layout.scss',
})
export class PublicLayoutComponent {}
