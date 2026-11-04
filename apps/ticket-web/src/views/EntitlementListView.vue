<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useMessage } from 'naive-ui';
import { getMyEntitlements } from '../api';
import type { Entitlement, EntitlementStatus } from '../api/types';

const router = useRouter();
const message = useMessage();

const entitlements = ref<Entitlement[]>([]);
const loading = ref(false);

const statusMap: Record<EntitlementStatus, { label: string; cls: string }> = {
  VALID: { label: '有效', cls: 'bg-green-100 text-green-700' },
  USED: { label: '已使用', cls: 'bg-gray-200 text-gray-500' },
  REVOKED: { label: '已撤销', cls: 'bg-red-100 text-red-600' },
};

function fmtTime(t: string | null) {
  if (!t) return '-';
  return t.replace('T', ' ').slice(0, 16);
}

async function load() {
  loading.value = true;
  try {
    const resp = await getMyEntitlements();
    entitlements.value = resp.data;
  } catch (e: any) {
    message.error(e?.response?.data?.message ?? '加载门票失败');
  } finally {
    loading.value = false;
  }
}

function openDetail(id: string) {
  router.push(`/entitlements/${id}`);
}

onMounted(load);
</script>

<template>
  <div class="min-h-screen bg-[var(--bg-page)]">
    <header class="sticky top-0 z-10 border-b border-[var(--border)] bg-white">
      <div class="mx-auto flex h-16 max-w-4xl items-center px-6">
        <button class="text-sm text-[var(--text-secondary)] hover:text-[var(--brand)]" @click="router.back()">
          ← 返回
        </button>
        <div class="mx-auto text-xl font-semibold text-[var(--brand)]">我的门票</div>
      </div>
    </header>

    <main class="mx-auto max-w-4xl px-6 py-10">
      <div v-if="loading" class="py-16 text-center text-[var(--text-tertiary)]">加载中…</div>

      <div v-else-if="entitlements.length" class="space-y-3">
        <div
          v-for="e in entitlements"
          :key="e.id"
          class="cursor-pointer overflow-hidden rounded-xl border border-[var(--border)] bg-white transition hover:shadow"
          @click="openDetail(e.id)"
        >
          <div class="flex items-center justify-between border-b border-dashed border-[var(--border)] px-5 py-4">
            <div class="font-semibold text-[var(--text-primary)]">{{ e.eventTitle }}</div>
            <span class="rounded-full px-3 py-1 text-xs font-medium" :class="statusMap[e.status].cls">
              {{ statusMap[e.status].label }}
            </span>
          </div>
          <div class="flex items-center justify-between px-5 py-4 text-sm">
            <div>
              <div class="text-[var(--text-primary)]">{{ e.tierName }} · 第 {{ e.ticketIndex }} 张</div>
              <div class="mt-1 text-xs text-[var(--text-tertiary)]">
                {{ e.venueName }} · {{ fmtTime(e.sessionStartAt) }}
              </div>
            </div>
            <div class="text-right text-xs text-[var(--text-tertiary)]">
              <div>凭证号</div>
              <div class="mt-1 font-mono text-[var(--text-secondary)]">{{ e.entitlementNo }}</div>
            </div>
          </div>
        </div>
      </div>

      <div v-else class="rounded-xl border border-dashed border-[var(--border)] bg-white p-12 text-center text-[var(--text-tertiary)]">
        暂无门票，支付成功后可在「我的门票」查看凭证
      </div>
    </main>
  </div>
</template>
