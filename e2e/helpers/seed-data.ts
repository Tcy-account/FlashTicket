import { promises as fs } from 'fs';
import path from 'path';

/** global-setup 产出的种子数据，供测试用例消费。 */
export interface SeedData {
  /** 普通用户登录账号（邮箱）。 */
  userEmail: string;
  userPassword: string;
  username: string;
  /** 已发布、可抢票的演出。 */
  eventId: string;
  eventTitle: string;
  sessionId: string;
  tierId: string;
  tierName: string;
}

/** seed 文件固定落在 e2e/ 目录下。 */
const SEED_FILE = path.resolve(__dirname, '..', '.seed.json');

export async function saveSeed(data: SeedData): Promise<void> {
  await fs.writeFile(SEED_FILE, JSON.stringify(data, null, 2), 'utf8');
}

export async function loadSeed(): Promise<SeedData> {
  const raw = await fs.readFile(SEED_FILE, 'utf8');
  return JSON.parse(raw) as SeedData;
}
