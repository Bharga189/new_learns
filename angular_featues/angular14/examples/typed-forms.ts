import { Component } from '@angular/core';
import { FormBuilder, FormControl, FormGroup, Validators } from '@angular/forms';

interface ProfileForm {
  email: FormControl<string>;
  age: FormControl<number>;
  marketing: FormControl<boolean>;
}

@Component({
  selector: 'app-profile-form',
  standalone: false,
  template: `<form><!-- bind controls in template --></form>`,
})
export class ProfileFormComponent {
  // Strongly typed reactive form (Angular 14+).
  readonly form = new FormGroup<ProfileForm>({
    email: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.email] }),
    age: new FormControl(18, { nonNullable: true, validators: [Validators.min(0)] }),
    marketing: new FormControl(false, { nonNullable: true }),
  });

  constructor(private fb: FormBuilder) {}

  submit(): void {
    if (this.form.invalid) return;
    const raw = this.form.getRawValue(); // { email: string; age: number; marketing: boolean }
    // raw.email is typed as string
    console.log('submit', raw);
  }

  // FormBuilder also infers types in Angular 14+.
  buildWithBuilder() {
    return this.fb.nonNullable.group({
      email: [''],
      age: [0],
    });
  }
}
