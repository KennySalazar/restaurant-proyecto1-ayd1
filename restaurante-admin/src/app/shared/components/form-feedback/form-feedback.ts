import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-form-feedback',
  templateUrl: './form-feedback.html',
  styleUrl: './form-feedback.scss',
})
export class FormFeedbackComponent {
  @Input() message: string | null = null;
  @Input() tone: 'error' | 'success' = 'error';
}