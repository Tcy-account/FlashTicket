<script setup lang="ts">
import { axios } from '@flashticket/api-client';
import { computed, ref } from 'vue';

interface LabEndpoint {
  id: string;
  group: string;
  method: 'GET' | 'POST' | 'PUT' | 'DELETE';
  path: string;
  name: string;
  available: boolean;
  defaultBody?: string;
}

/** 已接入接口清单（真实调用后端，available=false 表示后端该接口尚未实现）。 */
const endpoints: LabEndpoint[] = [
  { id: 'health', group: '系统健康', method: 'GET', path: '/health', name: '存活探针', available: true },
  { id: 'readiness', group: '系统健康', method: 'GET', path: '/health/readiness', name: '就绪探针（DB+Redis）', available: true },
  { id: 'send-code', group: '账号', method: 'POST', path: '/auth/verify-code', name: '发送邮箱验证码', available: false, defaultBody: '{\n  "email": "user@example.com",\n  "purpose": "register"\n}' },
  { id: 'register', group: '账号', method: 'POST', path: '/auth/register', name: '用户注册', available: false, defaultBody: '{\n  "username": "demo_user",\n  "email": "user@example.com",\n  "verifyCode": "123456",\n  "password": "a-strong-password"\n}' },
  { id: 'login', group: '账号', method: 'POST', path: '/auth/login', name: '用户登录', available: false, defaultBody: '{\n  "account": "demo_user",\n  "password": "a-strong-password"\n}' },
  { id: 'refresh', group: '账号', method: 'POST', path: '/auth/refresh', name: '刷新登录', available: false },
  { id: 'me', group: '账号', method: 'GET', path: '/users/me', name: '获取当前用户', available: false },
];

// 裸 axios 实例：绕过业务拦截器，完整展示状态码与原始响应
const labClient = axios.create({ baseURL: '/api/v1', timeout: 10000 });

const activeId = ref<string>('readiness');
const active = computed(() => endpoints.find((e) => e.id === activeId.value)!);
const body = ref<string>('');
const sending = ref(false);
const result = ref<{ status?: number; elapsed?: number; data?: unknown; error?: string } | null>(null);

function selectEndpoint(e: LabEndpoint) {
  activeId.value = e.id;
  body.value = e.defaultBody ?? '';
  result.value = null;
}

async function send() {
  const ep = active.value;
  sending.value = true;
  result.value = null;
  const start = performance.now();
  let parsedBody: unknown;
  if (ep.method !== 'GET' && body.value.trim()) {
    try {
      parsedBody = JSON.parse(body.value);
    } catch {
      result.value = { error: '请求体不是合法 JSON' };
      sending.value = false;
      return;
    }
  }
  try {
    const resp = await labClient.request({
      method: ep.method,
      url: ep.path,
      data: parsedBody,
    });
    result.value = {
      status: resp.status,
      elapsed: performance.now() - start,
      data: resp.data,
    };
  } catch (e: any) {
    if (e.response) {
      result.value = {
        status: e.response.status,
        elapsed: performance.now() - start,
        data: e.response.data,
      };
    } else {
      result.value = { error: e.message ?? '网络异常，请确认后端已启动' };
    }
  } finally {
    sending.value = false;
  }
}

function statusClass(status?: number): string {
  if (!status) return 'text-[var(--text-tertiary)]';
  if (status < 300) return 'text-[var(--success)]';
  if (status < 500) return 'text-[var(--error)]';
  return 'text-[var(--warning)]';
}

const methodColor: Record<string, string> = {
  GET: '#16A34A',
  POST: '#635BFF',
  PUT: '#3B82F6',
  DELETE: '#EF4444',
};

// 初始化：选中默认接口
selectEndpoint(endpoints.find((e) => e.id === 'readiness')!);
</script>

<template>
  <div class="min-h-screen bg-[var(--bg-page)]">
    <header class="border-b border-[var(--border)] bg-white">
      <div class="mx-auto flex h-14 max-w-7xl items-center justify-between px-6">
        <div class="font-semibold text-[var(--brand)]">API Lab · 开发专用</div>
        <router-link to="/" class="text-sm text-[var(--text-secondary)] hover:text-[var(--brand)]">← 返回</router-link>
      </div>
    </header>

    <main class="mx-auto grid max-w-7xl grid-cols-1 gap-6 px-6 py-6 lg:grid-cols-[280px_1fr]">
      <!-- 接口列表 -->
      <aside class="rounded-xl bg-white p-4">
        <div class="mb-3 text-xs text-[var(--text-tertiary)]">接口列表（按模块分类）</div>
        <div v-for="group in [...new Set(endpoints.map((e) => e.group))]" :key="group" class="mb-4">
          <div class="mb-2 text-xs font-medium text-[var(--text-secondary)]">{{ group }}</div>
          <button
            v-for="e in endpoints.filter((x) => x.group === group)"
            :key="e.id"
            class="mb-1 flex w-full items-center gap-2 rounded-lg px-3 py-2 text-left text-sm transition-colors"
            :class="activeId === e.id ? 'bg-[var(--brand-light)]' : 'hover:bg-gray-50'"
            @click="selectEndpoint(e)"
          >
            <span class="w-12 text-xs font-semibold" :style="{ color: methodColor[e.method] }">{{ e.method }}</span>
            <span class="flex-1 truncate">{{ e.name }}</span>
            <span v-if="!e.available" class="text-xs text-[var(--text-tertiary)]">待实现</span>
          </button>
        </div>
      </aside>

      <!-- 请求与响应 -->
      <section class="rounded-xl bg-white p-5">
        <div class="flex items-center gap-3">
          <span class="text-sm font-semibold" :style="{ color: methodColor[active.method] }">{{ active.method }}</span>
          <span class="font-mono text-sm">/api/v1{{ active.path }}</span>
          <span v-if="!active.available" class="rounded bg-gray-100 px-2 py-0.5 text-xs text-[var(--text-tertiary)]">后端待实现</span>
        </div>

        <div v-if="active.method !== 'GET'" class="mt-4">
          <div class="mb-1 text-xs text-[var(--text-secondary)]">请求体（JSON）</div>
          <textarea
            v-model="body"
            rows="6"
            class="w-full rounded-lg border border-[var(--border)] p-3 font-mono text-sm focus:border-[var(--brand)] focus:outline-none"
            spellcheck="false"
          />
        </div>

        <div class="mt-4 flex items-center gap-3">
          <n-button type="primary" :loading="sending" @click="send">发送请求</n-button>
        </div>

        <div v-if="result" class="mt-5">
          <div class="mb-2 flex items-center gap-4 text-sm">
            <span v-if="result.status" class="font-semibold" :class="statusClass(result.status)">
              HTTP {{ result.status }}
            </span>
            <span v-if="result.elapsed !== undefined" class="text-[var(--text-tertiary)]">
              耗时 {{ result.elapsed.toFixed(0) }}ms
            </span>
          </div>
          <div v-if="result.error" class="rounded-lg bg-red-50 p-3 text-sm text-[var(--error)]">
            {{ result.error }}
          </div>
          <pre
            v-else
            class="max-h-96 overflow-auto rounded-lg bg-[var(--brand-dark)] p-4 text-xs leading-relaxed text-green-300"
          >{{ JSON.stringify(result.data, null, 2) }}</pre>
        </div>
        <div v-else class="mt-5 text-sm text-[var(--text-tertiary)]">选择接口并发送请求，响应将在此展示。</div>
      </section>
    </main>
  </div>
</template>
