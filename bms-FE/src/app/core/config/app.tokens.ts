import { InjectionToken } from '@angular/core';

type RuntimeConfig = {
  apiBaseUrl?: string;
};

function resolveApiBaseUrl(): string {
  const runtimeConfig = (globalThis as typeof globalThis & { __TAARA_CONFIG__?: RuntimeConfig }).__TAARA_CONFIG__;
  return runtimeConfig?.apiBaseUrl ?? 'http://localhost:8080/api';
}

export const APP_API_URL = new InjectionToken<string>('APP_API_URL', {
  providedIn: 'root',
  factory: resolveApiBaseUrl
});
