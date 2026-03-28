import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterOutlet } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { ToggleSwitchModule } from 'primeng/toggleswitch';
import { AuthService } from '../../core/services/auth.service';
import { ThemeService } from '../../core/services/theme.service';

type NavItem = {
  label: string;
  caption: string;
  route: string;
  icon: string;
};

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [RouterOutlet, ReactiveFormsModule, ToggleSwitchModule, ButtonModule],
  templateUrl: './shell.component.html',
  styleUrl: './shell.component.css'
})
export class ShellComponent {
  private readonly themeService = inject(ThemeService);
  readonly authService = inject(AuthService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly router = inject(Router);

  readonly navItems: NavItem[] = [
    {
      label: 'Overview',
      caption: 'Dashboard',
      route: '/overview',
      icon: 'pi pi-home'
    },
    {
      label: 'Master Data',
      caption: 'Foundations',
      route: '/master-data',
      icon: 'pi pi-database'
    },
    {
      label: 'Yarn',
      caption: 'Orders',
      route: '/yarn',
      icon: 'pi pi-box'
    },
    {
      label: 'Knitting',
      caption: 'Factory Flow',
      route: '/spinning',
      icon: 'pi pi-sync'
    },
    {
      label: 'In-house',
      caption: 'Stock & Cutting',
      route: '/in-house',
      icon: 'pi pi-building'
    },
    {
      label: 'Stitching',
      caption: 'Delivery',
      route: '/stitching',
      icon: 'pi pi-sitemap'
    },
    {
      label: 'Printing',
      caption: 'Finish Flow',
      route: '/printing',
      icon: 'pi pi-palette'
    },
    {
      label: 'Packing',
      caption: 'Dispatch Prep',
      route: '/packing',
      icon: 'pi pi-shopping-bag'
    }
  ];

  readonly themeControl = new FormControl(this.themeService.isDarkMode(), { nonNullable: true });
  readonly themeLabel = this.themeService.mode;
  readonly sidebarCollapsed = signal(false);

  constructor() {
    this.themeControl.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((enabled) => {
      this.themeService.setDarkMode(enabled);
    });
  }

  navigateTo(route: string): void {
    void this.router.navigateByUrl(route);
  }

  openAccountDetails(): void {
    if (!this.authService.isAdmin()) {
      return;
    }
    void this.router.navigateByUrl('/admin/users');
  }

  toggleSidebar(): void {
    this.sidebarCollapsed.update((collapsed) => !collapsed);
  }

  isRouteActive(route: string): boolean {
    return route === '/overview' ? this.router.url === route : this.router.url.startsWith(route);
  }

  logout(): void {
    void this.authService.signOut();
  }
}
