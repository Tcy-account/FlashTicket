import axios, { type AxiosInstance, type AxiosRequestConfig, type AxiosResponse } from 'axios';
import { ErrorCode, type ApiResponse } from '@flashticket/shared-types';

/**
 * Access Token 内存存储（规范要求：Access Token 存内存，Refresh Token 用 HttpOnly Cookie）。
 * M02 登录后由 auth store 调用 setAccessToken 写入。
 */
let accessToken: string | null = null;

export function setAccessToken(token: string | null): void {
  accessToken = token;
}

export function getAccessToken(): string | null {
  return accessToken;
}

export interface ApiClientOptions {
  baseURL: string;
  /** 401 触发（刷新失败后的最终兜底，跳登录），由各 app 注入 */
  onUnauthorized?: () => void;
  /** 业务失败统一提示 */
  onBizError?: (code: string, message: string) => void;
  /** 网络层错误统一提示（请求未到达后端） */
  onNetworkError?: (message: string) => void;
  /** 单飞刷新：返回新 Access Token，失败返回 null */
  refreshAccessToken?: () => Promise<string | null>;
}

// 单飞刷新：并发 401 只触发一次刷新，其余请求复用同一 Promise
let refreshPromise: Promise<string | null> | null = null;

function singleFlightRefresh(options: ApiClientOptions): Promise<string | null> {
  if (!options.refreshAccessToken) {
    return Promise.resolve(null);
  }
  if (!refreshPromise) {
    refreshPromise = options.refreshAccessToken().finally(() => {
      refreshPromise = null;
    });
  }
  return refreshPromise;
}

/**
 * 创建带统一拦截器的 Axios 实例。
 *
 * 约定：HTTP 状态码管传输语义（2xx/4xx/5xx），业务码 code 管业务语义。
 * - 2xx 但 code !== SUCCESS 仍视为业务失败（此处统一透传，由调用方按 code 分支）。
 * - 401 先尝试单飞刷新并重试一次，刷新失败再触发 onUnauthorized。
 * - 带 withCredentials，保证 HttpOnly Refresh Cookie 随请求发送。
 */
export function createApiClient(options: ApiClientOptions): AxiosInstance {
  const client = axios.create({
    baseURL: options.baseURL,
    timeout: 10000,
    withCredentials: true,
  });

  client.interceptors.request.use((config) => {
    if (accessToken) {
      config.headers.Authorization = `Bearer ${accessToken}`;
    }
    return config;
  });

  client.interceptors.response.use(
    (response: AxiosResponse<ApiResponse>) => {
      const body = response.data;
      if (body && typeof body === 'object' && body.code && body.code !== ErrorCode.SUCCESS) {
        options.onBizError?.(body.code, body.message);
        return Promise.reject(new ApiError(body.code, body.message, body.requestId, body.traceId));
      }
      return response;
    },
    async (error) => {
      if (error.response) {
        const body: ApiResponse | undefined = error.response.data;
        const status = error.response.status;
        const config: (AxiosRequestConfig & { __retried?: boolean }) | undefined = error.config;

        if (status === 401 && config && !config.__retried && options.refreshAccessToken) {
          const newToken = await singleFlightRefresh(options);
          if (newToken) {
            config.__retried = true;
            setAccessToken(newToken);
            config.headers = config.headers ?? {};
            config.headers.Authorization = `Bearer ${newToken}`;
            return client.request(config);
          }
        }

        if (status === 401) {
          options.onUnauthorized?.();
        }
        if (body && body.code && body.message) {
          options.onBizError?.(body.code, body.message);
          return Promise.reject(new ApiError(body.code, body.message, body.requestId, body.traceId));
        }
        options.onBizError?.(String(status), `请求失败（HTTP ${status}）`);
        return Promise.reject(new ApiError(String(status), `请求失败（HTTP ${status}）`));
      }
      const message = error.message ?? '网络异常，请检查后端服务是否启动';
      options.onNetworkError?.(message);
      return Promise.reject(new ApiError('NETWORK_ERROR', message));
    },
  );

  return client;
}

/** 统一的 API 错误对象，供页面展示 requestId / traceId。 */
export class ApiError extends Error {
  readonly code: string;
  readonly requestId: string | null;
  readonly traceId: string | null;

  constructor(code: string, message: string, requestId?: string | null, traceId?: string | null) {
    super(message);
    this.name = 'ApiError';
    this.code = code;
    this.requestId = requestId ?? null;
    this.traceId = traceId ?? null;
  }
}

export { axios };
export type { AxiosInstance, AxiosRequestConfig };
