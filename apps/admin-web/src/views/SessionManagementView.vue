<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import type { Session, SessionRequest, SessionStatus, Venue } from '../api/types';
import {
  cancelSession,
  createSession,
  listSessions,
  listVenues,
  postponeSession,
  updateSession,
} from '../api';
import { useAuthStore } from '../stores/auth';

const route = useRoute();
const router = useRouter();
const auth = useAuthStore();

const eventId = route.params.eventId as string;
const eventTitle = (route.query.title as string) ?? '';

function goTiers(row: Session) {
  router.push({ path: `/sessions/${row.id}/tiers`, query: { name: row.name } });
}

const statusMap: Record<SessionStatus, string> = {
  SCHEDULED: '正常',
  POSTPONED: '已延期',
  CANCELLED: '已取消',
  COMPLETED: '已结束',
};

const statusTag: Record<SessionStatus, string> = {
  SCHEDULED: 'success',
  POSTPONED: 'warning',
  CANCELLED: 'danger',
  COMPLETED: 'info',
};

const loading = ref(false);
const rows = ref<Session[]>([]);
const venues = ref<Venue[]>([]);

const venueName = (id: string) => venues.value.find((v) => v.id === id)?.name ?? id;

async function load() {
  loading.value = true;
  try {
    const r = await listSessions(eventId);
    rows.value = r.data ?? [];
  } finally {
    loading.value = false;
  }
}

async function loadVenues() {
  const r = await listVenues({ page: 1, pageSize: 200 });
  venues.value = (r.data?.items ?? []).filter((v) => v.status === 'ENABLED');
}

// ===== 新建/编辑 =====
const dialogVisible = ref(false);
const editingId = ref<string | null>(null);
const saving = ref(false);
const form = reactive<SessionRequest>({
  venueId: '',
  name: '',
  startAt: '',
  endAt: '',
  entryStartAt: '',
  saleCapacity: 0,
  version: 0,
});

function openCreate() {
  editingId.value = null;
  Object.assign(form, {
    venueId: venues.value[0]?.id ?? '',
    name: '',
    startAt: '',
    endAt: '',
    entryStartAt: '',
    saleCapacity: 0,
    version: 0,
  });
  dialogVisible.value = true;
}

function openEdit(row: Session) {
  editingId.value = row.id;
  Object.assign(form, {
    venueId: row.venueId,
    name: row.name,
    startAt: row.startAt,
    endAt: row.endAt,
    entryStartAt: row.entryStartAt ?? '',
    saleCapacity: row.saleCapacity,
    version: row.version,
  });
  dialogVisible.value = true;
}

async function submit() {
  if (!form.startAt || !form.endAt) {
    ElMessage.warning('请填写开始与结束时间');
    return;
  }
  saving.value = true;
  try {
    const payload: SessionRequest = {
      venueId: form.venueId,
      name: form.name,
      startAt: form.startAt,
      endAt: form.endAt,
      entryStartAt: form.entryStartAt || null,
      saleCapacity: form.saleCapacity,
      version: form.version,
    };
    if (editingId.value) {
      await updateSession(editingId.value, payload);
      ElMessage.success('场次已更新');
    } else {
      await createSession(eventId, payload);
      ElMessage.success('场次已创建');
    }
    dialogVisible.value = false;
    load();
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message ?? '操作失败');
  } finally {
    saving.value = false;
  }
}

// ===== 延期 =====
async function onPostpone(row: Session) {
  const { value } = await ElMessageBox.prompt('请输入延期原因', '延期场次', {
    confirmButtonText: '确认延期',
    cancelButtonText: '取消',
    inputValidator: (v: string) => (v && v.trim() ? true : '延期原因不能为空'),
  });
  try {
    await postponeSession(row.id, { reason: value.trim() });
    ElMessage.success('场次已延期');
    load();
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message ?? '延期失败');
  }
}

// ===== 取消 =====
async function onCancel(row: Session) {
  const { value } = await ElMessageBox.prompt('请输入取消原因', '取消场次', {
    confirmButtonText: '确认取消',
    cancelButtonText: '再想想',
    inputValidator: (v: string) => (v && v.trim() ? true : '取消原因不能为空'),
  });
  try {
    await cancelSession(row.id, { reason: value.trim() });
    ElMessage.success('场次已取消');
    load();
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message ?? '取消失败');
  }
}

function fmtTime(t: string | null) {
  if (!t) return '-';
  return t.replace('T', ' ').slice(0, 16);
}

onMounted(() => {
  loadVenues();
  load();
});
</script>

<template>
  <div class="page">
    <div class="toolbar">
      <el-button @click="router.back()">返回</el-button>
      <span class="title">场次管理：{{ eventTitle || eventId }}</span>
      <div class="flex-1" />
      <el-button v-if="auth.hasPermission('event:session:create')" type="primary" @click="openCreate">
        新建场次
      </el-button>
    </div>

    <el-table v-loading="loading" :data="rows" stripe>
      <el-table-column prop="name" label="场次名称" min-width="120" />
      <el-table-column label="场馆" min-width="140">
        <template #default="{ row }">{{ venueName(row.venueId) }}</template>
      </el-table-column>
      <el-table-column label="开始时间" width="150">
        <template #default="{ row }">{{ fmtTime(row.startAt) }}</template>
      </el-table-column>
      <el-table-column label="结束时间" width="150">
        <template #default="{ row }">{{ fmtTime(row.endAt) }}</template>
      </el-table-column>
      <el-table-column prop="saleCapacity" label="可售容量" width="90" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusTag[row.status as SessionStatus]">{{ statusMap[row.status as SessionStatus] }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="260" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="auth.hasPermission('event:tier:list')"
            link
            type="primary"
            @click="goTiers(row)"
          >
            票档
          </el-button>
          <el-button
            v-if="auth.hasPermission('event:session:update') && row.status === 'SCHEDULED'"
            link
            type="primary"
            @click="openEdit(row)"
          >
            编辑
          </el-button>
          <el-button
            v-if="auth.hasPermission('event:session:postpone') && row.status === 'SCHEDULED'"
            link
            type="warning"
            @click="onPostpone(row)"
          >
            延期
          </el-button>
          <el-button
            v-if="auth.hasPermission('event:session:cancel') && (row.status === 'SCHEDULED' || row.status === 'POSTPONED')"
            link
            type="danger"
            @click="onCancel(row)"
          >
            取消
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑场次' : '新建场次'"
      width="560px"
      :close-on-click-modal="false"
    >
      <el-form label-width="90px">
        <el-form-item label="场馆" required>
          <el-select v-model="form.venueId" placeholder="选择场馆" style="width: 100%">
            <el-option v-for="v in venues" :key="v.id" :label="`${v.name}（${v.city}）`" :value="v.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="场次名称" required>
          <el-input v-model="form.name" maxlength="64" />
        </el-form-item>
        <el-form-item label="开始时间" required>
          <el-date-picker
            v-model="form.startAt"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            placeholder="演出开始（UTC）"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="结束时间" required>
          <el-date-picker
            v-model="form.endAt"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            placeholder="预计结束（UTC）"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="入场时间">
          <el-date-picker
            v-model="form.entryStartAt"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            placeholder="可选"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="可售容量" required>
          <el-input-number v-model="form.saleCapacity" :min="0" style="width: 100%" />
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
.title {
  font-size: 16px;
  font-weight: 600;
}
.flex-1 {
  flex: 1;
}
</style>
