<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useMessage } from 'naive-ui';
import { getNotifications, markAllNotificationsRead, markNotificationRead } from '../api';
import type { Notification, NotificationType } from '../api/types';
import { useNotificationStore } from '../stores/notification';

const router = useRouter();
const message = useMessage();
const notif = useNotificationStore();

const items = ref<Notification[]>([]);
const nextCursor = ref<string | null>(null);
const loading = ref(false);
const activeType = ref<'ALL' | NotificationType>('ALL');

const typeOptions: { key: 'ALL' | NotificationType; label: string }[] = [
  { key: 'ALL', label: '全部' },
  { key: 'TRANSACTION', label: '交易' },
  { key: 'EVENT', label: '演出' },
  { key: 'MARKETING', label: '营销' },
  { key: 'SYSTEM', label: '系统' },
];

async function load(reset = false) {
  if (loading.value) return;
  loading.value = true;
  try {
    const cursor = reset ? undefined : (nextCursor.value ?? undefined);
    const r = await getNotifications({
      type: activeType.value === 'ALL' ? undefined : activeType.value,
      cursor,
      limit: 20,
    });
    const data = r.data;
    items.value = reset ? data.items : [...items.value, ...data.items];
    nextCursor.value = data.nextCursor;
    notif.unreadCount = data.unreadCount;
  } catch (e: any) {
    message.error(e?.message ?? '加载消息失败');
  } finally {
    loading.value = false;
  }
}

function switchType(key: 'ALL' | NotificationType) {
  activeType.value = key;
  void load(true);
}

async function open(n: Notification) {
  if (!n.read) {
    try {
      await markNotificationRead(n.id);
      n.read = true;
      notif.unreadCount = Math.max(0, notif.unreadCount - 1);
    } catch {
      // 已读标记失败不阻断跳转
    }
  }
  if (!n.bizId) return;
  if (n.bizType === 'PURCHASE') {
    router.push(`/purchases/${n.bizId}`);
  } else {
    router.push(`/orders/${n.bizId}`);
  }
}

async function readAll() {
  try {
    await markAllNotificationsRead();
    notif.unreadCount = 0;
    message.success('已全部标记为已读');
    void load(true);
  } catch (e: any) {
    message.error(e?.message ?? '操作失败');
  }
}

function fmtTime(t: string) {
  const d = new Date(t);
  const now = new Date();
  const sameDay = d.toDateString() === now.toDateString();
  return sameDay
    ? d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', hour12: false })
    : d.toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' });
}

onMounted(() => void load(true));
</script>

<template>
  <div class="min-h-screen bg-[var(--bg-page)]">
    <header class="sticky top-0 z-10 border-b border-[var(--border)] bg-white">
      <div class="mx-auto flex h-16 max-w-4xl items-center justify-between px-6">
        <div class="text-lg font-semibold text-[var(--text-primary)]">消息中心</div>
        <button
          v-if="notif.unreadCount > 0"
          class="text-sm text-[var(--brand)] hover:underline"
          @click="readAll"
        >
          全部标记已读（{{ notif.unreadCount }}）
        </button>
      </div>
    </header>

    <main class="mx-auto max-w-4xl px-6 py-6">
      <!-- 分类 -->
      <div class="flex flex-wrap gap-3">
        <button
          v-for="t in typeOptions"
          :key="t.key"
          class="rounded-full border px-4 py-1.5 text-sm transition"
          :class="activeType === t.key
            ? 'border-[var(--brand)] bg-[var(--brand)] text-white'
            : 'border-[var(--border)] bg-white text-[var(--text-secondary)] hover:border-[var(--brand)]'"
          @click="switchType(t.key)"
        >
          {{ t.label }}
        </button>
      </div>

      <!-- 列表 -->
      <div class="mt-6 overflow-hidden rounded-xl border border-[var(--border)] bg-white">
        <div v-if="loading && !items.length" class="py-16 text-center text-[var(--text-tertiary)]">
          加载中…
        </div>
        <div v-else-if="!items.length" class="py-16 text-center text-[var(--text-tertiary)]">
          暂无消息
        </div>
        <template v-else>
          <div
            v-for="n in items"
            :key="n.id"
            class="flex cursor-pointer items-start gap-3 border-b border-[var(--border)] px-5 py-4 last:border-b-0 hover:bg-[var(--bg-page)]"
            @click="open(n)"
          >
            <span
              class="mt-2 h-2 w-2 shrink-0 rounded-full"
              :class="n.read ? 'bg-transparent' : 'bg-[var(--brand)]'"
            />
            <div class="min-w-0 flex-1">
              <div class="flex items-center justify-between gap-3">
                <div class="truncate font-medium text-[var(--text-primary)]">{{ n.title }}</div>
                <div class="shrink-0 text-xs text-[var(--text-tertiary)]">{{ fmtTime(n.receivedAt) }}</div>
              </div>
              <div class="mt-1 text-sm leading-relaxed text-[var(--text-secondary)]">{{ n.content }}</div>
            </div>
          </div>
          <div v-if="nextCursor" class="py-4 text-center">
            <button
              class="text-sm text-[var(--brand)] hover:underline"
              :disabled="loading"
              @click.stop="load(false)"
            >
              {{ loading ? '加载中…' : '加载更多' }}
            </button>
          </div>
        </template>
      </div>
    </main>
  </div>
</template>
