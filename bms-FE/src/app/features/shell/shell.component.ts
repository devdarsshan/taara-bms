import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterOutlet } from '@angular/router';
import { ToggleSwitchModule } from 'primeng/toggleswitch';
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
  imports: [RouterOutlet, ReactiveFormsModule, ToggleSwitchModule],
  templateUrl: './shell.component.html',
  styleUrl: './shell.component.css'
})
export class ShellComponent {
  private readonly themeService = inject(ThemeService);
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
      label: 'Spinning',
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

  toggleSidebar(): void {
    this.sidebarCollapsed.update((collapsed) => !collapsed);
  }

  isRouteActive(route: string): boolean {
    return route === '/overview' ? this.router.url === route : this.router.url.startsWith(route);
  }
}
