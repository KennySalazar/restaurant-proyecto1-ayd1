import { Component, EventEmitter, Input, Output } from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';

import { FormFeedbackComponent } from '../form-feedback/form-feedback';

@Component({
  selector: 'app-confirmation-dialog',
  imports: [FormFeedbackComponent, TranslocoPipe],
  templateUrl: './confirmation-dialog.html',
  styleUrl: './confirmation-dialog.scss',
})
export class ConfirmationDialogComponent {
  @Input() open = false;
  @Input({ required: true }) eyebrowKey!: string;
  @Input({ required: true }) titleKey!: string;
  @Input({ required: true }) descriptionKey!: string;
  @Input() descriptionParams: Record<string, unknown> = {};
  @Input({ required: true }) confirmKey!: string;
  @Input() cancelKey = 'common.cancel';
  @Input() isSaving = false;
  @Input() serverMessage: string | null = null;
  @Input() tone: 'default' | 'danger' = 'default';

  @Output() readonly confirmed = new EventEmitter<void>();
  @Output() readonly cancelled = new EventEmitter<void>();
}
