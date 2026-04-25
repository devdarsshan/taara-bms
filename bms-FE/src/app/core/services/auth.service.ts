import { HttpClient } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
import { Router } from '@angular/router';
import { createClient, Session, SupabaseClient } from '@supabase/supabase-js';
import { Observable } from 'rxjs';
import { firstValueFrom } from 'rxjs';
import { APP_API_URL, APP_SUPABASE_ANON_KEY, APP_SUPABASE_URL } from '../config/app.tokens';
import { CurrentUser, InviteUserRequest, ManagedUser, SignupRequest } from '../models/auth.models';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly apiUrl = inject(APP_API_URL).replace(/\/$/, '');
  private readonly supabaseUrl = inject(APP_SUPABASE_URL);
  private readonly supabaseAnonKey = inject(APP_SUPABASE_ANON_KEY);

  private readonly supabase: SupabaseClient = createClient(
    this.supabaseUrl || 'https://placeholder.supabase.co',
    this.supabaseAnonKey || 'placeholder-anon-key',
    {
      auth: {
        persistSession: true,
        autoRefreshToken: true,
        detectSessionInUrl: true
      }
    }
  );

  readonly session = signal<Session | null>(null);
  readonly currentUser = signal<CurrentUser | null>(null);
  readonly initialized = signal(false);
  readonly loadingProfile = signal(false);

  readonly isAuthenticated = computed(() => !!this.currentUser());
  readonly isAdmin = computed(() => this.currentUser()?.role === 'ADMIN');
  readonly isUser = computed(() => this.currentUser()?.role === 'USER');

  private readonly initializationPromise: Promise<void>;

  constructor() {
    this.initializationPromise = this.initialize();
  }

  async ensureInitialized(): Promise<void> {
    await this.initializationPromise;
  }

  async signIn(email: string, password: string): Promise<void> {
    const { data, error } = await this.supabase.auth.signInWithPassword({
      email: email.trim(),
      password
    });

    if (error) {
      throw new Error(error.message);
    }

    this.session.set(data.session);
    await this.refreshCurrentUser();
  }

  async signUp(payload: SignupRequest): Promise<void> {
    await firstValueFrom(this.http.post<{ message: string }>(`${this.apiUrl}/auth/signup`, payload));
    await this.signIn(payload.email, payload.password);
  }

  async signOut(navigateToLogin = true): Promise<void> {
    await this.supabase.auth.signOut();
    this.session.set(null);
    this.currentUser.set(null);
    if (navigateToLogin) {
      await this.router.navigateByUrl('/login');
    }
  }

  async getAccessToken(): Promise<string | null> {
    const { data } = await this.supabase.auth.getSession();
    return data.session?.access_token ?? null;
  }

  async refreshCurrentUser(): Promise<void> {
    const token = await this.getAccessToken();
    if (!token) {
      this.currentUser.set(null);
      return;
    }

    this.loadingProfile.set(true);
    try {
      const user = await firstValueFrom(this.http.get<CurrentUser>(`${this.apiUrl}/auth/me`));
      this.currentUser.set(user);
    } catch (error) {
      this.currentUser.set(null);
      await this.supabase.auth.signOut();
      throw error;
    } finally {
      this.loadingProfile.set(false);
    }
  }

  async routeAfterLogin(): Promise<void> {
    await this.router.navigateByUrl('/overview');
  }

  getManagedUsers(): Observable<ManagedUser[]> {
    return this.http.get<ManagedUser[]>(`${this.apiUrl}/auth/admin/users`);
  }

  inviteUser(payload: InviteUserRequest): Observable<ManagedUser> {
    return this.http.post<ManagedUser>(`${this.apiUrl}/auth/admin/users`, payload);
  }

  deleteManagedUser(userId: string): Observable<{ message: string }> {
    return this.http.delete<{ message: string }>(`${this.apiUrl}/auth/admin/users/${userId}`);
  }

  resetBusinessData(): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.apiUrl}/auth/admin/maintenance/reset-data`, {});
  }

  private async initialize(): Promise<void> {
    const { data } = await this.supabase.auth.getSession();
    this.session.set(data.session);

    if (data.session) {
      try {
        await this.refreshCurrentUser();
      } catch {
        this.currentUser.set(null);
      }
    }

    this.supabase.auth.onAuthStateChange((_event, session) => {
      this.session.set(session);
      if (session) {
        void this.refreshCurrentUser();
      } else {
        this.currentUser.set(null);
      }
    });

    this.initialized.set(true);
  }
}
