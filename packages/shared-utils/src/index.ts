/** 金额：后端整数「分」→ 前端「元」字符串，保留两位小数。 */
export function fenToYuan(fen: number | bigint | string | null | undefined): string {
  if (fen === null || fen === undefined || fen === '') return '0.00';
  const n = typeof fen === 'bigint' ? Number(fen) : Number(fen);
  if (!Number.isFinite(n)) return '0.00';
  return (n / 100).toFixed(2);
}

/** ISO-8601 UTC 时间 → 本地可读字符串。 */
export function formatDateTime(iso: string | null | undefined): string {
  if (!iso) return '-';
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return '-';
  return d.toLocaleString('zh-CN', { hour12: false });
}
