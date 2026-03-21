import { DOCUMENT } from '@angular/common';
import { inject, Injectable, signal } from '@angular/core';

export type ThemeMode = 'light' | 'dark';

@Injectable({
  providedIn: 'root'
})
export class ThemeService {
  private readonly document = inject(DOCUMENT);
  readonly mode = signal<ThemeMode>('light');

  constructor() {
    const initialMode = this.getStoredMode() ?? this.getSystemMode();
    this.applyMode(initialMode, false);
  }

  setDarkMode(enabled: boolean): void {
    this.applyMode(enabled ? 'dark' : 'light');
  }

  isDarkMode(): boolean {
    return this.mode() === 'dark';
  }

  private applyMode(mode: ThemeMode, persist = true): void {
    this.mode.set(mode);
    this.document.documentElement.dataset['theme'] = mode;
    this.document.documentElement.style.colorScheme = mode;
    this.document.documentElement.classList.toggle('app-dark', mode === 'dark');
    this.document.documentElement.classList.toggle('app-light', mode === 'light');
    this.document.body?.classList.toggle('app-dark', mode === 'dark');
    this.document.body?.classList.toggle('app-light', mode === 'light');

    if (persist) {
      globalThis.localStorage?.setItem('taara-theme-mode', mode);
    }
  }

  private getStoredMode(): ThemeMode | null {
    const value = globalThis.localStorage?.getItem('taara-theme-mode');
    return value === 'dark' || value === 'light' ? value : null;
  }

  private getSystemMode(): ThemeMode {
    return globalThis.matchMedia?.('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
  }
}
