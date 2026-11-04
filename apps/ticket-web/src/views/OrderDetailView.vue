<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useDialog, useMessage } from 'naive-ui';
import {
  applyRefund,
  cancelOrder,
  cancelRefund,
  createOrGetPayment,
  getMyOrder,
  getMyRefunds,
  getOrderOperations,
  mockPay,
} from '../api';
import type { Order, OrderOperation, OrderStatus, Payment, Refund, RefundReason, RefundStatus } from '../api/types';

const route = useRoute();
const router = useRouter();
const message = useMessage();
const dialog = useDialog();

const orderId = route.params.id as string;
const order = ref<Order | null>(null);
const operations = ref<OrderOperation[]>([]);
const loading = ref(false);
const cancelling = ref(false);

// 支付弹窗状态
const showPayModal = ref(false);
const payment = ref<Payment | null>(null);
const paying = ref(false);

// 退款状态
const refund = ref<Refund | null>(null);
const showRefundModal = ref(false);
const refundReason = ref<RefundReason>('PERSONAL');
const refundDetail = ref('');
const refunding = ref(false);
const cancellingRefund = ref(false);

// 待支付倒计时
const now = ref(Date.now());
let timer: ReturnType<typeof setInterval> | null = null;

const statusMap: Record<OrderStatus, { label: string; cls: string }> = {
  PENDING_PAYMENT: { label: '待支付', cls: 'bg-amber-100 text-amber-700' },
  PAID: { label: '已支付', cls: 'bg-green-100 text-green-700' },
  CLOSED: { label: '已关闭', cls: 'bg-gray-200 text-gray-500' },
};

const operationMap: Record<OrderOperation['operation'], string> = {
  ORDER_CREATED: '下单成功',
  USER_CANCELLED: '用户取消订单',
  TIMEOUT_CLOSED: '支付超时自动关闭',
  PAYMENT_CONFIRMED: '支付成功',
  REFUND_REQUESTED: '申请退款',
  REFUND_SUCCEEDED: '退款成功',
};

const closeReasonMap: Record<string, string> = {
  USER_CANCEL: '用户取消',
  PAYMENT_TIMEOUT: '支付超时',
};

const refundStatusMap: Record<RefundStatus, { label: string; cls: string }> = {
  PENDING_REVIEW: { label: '退款审核中', cls: 'bg-amber-100 text-amber-700' },
  APPROVED: { label: '退款已通过', cls: 'bg-blue-100 text-blue-700' },
  REJECTED: { label: '退款已拒绝', cls: 'bg-red-100 text-red-700' },
  PROCESSING: { label: '退款处理中', cls: 'bg-blue-100 text-blue-700' },
  SUCCEEDED: { label: '已退款', cls: 'bg-green-100 text-green-700' },
  FAILED: { label: '退款失败', cls: 'bg-red-100 text-red-700' },
  CANCELLED: { label: '已撤销', cls: 'bg-gray-200 text-gray-500' },
};

const refundReasonOptions: { label: string; value: RefundReason }[] = [
  { label: '个人原因', value: 'PERSONAL' },
  { label: '演出延期', value: 'EVENT_POSTPONED' },
  { label: '演出取消', value: 'EVENT_CANCELLED' },
  { label: '其他原因', value: 'OTHER' },
];

function fmtTime(t: string | null) {
  if (!t) return '-';
  return t.replace('T', ' ').slice(0, 16);
}

const fmtCent = (c: number) => `¥${(c / 100).toFixed(2)}`;

/** 待支付剩余时间文案。 */
function remaining(): string {
  const o = order.value;
  if (!o || o.status !== 'PENDING_PAYMENT') return '';
  const diff = new Date(o.expireAt).getTime() - now.value;
  if (diff <= 0) return '已超时，等待系统关闭';
  const m = Math.floor(diff / 60000);
  const s = Math.floor((diff % 60000) / 1000);
  return `请在 ${m} 分 ${s} 秒内完成支付`;
}

async function load() {
  loading.value = true;
  try {
    const resp = await getMyOrder(orderId);
    order.value = resp.data;
    await Promise.all([loadOperations(), loadRefund()]);
  } catch (e: any) {
    message.error(e?.response?.data?.message ?? '加载订单失败');
  } finally {
    loading.value = false;
  }
}

async function loadOperations() {
  try {
    const resp = await getOrderOperations(orderId);
    operations.value = resp.data;
  } catch {
    operations.value = [];
  }
}

/** 查找该订单的退款单（若有）。 */
async function loadRefund() {
  try {
    const resp = await getMyRefunds({ page: 1, pageSize: 50 });
    refund.value = resp.data?.items?.find((r) => r.orderId === orderId) ?? null;
  } catch {
    refund.value = null;
  }
}

/** 取消待支付订单（二次确认，幂等）。 */
function onCancel() {
  dialog.warning({
    title: '取消订单',
    content: '确定取消该订单吗？取消后预占的门票将立即释放。',
    positiveText: '确认取消',
    negativeText: '再想想',
    onPositiveClick: async () => {
      cancelling.value = true;
      try {
        const resp = await cancelOrder(orderId);
        order.value = resp.data;
        message.success('订单已取消');
        await loadOperations();
      } catch (e: any) {
        message.error(e?.response?.data?.message ?? '取消失败');
      } finally {
        cancelling.value = false;
      }
    },
  });
}

/** 点击「立即支付」：创建/获取支付单并弹出收银台。 */
async function onPay() {
  try {
    const resp = await createOrGetPayment(orderId);
    payment.value = resp.data;
    showPayModal.value = true;
  } catch (e: any) {
    message.error(e?.response?.data?.message ?? '发起支付失败');
    await load();
  }
}

/** 确认模拟支付：调用 mock-pay，成功后刷新订单并提示。 */
async function confirmPay() {
  if (!payment.value) return;
  paying.value = true;
  try {
    const resp = await mockPay(payment.value.id, { attemptRequestId: crypto.randomUUID() });
    if (resp.data.status === 'SUCCEEDED') {
      showPayModal.value = false;
      message.success('支付成功');
      await load();
      await loadOperations();
    } else {
      message.warning('支付未完成，请重试');
    }
  } catch (e: any) {
    message.error(e?.response?.data?.message ?? '支付失败');
    await load();
  } finally {
    paying.value = false;
  }
}

/** 点击「申请退款」：打开退款原因弹窗。 */
function onApplyRefund() {
  refundReason.value = 'PERSONAL';
  refundDetail.value = '';
  showRefundModal.value = true;
}

/** 提交退款申请。 */
async function confirmRefund() {
  refunding.value = true;
  try {
    const resp = await applyRefund(orderId, {
      refundRequestId: crypto.randomUUID(),
      reasonCode: refundReason.value,
      reasonDetail: refundDetail.value.trim() || undefined,
    });
    refund.value = resp.data;
    showRefundModal.value = false;
    message.success('退款申请已提交');
    await loadOperations();
  } catch (e: any) {
    message.error(e?.response?.data?.message ?? '申请退款失败');
  } finally {
    refunding.value = false;
  }
}

/** 撤销未审核的退款申请。 */
function onCancelRefund() {
  if (!refund.value) return;
  dialog.warning({
    title: '撤销退款申请',
    content: '确定撤销该退款申请吗？',
    positiveText: '确认撤销',
    negativeText: '再想想',
    onPositiveClick: async () => {
      cancellingRefund.value = true;
      try {
        const resp = await cancelRefund(refund.value!.id);
        refund.value = resp.data;
        message.success('退款申请已撤销');
      } catch (e: any) {
        message.error(e?.response?.data?.message ?? '撤销失败');
      } finally {
        cancellingRefund.value = false;
      }
    },
  });
}

onMounted(() => {
  load();
  timer = setInterval(() => {
    now.value = Date.now();
    // 倒计时归零后自动刷新，等待系统关单结果
    const o = order.value;
    if (o && o.status === 'PENDING_PAYMENT' && new Date(o.expireAt).getTime() <= Date.now()) {
      load();
    }
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
        <div class="mx-auto text-xl font-semibold text-[var(--brand)]">订单详情</div>
      </div>
    </header>

    <main v-if="order" class="mx-auto max-w-4xl px-6 py-10">
      <div class="rounded-2xl border border-[var(--border)] bg-white p-8">
        <div class="flex items-center justify-between">
          <div class="text-lg font-semibold text-[var(--text-primary)]">
            {{ order.items[0]?.eventTitle ?? '演出订单' }}
          </div>
          <span class="rounded-full px-3 py-1 text-xs font-medium" :class="statusMap[order.status].cls">
            {{ statusMap[order.status].label }}
          </span>
        </div>

        <div class="mt-6 grid gap-4 text-sm">
          <div v-if="order.items[0]" class="rounded-lg bg-[var(--bg-page)] p-4">
            <div class="flex items-center justify-between">
              <div>
                <div class="font-medium text-[var(--text-primary)]">{{ order.items[0].tierName }}</div>
                <div class="mt-1 text-xs text-[var(--text-tertiary)]">
                  {{ order.items[0].venueName }} · {{ fmtTime(order.items[0].sessionStartAt) }}
                </div>
              </div>
              <div class="text-right">
                <div class="text-[var(--text-secondary)]">
                  {{ fmtCent(order.items[0].unitPriceCent) }} × {{ order.items[0].quantity }}
                </div>
                <div class="font-semibold text-[var(--brand)]">{{ fmtCent(order.items[0].amountCent) }}</div>
              </div>
            </div>
          </div>

          <div class="grid grid-cols-2 gap-4 rounded-lg bg-[var(--bg-page)] p-4">
            <div>
              <div class="text-xs text-[var(--text-tertiary)]">订单号</div>
              <div class="mt-1 font-mono text-[var(--text-primary)]">{{ order.orderNo }}</div>
            </div>
            <div>
              <div class="text-xs text-[var(--text-tertiary)]">下单时间</div>
              <div class="mt-1 text-[var(--text-primary)]">{{ fmtTime(order.createdAt) }}</div>
            </div>
            <div>
              <div class="text-xs text-[var(--text-tertiary)]">应付金额</div>
              <div class="mt-1 font-semibold text-[var(--brand)]">{{ fmtCent(order.payableAmountCent) }}</div>
            </div>
            <div>
              <div class="text-xs text-[var(--text-tertiary)]">实付金额</div>
              <div class="mt-1 text-[var(--text-primary)]">{{ fmtCent(order.paidAmountCent) }}</div>
            </div>
          </div>

          <div v-if="order.status === 'PENDING_PAYMENT'" class="rounded-lg bg-amber-50 p-4 text-amber-700">
            {{ remaining() }}
          </div>
          <div v-else-if="order.status === 'CLOSED'" class="rounded-lg bg-gray-50 p-4 text-[var(--text-tertiary)]">
            关闭原因：{{ closeReasonMap[order.closeReason ?? ''] ?? order.closeReason ?? '-' }}
            <span v-if="order.closedAt" class="ml-2">（{{ fmtTime(order.closedAt) }}）</span>
          </div>

          <!-- 退款进度 -->
          <div v-if="refund" class="rounded-lg border border-[var(--border)] p-4">
            <div class="flex items-center justify-between">
              <div class="flex items-center gap-2">
                <span class="text-sm text-[var(--text-tertiary)]">退款进度</span>
                <span class="rounded-full px-2.5 py-0.5 text-xs font-medium" :class="refundStatusMap[refund.status].cls">
                  {{ refundStatusMap[refund.status].label }}
                </span>
              </div>
              <span class="text-sm font-semibold text-[var(--brand)]">{{ fmtCent(refund.amountCent) }}</span>
            </div>
            <div class="mt-2 flex items-center justify-between text-xs text-[var(--text-tertiary)]">
              <span>退款单号：<span class="font-mono">{{ refund.refundNo }}</span></span>
              <span>原因：{{ refundReasonOptions.find((o) => o.value === refund!.reasonCode)?.label ?? refund!.reasonCode }}</span>
            </div>
            <div v-if="refund.reviewComment" class="mt-2 text-xs text-[var(--text-tertiary)]">
              审核意见：{{ refund.reviewComment }}
            </div>
            <div v-if="refund.status === 'PENDING_REVIEW'" class="mt-3 flex justify-end">
              <button
                class="rounded-lg border border-[var(--border)] px-4 py-1.5 text-xs text-[var(--text-secondary)] hover:border-red-300 hover:text-red-500"
                :disabled="cancellingRefund"
                @click="onCancelRefund"
              >
                {{ cancellingRefund ? '撤销中…' : '撤销申请' }}
              </button>
            </div>
          </div>

          <div v-if="order.status === 'PENDING_PAYMENT'" class="flex justify-end gap-3">
            <button
              class="rounded-lg border border-[var(--border)] px-4 py-2 text-sm text-[var(--text-secondary)] hover:border-red-300 hover:text-red-500"
              :disabled="cancelling"
              @click="onCancel"
            >
              {{ cancelling ? '取消中…' : '取消订单' }}
            </button>
            <button
              class="rounded-lg bg-[var(--brand)] px-6 py-2 text-sm font-medium text-white transition hover:opacity-90"
              @click="onPay"
            >
              立即支付
            </button>
          </div>

          <div v-else-if="order.status === 'PAID' && !refund" class="flex justify-end gap-3">
            <button
              class="rounded-lg border border-[var(--border)] px-6 py-2 text-sm text-[var(--text-secondary)] hover:border-red-300 hover:text-red-500"
              @click="onApplyRefund"
            >
              申请退款
            </button>
          </div>
        </div>
      </div>

      <!-- 操作流水 -->
      <div v-if="operations.length" class="mt-6 rounded-2xl border border-[var(--border)] bg-white p-8">
        <div class="text-base font-semibold text-[var(--text-primary)]">订单记录</div>
        <div class="mt-4 space-y-3">
          <div v-for="op in operations" :key="op.id" class="flex items-center justify-between text-sm">
            <div class="flex items-center gap-2">
              <span class="h-2 w-2 rounded-full bg-[var(--brand)]"></span>
              <span class="text-[var(--text-primary)]">{{ operationMap[op.operation] ?? op.operation }}</span>
            </div>
            <span class="text-xs text-[var(--text-tertiary)]">{{ fmtTime(op.createdAt) }}</span>
          </div>
        </div>
      </div>
    </main>

    <main v-else-if="loading" class="mx-auto max-w-4xl px-6 py-20 text-center text-[var(--text-tertiary)]">
      加载中…
    </main>

    <!-- 模拟支付收银台 -->
    <n-modal v-model:show="showPayModal" preset="card" title="模拟支付收银台" style="width: 420px">
      <div v-if="payment" class="space-y-4">
        <div class="flex items-center justify-between text-sm">
          <span class="text-[var(--text-tertiary)]">支付单号</span>
          <span class="font-mono text-[var(--text-primary)]">{{ payment.paymentNo }}</span>
        </div>
        <div class="flex items-center justify-between text-sm">
          <span class="text-[var(--text-tertiary)]">支付渠道</span>
          <span class="text-[var(--text-primary)]">模拟钱包（MOCK_WALLET）</span>
        </div>
        <div class="rounded-lg bg-[var(--bg-page)] p-4 text-center">
          <div class="text-xs text-[var(--text-tertiary)]">应付金额</div>
          <div class="mt-1 text-3xl font-semibold text-[var(--brand)]">{{ fmtCent(payment.amountCent) }}</div>
        </div>
        <div class="text-xs text-[var(--text-tertiary)]">
          此为内部模拟支付，不会产生真实资金划转。点击「确认支付」后订单将立即转为已支付并出票。
        </div>
        <div class="flex justify-end gap-3">
          <button
            class="rounded-lg border border-[var(--border)] px-4 py-2 text-sm text-[var(--text-secondary)]"
            :disabled="paying"
            @click="showPayModal = false"
          >
            取消
          </button>
          <button
            class="rounded-lg bg-[var(--brand)] px-6 py-2 text-sm font-medium text-white transition hover:opacity-90 disabled:opacity-50"
            :disabled="paying"
            @click="confirmPay"
          >
            {{ paying ? '支付中…' : '确认支付' }}
          </button>
        </div>
      </div>
    </n-modal>

    <!-- 申请退款弹窗 -->
    <n-modal v-model:show="showRefundModal" preset="card" title="申请退款" style="width: 440px">
      <div class="space-y-4">
        <div>
          <div class="mb-2 text-sm text-[var(--text-secondary)]">退款原因</div>
          <n-select v-model:value="refundReason" :options="refundReasonOptions" />
        </div>
        <div>
          <div class="mb-2 text-sm text-[var(--text-secondary)]">补充说明（可选）</div>
          <n-input
            v-model:value="refundDetail"
            type="textarea"
            :autosize="{ minRows: 2, maxRows: 4 }"
            placeholder="请填写退款说明"
          />
        </div>
        <div class="rounded-lg bg-[var(--bg-page)] p-3 text-xs text-[var(--text-tertiary)]">
          退款金额：<span class="font-semibold text-[var(--brand)]">{{ order ? fmtCent(order.payableAmountCent) : '-' }}</span>
          （全额退款，退款成功后门票将失效，不重新上架）
        </div>
        <div class="flex justify-end gap-3">
          <button
            class="rounded-lg border border-[var(--border)] px-4 py-2 text-sm text-[var(--text-secondary)]"
            :disabled="refunding"
            @click="showRefundModal = false"
          >
            取消
          </button>
          <button
            class="rounded-lg bg-[var(--brand)] px-6 py-2 text-sm font-medium text-white transition hover:opacity-90 disabled:opacity-50"
            :disabled="refunding"
            @click="confirmRefund"
          >
            {{ refunding ? '提交中…' : '提交申请' }}
          </button>
        </div>
      </div>
    </n-modal>
  </div>
</template>
