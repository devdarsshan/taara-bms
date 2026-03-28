import { inject, Injectable } from '@angular/core';
import { MessageService } from 'primeng/api';

@Injectable({
  providedIn: 'root'
})
export class NotificationService {
  private readonly messageService = inject(MessageService);

  success(summary: string, detail: string): void {
    this.add('success', summary, detail);
  }

  error(summary: string, detail: string): void {
    this.add('error', summary, detail);
  }

  warn(summary: string, detail: string): void {
    this.add('warn', summary, detail);
  }

  info(summary: string, detail: string): void {
    this.add('info', summary, detail);
  }

  private add(severity: 'success' | 'error' | 'warn' | 'info', summary: string, detail: string): void {
    this.messageService.add({
      severity,
      summary,
      detail
    });
  }
}
