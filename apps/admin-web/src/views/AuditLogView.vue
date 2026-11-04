<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { listAuditLogs } from '../api';
import type { AuditLog } from '../api/types';

const loading = ref(false);
const logs = ref<AuditLog[]>([]);
const total = ref(0);
const query = reactive({ page: 1, pageSize: 20 });

const actionText: Record<string, string> = {
  USER_CREATE: '创建员工',
  USER_UPDATE: '修改员工',
  USER_DISABLE: '禁用账号',
  USER_ENABLE: '启用账号',
  ROLE_ASSIGN: '分配角色',
  SESSION_REVOKE: '强制下线',
  PASSWORD_RESET: '重置密码',
  ROLE_PERMISSION_GRANT: '配置角色权限',
};

async function loadLogs() {
  loading.value = true;
  try {
    const resp = await listAuditLogs({ page: query.page, pageSize: query.pageSize });
    logs.value = resp.data.items;
    total.value = resp.data.total;
  } catch (e: any) {
    ElMessage.error(e?.message ?? '加载审计日志失败');
  } finally {
    loading.value = false;
  }
}

function formatDetail(detail: string | null): string {
  if (!detail) return '-';
  try {
    return JSON.stringify(JSON.parse(detail), null, 2);
  } catch {
    return detail;
  }
}

onMounted(loadLogs);
</script>

<template>
  <div>
    <h2 class="page-title">审计日志</h2>
    <el-card shadow="never">
      <el-table :data="logs" v-loading="loading" border stripe>
        <el-table-column label="时间" width="180">
          <template #default="{ row }">{{ row.createdAt ?? '-' }}</template>
        </el-table-column>
        <el-table-column prop="operatorUsername" label="操作员" width="140" />
        <el-table-column label="动作" width="120">
          <template #default="{ row }">
            <el-tag size="small">{{ actionText[row.action] ?? row.action }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="目标" width="200">
          <template #default="{ row }">
            {{ row.targetType }}#{{ row.targetId ?? '-' }}
          </template>
        </el-table-column>
        <el-table-column label="详情" min-width="280">
          <template #default="{ row }">
            <pre class="detail">{{ formatDetail(row.detail) }}</pre>
          </template>
        </el-table-column>
        <el-table-column prop="ip" label="IP" width="130" />
      </el-table>

      <el-pagination
        class="pagination"
        layout="total, prev, pager, next"
        :total="total"
        :page-size="query.pageSize"
        :current-page="query.page"
        @current-change="(p: number) => { query.page = p; loadLogs(); }"
      />
    </el-card>
  </div>
</template>

<style scoped>
.page-title {
  margin: 0 0 16px;
  font-size: 20px;
  font-weight: 600;
}
.detail {
  margin: 0;
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-all;
  color: #374151;
}
.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
