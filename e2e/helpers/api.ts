import axios, { AxiosInstance } from 'axios';

/** 后端统一响应信封（见后端 common/api）。 */
export interface ApiResponse<T> {
  code: string;
  message: string;
  data: T;
  requestId: string;
  traceId: string;
  timestamp: string;
}

const BASE = process.env.E2E_API_BASE ?? 'http://localhost:8080/api/v1';

/** 构造后端 API 客户端；可选携带 Bearer Token。 */
export function api(token?: string): AxiosInstance {
  const client = axios.create({ baseURL: BASE, timeout: 20_000, proxy: false });
  if (token) {
    client.defaults.headers.common.Authorization = `Bearer ${token}`;
  }
  return client;
}

/** 断言响应业务成功并解包 data。 */
export async function unwrap<T>(p: Promise<{ data: ApiResponse<T> }>): Promise<T> {
  try {
    const r = await p;
    if (r.data.code !== 'SUCCESS') {
      throw new Error(`API 调用失败 code=${r.data.code} message=${r.data.message}`);
    }
    return r.data.data;
  } catch (e: any) {
    const method = e?.config?.method?.toUpperCase() ?? '?';
    const url = e?.config?.url ?? '?';
    if (e?.response) {
      console.error(
        `[api] HTTP ${e.response.status} ${method} ${url} → ${JSON.stringify(e.response.data)?.slice(0, 300)}`,
      );
    } else {
      console.error(`[api] ${method} ${url} → ${e?.code ?? e?.message}`);
    }
    throw e;
  }
}

/** 随机幂等键：优先 crypto.randomUUID，降级时间戳+随机。 */
export function genId(prefix: string): string {
  const rand =
    typeof crypto !== 'undefined' && (crypto as any).randomUUID
      ? (crypto as any).randomUUID()
      : `${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;
  return `${prefix}-${rand}`;
}
