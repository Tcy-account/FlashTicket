<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useMessage } from 'naive-ui';
import { getCategories, getEvents, getVenues, logout as apiLogout } from '../api';
import type { Category, Event, Venue } from '../api/types';
import { useAuthStore } from '../stores/auth';
import { useNotificationStore } from '../stores/notification';

const auth = useAuthStore();
const router = useRouter();
const message = useMessage();
const notif = useNotificationStore();

const categories = ref<Category[]>([]);
const venues = ref<Venue[]>([]);
const events = ref<Event[]>([]);
const selectedCategoryId = ref<string | null>(null);
const loading = ref(false);

const categoryName = computed(() => {
  const map = new Map(categories.value.map((c) => [c.id, c.name]));
  return (id: string) => map.get(id) ?? '';
});

const filteredEvents = computed(() => {
  if (!selectedCategoryId.value) return events.value;
  return events.value.filter((e) => e.categoryId === selectedCategoryId.value);
});

async function loadCatalog() {
  loading.value = true;
  try {
    const [catResp, venueResp, eventResp] = await Promise.all([
      getCategories(),
      getVenues({ page: 1, pageSize: 12 }),
      getEvents({ page: 1, pageSize: 30 }),
    ]);
    categories.value = catResp.data;
    venues.value = venueResp.data.items;
    events.value = eventResp.data.items;
  } catch (e: any) {
    message.error(e?.message ?? '加载演出目录失败');
  } finally {
    loading.value = false;
  }
}

function selectCategory(id: string | null) {
  selectedCategoryId.value = id;
}

function openEvent(id: string) {
  router.push(`/events/${id}`);
}

async function onLogout() {
  try {
    await apiLogout();
  } catch {
    // 即使后端退出失败也清本地态
  }
  auth.logout();
  message.success('已退出登录');
  router.push('/login');
}

onMounted(loadCatalog);
</script>

<template>
  <div class="min-h-screen bg-[var(--bg-page)]">
    <header class="sticky top-0 z-10 border-b border-[var(--border)] bg-white">
      <div class="mx-auto flex h-16 max-w-6xl items-center justify-between px-6">
        <div class="text-xl font-semibold text-[var(--brand)]">FlashTicket</div>
        <nav class="flex items-center gap-6 text-sm">
          <router-link to="/dev/api-lab" class="text-[var(--text-secondary)] hover:text-[var(--brand)]">
            API Lab
          </router-link>
          <template v-if="auth.isLoggedIn">
            <router-link to="/orders" class="text-[var(--text-secondary)] hover:text-[var(--brand)]">
              我的订单
            </router-link>
            <router-link to="/entitlements" class="text-[var(--text-secondary)] hover:text-[var(--brand)]">
              我的门票
            </router-link>
            <router-link to="/coupons" class="text-[var(--text-secondary)] hover:text-[var(--brand)]">
              优惠券
            </router-link>
            <router-link to="/notifications" class="relative text-[var(--text-secondary)] hover:text-[var(--brand)]">
              消息
              <span
                v-if="notif.unreadCount > 0"
                class="absolute -right-3 -top-1 min-w-[16px] rounded-full bg-[var(--brand)] px-1 text-center text-[10px] leading-4 text-white"
              >
                {{ notif.unreadCount > 99 ? '99+' : notif.unreadCount }}
              </span>
            </router-link>
            <router-link to="/profile" class="text-[var(--text-secondary)] hover:text-[var(--brand)]">
              {{ auth.user?.nickname ?? auth.user?.username }}
            </router-link>
            <button class="text-[var(--text-secondary)] hover:text-[var(--brand)]" @click="onLogout">
              退出
            </button>
          </template>
          <router-link v-else to="/login" class="text-[var(--brand)]">登录</router-link>
        </nav>
      </div>
    </header>

    <main class="mx-auto max-w-6xl px-6 py-12">
      <h1 class="text-3xl font-bold">精彩演出</h1>
      <p class="mt-2 text-[var(--text-secondary)]">发现正在售票的演出活动</p>

      <!-- 分类筛选 -->
      <section v-if="categories.length" class="mt-8">
        <div class="flex flex-wrap gap-3">
          <button
            class="rounded-full border px-4 py-1.5 text-sm transition"
            :class="selectedCategoryId === null
              ? 'border-[var(--brand)] bg-[var(--brand)] text-white'
              : 'border-[var(--border)] bg-white text-[var(--text-secondary)] hover:border-[var(--brand)]'"
            @click="selectCategory(null)"
          >
            全部
          </button>
          <button
            v-for="c in categories"
            :key="c.id"
            class="rounded-full border px-4 py-1.5 text-sm transition"
            :class="selectedCategoryId === c.id
              ? 'border-[var(--brand)] bg-[var(--brand)] text-white'
              : 'border-[var(--border)] bg-white text-[var(--text-secondary)] hover:border-[var(--brand)]'"
            @click="selectCategory(c.id)"
          >
            {{ c.name }}
          </button>
        </div>
      </section>

      <!-- 演出卡片 -->
      <section class="mt-8">
        <div v-if="loading" class="py-16 text-center text-[var(--text-tertiary)]">加载中…</div>
        <div v-else-if="filteredEvents.length" class="grid gap-6 md:grid-cols-3">
          <div
            v-for="e in filteredEvents"
            :key="e.id"
            class="cursor-pointer overflow-hidden rounded-xl border border-[var(--border)] bg-white transition hover:shadow-lg"
            @click="openEvent(e.id)"
          >
            <div class="flex h-40 items-center justify-center bg-gradient-to-br from-[var(--brand)] to-[#7c3aed] text-white">
              <span class="text-lg font-semibold">{{ e.title.slice(0, 2) }}</span>
            </div>
            <div class="p-5">
              <div class="text-xs text-[var(--brand)]">{{ categoryName(e.categoryId) }}</div>
              <div class="mt-1 font-semibold text-[var(--text-primary)]">{{ e.title }}</div>
              <div v-if="e.subtitle" class="mt-1 truncate text-sm text-[var(--text-tertiary)]">{{ e.subtitle }}</div>
              <div v-if="e.organizerName" class="mt-3 text-xs text-[var(--text-tertiary)]">{{ e.organizerName }}</div>
            </div>
          </div>
        </div>
        <div v-else class="py-16 text-center text-[var(--text-tertiary)]">
          暂无演出，敬请期待
        </div>
      </section>

      <!-- 场馆展示 -->
      <section v-if="venues.length" class="mt-12">
        <h2 class="mb-4 text-lg font-semibold text-[var(--text-primary)]">合作场馆</h2>
        <div class="grid gap-4 md:grid-cols-3">
          <div
            v-for="v in venues"
            :key="v.id"
            class="rounded-xl border border-[var(--border)] bg-white p-5"
          >
            <div class="font-semibold text-[var(--text-primary)]">{{ v.name }}</div>
            <div class="mt-1 text-sm text-[var(--text-tertiary)]">
              {{ v.province ? `${v.province} · ` : '' }}{{ v.city }}{{ v.district ? ` · ${v.district}` : '' }}
            </div>
            <div class="mt-3 text-xs text-[var(--text-tertiary)]">核定容量 {{ v.maxCapacity }} 人</div>
          </div>
        </div>
      </section>
    </main>
  </div>
</template>
