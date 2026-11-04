<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { createVenue, listVenues, updateVenue, updateVenueStatus } from '../api';
import type { Venue } from '../api/types';
import { useAuthStore } from '../stores/auth';

const auth = useAuthStore();

const loading = ref(false);
const venues = ref<Venue[]>([]);
const total = ref(0);

const query = reactive({
  page: 1,
  pageSize: 20,
  city: '',
  status: '',
  name: '',
});

async function loadVenues() {
  loading.value = true;
  try {
    const resp = await listVenues({
      page: query.page,
      pageSize: query.pageSize,
      city: query.city || undefined,
      status: query.status || undefined,
      name: query.name || undefined,
    });
    venues.value = resp.data.items;
    total.value = resp.data.total;
  } catch (e: any) {
    ElMessage.error(e?.message ?? '加载场馆失败');
  } finally {
    loading.value = false;
  }
}

function onSearch() {
  query.page = 1;
  loadVenues();
}

/* ===== 新建 / 编辑 ===== */
const formVisible = ref(false);
const isEdit = ref(false);
const form = reactive({
  id: '',
  name: '',
  countryCode: 'CN',
  province: '',
  city: '',
  district: '',
  address: '',
  timezone: 'Asia/Shanghai',
  maxCapacity: 1000,
  description: '',
});

function openCreate() {
  isEdit.value = false;
  Object.assign(form, {
    id: '', name: '', countryCode: 'CN', province: '', city: '', district: '',
    address: '', timezone: 'Asia/Shanghai', maxCapacity: 1000, description: '',
  });
  formVisible.value = true;
}

function openEdit(row: Venue) {
  isEdit.value = true;
  Object.assign(form, {
    id: row.id,
    name: row.name,
    countryCode: row.countryCode,
    province: row.province ?? '',
    city: row.city,
    district: row.district ?? '',
    address: row.address,
    timezone: row.timezone,
    maxCapacity: row.maxCapacity,
    description: row.description ?? '',
  });
  formVisible.value = true;
}

async function onSubmit() {
  const payload = {
    name: form.name,
    countryCode: form.countryCode,
    province: form.province || null,
    city: form.city,
    district: form.district || null,
    address: form.address,
    timezone: form.timezone,
    maxCapacity: form.maxCapacity,
    description: form.description || null,
  };
  try {
    if (isEdit.value) {
      await updateVenue(form.id, payload);
      ElMessage.success('场馆修改成功');
    } else {
      await createVenue(payload);
      ElMessage.success('场馆创建成功');
    }
    formVisible.value = false;
    loadVenues();
  } catch (e: any) {
    ElMessage.error(e?.message ?? '保存失败');
  }
}

/* ===== 启用 / 停用 ===== */
async function onToggleStatus(row: Venue) {
  const target = row.status === 'ENABLED' ? 'DISABLED' : 'ENABLED';
  const actionText = target === 'DISABLED' ? '停用' : '启用';
  try {
    await ElMessageBox.confirm(`确认${actionText}场馆「${row.name}」？`, '提示', { type: 'warning' });
  } catch {
    return;
  }
  try {
    await updateVenueStatus(row.id, { status: target });
    ElMessage.success(`${actionText}成功`);
    loadVenues();
  } catch (e: any) {
    ElMessage.error(e?.message ?? `${actionText}失败`);
  }
}

onMounted(loadVenues);
</script>

<template>
  <div>
    <div class="toolbar">
      <h2 class="page-title">场馆管理</h2>
      <el-button v-if="auth.hasPermission('event:venue:create')" type="primary" @click="openCreate">
        新建场馆
      </el-button>
    </div>

    <el-card shadow="never" class="filter">
      <el-form inline>
        <el-form-item label="城市">
          <el-input v-model="query.city" placeholder="精确城市，如：北京" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 140px">
            <el-option label="启用" value="ENABLED" />
            <el-option label="停用" value="DISABLED" />
          </el-select>
        </el-form-item>
        <el-form-item label="名称">
          <el-input v-model="query.name" placeholder="模糊搜索" clearable style="width: 180px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onSearch">查询</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <el-table :data="venues" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" width="170" show-overflow-tooltip />
        <el-table-column prop="name" label="场馆名称" min-width="140" />
        <el-table-column label="城市" min-width="100">
          <template #default="{ row }">{{ row.province ? `${row.province} · ` : '' }}{{ row.city }}</template>
        </el-table-column>
        <el-table-column prop="district" label="区县" min-width="90" />
        <el-table-column prop="address" label="详细地址" min-width="180" show-overflow-tooltip />
        <el-table-column prop="maxCapacity" label="核定容量" width="100" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ENABLED' ? 'success' : 'info'" size="small">
              {{ row.status === 'ENABLED' ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button v-if="auth.hasPermission('event:venue:update')" size="small" @click="openEdit(row)">
              编辑
            </el-button>
            <el-button
              v-if="auth.hasPermission('event:venue:toggle')"
              size="small"
              :type="row.status === 'ENABLED' ? 'danger' : 'success'"
              @click="onToggleStatus(row)"
            >
              {{ row.status === 'ENABLED' ? '停用' : '启用' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        class="pagination"
        layout="total, prev, pager, next"
        :total="total"
        :page-size="query.pageSize"
        :current-page="query.page"
        @current-change="(p: number) => { query.page = p; loadVenues(); }"
      />
    </el-card>

    <el-dialog v-model="formVisible" :title="isEdit ? '编辑场馆' : '新建场馆'" width="620px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="场馆名称" required>
          <el-input v-model="form.name" placeholder="如：星光体育馆" />
        </el-form-item>
        <el-form-item label="国家代码">
          <el-input v-model="form.countryCode" placeholder="ISO 国家代码，默认 CN" />
        </el-form-item>
        <el-form-item label="省份">
          <el-input v-model="form.province" placeholder="如：北京市" />
        </el-form-item>
        <el-form-item label="城市" required>
          <el-input v-model="form.city" placeholder="如：北京" />
        </el-form-item>
        <el-form-item label="区县">
          <el-input v-model="form.district" placeholder="如：朝阳区" />
        </el-form-item>
        <el-form-item label="详细地址" required>
          <el-input v-model="form.address" placeholder="线下观演地点" />
        </el-form-item>
        <el-form-item label="时区">
          <el-input v-model="form.timezone" placeholder="IANA 时区，默认 Asia/Shanghai" />
        </el-form-item>
        <el-form-item label="核定容量" required>
          <el-input-number v-model="form.maxCapacity" :min="1" :step="100" style="width: 100%" />
        </el-form-item>
        <el-form-item label="简介">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="场馆简介、入场说明" />
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
.filter {
  margin-bottom: 16px;
}
.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
