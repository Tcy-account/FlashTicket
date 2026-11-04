<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useMessage } from 'naive-ui';
import { checkout, getEvent, getEventSessions, getMyCoupons, getSessionTiers, getTierAvailability, getVenues } from '../api';
import type { Event, SalePlan, Session, TicketTier, TierAvailability, UserCoupon, Venue } from '../api/types';
import { useAuthStore } from '../stores/auth';

const route = useRoute();
const router = useRouter();
const message = useMessage();
const auth = useAuthStore();

const eventId = route.params.id as string;
const event = ref<Event | null>(null);
const sessions = ref<Session[]>([]);
const venues = ref<Venue[]>([]);
const tiersBySession = ref<Record<string, TicketTier[]>>({});
const availabilityByTier = ref<Record<string, TierAvailability>>({});
const loading = ref(false);

// 抢票弹窗状态
const checkoutTier = ref<TicketTier | null>(null);
const checkoutQty = ref(1);
const checkoutLoading = ref(false);

// 下单选券状态（M13）
const myCoupons = ref<UserCoupon[]>([]);
const selectedCouponId = ref<string | null>(null);
const couponsLoading = ref(false);

// 倒计时基准时间（每秒刷新）
const now = ref(Date.now());
let timer: ReturnType<typeof setInterval> | null = null;

const venueName = (id: string) => venues.value.find((v) => v.id === id)?.name ?? id;

function fmtTime(t: string | null) {
  if (!t) return '-';
  return t.replace('T', ' ').slice(0, 16);
}

const fmtCent = (c: number) => `¥${(c / 100).toFixed(2)}`;

/** 当前选券对指定原价可抵扣的优惠金额（前端预览，实际以服务端计算为准）。 */
function discountOf(c: UserCoupon | null, original: number): number {
  if (!c) return 0;
  if (c.type === 'FULL_REDUCTION') {
    if (original < c.thresholdCent) return 0;
    return Math.min(c.discountCent, original);
  }
  const d = Math.floor((original * c.discountRate) / 10000);
  return c.maxDiscountCent > 0 ? Math.min(d, c.maxDiscountCent) : d;
}

/** 当前订单原价与应付金额。 */
const originalAmount = () => (checkoutTier.value ? checkoutTier.value.unitPriceCent * checkoutQty.value : 0);
const discountAmount = () => discountOf(selectedCoupon(), originalAmount());
const payableAmount = () => Math.max(0, originalAmount() - discountAmount());

function selectedCoupon(): UserCoupon | null {
  return myCoupons.value.find((c) => c.id === selectedCouponId.value) ?? null;
}

/** 券在选券下拉中的展示文案。 */
function couponLabel(c: UserCoupon): string {
  if (c.type === 'FULL_REDUCTION') {
    const threshold = c.thresholdCent > 0 ? `满${fmtCent(c.thresholdCent)}` : '';
    return `${c.templateName}（${threshold}减${fmtCent(c.discountCent)}）`;
  }
  return `${c.templateName}（${(c.discountRate / 100).toFixed(1).replace(/\.0$/, '')}折）`;
}

/** 选券下拉选项（未达门槛的券禁用）。 */
function couponOptions() {
  const opts: { label: string; value: string; disabled?: boolean }[] = [
    { label: '不使用优惠券', value: '' },
  ];
  for (const c of myCoupons.value) {
    opts.push({ label: couponLabel(c), value: c.id, disabled: originalAmount() < c.thresholdCent });
  }
  return opts;
}

/** 售票状态：未开售 / 抢票中 / 已结束 / 已暂停。 */
function saleState(p: SalePlan | null): { label: string; color: string } {
  if (!p) return { label: '待开售', color: 'default' };
  if (p.saleStatus === 'PAUSED') return { label: '已暂停', color: 'error' };
  const start = new Date(p.saleStartAt).getTime();
  const end = new Date(p.saleEndAt).getTime();
  if (now.value < start) return { label: '未开售', color: 'warning' };
  if (now.value >= end) return { label: '已结束', color: 'default' };
  return { label: '抢票中', color: 'success' };
}

/** 距开售倒计时文案；非未开售状态返回空串。 */
function countdown(p: SalePlan | null): string {
  if (!p || p.saleStatus === 'PAUSED') return '';
  const start = new Date(p.saleStartAt).getTime();
  const diff = start - now.value;
  if (diff <= 0) return '';
  const d = Math.floor(diff / 86400000);
  const h = Math.floor((diff % 86400000) / 3600000);
  const m = Math.floor((diff % 3600000) / 60000);
  const s = Math.floor((diff % 60000) / 1000);
  return `距开售 ${d} 天 ${h} 时 ${m} 分 ${s} 秒`;
}

/** 抢票数量上限：每单限购与实时余量取较小值（至少 1）。 */
function maxQty(t: TicketTier): number {
  const avail = availabilityByTier.value[t.id]?.availableQuantity;
  const cap = avail != null ? Math.min(t.maxPurchase, avail) : t.maxPurchase;
  return Math.max(1, cap);
}

/** 弹窗关闭时清空选中的票档（v-model 表达式的等价写法）。 */
function onModalShowChange(show: boolean) {
  if (!show) checkoutTier.value = null;
}

function openCheckout(t: TicketTier) {  if (!auth.isLoggedIn) {
    message.warning('请先登录后抢票');
    router.push({ path: '/login', query: { redirect: route.fullPath } });
    return;
  }
  checkoutTier.value = t;
  checkoutQty.value = 1;
  selectedCouponId.value = null;
  myCoupons.value = [];
  loadAvailableCoupons();
}

/** 加载当前用户可用券，供下单选券。 */
async function loadAvailableCoupons() {
  couponsLoading.value = true;
  try {
    const r = await getMyCoupons('AVAILABLE');
    myCoupons.value = r.data ?? [];
  } catch {
    myCoupons.value = [];
  } finally {
    couponsLoading.value = false;
  }
}

async function submitCheckout() {
  const tier = checkoutTier.value;
  if (!tier) return;
  checkoutLoading.value = true;
  try {
    const requestId = genUuid();
    const resp = await checkout({
      requestId,
      tierId: tier.id,
      quantity: checkoutQty.value,
      selectedCouponId: selectedCouponId.value ? selectedCouponId.value : undefined,
    });
    checkoutTier.value = null;
    // M12 异步抢票：受理成功返回 QUEUED，跳排队页轮询最终状态；幂等命中已建单则直接进订单
    if (resp.data.status === 'SUCCEEDED' && resp.data.order) {
      message.success('抢票成功，订单已创建');
      router.push(`/orders/${resp.data.order.id}`);
    } else {
      router.push(`/purchases/${resp.data.requestId}`);
    }
  } catch (e: any) {
    message.error(e?.response?.data?.message ?? '抢票失败，请稍后重试');
  } finally {
    checkoutLoading.value = false;
  }
}

/** 幂等键：优先 crypto.randomUUID，降级时间戳+随机数。 */
function genUuid(): string {
  if (typeof crypto !== 'undefined' && crypto.randomUUID) {
    return crypto.randomUUID();
  }
  return `${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;
}

async function load() {
  loading.value = true;
  try {
    const [evResp, ssResp, vnResp] = await Promise.all([
      getEvent(eventId),
      getEventSessions(eventId),
      getVenues({ page: 1, pageSize: 200 }),
    ]);
    event.value = evResp.data;
    sessions.value = ssResp.data;
    venues.value = vnResp.data.items;

    // 并行加载每个场次的票档
    const map: Record<string, TicketTier[]> = {};
    await Promise.all(
      ssResp.data.map(async (s) => {
        try {
          const r = await getSessionTiers(s.id);
          map[s.id] = r.data ?? [];
        } catch {
          map[s.id] = [];
        }
      }),
    );
    tiersBySession.value = map;

    // 并行加载每个票档的可售状态与库存余量（M07）
    const avail: Record<string, TierAvailability> = {};
    await Promise.all(
      Object.values(map)
        .flat()
        .map(async (t) => {
          try {
            const r = await getTierAvailability(t.id);
            avail[t.id] = r.data;
          } catch {
            // 忽略，界面用 initialQuantity 兜底
          }
        }),
    );
    availabilityByTier.value = avail;
  } catch (e: any) {
    message.error(e?.response?.data?.message ?? '加载演出详情失败');
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  load();
  timer = setInterval(() => {
    now.value = Date.now();
  }, 1000);
});

onUnmounted(() => {
  if (timer) clearInterval(timer);
});
</script>

<template>
  <div class="min-h-screen bg-[var(--bg-page)]">
    <header class="sticky top-0 z-10 border-b border-[var(--border)] bg-white">
      <div class="mx-auto flex h-16 max-w-4xl items-center px-6">
        <button class="text-sm text-[var(--text-secondary)] hover:text-[var(--brand)]" @click="router.back()">
          ← 返回
        </button>
        <div class="mx-auto text-xl font-semibold text-[var(--brand)]">演出详情</div>
      </div>
    </header>

    <main v-if="event" class="mx-auto max-w-4xl px-6 py-10">
      <div class="rounded-2xl border border-[var(--border)] bg-white p-8">
        <h1 class="text-2xl font-bold text-[var(--text-primary)]">{{ event.title }}</h1>
        <div v-if="event.subtitle" class="mt-1 text-[var(--text-secondary)]">{{ event.subtitle }}</div>

        <div class="mt-4 flex flex-wrap gap-4 text-sm text-[var(--text-tertiary)]">
          <span v-if="event.organizerName">主办方：{{ event.organizerName }}</span>
          <span v-if="event.publishedAt">上架时间：{{ fmtTime(event.publishedAt) }}</span>
        </div>

        <div v-if="event.description" class="mt-6 text-[var(--text-secondary)]">{{ event.description }}</div>

        <div v-if="event.attendanceNotice" class="mt-6">
          <h3 class="mb-2 font-semibold text-[var(--text-primary)]">观演须知</h3>
          <p class="whitespace-pre-wrap text-sm text-[var(--text-secondary)]">{{ event.attendanceNotice }}</p>
        </div>

        <div v-if="event.refundPolicy" class="mt-6">
          <h3 class="mb-2 font-semibold text-[var(--text-primary)]">退票规则</h3>
          <p class="whitespace-pre-wrap text-sm text-[var(--text-secondary)]">{{ event.refundPolicy }}</p>
        </div>
      </div>

      <section class="mt-8">
        <h2 class="mb-4 text-lg font-semibold text-[var(--text-primary)]">场次与票档</h2>
        <div v-if="sessions.length" class="space-y-4">
          <div
            v-for="s in sessions"
            :key="s.id"
            class="rounded-xl border border-[var(--border)] bg-white p-5"
          >
            <div class="flex items-center justify-between">
              <div>
                <div class="font-semibold text-[var(--text-primary)]">{{ s.name }}</div>
                <div class="mt-1 text-sm text-[var(--text-tertiary)]">
                  {{ venueName(s.venueId) }} · {{ fmtTime(s.startAt) }} - {{ fmtTime(s.endAt) }}
                </div>
              </div>
              <div class="text-right text-xs text-[var(--text-tertiary)]">
                可售 {{ s.saleCapacity }} 张
              </div>
            </div>

            <!-- 票档列表 -->
            <div v-if="tiersBySession[s.id]?.length" class="mt-4 space-y-2">
              <div
                v-for="t in tiersBySession[s.id]"
                :key="t.id"
                class="flex items-center justify-between rounded-lg border border-[var(--border)] bg-[var(--bg-page)] px-4 py-3"
              >
                <div>
                  <div class="font-medium text-[var(--text-primary)]">{{ t.name }}</div>
                  <div class="mt-0.5 text-xs text-[var(--text-tertiary)]">
                    剩余 {{ availabilityByTier[t.id]?.availableQuantity ?? t.initialQuantity }} 张 · 每单限 {{ t.maxPurchase }} 张 · 每人限 {{ t.perUserLimit }} 张
                  </div>
                </div>
                <div class="flex items-center gap-3">
                  <div class="text-right">
                    <div class="font-semibold text-[var(--brand)]">{{ fmtCent(t.unitPriceCent) }}</div>
                    <div class="text-xs" :class="saleState(t.salePlan).color === 'error' ? 'text-red-500' : saleState(t.salePlan).color === 'success' ? 'text-green-600' : saleState(t.salePlan).color === 'warning' ? 'text-amber-500' : 'text-[var(--text-tertiary)]'">
                      {{ countdown(t.salePlan) || saleState(t.salePlan).label }}
                    </div>
                  </div>
                  <button
                    class="rounded-lg px-4 py-2 text-sm font-medium text-white transition"
                    :class="saleState(t.salePlan).label === '抢票中' ? 'bg-[var(--brand)] hover:opacity-90' : 'cursor-not-allowed bg-gray-300'"
                    :disabled="saleState(t.salePlan).label !== '抢票中'"
                    @click="openCheckout(t)"
                  >
                    立即抢票
                  </button>
                </div>
              </div>
            </div>
            <div v-else class="mt-4 text-sm text-[var(--text-tertiary)]">该场次暂无票档</div>
          </div>
        </div>
        <div v-else-if="!loading" class="rounded-xl border border-dashed border-[var(--border)] bg-white p-8 text-center text-[var(--text-tertiary)]">
          暂无场次信息
        </div>
      </section>
    </main>

    <main v-else-if="loading" class="mx-auto max-w-4xl px-6 py-20 text-center text-[var(--text-tertiary)]">
      加载中…
    </main>

    <!-- 抢票数量选择弹窗 -->
    <n-modal :show="checkoutTier != null" @update:show="onModalShowChange" preset="card" title="确认抢票" style="width: 420px">
      <div v-if="checkoutTier" class="space-y-4">
        <div class="rounded-lg bg-[var(--bg-page)] p-4 text-sm">
          <div class="font-medium text-[var(--text-primary)]">{{ checkoutTier.name }}</div>
          <div class="mt-1 text-[var(--brand)]">{{ fmtCent(checkoutTier.unitPriceCent) }} / 张</div>
          <div class="mt-1 text-xs text-[var(--text-tertiary)]">
            每单限 {{ checkoutTier.maxPurchase }} 张 · 每人限 {{ checkoutTier.perUserLimit }} 张
          </div>
        </div>
        <div class="flex items-center justify-between">
          <span class="text-sm text-[var(--text-secondary)]">购买数量</span>
          <div class="flex items-center gap-2">
            <button
              class="h-8 w-8 rounded border border-[var(--border)] text-lg leading-none disabled:opacity-40"
              :disabled="checkoutQty <= 1"
              @click="checkoutQty--"
            >
              −
            </button>
            <span class="w-8 text-center font-semibold text-[var(--text-primary)]">{{ checkoutQty }}</span>
            <button
              class="h-8 w-8 rounded border border-[var(--border)] text-lg leading-none disabled:opacity-40"
              :disabled="checkoutQty >= maxQty(checkoutTier)"
              @click="checkoutQty++"
            >
              +
            </button>
          </div>
        </div>
        <!-- 优惠券选择（M13） -->
        <div v-if="myCoupons.length" class="space-y-2">
          <span class="text-sm text-[var(--text-secondary)]">优惠券</span>
          <n-select
            v-model:value="selectedCouponId"
            :options="couponOptions()"
            placeholder="选择优惠券（不使用则留空）"
          />
        </div>
        <div class="space-y-1 rounded-lg bg-[var(--bg-page)] p-3 text-sm">
          <div class="flex justify-between text-[var(--text-tertiary)]">
            <span>商品金额</span>
            <span>{{ fmtCent(originalAmount()) }}</span>
          </div>
          <div v-if="discountAmount() > 0" class="flex justify-between text-[var(--text-tertiary)]">
            <span>优惠</span>
            <span class="text-red-500">-{{ fmtCent(discountAmount()) }}</span>
          </div>
          <div class="flex justify-between font-semibold text-[var(--text-primary)]">
            <span>应付</span>
            <span class="text-[var(--brand)]">{{ fmtCent(payableAmount()) }}</span>
          </div>
        </div>
      </div>
      <template #footer>
        <div class="flex justify-end gap-3">
          <button class="rounded-lg px-4 py-2 text-sm text-[var(--text-secondary)]" @click="checkoutTier = null">
            取消
          </button>
          <button
            class="rounded-lg bg-[var(--brand)] px-4 py-2 text-sm font-medium text-white disabled:opacity-60"
            :disabled="checkoutLoading"
            @click="submitCheckout"
          >
            {{ checkoutLoading ? '抢票中…' : '确认抢票' }}
          </button>
        </div>
      </template>
    </n-modal>
  </div>
</template>
