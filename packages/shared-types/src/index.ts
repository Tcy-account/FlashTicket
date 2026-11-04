// FlashTicket 前后端共享类型（契约见 doc/M00-工程初始化.md 3.1 / 3.2）
// 前端一律以此文件为准解析后端响应，避免字段不一致。

/** 统一响应信封，与后端 ApiResponse<T> 逐字段对应。 */
export interface ApiResponse<T = unknown> {
  /** 业务码，大写语义码，如 SUCCESS / PARAM_ERROR，非 HTTP 状态码 */
  code: string;
  /** 人可读提示，简体中文 */
  message: string;
  /** 业务数据 */
  data: T;
  /** 本次请求业务号 */
  requestId: string | null;
  /** 链路追踪 ID，仅观测用途 */
  traceId: string | null;
  /** ISO-8601 UTC 时间戳 */
  timestamp: string;
}

/** 分页数据结构（列表接口统一）。 */
export interface Page<T> {
  items: T[];
  total: number;
  page: number;
  pageSize: number;
}

/** 健康检查状态。 */
export type HealthStatus = 'UP' | 'DOWN';

/** 通用错误码（与后端 ErrorCode 枚举一致，用于前端分支处理）。 */
export const ErrorCode = {
  SUCCESS: 'SUCCESS',
  PARAM_ERROR: 'PARAM_ERROR',
  UNAUTHORIZED: 'UNAUTHORIZED',
  FORBIDDEN: 'FORBIDDEN',
  NOT_FOUND: 'NOT_FOUND',
  CONFLICT: 'CONFLICT',
  TOO_MANY_REQUESTS: 'TOO_MANY_REQUESTS',
  INTERNAL_ERROR: 'INTERNAL_ERROR',
  SERVICE_UNAVAILABLE: 'SERVICE_UNAVAILABLE',
} as const;

export type ErrorCodeValue = (typeof ErrorCode)[keyof typeof ErrorCode];
