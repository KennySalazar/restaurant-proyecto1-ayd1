import { Component, signal } from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';

import { PageHeadingComponent } from '../../../shared/components/page-heading/page-heading';
import { DishesTabComponent } from './dishes-tab/dishes-tab';

type MenuTab = 'dishes' | 'modifiers' | 'combos';

@Component({
  selector: 'app-menu-page',
  imports: [DishesTabComponent, PageHeadingComponent, TranslocoPipe],
  templateUrl: './menu.html',
  styleUrl: './menu.scss',
})
export class MenuPageComponent {
  readonly activeTab = signal<MenuTab>('dishes');

  isTabActive(tab: MenuTab): boolean {
    return this.activeTab() === tab;
  }

  selectTab(tab: MenuTab): void {
    this.activeTab.set(tab);
  }
}
