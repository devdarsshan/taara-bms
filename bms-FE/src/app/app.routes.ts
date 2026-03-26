import { Routes } from '@angular/router';
import { adminGuard, authGuard, guestOnlyGuard } from './core/guards/auth.guards';

export const routes: Routes = [
  {
    path: 'login',
    canActivate: [guestOnlyGuard],
    loadComponent: () => import('./features/auth/auth-page.component').then((m) => m.AuthPageComponent),
    data: { mode: 'login' }
  },
  {
    path: 'signup',
    canActivate: [guestOnlyGuard],
    loadComponent: () => import('./features/auth/auth-page.component').then((m) => m.AuthPageComponent),
    data: { mode: 'signup' }
  },
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () => import('./features/shell/shell.component').then((m) => m.ShellComponent),
    children: [
      {
        path: '',
        pathMatch: 'full',
        redirectTo: 'overview'
      },
      {
        path: 'overview',
        loadComponent: () => import('./features/overview/overview-page.component').then((m) => m.OverviewPageComponent)
      },
      {
        path: 'master-data',
        loadComponent: () => import('./features/master-data/master-data-page.component').then((m) => m.MasterDataPageComponent)
      },
      {
        path: 'yarn',
        loadComponent: () => import('./features/yarn/yarn-page.component').then((m) => m.YarnPageComponent)
      },
      {
        path: 'spinning',
        loadComponent: () => import('./features/spinning/spinning-page.component').then((m) => m.SpinningPageComponent)
      },
      {
        path: 'in-house',
        loadComponent: () => import('./features/in-house/in-house-page.component').then((m) => m.InHousePageComponent)
      },
      {
        path: 'stitching',
        loadComponent: () => import('./features/stitching/stitching-page.component').then((m) => m.StitchingPageComponent)
      },
      {
        path: 'printing',
        loadComponent: () => import('./features/printing/printing-page.component').then((m) => m.PrintingPageComponent)
      },
      {
        path: 'packing',
        loadComponent: () => import('./features/packing/packing-page.component').then((m) => m.PackingPageComponent)
      },
      {
        path: 'admin/users',
        canActivate: [adminGuard],
        loadComponent: () => import('./features/admin/admin-users-page.component').then((m) => m.AdminUsersPageComponent)
      }
    ]
  },
  {
    path: '**',
    redirectTo: '/login'
  }
];
