import { AbstractControl } from '@angular/forms';
import { Component, Input } from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';

type FieldErrorKind = 'generic' | 'password';

@Component({
  selector: 'app-field-error',
  imports: [TranslocoPipe],
  templateUrl: './field-error.html',
})
export class FieldErrorComponent {
  @Input() control: AbstractControl | null = null;
  @Input() submitted = false;
  @Input() kind: FieldErrorKind = 'generic';

  get visible(): boolean {
    return (
      !!this.control &&
      this.control.invalid &&
      (this.control.touched || this.submitted)
    );
  }

  get messageKey(): string {
    if (!this.control) {
      return 'validation.invalid';
    }

    if (this.control.hasError('required')) {
      return 'validation.required';
    }

    if (this.control.hasError('email')) {
      return 'validation.email';
    }

    if (
      this.control.hasError('minlength') ||
      this.control.hasError('maxlength')
    ) {
      return 'validation.passwordLength';
    }

    if (this.control.hasError('pattern')) {
      return 'validation.passwordPolicy';
    }

    return 'validation.invalid';
  }
}
