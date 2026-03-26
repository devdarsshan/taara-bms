import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { AppUserRole, ManagedUser } from '../../core/models/auth.models';
import { AuthService } from '../../core/services/auth.service';
import { NotificationService } from '../../core/services/notification.service';
import { getApiErrorMessage } from '../../core/utils/api-error.utils';

@Component({
  selector: 'app-admin-users-page',
  standalone: true,
  imports: [ReactiveFormsModule, DatePipe, ButtonModule, DialogModule, InputTextModule, SelectModule, TableModule, TagModule],
  templateUrl: './admin-users-page.component.html',
  styleUrl: './admin-users-page.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AdminUsersPageComponent {
  private readonly authService = inject(AuthService);
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly notificationService = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);

  readonly loading = signal(true);
  readonly users = signal<ManagedUser[]>([]);
  readonly inviteDialogVisible = signal(false);
  readonly inviting = signal(false);

  readonly inviteForm = this.fb.group({
    email: this.fb.control('', [Validators.required, Validators.email]),
    role: this.fb.control<AppUserRole>('USER', Validators.required)
  });

  readonly roleOptions = [
    { label: 'Admin', value: 'ADMIN' as AppUserRole },
    { label: 'User', value: 'USER' as AppUserRole }
  ];

  readonly currentUser = computed(() => this.authService.currentUser());
  readonly invitedCount = computed(() => this.users().filter((user) => user.status === 'INVITED').length);
  readonly activeCount = computed(() => this.users().filter((user) => user.status === 'ACTIVE').length);

  constructor() {
    this.loadUsers();
  }

  loadUsers(): void {
    this.loading.set(true);
    this.authService.getManagedUsers()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (users) => {
          this.users.set(users);
          this.loading.set(false);
        },
        error: (error) => {
          this.loading.set(false);
          this.notificationService.error('Unable to load access management', getApiErrorMessage(error));
        }
      });
  }

  openInviteDialog(): void {
    this.inviteForm.reset({ email: '', role: 'USER' });
    this.inviteDialogVisible.set(true);
  }

  inviteUser(): void {
    if (this.inviteForm.invalid) {
      this.inviteForm.markAllAsTouched();
      this.notificationService.warn('Invite incomplete', 'Enter a valid email and role before inviting.');
      return;
    }

    this.inviting.set(true);
    const value = this.inviteForm.getRawValue();
    this.authService.inviteUser({ email: value.email, role: value.role })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.inviting.set(false);
          this.inviteDialogVisible.set(false);
          this.notificationService.success('User invited', 'The user can now complete signup from the app.');
          this.loadUsers();
        },
        error: (error) => {
          this.inviting.set(false);
          this.notificationService.error('Unable to invite user', getApiErrorMessage(error));
        }
      });
  }

  statusSeverity(status: ManagedUser['status']): 'success' | 'warn' | 'danger' {
    if (status === 'ACTIVE') {
      return 'success';
    }
    if (status === 'INVITED') {
      return 'warn';
    }
    return 'danger';
  }
}
