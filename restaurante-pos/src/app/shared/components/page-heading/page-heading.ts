import { Component, Input } from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';

@Component({
  selector: 'app-page-heading',
  imports: [TranslocoPipe],
  templateUrl: './page-heading.html',
  styleUrl: './page-heading.scss',
})
export class PageHeadingComponent {
  @Input({ required: true }) titleKey!: string;
  @Input() descriptionKey?: string;
  @Input() eyebrowKey?: string;
}
