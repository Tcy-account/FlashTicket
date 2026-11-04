<script setup lang="ts">
import { onMounted, reactive, ref, computed } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import type { Category, Event, EventRequest, EventStatus } from '../api/types';
import {
  cancelEvent,
  createEvent,
  listCategories,
  listEvents,
  publishEvent,
  reviewEvent,
  submitReview,
  updateEvent,
} from '../api';
import { useAuthStore } from '../stores/auth';

const router = useRouter();
const auth = useAuthStore();

const statusMap: Record<EventStatus, string> = {
  DRAFT: '草稿',
  PENDING_REVIEW: '待审核',
  REJECTED: '已驳回',
  APPROVED: '待发布',
  PUBLISHED: '已发布',
  CANCELLED: '已取消',
  ARCHIVED: '已归档',
};

const statusTag: Record<EventStatus, string> = {
  DRAFT: 'info',
  PENDING_REVIEW: 'warning',
  REJECTED: 'danger',
  APPROVED: 'warning',
  PUBLISHED: 'success',
  CANCELLED: 'danger',
  ARCHIVED: 'info',
};

const loading = ref(false);
const rows = ref<Event[]>([]);
const total = ref(0);
const categories = ref<Category[]>([]);

const query = reactive({ page: 1, pageSize: 20, status: '', categoryId: '', keyword: '' });

const categoryName = computed(() => {
  const map = new Map(categories.value.map((c) => [c.id, c.name]));
  return (id: string) => map.get(id) ?? id;
});

async function load() {
  loading.value = true;
  try {
    const r = await listEvents({
      page: query.page,
      pageSize: query.pageSize,
      status: query.status || undefined,
      categoryId: query.categoryId || undefined,
      keyword: query.keyword || undefined,
    });
    rows.value = r.data?.items ?? [];
    total.value = r.data?.total ?? 0;
  } finally {
    loading.value = false;
  }
}

async function loadCategories() {
  const r = await listCategories();
  categories.value = r.data ?? [];
}

function search() {
  query.page = 1;
  load();
}

// ===== 新建/编辑 =====
const dialogVisible = ref(false);
const editingId = ref<string | null>(null);
const saving = ref(false);
const form = reactive<EventRequest>({
  categoryId: '',
  title: '',
  subtitle: '',
  coverUrl: '',
  description: '',
  detailContent: '',
  organizerName: '',
  attendanceNotice: '',
  refundPolicy: '',
  version: 0,
});

function openCreate() {
  editingId.value = null;
  Object.assign(form, {
    categoryId: categories.value[0]?.id ?? '',
    title: '',
    subtitle: '',
    coverUrl: '',
    description: '',
    detailContent: '',
    organizerName: '',
    attendanceNotice: '',
    refundPolicy: '',
    version: 0,
  });
  dialogVisible.value = true;
}

function openEdit(row: Event) {
  editingId.value = row.id;
  Object.assign(form, {
    categoryId: row.categoryId,
    title: row.title,
    subtitle: row.subtitle ?? '',
    coverUrl: row.coverUrl ?? '',
    description: row.description ?? '',
    detailContent: row.detailContent ?? '',
    organizerName: row.organizerName ?? '',
    attendanceNotice: row.attendanceNotice ?? '',
    refundPolicy: row.refundPolicy ?? '',
    version: row.version,
  });
  dialogVisible.value = true;
}

async function submit() {
  saving.value = true;
  try {
    if (editingId.value) {
      await updateEvent(editingId.value, { ...form });
      ElMessage.success('活动已更新');
    } else {
      await createEvent({ ...form });
      ElMessage.success('活动已创建');
    }
    dialogVisible.value = false;
    load();
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message ?? '操作失败');
  } finally {
    saving.value = false;
  }
}

// ===== 取消 =====
async function onCancel(row: Event) {
  const { value } = await ElMessageBox.prompt('请输入取消原因', '取消活动', {
    confirmButtonText: '确认取消',
    cancelButtonText: '再想想',
    inputPlaceholder: '取消原因（必填）',
    inputValidator: (v: string) => (v && v.trim() ? true : '取消原因不能为空'),
  });
  try {
    await cancelEvent(row.id, { reason: value.trim() });
    ElMessage.success('活动已取消');
    load();
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message ?? '取消失败');
  }
}

// ===== 审核发布 =====
async function onSubmitReview(row: Event) {
  try {
    await submitReview(row.id);
    ElMessage.success('已提交审核');
    load();
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message ?? '提交审核失败');
  }
}

async function onApprove(row: Event) {
  try {
    await ElMessageBox.confirm('确认审核通过该活动？通过后进入待发布状态。', '审核通过', {
      confirmButtonText: '审核通过',
      cancelButtonText: '取消',
      type: 'warning',
    });
  } catch {
    return;
  }
  try {
    await reviewEvent(row.id, { approve: true });
    ElMessage.success('审核已通过');
    load();
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message ?? '审核失败');
  }
}

async function onReject(row: Event) {
  const { value } = await ElMessageBox.prompt('请输入驳回原因', '审核驳回', {
    confirmButtonText: '确认驳回',
    cancelButtonText: '取消',
    inputValidator: (v: string) => (v && v.trim() ? true : '驳回原因不能为空'),
  });
  try {
    await reviewEvent(row.id, { approve: false, comment: value.trim() });
    ElMessage.success('已驳回');
    load();
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message ?? '驳回失败');
  }
}

async function onPublish(row: Event) {
  try {
    await ElMessageBox.confirm('确认正式发布该活动？发布后用户端立即可见。', '发布活动', {
      confirmButtonText: '确认发布',
      cancelButtonText: '取消',
      type: 'warning',
    });
  } catch {
    return;
  }
  try {
    await publishEvent(row.id);
    ElMessage.success('活动已发布');
    load();
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message ?? '发布失败');
  }
}

function goSessions(row: Event) {
  router.push({ path: `/events/${row.id}/sessions`, query: { title: row.title } });
}

onMounted(() => {
  loadCategories();
  load();
});
</script>

<template>
  <div class="page">
    <div class="toolbar">
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 140px">
        <el-option v-for="(label, key) in statusMap" :key="key" :label="label" :value="key" />
      </el-select>
      <el-select v-model="query.categoryId" placeholder="分类" clearable style="width: 160px">
        <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
      </el-select>
      <el-input v-model="query.keyword" placeholder="标题关键词" clearable style="width: 200px" @keyup.enter="search" />
      <el-button type="primary" @click="search">查询</el-button>
      <div class="flex-1" />
      <el-button v-if="auth.hasPermission('event:event:create')" type="primary" @click="openCreate">
        新建活动
      </el-button>
    </div>

    <el-table v-loading="loading" :data="rows" stripe>
      <el-table-column prop="title" label="演出名称" min-width="180" />
      <el-table-column label="分类" width="110">
        <template #default="{ row }">{{ categoryName(row.categoryId) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusTag[row.status as EventStatus]">{{ statusMap[row.status as EventStatus] }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="organizerName" label="主办方" min-width="140" />
      <el-table-column prop="createdAt" label="创建时间" width="180">
        <template #default="{ row }">{{ row.createdAt?.replace('T', ' ').slice(0, 19) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="360" fixed="right">
        <template #default="{ row }">
          <el-button v-if="auth.hasPermission('event:session:list')" link type="primary" @click="goSessions(row)">
            场次
          </el-button>
          <el-button
            v-if="auth.hasPermission('event:event:update') && (row.status === 'DRAFT' || row.status === 'REJECTED')"
            link
            type="primary"
            @click="openEdit(row)"
          >
            编辑
          </el-button>
          <el-button
            v-if="auth.hasPermission('event:event:submit') && (row.status === 'DRAFT' || row.status === 'REJECTED')"
            link
            type="primary"
            @click="onSubmitReview(row)"
          >
            提交审核
          </el-button>
          <el-button
            v-if="auth.hasPermission('event:event:review') && row.status === 'PENDING_REVIEW'"
            link
            type="success"
            @click="onApprove(row)"
          >
            通过
          </el-button>
          <el-button
            v-if="auth.hasPermission('event:event:review') && row.status === 'PENDING_REVIEW'"
            link
            type="warning"
            @click="onReject(row)"
          >
            驳回
          </el-button>
          <el-button
            v-if="auth.hasPermission('event:event:publish') && row.status === 'APPROVED'"
            link
            type="success"
            @click="onPublish(row)"
          >
            发布
          </el-button>
          <el-button
            v-if="auth.hasPermission('event:event:cancel') && (row.status === 'DRAFT' || row.status === 'REJECTED' || row.status === 'PENDING_REVIEW' || row.status === 'APPROVED')"
            link
            type="danger"
            @click="onCancel(row)"
          >
            取消
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="query.page"
      v-model:page-size="query.pageSize"
      :total="total"
      layout="total, prev, pager, next"
      class="pagination"
      @current-change="load"
    />

    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑活动' : '新建活动'"
      width="640px"
      :close-on-click-modal="false"
    >
      <el-form label-width="90px">
        <el-form-item label="分类" required>
          <el-select v-model="form.categoryId" placeholder="选择分类" style="width: 100%">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="演出名称" required>
          <el-input v-model="form.title" maxlength="100" placeholder="5-100 字符" />
        </el-form-item>
        <el-form-item label="副标题">
          <el-input v-model="form.subtitle" maxlength="200" />
        </el-form-item>
        <el-form-item label="封面 URL">
          <el-input v-model="form.coverUrl" placeholder="https://..." />
        </el-form-item>
        <el-form-item label="简介">
          <el-input v-model="form.description" type="textarea" :rows="2" maxlength="500" />
        </el-form-item>
        <el-form-item label="主办方">
          <el-input v-model="form.organizerName" maxlength="100" />
        </el-form-item>
        <el-form-item label="观演须知">
          <el-input v-model="form.attendanceNotice" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="退票规则">
          <el-input v-model="form.refundPolicy" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page {
  padding: 16px;
}
.toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  align-items: center;
}
.flex-1 {
  flex: 1;
}
.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
