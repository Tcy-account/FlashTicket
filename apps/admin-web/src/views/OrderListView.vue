<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { listOrders } from '../api';
import type { Order, OrderStatus } from '../api/types';

const loading = ref(false);
const rows = ref<Order[]>([]);
const total = ref(0);
const detail = ref<Order | null>(null);
const detailVisible = ref(false);

const query = reactive({ page: 1, pageSize: 20, status: '', orderNo: '' });

const statusMap: Record<OrderStatus, { label: string; tag: string }> = {
  PENDING_PAYMENT: { label: '待支付', tag: 'warning' },
  PAID: { label: '已支付', tag: 'success' },
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
    const r = await listOrders({
      page: query.page,
      pageSize: query.pageSize,
      status: query.status || undefined,
      orderNo: query.orderNo || undefined,
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

function openDetail(row: Order) {
  detail.value = row;
  detailVisible.value = true;
}

onMounted(load);
</script>

<template>
  <div>
    <el-card shadow="never">
      <div class="mb-4 flex items-center gap-3">
        <el-select v-model="query.status" placeholder="订单状态" clearable style="width: 160px">
          <el-option label="待支付" value="PENDING_PAYMENT" />
          <el-option label="已支付" value="PAID" />
          <el-option label="已关闭" value="CLOSED" />
        </el-select>
        <el-input
          v-model="query.orderNo"
          placeholder="订单号"
          clearable
          style="width: 220px"
          @keyup.enter="search"
        />
        <el-button type="primary" @click="search">查询</el-button>
      </div>

      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column prop="orderNo" label="订单号" min-width="200" />
        <el-table-column prop="userId" label="用户 ID" width="180" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusMap[row.status as OrderStatus].tag" size="small">
              {{ statusMap[row.status as OrderStatus].label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="应付金额" width="120" align="right">
          <template #default="{ row }">{{ fmtCent(row.payableAmountCent) }}</template>
        </el-table-column>
        <el-table-column label="实付金额" width="120" align="right">
          <template #default="{ row }">{{ fmtCent(row.paidAmountCent) }}</template>
        </el-table-column>
        <el-table-column label="下单时间" width="170">
          <template #default="{ row }">{{ fmtTime(row.createdAt) }}</template>
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

    <el-dialog v-model="detailVisible" title="订单详情" width="560px">
      <template v-if="detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="订单号">{{ detail.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusMap[detail.status].tag" size="small">
              {{ statusMap[detail.status].label }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="用户 ID">{{ detail.userId }}</el-descriptions-item>
          <el-descriptions-item label="下单时间">{{ fmtTime(detail.createdAt) }}</el-descriptions-item>
          <el-descriptions-item label="应付金额">{{ fmtCent(detail.payableAmountCent) }}</el-descriptions-item>
          <el-descriptions-item label="实付金额">{{ fmtCent(detail.paidAmountCent) }}</el-descriptions-item>
          <el-descriptions-item label="支付截止">{{ fmtTime(detail.expireAt) }}</el-descriptions-item>
          <el-descriptions-item label="关闭原因">{{ detail.closeReason ?? '-' }}</el-descriptions-item>
        </el-descriptions>

        <div v-if="detail.items?.length" class="mt-4">
          <div class="mb-2 text-sm font-medium">明细快照</div>
          <div
            v-for="it in detail.items"
            :key="it.id"
            class="rounded border border-gray-100 bg-gray-50 p-3 text-sm"
          >
            <div class="flex items-center justify-between">
              <span class="font-medium">{{ it.eventTitle }} · {{ it.tierName }}</span>
              <span class="text-gray-500">{{ fmtCent(it.unitPriceCent) }} × {{ it.quantity }}</span>
            </div>
            <div class="mt-1 text-xs text-gray-400">
              {{ it.venueName }} · {{ fmtTime(it.sessionStartAt) }}
            </div>
          </div>
        </div>
      </template>
    </el-dialog>
  </div>
</template>
