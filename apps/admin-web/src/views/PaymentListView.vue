<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { listPayments } from '../api';
import type { Payment, PaymentStatus } from '../api/types';

const loading = ref(false);
const rows = ref<Payment[]>([]);
const total = ref(0);
const detail = ref<Payment | null>(null);
const detailVisible = ref(false);

const query = reactive({ page: 1, pageSize: 20, status: '', paymentNo: '' });

const statusMap: Record<PaymentStatus, { label: string; tag: string }> = {
  CREATED: { label: '已创建', tag: 'info' },
  PROCESSING: { label: '处理中', tag: 'warning' },
  SUCCEEDED: { label: '支付成功', tag: 'success' },
  FAILED: { label: '支付失败', tag: 'danger' },
  CLOSED: { label: '已关闭', tag: 'info' },
};

function fmtTime(t: string | null) {
  if (!t) return '-';
  return t.replace('T', ' ').slice(0, 16);
}

const fmtCent = (c: number) => `¥${(c / 100).toFixed(2)}`;

async function load() {
  loading.value = true;
  try {
    const r = await listPayments({
      page: query.page,
      pageSize: query.pageSize,
      status: query.status || undefined,
      paymentNo: query.paymentNo || undefined,
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

function openDetail(row: Payment) {
  detail.value = row;
  detailVisible.value = true;
}

onMounted(load);
</script>

<template>
  <div>
    <el-card shadow="never">
      <div class="mb-4 flex items-center gap-3">
        <el-select v-model="query.status" placeholder="支付状态" clearable style="width: 160px">
          <el-option label="已创建" value="CREATED" />
          <el-option label="处理中" value="PROCESSING" />
          <el-option label="支付成功" value="SUCCEEDED" />
          <el-option label="支付失败" value="FAILED" />
          <el-option label="已关闭" value="CLOSED" />
        </el-select>
        <el-input
          v-model="query.paymentNo"
          placeholder="支付单号"
          clearable
          style="width: 220px"
          @keyup.enter="search"
        />
        <el-button type="primary" @click="search">查询</el-button>
      </div>

      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column prop="paymentNo" label="支付单号" min-width="200" />
        <el-table-column prop="orderId" label="订单 ID" width="180" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusMap[row.status as PaymentStatus].tag" size="small">
              {{ statusMap[row.status as PaymentStatus].label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="金额" width="120" align="right">
          <template #default="{ row }">{{ fmtCent(row.amountCent) }}</template>
        </el-table-column>
        <el-table-column prop="channel" label="渠道" width="140" />
        <el-table-column label="支付成功时间" width="170">
          <template #default="{ row }">{{ fmtTime(row.succeededAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
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

    <el-dialog v-model="detailVisible" title="支付单详情" width="520px">
      <template v-if="detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="支付单号" :span="2">{{ detail.paymentNo }}</el-descriptions-item>
          <el-descriptions-item label="订单 ID">{{ detail.orderId }}</el-descriptions-item>
          <el-descriptions-item label="用户 ID">{{ detail.userId }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusMap[detail.status].tag" size="small">
              {{ statusMap[detail.status].label }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="渠道">{{ detail.channel }}</el-descriptions-item>
          <el-descriptions-item label="金额">{{ fmtCent(detail.amountCent) }}</el-descriptions-item>
          <el-descriptions-item label="币种">{{ detail.currency }}</el-descriptions-item>
          <el-descriptions-item label="支付截止">{{ fmtTime(detail.expireAt) }}</el-descriptions-item>
          <el-descriptions-item label="支付成功时间">{{ fmtTime(detail.succeededAt) }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ fmtTime(detail.createdAt) }}</el-descriptions-item>
        </el-descriptions>
      </template>
    </el-dialog>
  </div>
</template>
