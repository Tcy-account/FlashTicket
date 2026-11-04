import Redis from 'ioredis';

const REDIS_HOST = process.env.E2E_REDIS_HOST ?? 'localhost';
const REDIS_PORT = Number(process.env.E2E_REDIS_PORT ?? 16379);

/**
 * 读取邮箱验证码。本地环境验证码走日志（LogVerificationCodeSender），
 * 但值同时落在 Redis（key 形如 auth:verify:code:{PURPOSE}:{email}），
 * 因此 E2E 直接从 Redis 取码完成注册，避免依赖后端日志。
 */
export async function getVerifyCode(email: string, purpose: string): Promise<string | null> {
  const redis = new Redis(REDIS_PORT, REDIS_HOST, { maxRetriesPerRequest: 1 });
  const key = `auth:verify:code:${purpose}:${email.trim().toLowerCase()}`;
  try {
    return await redis.get(key);
  } finally {
    redis.disconnect();
  }
}
