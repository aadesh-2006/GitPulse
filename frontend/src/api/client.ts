import { ApiErrorResponse, QueryParams } from '../types/api';

export class ApiError extends Error {
  public readonly status: number;
  public readonly error: string;
  public readonly path: string;
  public readonly validationErrors?: Record<string, string>;

  constructor(errorResponse: ApiErrorResponse) {
    super(errorResponse.message || `API Error ${errorResponse.status}: ${errorResponse.error}`);
    this.name = 'ApiError';
    this.status = errorResponse.status;
    this.error = errorResponse.error;
    this.path = errorResponse.path;
    this.validationErrors = errorResponse.validationErrors;
  }
}

export interface RequestOptions {
  headers?: Record<string, string>;
  params?: QueryParams;
}

const DEFAULT_BASE_URL = '/api/v1';

export class HttpClient {
  private readonly baseUrl: string;

  constructor(baseUrl?: string) {
    const configuredBaseUrl = (import.meta as unknown as { env?: Record<string, string> })?.env?.VITE_API_BASE_URL;
    this.baseUrl = baseUrl || configuredBaseUrl || DEFAULT_BASE_URL;
  }

  private buildUrl(path: string, params?: QueryParams): string {
    const cleanBase = this.baseUrl.replace(/\/+$/, '');
    const cleanPath = path.startsWith('/') ? path : `/${path}`;
    let url = `${cleanBase}${cleanPath}`;

    if (params) {
      const searchParams = new URLSearchParams();
      for (const [key, value] of Object.entries(params)) {
        if (value !== undefined && value !== null && value !== '') {
          searchParams.append(key, String(value));
        }
      }
      const queryString = searchParams.toString();
      if (queryString) {
        url += (url.includes('?') ? '&' : '?') + queryString;
      }
    }

    return url;
  }

  private async request<T>(
    path: string,
    method: string,
    body?: unknown,
    options: RequestOptions = {}
  ): Promise<T> {
    const url = this.buildUrl(path, options.params);
    const headers: Record<string, string> = {
      Accept: 'application/json',
      ...options.headers,
    };

    const init: RequestInit = {
      method,
      headers,
    };

    if (body !== undefined) {
      headers['Content-Type'] = 'application/json';
      init.body = JSON.stringify(body);
    }

    let response: Response;
    try {
      response = await fetch(url, init);
    } catch (networkError) {
      throw new ApiError({
        timestamp: new Date().toISOString(),
        status: 0,
        error: 'NetworkError',
        message: networkError instanceof Error ? networkError.message : 'Network request failed',
        path: url,
      });
    }

    if (!response.ok) {
      let errorData: ApiErrorResponse;
      try {
        errorData = await response.json();
      } catch {
        errorData = {
          timestamp: new Date().toISOString(),
          status: response.status,
          error: response.statusText || 'HttpError',
          message: `Request failed with status ${response.status}`,
          path: url,
        };
      }
      throw new ApiError(errorData);
    }

    if (response.status === 204) {
      return undefined as unknown as T;
    }

    return response.json();
  }

  public get<T>(path: string, options?: RequestOptions): Promise<T> {
    return this.request<T>(path, 'GET', undefined, options);
  }

  public post<T>(path: string, body?: unknown, options?: RequestOptions): Promise<T> {
    return this.request<T>(path, 'POST', body, options);
  }

  public put<T>(path: string, body?: unknown, options?: RequestOptions): Promise<T> {
    return this.request<T>(path, 'PUT', body, options);
  }

  public delete<T>(path: string, options?: RequestOptions): Promise<T> {
    return this.request<T>(path, 'DELETE', undefined, options);
  }
}

export const apiClient = new HttpClient();
