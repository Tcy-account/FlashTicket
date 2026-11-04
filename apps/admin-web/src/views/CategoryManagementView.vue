<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { createCategory, listCategories, updateCategory, updateCategoryStatus } from '../api';
import type { Category } from '../api/types';
import { useAuthStore } from '../stores/auth';

const auth = useAuthStore();

const loading = ref(false);
const categories = ref<Category[]>([]);

async function loadCategories() {
  loading.value = true;
  try {
    const resp = await listCategories();
    categories.value = resp.data;
  } catch (e: any) {
    ElMessage.error(e?.message ?? '加载分类失败');
  } finally {
    loading.value = false;
  }
}

/* ===== 新建 / 编辑 ===== */
const formVisible = ref(false);
const isEdit = ref(false);
const form = reactive({ id: '', code: '', name: '', description: '', sortOrder: 0 });

function openCreate() {
  isEdit.value = false;
  Object.assign(form, { id: '', code: '', name: '', description: '', sortOrder: 0 });
  formVisible.value = true;
}

function openEdit(row: Category) {
  isEdit.value = true;
  Object.assign(form, {
    id: row.id,
    code: row.code,
    name: row.name,
    description: row.description ?? '',
    sortOrder: row.sortOrder,
  });
  formVisible.value = true;
}

async function onSubmit() {
  const payload = {
    code: form.code,
    name: form.name,
    description: form.description || null,
    sortOrder: form.sortOrder,
  };
  try {
    if (isEdit.value) {
      await updateCategory(form.id, payload);
      ElMessage.success('分类修改成功');
    } else {
      await createCategory(payload);
      ElMessage.success('分类创建成功');
    }
    formVisible.value = false;
    loadCategories();
  } catch (e: any) {
    ElMessage.error(e?.message ?? '保存失败');
  }
}

/* ===== 启用 / 停用 ===== */
async function onToggleStatus(row: Category) {
  const target = row.status === 'ENABLED' ? 'DISABLED' : 'ENABLED';
  const actionText = target === 'DISABLED' ? '停用' : '启用';
  try {
    await ElMessageBox.confirm(`确认${actionText}分类「${row.name}」？`, '提示', { type: 'warning' });
  } catch {
    return;
  }
  try {
    await updateCategoryStatus(row.id, { status: target });
    ElMessage.success(`${actionText}成功`);
    loadCategories();
  } catch (e: any) {
    ElMessage.error(e?.message ?? `${actionText}失败`);
  }
}

onMounted(loadCategories);
</script>

<template>
  <div>
    <div class="toolbar">
      <h2 class="page-title">演出分类管理</h2>
      <el-button v-if="auth.hasPermission('event:category:create')" type="primary" @click="openCreate">
        新建分类
      </el-button>
    </div>

    <el-card shadow="never">
      <el-table :data="categories" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" width="170" show-overflow-tooltip />
        <el-table-column prop="code" label="分类码" min-width="140" />
        <el-table-column prop="name" label="分类名称" min-width="120" />
        <el-table-column prop="description" label="说明" min-width="160" show-overflow-tooltip />
        <el-table-column prop="sortOrder" label="排序" width="80" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ENABLED' ? 'success' : 'info'" size="small">
              {{ row.status === 'ENABLED' ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button v-if="auth.hasPermission('event:category:update')" size="small" @click="openEdit(row)">
              编辑
            </el-button>
            <el-button
              v-if="auth.hasPermission('event:category:toggle')"
              size="small"
              :type="row.status === 'ENABLED' ? 'danger' : 'success'"
              @click="onToggleStatus(row)"
            >
              {{ row.status === 'ENABLED' ? '停用' : '启用' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="formVisible" :title="isEdit ? '编辑分类' : '新建分类'" width="480px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="分类码" required>
          <el-input
            v-model="form.code"
            placeholder="大写字母开头，如：BALLET"
            :disabled="isEdit"
          />
          <div v-if="isEdit" class="hint">分类码为稳定业务标识，创建后不可修改</div>
        </el-form-item>
        <el-form-item label="分类名称" required>
          <el-input v-model="form.name" placeholder="如：芭蕾舞剧" />
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="分类说明" />
        </el-form-item>
        <el-form-item label="排序权重">
          <el-input-number v-model="form.sortOrder" :min="0" :step="10" style="width: 100%" />
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
.hint {
  font-size: 12px;
  color: #9ca3af;
  line-height: 1.4;
}
</style>
