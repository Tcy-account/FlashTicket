<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useMessage } from 'naive-ui';
import { getMyEntitlement } from '../api';
import type { Entitlement, EntitlementStatus } from '../api/types';

const route = useRoute();
const router = useRouter();
const message = useMessage();

const entitlementId = route.params.id as string;
const entitlement = ref<Entitlement | null>(null);
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
    const resp = await getMyEntitlement(entitlementId);
    entitlement.value = resp.data;
  } catch (e: any) {
    message.error(e?.response?.data?.message ?? '加载凭证失败');
  } finally {
    loading.value = false;
  }
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
        <div class="mx-auto text-xl font-semibold text-[var(--brand)]">票务凭证</div>
      </div>
    </header>

    <main v-if="entitlement" class="mx-auto max-w-md px-6 py-10">
      <!-- 电子票卡片 -->
      <div class="overflow-hidden rounded-2xl border border-[var(--border)] bg-white shadow-sm">
        <div class="bg-gradient-to-br from-[var(--brand)] to-[#7c3aed] px-6 py-6 text-white">
          <div class="text-xs opacity-80">{{ entitlement.venueName }}</div>
          <div class="mt-1 text-2xl font-bold">{{ entitlement.eventTitle }}</div>
          <div class="mt-2 text-sm opacity-90">
            {{ fmtTime(entitlement.sessionStartAt) }}
          </div>
        </div>

        <div class="space-y-4 px-6 py-6">
          <div class="flex items-center justify-between">
            <div>
              <div class="text-xs text-[var(--text-tertiary)]">票档</div>
              <div class="mt-1 text-lg font-semibold text-[var(--text-primary)]">{{ entitlement.tierName }}</div>
            </div>
            <div class="text-right">
              <div class="text-xs text-[var(--text-tertiary)]">票序号</div>
              <div class="mt-1 text-lg font-semibold text-[var(--text-primary)]">第 {{ entitlement.ticketIndex }} 张</div>
            </div>
          </div>

          <div class="rounded-lg bg-[var(--bg-page)] p-4 text-center">
            <div class="text-xs text-[var(--text-tertiary)]">凭证号</div>
            <div class="mt-1 font-mono text-lg tracking-wider text-[var(--brand)]">{{ entitlement.entitlementNo }}</div>
          </div>

          <div class="flex items-center justify-between">
            <span class="text-sm text-[var(--text-tertiary)]">出票时间</span>
            <span class="text-sm text-[var(--text-primary)]">{{ fmtTime(entitlement.issuedAt) }}</span>
          </div>
          <div class="flex items-center justify-between">
            <span class="text-sm text-[var(--text-tertiary)]">权益状态</span>
            <span class="rounded-full px-3 py-1 text-xs font-medium" :class="statusMap[entitlement.status].cls">
              {{ statusMap[entitlement.status].label }}
            </span>
          </div>
        </div>

        <div class="border-t border-dashed border-[var(--border)] px-6 py-4 text-center text-xs text-[var(--text-tertiary)]">
          此凭证仅供本人使用，入场时请出示凭证号
        </div>
      </div>
    </main>

    <main v-else-if="loading" class="mx-auto max-w-md px-6 py-20 text-center text-[var(--text-tertiary)]">
      加载中…
    </main>
  </div>
</template>
