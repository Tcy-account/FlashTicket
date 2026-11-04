/**
 * M17-02 压测数据准备：
 *   1. 造一个「压测专用活动」（大量库存，供抢票压测）
 *   2. 批量注册 N 个普通用户并登录，输出 token 到 CSV（供 JMeter 读取）
 *   3. 输出压测上下文（tierId / tierName）到 loadtest.json
 *
 * 运行：node loadtest/seed-loadtest.js [用户数]
 */
import { api, unwrap } from '../helpers/api';
import { getVerifyCode } from '../helpers/redis';
import { promises as fs } from 'fs';
import path from 'path';

const SUPERADMIN = {
  account: 'superadmin',
  password: process.env.E2E_SUPERADMIN_PASSWORD ?? 'SuperAdmin@2026',
};
const USER_PASSWORD = 'Loadtest@123';

function fmtUTC(d: Date): string {
  const p = (n: number) => String(n).padStart(2, '0');
  return `${d.getUTCFullYear()}-${p(d.getUTCMonth() + 1)}-${p(d.getUTCDate())}T${p(d.getUTCHours())}:${p(d.getUTCMinutes())}:${p(d.getUTCSeconds())}`;
}
const daysFromNow = (days: number, hours = 0) => fmtUTC(new Date(Date.now() + days * 86_400_000 + hours * 3_600_000));

const USER_COUNT = Number(process.argv[2] ?? 200);

async function createEvent(sa: any, stamp: string): Promise<{ tierId: string; tierName: string }> {
  const roles = await unwrap<any[]>(sa.get('/admin/roles'));
  const operatorRole = roles.find((r: any) => r.code === 'OPERATOR');

  const opName = `ltop_${stamp}`;
  await unwrap(sa.post('/admin/staff', {
    username: opName, email: `${opName}@test.com`, nickname: '压测审核员',
    password: 'LoadtestOp@123', roleIds: [operatorRole.id],
  }));
  const opToken = await unwrap<any>(api().post('/admin/auth/login', { account: opName, password: 'LoadtestOp@123' })).then((d: any) => d.accessToken);
  const op = api(opToken);

  const category = await unwrap<any>(sa.post('/admin/categories', { code: `LT${stamp.toUpperCase()}`, name: '压测分类' }));
  const venue = await unwrap<any>(sa.post('/admin/venues', { name: '压测场馆', city: '上海', address: '压测路 1 号', maxCapacity: 100000 }));
  const event = await unwrap<any>(sa.post('/admin/events', {
    categoryId: category.id, title: `压测演出 ${stamp}`,
    description: 'JMeter 压测专用', coverUrl: 'https://example.com/cover.jpg',
    detailContent: '压测详情', organizerName: '压测主办方', attendanceNotice: '压测须知',
  }));
  const session = await unwrap<any>(sa.post(`/admin/events/${event.id}/sessions`, {
    venueId: venue.id, name: '压测场次', startAt: daysFromNow(7, 19), endAt: daysFromNow(7, 21), saleCapacity: 100000,
  }));
  const tier = await unwrap<any>(sa.post(`/admin/sessions/${session.id}/tiers`, {
    name: '压测票档', unitPriceCent: 10000, initialQuantity: 100000,
    minPurchase: 1, maxPurchase: 5, perUserLimit: 5,
  }));
  await unwrap(sa.put(`/admin/tiers/${tier.id}/sale-plan`, { saleStartAt: daysFromNow(0, -1), saleEndAt: daysFromNow(30) }));
  await unwrap(op.post(`/admin/events/${event.id}/submit-review`));
  await unwrap(sa.post(`/admin/events/${event.id}/review`, { approve: true, comment: '压测通过' }));
  await unwrap(sa.post(`/admin/events/${event.id}/publish`));

  return { tierId: tier.id, tierName: tier.name };
}

async function registerOne(i: number): Promise<string> {
  const username = `lt_${Date.now().toString(36)}_${i}`;
  const email = `${username}@test.com`;
  await unwrap(api().post('/auth/verify-code/send', { email, purpose: 'REGISTER' }));
  const code = await getVerifyCode(email, 'REGISTER');
  if (!code) throw new Error(`未读到验证码 ${email}`);
  await unwrap(api().post('/auth/register', { username, email, verifyCode: code, password: USER_PASSWORD }));
  const token = await unwrap<any>(api().post('/auth/login', { account: email, password: USER_PASSWORD })).then((d: any) => d.accessToken);
  return token;
}

async function main() {
  const stamp = Date.now().toString(36);

  // 1. 超管登录 + 造压测活动
  const saToken = await unwrap<any>(api().post('/admin/auth/login', SUPERADMIN)).then((d: any) => d.accessToken);
  const sa = api(saToken);
  const { tierId, tierName } = await createEvent(sa, stamp);

  // 2. 批量注册用户（分批并发，每批 20）
  const tokens: string[] = [];
  const batch = 20;
  for (let start = 0; start < USER_COUNT; start += batch) {
    const end = Math.min(start + batch, USER_COUNT);
    const idxs = Array.from({ length: end - start }, (_, k) => start + k);
    const results = await Promise.all(idxs.map((i) => registerOne(i)));
    tokens.push(...results);
    console.log(`[seed-loadtest] 已注册 ${end}/${USER_COUNT}`);
  }

  // 3. 输出 CSV（token）与上下文
  const csv = tokens.map((t) => t).join('\n');
  const outDir = path.resolve(__dirname);
  await fs.writeFile(path.join(outDir, 'users.csv'), 'token\n' + csv, 'utf8');
  await fs.writeFile(path.join(outDir, 'loadtest.json'), JSON.stringify({ tierId, tierName, userCount: USER_COUNT }, null, 2), 'utf8');
  console.log(`[seed-loadtest] 完成：${USER_COUNT} 用户，tierId=${tierId}`);
}

main().catch((e) => {
  console.error('[seed-loadtest] 失败：', e?.response?.data ?? e?.message ?? e);
  process.exit(1);
});
