import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { SelectModule } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { TabViewModule } from 'primeng/tabview';
import { TagModule } from 'primeng/tag';
import { AppUserRole, ManagedUser } from '../../core/models/auth.models';
import { AuthService } from '../../core/services/auth.service';
import { NotificationService } from '../../core/services/notification.service';
import { getApiErrorMessage } from '../../core/utils/api-error.utils';

@Component({
  selector: 'app-admin-users-page',
  standalone: true,
  imports: [ReactiveFormsModule, DatePipe, ButtonModule, DialogModule, InputTextModule, MessageModule, SelectModule, TableModule, TabViewModule, TagModule],
  templateUrl: './admin-users-page.component.html',
  styleUrl: './admin-users-page.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AdminUsersPageComponent {
  private readonly authService = inject(AuthService);
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly notificationService = inject(NotificationService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly destroyRef = inject(DestroyRef);

  readonly loading = signal(true);
  readonly users = signal<ManagedUser[]>([]);
  readonly selectedUsers = signal<ManagedUser[]>([]);
  readonly inviteDialogVisible = signal(false);
  readonly inviting = signal(false);
  readonly deletingUsers = signal(false);
  readonly resettingData = signal(false);
  readonly resetConfirmText = signal('');

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
  readonly resetReady = computed(() => this.resetConfirmText().trim().toUpperCase() === 'RESET');

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
          this.selectedUsers.set([]);
          this.loading.set(false);
        },
        error: (error) => {
          this.loading.set(false);
          this.notificationService.error('Unable to load access management', getApiErrorMessage(error));
        }
      });
  }

  confirmDeleteUser(user: ManagedUser): void {
    this.confirmDeleteUsers([user]);
  }

  confirmDeleteSelectedUsers(): void {
    this.confirmDeleteUsers(this.selectedUsers());
  }

  resetBusinessData(): void {
    if (!this.resetReady() || this.resettingData()) {
      this.notificationService.warn('Reset not ready', 'Type RESET before clearing business data.');
      return;
    }

    this.confirmationService.confirm({
      header: 'Reset business data',
      message: 'This permanently clears all business and master data while keeping allowed users. Continue?',
      acceptLabel: 'Reset data',
      rejectLabel: 'Cancel',
      acceptButtonStyleClass: 'p-button-danger',
      rejectButtonStyleClass: 'p-button-outlined p-button-secondary',
      accept: () => {
        this.resettingData.set(true);
        this.authService.resetBusinessData()
          .pipe(takeUntilDestroyed(this.destroyRef))
          .subscribe({
            next: (response) => {
              this.resettingData.set(false);
              this.resetConfirmText.set('');
              this.notificationService.success('Business data reset', response.message);
            },
            error: (error) => {
              this.resettingData.set(false);
              this.notificationService.error('Unable to reset business data', getApiErrorMessage(error));
            }
          });
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

  canDeleteUser(user: ManagedUser): boolean {
    return user.email.toLowerCase() !== this.currentUser()?.email?.toLowerCase();
  }

  private confirmDeleteUsers(users: ManagedUser[]): void {
    const deletableUsers = users.filter((user) => this.canDeleteUser(user));
    if (!deletableUsers.length) {
      this.notificationService.warn('User not deleted', 'You cannot delete your own admin access.');
      return;
    }

    this.confirmationService.confirm({
      header: deletableUsers.length === 1 ? 'Remove allowed user' : 'Remove allowed users',
      message: `Remove app access for ${deletableUsers.length} selected user(s)? Supabase Auth users will not be deleted.`,
      acceptLabel: 'Remove access',
      rejectLabel: 'Cancel',
      acceptButtonStyleClass: 'p-button-danger',
      rejectButtonStyleClass: 'p-button-outlined p-button-secondary',
      accept: () => {
        this.deletingUsers.set(true);
        forkJoin(deletableUsers.map((user) => this.authService.deleteManagedUser(user.id)))
          .pipe(takeUntilDestroyed(this.destroyRef))
          .subscribe({
            next: () => {
              this.deletingUsers.set(false);
              this.notificationService.success('Allowed users removed', 'Selected users were removed from app access.');
              this.loadUsers();
            },
            error: (error) => {
              this.deletingUsers.set(false);
              this.notificationService.error('Unable to remove allowed users', getApiErrorMessage(error));
            }
          });
      }
    });
  }
}
