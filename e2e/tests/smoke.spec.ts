import { test, expect } from '@playwright/test';
import { loadSeed } from '../helpers/seed-data';

/**
 * 用户端关键路径冒烟：登录 → 浏览演出 → 抢票排队 → 订单支付 → 门票权益 → 申请退款。
 *
 * 依赖 global-setup 预置的种子数据（已发布演出 + 普通用户账号）。
 * 抢票为异步（M12），受理返回 QUEUED 后由消费者建单，排队页轮询到 SUCCEEDED 自动跳订单详情。
 */
test('用户端关键路径冒烟：登录→浏览→抢票→支付→退款', async ({ page }) => {
  const seed = await loadSeed();

  // 1. 登录（seed 普通用户）
  await page.goto('/login');
  await page.fill('input[placeholder="用户名或已验证邮箱"]', seed.userEmail);
  await page.fill('input[placeholder="请输入密码"]', seed.userPassword);
  await page.getByRole('button', { name: '登录' }).click();
  // 登录态 header 出现「我的订单」
  await expect(page.getByRole('link', { name: '我的订单' })).toBeVisible();

  // 2. 首页浏览：已发布演出可见（公开列表 published_at DESC，seed 最新）
  await expect(page.getByText(seed.eventTitle).first()).toBeVisible();

  // 3. 进入演出详情
  await page.getByText(seed.eventTitle).first().click();
  await page.waitForURL(`**/events/${seed.eventId}`);
  await expect(page.getByText('场次与票档')).toBeVisible();

  // 4. 抢票（弹窗确认 → 排队页轮询 → 异步建单成功 → 点「去订单详情支付」）
  await page.getByRole('button', { name: '立即抢票' }).first().click();
  await page.getByRole('button', { name: '确认抢票' }).click();
  await expect(page.getByRole('heading', { name: '抢票成功，订单已创建' })).toBeVisible({ timeout: 30_000 });
  await page.getByRole('button', { name: '去订单详情支付' }).click();
  await page.waitForURL(/\/orders\/[0-9A-Za-z]+$/, { timeout: 15_000 });
  const orderId = page.url().split('/orders/')[1];

  // 5. 模拟支付（收银台确认）
  await page.getByRole('button', { name: '立即支付' }).click();
  await page.getByRole('button', { name: '确认支付' }).click();
  await expect(page.getByText('已支付').first()).toBeVisible();

  // 6. 门票权益已出票
  await page.goto('/entitlements');
  await expect(page.getByText(seed.tierName).first()).toBeVisible();

  // 7. 申请退款（全额，进入审核中）
  await page.goto(`/orders/${orderId}`);
  await page.getByRole('button', { name: '申请退款' }).click();
  await page.getByRole('button', { name: '提交申请' }).click();
  await expect(page.getByText('退款审核中').first()).toBeVisible();
});
