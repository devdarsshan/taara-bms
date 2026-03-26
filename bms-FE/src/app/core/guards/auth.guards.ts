import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const authGuard: CanActivateFn = async () => {
  const authService = inject(AuthService);
  const router = inject(Router);
  await authService.ensureInitialized();

  if (authService.isAuthenticated()) {
    return true;
  }

  return router.parseUrl('/login');
};

export const guestOnlyGuard: CanActivateFn = async () => {
  const authService = inject(AuthService);
  const router = inject(Router);
  await authService.ensureInitialized();

  if (!authService.isAuthenticated()) {
    return true;
  }

  return router.parseUrl(authService.isAdmin() ? '/overview' : '/master-data');
};

export const adminGuard: CanActivateFn = async () => {
  const authService = inject(AuthService);
  const router = inject(Router);
  await authService.ensureInitialized();

  if (!authService.isAuthenticated()) {
    return router.parseUrl('/login');
  }

  if (authService.isAdmin()) {
    return true;
  }

  return router.parseUrl('/master-data');
};
