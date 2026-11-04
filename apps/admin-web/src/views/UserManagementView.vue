<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  assignRoles,
  createStaff,
  listRoles,
  listUsers,
  resetPassword,
  revokeSessions,
  updateStaff,
  updateUserStatus,
} from '../api';
import type { AdminUser, Role } from '../api/types';
import { useAuthStore } from '../stores/auth';

const auth = useAuthStore();

const loading = ref(false);
const users = ref<AdminUser[]>([]);
const total = ref(0);
const roles = ref<Role[]>([]);

const query = reactive({
  page: 1,
  pageSize: 20,
  username: '',
  status: '',
  role: '',
});

const statusText: Record<string, string> = {
  ACTIVE: '正常',
  DISABLED: '已禁用',
  DELETION_PENDING: '注销中',
  DELETED: '已删除',
};

async function loadUsers() {
  loading.value = true;
  try {
    const resp = await listUsers({
      page: query.page,
      pageSize: query.pageSize,
      username: query.username || undefined,
      status: query.status || undefined,
      role: query.role || undefined,
    });
    users.value = resp.data.items;
    total.value = resp.data.total;
  } catch (e: any) {
    ElMessage.error(e?.message ?? '加载用户失败');
  } finally {
    loading.value = false;
  }
}

async function loadRoles() {
  const resp = await listRoles();
  roles.value = resp.data;
}

function onSearch() {
  query.page = 1;
  loadUsers();
}

/* ===== 新建员工 ===== */
const createVisible = ref(false);
const createForm = reactive({ username: '', email: '', nickname: '', password: '', roleIds: [] as number[] });

function openCreate() {
  Object.assign(createForm, { username: '', email: '', nickname: '', password: '', roleIds: [] });
  createVisible.value = true;
}

async function onCreate() {
  try {
    await createStaff({ ...createForm });
    ElMessage.success('员工创建成功');
    createVisible.value = false;
    loadUsers();
  } catch (e: any) {
    ElMessage.error(e?.message ?? '创建失败');
  }
}

/* ===== 编辑员工 ===== */
const editVisible = ref(false);
const editForm = reactive({ id: '', nickname: '', email: '' });

function openEdit(row: AdminUser) {
  Object.assign(editForm, { id: row.id, nickname: row.nickname, email: row.email });
  editVisible.value = true;
}

async function onEdit() {
  try {
    await updateStaff(editForm.id, { nickname: editForm.nickname, email: editForm.email });
    ElMessage.success('修改成功');
    editVisible.value = false;
    loadUsers();
  } catch (e: any) {
    ElMessage.error(e?.message ?? '修改失败');
  }
}

/* ===== 启用/禁用 ===== */
async function onToggleStatus(row: AdminUser) {
  const target = row.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE';
  const actionText = target === 'DISABLED' ? '禁用' : '启用';
  try {
    await ElMessageBox.confirm(`确认${actionText}账号「${row.username}」？`, '提示', { type: 'warning' });
  } catch {
    return;
  }
  try {
    await updateUserStatus(row.id, { status: target });
    ElMessage.success(`${actionText}成功`);
    loadUsers();
  } catch (e: any) {
    ElMessage.error(e?.message ?? `${actionText}失败`);
  }
}

/* ===== 分配角色 ===== */
const roleVisible = ref(false);
const roleForm = reactive({ id: '', username: '', roleIds: [] as number[] });

function openAssignRoles(row: AdminUser) {
  roleForm.id = row.id;
  roleForm.username = row.username;
  roleForm.roleIds = row.roles
    .map((code) => roles.value.find((r) => r.code === code)?.id)
    .filter((id): id is string => id !== undefined)
    .map((id) => Number(id));
  roleVisible.value = true;
}

async function onAssignRoles() {
  try {
    await assignRoles(roleForm.id, { roleIds: roleForm.roleIds });
    ElMessage.success('角色分配成功，相关会话已下线');
    roleVisible.value = false;
    loadUsers();
  } catch (e: any) {
    ElMessage.error(e?.message ?? '分配失败');
  }
}

/* ===== 重置密码 ===== */
const pwdVisible = ref(false);
const pwdForm = reactive({ id: '', username: '', newPassword: '' });

function openResetPassword(row: AdminUser) {
  pwdForm.id = row.id;
  pwdForm.username = row.username;
  pwdForm.newPassword = '';
  pwdVisible.value = true;
}

async function onResetPassword() {
  try {
    await resetPassword(pwdForm.id, { newPassword: pwdForm.newPassword });
    ElMessage.success('密码已重置，相关会话已下线');
    pwdVisible.value = false;
  } catch (e: any) {
    ElMessage.error(e?.message ?? '重置失败');
  }
}

/* ===== 强制下线 ===== */
async function onRevoke(row: AdminUser) {
  try {
    await ElMessageBox.confirm(`确认强制下线账号「${row.username}」的全部会话？`, '提示', { type: 'warning' });
  } catch {
    return;
  }
  try {
    await revokeSessions(row.id);
    ElMessage.success('已强制下线');
  } catch (e: any) {
    ElMessage.error(e?.message ?? '操作失败');
  }
}

onMounted(() => {
  loadUsers();
  loadRoles();
});
</script>

<template>
  <div>
    <div class="toolbar">
      <h2 class="page-title">用户管理</h2>
      <el-button v-if="auth.hasPermission('admin:user:create')" type="primary" @click="openCreate">
        新建员工
      </el-button>
    </div>

    <el-card shadow="never" class="filter">
      <el-form inline>
        <el-form-item label="用户名">
          <el-input v-model="query.username" placeholder="模糊搜索" clearable style="width: 180px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 140px">
            <el-option label="正常" value="ACTIVE" />
            <el-option label="已禁用" value="DISABLED" />
          </el-select>
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="query.role" placeholder="全部" clearable style="width: 160px">
            <el-option v-for="r in roles" :key="r.code" :label="r.name" :value="r.code" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onSearch">查询</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <el-table :data="users" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" width="160" show-overflow-tooltip />
        <el-table-column prop="username" label="用户名" min-width="120" />
        <el-table-column prop="nickname" label="昵称" min-width="120" />
        <el-table-column prop="email" label="邮箱" min-width="180" show-overflow-tooltip />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'danger'" size="small">
              {{ statusText[row.status] ?? row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="角色" min-width="140">
          <template #default="{ row }">
            <el-tag v-for="code in row.roles" :key="code" size="small" class="role-tag">{{ code }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="320" fixed="right">
          <template #default="{ row }">
            <el-button v-if="auth.hasPermission('admin:user:update')" size="small" @click="openEdit(row)">
              编辑
            </el-button>
            <el-button v-if="auth.hasPermission('admin:user:roles')" size="small" @click="openAssignRoles(row)">
              角色
            </el-button>
            <el-button
              v-if="auth.hasPermission('admin:user:status')"
              size="small"
              :type="row.status === 'ACTIVE' ? 'danger' : 'success'"
              @click="onToggleStatus(row)"
            >
              {{ row.status === 'ACTIVE' ? '禁用' : '启用' }}
            </el-button>
            <el-dropdown v-if="auth.hasPermission('admin:user:reset-pwd') || auth.hasPermission('admin:user:revoke')">
              <el-button size="small">更多</el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item
                    v-if="auth.hasPermission('admin:user:reset-pwd')"
                    @click="openResetPassword(row)"
                  >
                    重置密码
                  </el-dropdown-item>
                  <el-dropdown-item
                    v-if="auth.hasPermission('admin:user:revoke')"
                    @click="onRevoke(row)"
                  >
                    强制下线
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        class="pagination"
        layout="total, prev, pager, next"
        :total="total"
        :page-size="query.pageSize"
        :current-page="query.page"
        @current-change="(p: number) => { query.page = p; loadUsers(); }"
      />
    </el-card>

    <!-- 新建员工 -->
    <el-dialog v-model="createVisible" title="新建员工" width="480px">
      <el-form label-width="90px">
        <el-form-item label="用户名">
          <el-input v-model="createForm.username" placeholder="4-32 位字母/数字/下划线" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="createForm.email" />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="createForm.nickname" />
        </el-form-item>
        <el-form-item label="初始密码">
          <el-input v-model="createForm.password" type="password" show-password placeholder="12-64 位" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="createForm.roleIds" multiple placeholder="选择角色" style="width: 100%">
            <el-option v-for="r in roles" :key="r.id" :label="`${r.name}（${r.code}）`" :value="Number(r.id)" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" @click="onCreate">确定</el-button>
      </template>
    </el-dialog>

    <!-- 编辑员工 -->
    <el-dialog v-model="editVisible" title="编辑员工" width="440px">
      <el-form label-width="70px">
        <el-form-item label="昵称">
          <el-input v-model="editForm.nickname" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="editForm.email" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="onEdit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 分配角色 -->
    <el-dialog v-model="roleVisible" :title="`分配角色 - ${roleForm.username}`" width="440px">
      <el-select v-model="roleForm.roleIds" multiple placeholder="选择角色" style="width: 100%">
        <el-option v-for="r in roles" :key="r.id" :label="`${r.name}（${r.code}）`" :value="Number(r.id)" />
      </el-select>
      <template #footer>
        <el-button @click="roleVisible = false">取消</el-button>
        <el-button type="primary" @click="onAssignRoles">确定</el-button>
      </template>
    </el-dialog>

    <!-- 重置密码 -->
    <el-dialog v-model="pwdVisible" :title="`重置密码 - ${pwdForm.username}`" width="440px">
      <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="新密码（12-64 位）" />
      <template #footer>
        <el-button @click="pwdVisible = false">取消</el-button>
        <el-button type="primary" @click="onResetPassword">确定</el-button>
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
.role-tag {
  margin-right: 4px;
}
.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
