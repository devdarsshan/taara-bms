import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
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
      }
    ]
  },
  {
    path: '**',
    redirectTo: '/overview'
  }
];
