import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { APP_API_URL } from '../config/app.tokens';

type Primitive = string | number | boolean;
type ParamValue = Primitive | Primitive[] | null | undefined;

@Injectable({
  providedIn: 'root'
})
export class ApiService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = inject(APP_API_URL).replace(/\/$/, '');

  get<T>(path: string, params?: Record<string, ParamValue>): Observable<T> {
    return this.http.get<T>(this.resolveUrl(path), {
      params: this.buildParams(params)
    });
  }

  post<TResponse, TBody>(path: string, body: TBody): Observable<TResponse> {
    return this.http.post<TResponse>(this.resolveUrl(path), body);
  }

  put<TResponse, TBody>(path: string, body: TBody): Observable<TResponse> {
    return this.http.put<TResponse>(this.resolveUrl(path), body);
  }

  patch<TResponse, TBody>(path: string, body: TBody): Observable<TResponse> {
    return this.http.patch<TResponse>(this.resolveUrl(path), body);
  }

  delete(path: string): Observable<void> {
    return this.http.delete<void>(this.resolveUrl(path));
  }

  private resolveUrl(path: string): string {
    return path.startsWith('/') ? `${this.apiUrl}${path}` : `${this.apiUrl}/${path}`;
  }

  private buildParams(params?: Record<string, ParamValue>): HttpParams {
    if (!params) {
      return new HttpParams();
    }

    let httpParams = new HttpParams();

    for (const [key, value] of Object.entries(params)) {
      if (value === undefined || value === null || value === '') {
        continue;
      }

      if (Array.isArray(value)) {
        value.forEach((item) => {
          httpParams = httpParams.append(key, String(item));
        });
        continue;
      }

      httpParams = httpParams.set(key, String(value));
    }

    return httpParams;
  }
}
