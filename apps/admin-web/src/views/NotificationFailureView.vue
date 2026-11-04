<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { listNotificationFailures, retryNotification } from '../api';
import type { NotificationFailure } from '../api/types';

const loading = ref(false);
const list = ref<NotificationFailure[]>([]);

async function load() {
  loading.value = true;
  try {
    const r = await listNotificationFailures();
    list.value = r.data;
  } catch (e: any) {
    ElMessage.error(e?.message ?? '加载失败工单失败');
  } finally {
    loading.value = false;
  }
}

async function retry(row: NotificationFailure) {
  try {
    await retryNotification(row.id);
    ElMessage.success('已重置重试');
    await load();
  } catch (e: any) {
    ElMessage.error(e?.message ?? '重试失败');
  }
}

function fmtTime(t: string) {
  return new Date(t).toLocaleString('zh-CN', { hour12: false });
}

onMounted(load);
</script>

<template>
  <div>
    <div class="mb-4 flex items-center justify-between">
      <h2 class="text-lg font-semibold">通知失败工单</h2>
      <el-button :loading="loading" @click="load">刷新</el-button>
    </div>

    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column prop="id" label="工单 ID" width="200" />
      <el-table-column prop="eventId" label="事件 ID" width="260" show-overflow-tooltip />
      <el-table-column prop="aggregateType" label="业务类型" width="110" />
      <el-table-column prop="aggregateId" label="业务 ID" width="200" show-overflow-tooltip />
      <el-table-column prop="attemptCount" label="已尝试次数" width="110" />
      <el-table-column prop="lastError" label="最近错误" show-overflow-tooltip />
      <el-table-column label="创建时间" width="180">
        <template #default="{ row }">{{ fmtTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" size="small" @click="retry(row)">重试</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>
