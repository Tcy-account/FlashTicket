<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { approveRefund, listRefunds, rejectRefund, retryRefund } from '../api';
import type { Refund, RefundStatus } from '../api/types';
import { useAuthStore } from '../stores/auth';

const auth = useAuthStore();

const loading = ref(false);
const rows = ref<Refund[]>([]);
const total = ref(0);
const detail = ref<Refund | null>(null);
const detailVisible = ref(false);
const acting = ref(false);

const query = reactive({ page: 1, pageSize: 20, status: '', refundNo: '' });

const statusMap: Record<RefundStatus, { label: string; tag: string }> = {
  PENDING_REVIEW: { label: '待审核', tag: 'warning' },
  APPROVED: { label: '已通过', tag: 'info' },
  REJECTED: { label: '已拒绝', tag: 'danger' },
  PROCESSING: { label: '处理中', tag: 'warning' },
  SUCCEEDED: { label: '退款成功', tag: 'success' },
  FAILED: { label: '退款失败', tag: 'danger' },
  CANCELLED: { label: '已撤销', tag: 'info' },
};

const reasonMap: Record<string, string> = {
  PERSONAL: '个人原因',
  EVENT_POSTPONED: '演出延期',
  EVENT_CANCELLED: '演出取消',
  OTHER: '其他原因',
};

function fmtTime(t: string | null) {
  if (!t) return '-';
  return t.replace('T', ' ').slice(0, 16);
}

const fmtCent = (c: number) => `¥${(c / 100).toFixed(2)}`;

async function load() {
  loading.value = true;
  try {
    const r = await listRefunds({
      page: query.page,
      pageSize: query.pageSize,
      status: query.status || undefined,
      refundNo: query.refundNo || undefined,
    });
    rows.value = r.data?.items ?? [];
    total.value = r.data?.total ?? 0;
  } finally {
    loading.value = false;
  }
}

function search() {
  query.page = 1;
  load();
}

function openDetail(row: Refund) {
  detail.value = row;
  detailVisible.value = true;
}

async function onApprove(row: Refund) {
  try {
    await ElMessageBox.confirm(`确定审核通过该退款申请（${fmtCent(row.amountCent)}）吗？通过后将自动执行退款。`, '审核通过', {
      confirmButtonText: '确认通过',
      cancelButtonText: '取消',
      type: 'warning',
    });
  } catch {
    return;
  }
  acting.value = true;
  try {
    const r = await approveRefund(row.id, {});
    detail.value = r.data;
    row.status = r.data.status;
    row.succeededAt = r.data.succeededAt;
    ElMessage.success('审核通过，退款已执行成功');
    load();
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message ?? '操作失败');
  } finally {
    acting.value = false;
  }
}

async function onReject(row: Refund) {
  let comment = '';
  try {
    const { value } = await ElMessageBox.prompt('请填写拒绝原因', '审核拒绝', {
      confirmButtonText: '确认拒绝',
      cancelButtonText: '取消',
      inputValidator: (v: string) => (v && v.trim() ? true : '拒绝原因不能为空'),
    });
    comment = (value ?? '').trim();
  } catch {
    return;
  }
  acting.value = true;
  try {
    const r = await rejectRefund(row.id, { comment });
    detail.value = r.data;
    row.status = r.data.status;
    ElMessage.success('已拒绝该退款申请');
    load();
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message ?? '操作失败');
  } finally {
    acting.value = false;
  }
}

async function onRetry(row: Refund) {
  acting.value = true;
  try {
    const r = await retryRefund(row.id);
    detail.value = r.data;
    row.status = r.data.status;
    ElMessage.success('退款已重新执行成功');
    load();
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message ?? '操作失败');
  } finally {
    acting.value = false;
  }
}

onMounted(load);
</script>

<template>
  <div>
    <el-card shadow="never">
      <div class="mb-4 flex items-center gap-3">
        <el-select v-model="query.status" placeholder="退款状态" clearable style="width: 160px">
          <el-option label="待审核" value="PENDING_REVIEW" />
          <el-option label="已通过" value="APPROVED" />
          <el-option label="已拒绝" value="REJECTED" />
          <el-option label="处理中" value="PROCESSING" />
          <el-option label="退款成功" value="SUCCEEDED" />
          <el-option label="退款失败" value="FAILED" />
          <el-option label="已撤销" value="CANCELLED" />
        </el-select>
        <el-input
          v-model="query.refundNo"
          placeholder="退款单号"
          clearable
          style="width: 220px"
          @keyup.enter="search"
        />
        <el-button type="primary" @click="search">查询</el-button>
      </div>

      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column prop="refundNo" label="退款单号" min-width="200" />
        <el-table-column prop="orderId" label="订单 ID" width="180" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusMap[row.status as RefundStatus].tag" size="small">
              {{ statusMap[row.status as RefundStatus].label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="金额" width="120" align="right">
          <template #default="{ row }">{{ fmtCent(row.amountCent) }}</template>
        </el-table-column>
        <el-table-column label="原因" width="120">
          <template #default="{ row }">{{ reasonMap[row.reasonCode] ?? row.reasonCode }}</template>
        </el-table-column>
        <el-table-column label="申请时间" width="170">
          <template #default="{ row }">{{ fmtTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <template v-if="row.status === 'PENDING_REVIEW' && auth.hasPermission('refund:approve')">
              <el-button link type="success" @click="onApprove(row)">通过</el-button>
              <el-button link type="danger" @click="onReject(row)">拒绝</el-button>
            </template>
            <el-button
              v-if="row.status === 'FAILED' && auth.hasPermission('refund:retry')"
              link
              type="warning"
              @click="onRetry(row)"
            >
              重试
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="mt-4 flex justify-end">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="load"
        />
      </div>
    </el-card>

    <el-dialog v-model="detailVisible" title="退款单详情" width="560px">
      <template v-if="detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="退款单号" :span="2">{{ detail.refundNo }}</el-descriptions-item>
          <el-descriptions-item label="订单 ID">{{ detail.orderId }}</el-descriptions-item>
          <el-descriptions-item label="支付单 ID">{{ detail.paymentId }}</el-descriptions-item>
          <el-descriptions-item label="申请人">{{ detail.userId }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusMap[detail.status].tag" size="small">
              {{ statusMap[detail.status].label }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="退款金额">{{ fmtCent(detail.amountCent) }}</el-descriptions-item>
          <el-descriptions-item label="币种">{{ detail.currency }}</el-descriptions-item>
          <el-descriptions-item label="原因" :span="2">
            {{ reasonMap[detail.reasonCode] ?? detail.reasonCode }}
            <span v-if="detail.reasonDetail" class="text-gray-400">（{{ detail.reasonDetail }}）</span>
          </el-descriptions-item>
          <el-descriptions-item label="审核人" :span="2">
            {{ detail.approvedBy ?? detail.rejectedBy ?? '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="审核意见" :span="2">{{ detail.reviewComment ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="退款完成时间">{{ fmtTime(detail.succeededAt) }}</el-descriptions-item>
          <el-descriptions-item label="申请时间">{{ fmtTime(detail.createdAt) }}</el-descriptions-item>
        </el-descriptions>

        <div v-if="detail.status === 'PENDING_REVIEW' && auth.hasPermission('refund:approve')" class="mt-4 flex justify-end gap-3">
          <el-button type="danger" :loading="acting" @click="onReject(detail)">拒绝</el-button>
          <el-button type="success" :loading="acting" @click="onApprove(detail)">审核通过并退款</el-button>
        </div>
        <div v-if="detail.status === 'FAILED' && auth.hasPermission('refund:retry')" class="mt-4 flex justify-end gap-3">
          <el-button type="warning" :loading="acting" @click="onRetry(detail)">重试执行</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>
