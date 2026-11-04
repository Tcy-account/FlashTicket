<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  cancelCampaign,
  createCampaign,
  endCampaign,
  listCampaigns,
  reviewCampaign,
  submitCampaignReview,
  updateCampaign,
} from '../api';
import type { Campaign, CampaignStatus } from '../api/types';
import { useAuthStore } from '../stores/auth';

const auth = useAuthStore();

const loading = ref(false);
const rows = ref<Campaign[]>([]);
const total = ref(0);
const query = reactive({ page: 1, pageSize: 20, status: '' });

const statusMap: Record<CampaignStatus, { label: string; tag: string }> = {
  DRAFT: { label: '草稿', tag: 'info' },
  PENDING_REVIEW: { label: '待审核', tag: 'warning' },
  PUBLISHED: { label: '已发布', tag: 'success' },
  ENDED: { label: '已结束', tag: 'info' },
  CANCELLED: { label: '已取消', tag: 'danger' },
};

const typeMap: Record<string, string> = {
  FLASH_CLAIM: '限时抢领',
  NEW_USER: '新人专享',
  EVENT_EXCLUSIVE: '活动专属',
};

function fmtTime(t: string | null) {
  if (!t) return '-';
  return t.replace('T', ' ').slice(0, 16);
}

async function load() {
  loading.value = true;
  try {
    const r = await listCampaigns({
      page: query.page,
      pageSize: query.pageSize,
      status: query.status || undefined,
    });
    rows.value = r.data?.items ?? [];
    total.value = r.data?.total ?? 0;
  } catch (e: any) {
    ElMessage.error(e?.message ?? '加载活动失败');
  } finally {
    loading.value = false;
  }
}

function search() {
  query.page = 1;
  load();
}

/* ===== 新建 / 编辑 ===== */
const formVisible = ref(false);
const isEdit = ref(false);
const form = reactive({
  id: '',
  name: '',
  type: 'FLASH_CLAIM',
  description: '',
  startAt: '',
  endAt: '',
});

function openCreate() {
  isEdit.value = false;
  Object.assign(form, { id: '', name: '', type: 'FLASH_CLAIM', description: '', startAt: '', endAt: '' });
  formVisible.value = true;
}

function openEdit(row: Campaign) {
  isEdit.value = true;
  Object.assign(form, {
    id: row.id,
    name: row.name,
    type: row.type,
    description: row.description ?? '',
    startAt: row.startAt,
    endAt: row.endAt,
  });
  formVisible.value = true;
}

async function onSubmit() {
  const payload = {
    name: form.name,
    type: form.type,
    description: form.description || null,
    startAt: form.startAt,
    endAt: form.endAt,
  };
  try {
    if (isEdit.value) {
      await updateCampaign(form.id, payload);
      ElMessage.success('活动修改成功');
    } else {
      await createCampaign(payload);
      ElMessage.success('活动创建成功');
    }
    formVisible.value = false;
    load();
  } catch (e: any) {
    ElMessage.error(e?.message ?? '保存失败');
  }
}

/* ===== 状态流转 ===== */
async function onAction(fn: () => Promise<unknown>, successText: string) {
  try {
    await fn();
    ElMessage.success(successText);
    load();
  } catch (e: any) {
    ElMessage.error(e?.message ?? '操作失败');
  }
}

function onSubmitReview(row: Campaign) {
  onAction(() => submitCampaignReview(row.id), '已提交审核');
}

async function onReview(row: Campaign, approve: boolean) {
  let comment = '';
  try {
    const { value } = await ElMessageBox.prompt(
      approve ? '（可选）填写审核意见' : '请填写驳回原因',
      approve ? '审核通过' : '审核驳回',
      {
        confirmButtonText: approve ? '确认通过' : '确认驳回',
        cancelButtonText: '取消',
        inputValue: '',
        inputValidator: approve ? undefined : (v: string) => (v && v.trim() ? true : '驳回原因不能为空'),
      },
    );
    comment = (value ?? '').trim();
  } catch {
    return;
  }
  onAction(() => reviewCampaign(row.id, { approve, comment: comment || undefined }), approve ? '审核通过' : '已驳回');
}

function onEnd(row: Campaign) {
  ElMessageBox.confirm(`确认结束活动「${row.name}」？`, '提示', { type: 'warning' })
    .then(() => onAction(() => endCampaign(row.id), '活动已结束'))
    .catch(() => {});
}

function onCancel(row: Campaign) {
  ElMessageBox.confirm(`确认取消活动「${row.name}」？`, '提示', { type: 'warning' })
    .then(() => onAction(() => cancelCampaign(row.id), '活动已取消'))
    .catch(() => {});
}

onMounted(load);
</script>

<template>
  <div>
    <div class="toolbar">
      <h2 class="page-title">营销活动管理</h2>
      <el-button v-if="auth.hasPermission('campaign:manage')" type="primary" @click="openCreate">
        新建活动
      </el-button>
    </div>

    <el-card shadow="never">
      <div class="mb-4 flex items-center gap-3">
        <el-select v-model="query.status" placeholder="活动状态" clearable style="width: 160px">
          <el-option label="草稿" value="DRAFT" />
          <el-option label="待审核" value="PENDING_REVIEW" />
          <el-option label="已发布" value="PUBLISHED" />
          <el-option label="已结束" value="ENDED" />
          <el-option label="已取消" value="CANCELLED" />
        </el-select>
        <el-button type="primary" @click="search">查询</el-button>
      </div>

      <el-table v-loading="loading" :data="rows" border stripe>
        <el-table-column prop="name" label="活动名称" min-width="180" show-overflow-tooltip />
        <el-table-column label="类型" width="120">
          <template #default="{ row }">{{ typeMap[row.type] ?? row.type }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusMap[row.status as CampaignStatus].tag" size="small">
              {{ statusMap[row.status as CampaignStatus].label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="开始时间" width="160">
          <template #default="{ row }">{{ fmtTime(row.startAt) }}</template>
        </el-table-column>
        <el-table-column label="结束时间" width="160">
          <template #default="{ row }">{{ fmtTime(row.endAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <template v-if="auth.hasPermission('campaign:manage')">
              <el-button v-if="row.status === 'DRAFT'" link type="primary" @click="openEdit(row)">编辑</el-button>
              <el-button v-if="row.status === 'DRAFT'" link type="success" @click="onSubmitReview(row)">提交审核</el-button>
              <el-button v-if="row.status === 'PUBLISHED'" link type="warning" @click="onEnd(row)">结束</el-button>
              <el-button v-if="row.status === 'PUBLISHED'" link type="danger" @click="onCancel(row)">取消</el-button>
            </template>
            <template v-if="row.status === 'PENDING_REVIEW' && auth.hasPermission('campaign:review')">
              <el-button link type="success" @click="onReview(row, true)">通过</el-button>
              <el-button link type="danger" @click="onReview(row, false)">驳回</el-button>
            </template>
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

    <el-dialog v-model="formVisible" :title="isEdit ? '编辑活动' : '新建活动'" width="520px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="活动名称" required>
          <el-input v-model="form.name" placeholder="如：618 年中大促" />
        </el-form-item>
        <el-form-item label="活动类型" required>
          <el-select v-model="form.type" style="width: 100%">
            <el-option label="限时抢领" value="FLASH_CLAIM" />
            <el-option label="新人专享" value="NEW_USER" />
            <el-option label="活动专属" value="EVENT_EXCLUSIVE" />
          </el-select>
        </el-form-item>
        <el-form-item label="活动说明">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="活动说明" />
        </el-form-item>
        <el-form-item label="开始时间" required>
          <el-date-picker
            v-model="form.startAt"
            type="datetime"
            placeholder="选择开始时间"
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="结束时间" required>
          <el-date-picker
            v-model="form.endAt"
            type="datetime"
            placeholder="选择结束时间"
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" @click="onSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.page-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
}
</style>
