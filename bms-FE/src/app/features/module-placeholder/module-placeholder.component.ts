import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { TagModule } from 'primeng/tag';

type PlaceholderRouteData = {
  title: string;
  description: string;
  highlights: string[];
};

@Component({
  selector: 'app-module-placeholder',
  standalone: true,
  imports: [RouterLink, ButtonModule, CardModule, TagModule],
  templateUrl: './module-placeholder.component.html',
  styleUrl: './module-placeholder.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ModulePlaceholderComponent {
  private readonly route = inject(ActivatedRoute);

  readonly data = this.route.snapshot.data as PlaceholderRouteData;
}
