import { defineConfig, devices } from '@playwright/test';

/**
 * FlashTicket 前端 E2E 配置。
 *
 * 前置条件（不在 Playwright 内管理的部分，需外部先起）：
 *   1. Docker 中间件：docker compose up -d（MySQL/Redis/RocketMQ）
 *   2. 后端：cd backend && mvn spring-boot:run（8080，local profile）
 * 前端 ticket-web dev server（5173）由本文件的 webServer 自动拉起。
 *
 * 可覆盖环境变量：
 *   E2E_BASE_URL            前端地址（默认 http://localhost:5173）
 *   E2E_API_BASE            后端地址（默认 http://localhost:8080/api/v1）
 *   E2E_REDIS_HOST/PORT     Redis（默认 localhost:16379）
 *   E2E_SUPERADMIN_PASSWORD 超管密码（默认读 application.yml bootstrap 值）
 */
export default defineConfig({
  testDir: './tests',
  globalSetup: './global-setup',
  timeout: 90_000,
  expect: { timeout: 15_000 },
  // 冒烟测试共享同一账号/订单链路，串行执行避免限流与数据相互干扰
  fullyParallel: false,
  workers: 1,
  retries: 0,
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL: process.env.E2E_BASE_URL ?? 'http://localhost:5173',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'off',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
  webServer: {
    command: 'pnpm --filter @flashticket/ticket-web dev',
    cwd: '..',
    url: 'http://localhost:5173',
    reuseExistingServer: !process.env.CI,
    timeout: 120_000,
  },
});
