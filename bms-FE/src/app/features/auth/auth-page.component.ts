import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { DividerModule } from 'primeng/divider';
import { InputTextModule } from 'primeng/inputtext';
import { PasswordModule } from 'primeng/password';
import { AuthService } from '../../core/services/auth.service';
import { NotificationService } from '../../core/services/notification.service';
import { getApiErrorMessage } from '../../core/utils/api-error.utils';

@Component({
  selector: 'app-auth-page',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, ButtonModule, CardModule, DividerModule, InputTextModule, PasswordModule],
  templateUrl: './auth-page.component.html',
  styleUrl: './auth-page.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AuthPageComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly notificationService = inject(NotificationService);

  readonly mode = signal<'login' | 'signup'>((this.route.snapshot.data['mode'] as 'login' | 'signup') ?? 'login');
  readonly submitting = signal(false);

  readonly form = this.fb.group({
    email: this.fb.nonNullable.control('', [Validators.required, Validators.email]),
    password: this.fb.nonNullable.control('', [Validators.required, Validators.minLength(8)]),
    confirmPassword: this.fb.nonNullable.control('')
  });

  readonly pageTitle = computed(() => this.mode() === 'login' ? 'Welcome back' : 'Complete your invited signup');
  readonly pageSubtitle = computed(() =>
    this.mode() === 'login'
      ? 'Sign in with your approved Taara account to continue.'
      : 'Your email must already be pre-approved by an admin before you can create a password.'
  );
  readonly actionLabel = computed(() => this.mode() === 'login' ? 'Sign In' : 'Create Account');

  isInvalid(control: AbstractControl | null): boolean {
    return !!control && control.invalid && (control.touched || control.dirty);
  }

  async submit(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.notificationService.warn('Form incomplete', 'Fill the required fields before continuing.');
      return;
    }

    const { email, password, confirmPassword } = this.form.getRawValue();
    if (this.mode() === 'signup' && password !== confirmPassword) {
      this.notificationService.warn('Passwords do not match', 'Confirm the same password before continuing.');
      return;
    }

    this.submitting.set(true);
    try {
      if (this.mode() === 'login') {
        await this.authService.signIn(email, password);
        this.notificationService.success('Signed in', 'You are now signed in.');
      } else {
        await this.authService.signUp({ email, password });
        this.notificationService.success('Account ready', 'Your invited account is ready to use.');
      }
      await this.authService.routeAfterLogin();
    } catch (error) {
      this.notificationService.error(
        this.mode() === 'login' ? 'Unable to sign in' : 'Unable to complete signup',
        getApiErrorMessage(error)
      );
    } finally {
      this.submitting.set(false);
    }
  }

  async switchMode(mode: 'login' | 'signup'): Promise<void> {
    this.mode.set(mode);
    this.form.markAsPristine();
    await this.router.navigateByUrl(mode === 'login' ? '/login' : '/signup');
  }
}
