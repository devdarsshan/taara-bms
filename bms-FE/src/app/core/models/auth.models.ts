export type AppUserRole = 'ADMIN' | 'USER';
export type AppUserStatus = 'INVITED' | 'ACTIVE' | 'DISABLED';

export interface CurrentUser {
  email: string;
  role: AppUserRole;
  status: AppUserStatus;
}

export interface ManagedUser {
  id: string;
  email: string;
  role: AppUserRole;
  status: AppUserStatus;
  supabaseUserId: string | null;
  invitedAt: string;
  activatedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface SignupRequest {
  email: string;
  password: string;
}

export interface InviteUserRequest {
  email: string;
  role: AppUserRole;
}
