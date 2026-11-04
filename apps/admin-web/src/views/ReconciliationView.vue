<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import {
  createReconcileJob,
  listReconcileJobs,
  listReconcileExceptions,
  reviewReconcileException,
  listCompensations,
  executeCompensation,
} from '../api';
import type { ReconcileJob, ReconcileException, CompensationTask } from '../api/types';
import { useAuthStore } from '../stores/auth';

const auth = useAuthStore();

const activeTab = ref('jobs');

const jobs = ref<ReconcileJob[]>([]);
const jobTotal = ref(0);
const jobPage = ref(1);
const scope = ref('INVENTORY');

const exceptions = ref<ReconcileException[]>([]);
const exTotal = ref(0);
const exPage = ref(1);
const exStatus = ref('');

const comps = ref<CompensationTask[]>([]);
const compTotal = ref(0);
const compPage = ref(1);

const reviewDialog = ref(false);
const reviewTarget = ref<ReconcileException | null>(null);
const reviewResolution = ref('RESOLVED');
const reviewComment = ref('');

const PAGE_SIZE = 20;

const scopes = ['INVENTORY', 'ORDER', 'PAYMENT', 'REFUND'];

function statusTag(status: string): string {
  const map: Record<string, string> = {
    PENDING: 'warning', RUNNING: 'primary', SUCCEEDED: 'success', FAILED: 'danger',
    OPEN: 'danger', INVESTIGATING: 'warning', RESOLVED: 'success', CLOSED: 'info',
    RETRY_WAIT: 'warning', MANUAL_REVIEW: 'danger',
  };
  return map[status] ?? 'info';
}

function severityTag(s: string): string {
  const map: Record<string, string> = {
    LOW: 'info', MEDIUM: 'warning', HIGH: 'danger', CRITICAL: 'danger',
  };
  return map[s] ?? 'info';
}

async function loadJobs() {
  const r = await listReconcileJobs({ page: jobPage.value, size: PAGE_SIZE });
  jobs.value = r.data?.items ?? [];
  jobTotal.value = r.data?.total ?? 0;
}

async function loadExceptions() {
  const r = await listReconcileExceptions({
    status: exStatus.value || undefined,
    page: exPage.value,
    size: PAGE_SIZE,
  });
  exceptions.value = r.data?.items ?? [];
  exTotal.value = r.data?.total ?? 0;
}

async function loadComps() {
  const r = await listCompensations({ page: compPage.value, size: PAGE_SIZE });
  comps.value = r.data?.items ?? [];
  compTotal.value = r.data?.total ?? 0;
}

async function onCreateJob() {
  await createReconcileJob(scope.value);
  ElMessage.success('对账任务已执行');
  loadJobs();
}

function openReview(row: ReconcileException) {
  reviewTarget.value = row;
  reviewResolution.value = 'RESOLVED';
  reviewComment.value = '';
  reviewDialog.value = true;
}

async function submitReview() {
  if (!reviewTarget.value) return;
  await reviewReconcileException(reviewTarget.value.id, {
    resolution: reviewResolution.value,
    comment: reviewComment.value,
  });
  ElMessage.success('已提交复核结论');
  reviewDialog.value = false;
  loadExceptions();
}

async function onExecute(row: CompensationTask) {
  await executeCompensation(row.id);
  ElMessage.success('补偿已执行');
  loadComps();
}

onMounted(() => {
  loadJobs();
  loadExceptions();
  loadComps();
});
</script>

<template>
  <div>
    <h2 class="page-title">系统对账与补偿</h2>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="对账任务" name="jobs">
        <div class="toolbar">
          <el-select v-model="scope" style="width: 200px">
            <el-option v-for="s in scopes" :key="s" :label="s" :value="s" />
          </el-select>
          <el-button
            v-if="auth.hasPermission('reconciliation:execute')"
            type="primary"
            @click="onCreateJob"
          >
            执行对账
          </el-button>
        </div>
        <el-table :data="jobs" size="small">
          <el-table-column prop="jobNo" label="任务号" min-width="200" />
          <el-table-column prop="scope" label="领域" width="120" />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="statusTag(row.status)" size="small">{{ row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="totalChecked" label="检查数" width="100" />
          <el-table-column prop="exceptionCount" label="差异数" width="100" />
          <el-table-column prop="completedAt" label="完成时间" min-width="170" />
        </el-table>
        <el-pagination
          v-model:current-page="jobPage"
          :total="jobTotal"
          :page-size="PAGE_SIZE"
          layout="total, prev, pager, next"
          @current-change="loadJobs"
          style="margin-top: 12px"
        />
      </el-tab-pane>

      <el-tab-pane label="异常工单" name="exceptions">
        <div class="toolbar">
          <el-select
            v-model="exStatus"
            clearable
            placeholder="按状态筛选"
            style="width: 200px"
            @change="loadExceptions"
          >
            <el-option label="OPEN" value="OPEN" />
            <el-option label="INVESTIGATING" value="INVESTIGATING" />
            <el-option label="RESOLVED" value="RESOLVED" />
            <el-option label="CLOSED" value="CLOSED" />
          </el-select>
        </div>
        <el-table :data="exceptions" size="small">
          <el-table-column prop="exceptionNo" label="工单号" min-width="190" />
          <el-table-column prop="checkType" label="检查项" min-width="180" />
          <el-table-column label="严重级" width="100">
            <template #default="{ row }">
              <el-tag :type="severityTag(row.severity)" size="small">{{ row.severity }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="110">
            <template #default="{ row }">
              <el-tag :type="statusTag(row.status)" size="small">{{ row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="description" label="描述" min-width="220" show-overflow-tooltip />
          <el-table-column label="操作" width="110" fixed="right">
            <template #default="{ row }">
              <el-button
                v-if="auth.hasPermission('reconciliation:resolve') && (row.status === 'OPEN' || row.status === 'INVESTIGATING')"
                size="small"
                type="primary"
                link
                @click="openReview(row)"
              >
                复核
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination
          v-model:current-page="exPage"
          :total="exTotal"
          :page-size="PAGE_SIZE"
          layout="total, prev, pager, next"
          @current-change="loadExceptions"
          style="margin-top: 12px"
        />
      </el-tab-pane>

      <el-tab-pane label="补偿任务" name="compensations">
        <el-table :data="comps" size="small">
          <el-table-column prop="operationId" label="操作号" min-width="220" />
          <el-table-column prop="taskType" label="类型" width="140" />
          <el-table-column label="状态" width="120">
            <template #default="{ row }">
              <el-tag :type="statusTag(row.status)" size="small">{{ row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="entityType" label="实体" width="100" />
          <el-table-column prop="entityId" label="实体 ID" width="140" />
          <el-table-column prop="attemptCount" label="尝试次数" width="100" />
          <el-table-column label="操作" width="110" fixed="right">
            <template #default="{ row }">
              <el-button
                v-if="auth.hasPermission('compensation:execute') && (row.status === 'PENDING' || row.status === 'RETRY_WAIT')"
                size="small"
                type="primary"
                link
                @click="onExecute(row)"
              >
                执行
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination
          v-model:current-page="compPage"
          :total="compTotal"
          :page-size="PAGE_SIZE"
          layout="total, prev, pager, next"
          @current-change="loadComps"
          style="margin-top: 12px"
        />
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="reviewDialog" title="提交复核结论" width="480px">
      <el-form label-width="90px">
        <el-form-item label="工单号">{{ reviewTarget?.exceptionNo }}</el-form-item>
        <el-form-item label="结论">
          <el-radio-group v-model="reviewResolution">
            <el-radio value="RESOLVED">已解决</el-radio>
            <el-radio value="FALSE_POSITIVE">误报</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="reviewComment" type="textarea" :rows="3" placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewDialog = false">取消</el-button>
        <el-button type="primary" @click="submitReview">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-title {
  margin: 0 0 16px;
  font-size: 20px;
  font-weight: 600;
}
.toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
}
</style>
