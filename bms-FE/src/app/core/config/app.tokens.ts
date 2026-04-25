import { InjectionToken } from '@angular/core';

type RuntimeConfig = {
  apiBaseUrl?: string;
  supabaseUrl?: string;
  supabaseAnonKey?: string;
};

function resolveRuntimeConfig(): RuntimeConfig | undefined {
  return (globalThis as typeof globalThis & { __TAARA_CONFIG__?: RuntimeConfig }).__TAARA_CONFIG__;
}

function resolveApiBaseUrl(): string {
  const runtimeConfig = resolveRuntimeConfig();
  return runtimeConfig?.apiBaseUrl ?? '/api';
}

function resolveSupabaseUrl(): string {
  const runtimeConfig = resolveRuntimeConfig();
  return runtimeConfig?.supabaseUrl ?? '';
}

function resolveSupabaseAnonKey(): string {
  const runtimeConfig = resolveRuntimeConfig();
  return runtimeConfig?.supabaseAnonKey ?? '';
}

export const APP_API_URL = new InjectionToken<string>('APP_API_URL', {
  providedIn: 'root',
  factory: resolveApiBaseUrl
});

export const APP_SUPABASE_URL = new InjectionToken<string>('APP_SUPABASE_URL', {
  providedIn: 'root',
  factory: resolveSupabaseUrl
});

export const APP_SUPABASE_ANON_KEY = new InjectionToken<string>('APP_SUPABASE_ANON_KEY', {
  providedIn: 'root',
  factory: resolveSupabaseAnonKey
});
