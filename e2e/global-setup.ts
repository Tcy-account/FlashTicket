import { api, unwrap, genId } from './helpers/api';
import { getVerifyCode } from './helpers/redis';
import { saveSeed, SeedData } from './helpers/seed-data';

/** 超管账号（SuperAdminBootstrap 引导创建，密码优先读 E2E_SUPERADMIN_PASSWORD；未设时用明显测试占位（须与本地 SUPER_ADMIN_PASSWORD 一致））。 */
const SUPERADMIN = {
  account: 'superadmin',
  password: process.env.E2E_SUPERADMIN_PASSWORD ?? 'TestOnly_SuperAdmin_ChangeMe',
};

/** 员工/用户密码（满足 12-64 位规则）。 */
const OP_PASSWORD = 'E2eOp@123456';
const USER_PASSWORD = 'E2eUser@12345';

/** UTC 朴素时间（后端 DATETIME 按 UTC 存储与比较，必须用 UTC 而非本地时区）。 */
function fmt(d: Date): string {
  const p = (n: number) => String(n).padStart(2, '0');
  return `${d.getUTCFullYear()}-${p(d.getUTCMonth() + 1)}-${p(d.getUTCDate())}T${p(d.getUTCHours())}:${p(d.getUTCMinutes())}:${p(d.getUTCSeconds())}`;
}

function daysFromNow(days: number, hours = 0): string {
  const d = new Date(Date.now() + days * 86_400_000 + hours * 3_600_000);
  return fmt(d);
}

async function main(): Promise<void> {
  const stamp = Date.now().toString(36);

  // 1. 超管登录
  const saToken = await unwrap<any>(api().post('/admin/auth/login', SUPERADMIN)).then((d) => d.accessToken);
  const sa = api(saToken);

  // 2. 查 OPERATOR 角色 id（审核提交人）
  const roles = await unwrap<any[]>(sa.get('/admin/roles'));
  const operatorRole = roles.find((r) => r.code === 'OPERATOR');
  if (!operatorRole) throw new Error('未找到 OPERATOR 角色，检查 RBAC 种子数据');

  // 3. 造审核员工（operator：负责提交审核）
  const opName = `e2eop_${stamp}`;
  await unwrap(sa.post('/admin/staff', {
    username: opName,
    email: `${opName}@test.com`,
    nickname: 'E2E审核员',
    password: OP_PASSWORD,
    roleIds: [operatorRole.id],
  }));
  const opToken = await unwrap<any>(api().post('/admin/auth/login', { account: opName, password: OP_PASSWORD })).then((d) => d.accessToken);
  const op = api(opToken);

  // 4. 造分类（分类码：大写字母开头，2-32 位大写字母/数字/下划线）
  const category = await unwrap<any>(sa.post('/admin/categories', {
    code: `E2E${stamp.toUpperCase()}`,
    name: 'E2E 测试分类',
  }));

  // 5. 造场馆
  const venue = await unwrap<any>(sa.post('/admin/venues', {
    name: 'E2E 测试场馆',
    city: '上海',
    address: '测试路 1 号',
    maxCapacity: 500,
  }));

  // 6. 造活动（发布要求封面/详情/主办方/观演须知必填）
  const eventTitle = `E2E 冒烟演出 ${stamp}`;
  const event = await unwrap<any>(sa.post('/admin/events', {
    categoryId: category.id,
    title: eventTitle,
    description: 'Playwright 冒烟测试专用演出',
    coverUrl: 'https://example.com/cover.jpg',
    detailContent: 'E2E 测试演出详情内容',
    organizerName: 'E2E 主办方',
    attendanceNotice: '请提前 30 分钟入场',
    refundPolicy: '演出前 72 小时可退票',
  }));

  // 7. 造场次（7 天后开演）
  const session = await unwrap<any>(sa.post(`/admin/events/${event.id}/sessions`, {
    venueId: venue.id,
    name: 'E2E 场次',
    startAt: daysFromNow(7, 19),
    endAt: daysFromNow(7, 21),
    saleCapacity: 100,
  }));

  // 8. 造票档（100 元 / 张，初始 50 张）
  const tier = await unwrap<any>(sa.post(`/admin/sessions/${session.id}/tiers`, {
    name: 'E2E 看台票',
    unitPriceCent: 10000,
    initialQuantity: 50,
    minPurchase: 1,
    maxPurchase: 2,
    perUserLimit: 2,
  }));

  // 9. 售票计划：已开售（过去 1h ~ 未来 30d）
  await unwrap(sa.put(`/admin/tiers/${tier.id}/sale-plan`, {
    saleStartAt: daysFromNow(0, -1),
    saleEndAt: daysFromNow(30),
  }));

  // 10. operator 提交审核 → 超管审核通过 → 超管发布（审核人 ≠ 提交人）
  await unwrap(op.post(`/admin/events/${event.id}/submit-review`));
  await unwrap(sa.post(`/admin/events/${event.id}/review`, { approve: true, comment: 'E2E 通过' }));
  await unwrap(sa.post(`/admin/events/${event.id}/publish`));

  // 11. 注册普通用户（发验证码 → Redis 取码 → 注册）
  const username = `e2e_${stamp}`;
  const userEmail = `${username}@test.com`;
  await unwrap(api().post('/auth/verify-code/send', { email: userEmail, purpose: 'REGISTER' }));
  const code = await getVerifyCode(userEmail, 'REGISTER');
  if (!code) throw new Error(`未从 Redis 读到验证码（email=${userEmail}），确认 Redis 可达且 key 正确`);
  await unwrap(api().post('/auth/register', {
    username,
    email: userEmail,
    verifyCode: code,
    password: USER_PASSWORD,
  }));

  // 12. 落盘 seed 供测试读取
  const seed: SeedData = {
    userEmail,
    userPassword: USER_PASSWORD,
    username,
    eventId: event.id,
    eventTitle,
    sessionId: session.id,
    tierId: tier.id,
    tierName: tier.name,
  };
  await saveSeed(seed);
  console.log('[global-setup] E2E 种子数据就绪：', JSON.stringify(seed));
}

export default main;
