<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { getRolePermissions, grantRolePermissions, listPermissions, listRoles } from '../api';
import type { Permission, Role } from '../api/types';
import { useAuthStore } from '../stores/auth';

const auth = useAuthStore();

const roles = ref<Role[]>([]);
const allPermissions = ref<Permission[]>([]);
const selectedRole = ref<Role | null>(null);
const checkedPermissionIds = ref<number[]>([]);
const loading = ref(false);
const saving = ref(false);

const canGrant = computed(() => auth.hasPermission('admin:role:grant'));

async function loadRoles() {
  const resp = await listRoles();
  roles.value = resp.data;
  if (roles.value.length && !selectedRole.value) {
    await selectRole(roles.value[0]);
  }
}

async function loadPermissions() {
  const resp = await listPermissions();
  allPermissions.value = resp.data;
}

async function selectRole(role: Role) {
  selectedRole.value = role;
  loading.value = true;
  try {
    const resp = await getRolePermissions(role.id);
    checkedPermissionIds.value = resp.data.map((p) => Number(p.id));
  } catch (e: any) {
    ElMessage.error(e?.message ?? '加载角色权限失败');
  } finally {
    loading.value = false;
  }
}

async function onSave() {
  if (!selectedRole.value) return;
  saving.value = true;
  try {
    await grantRolePermissions(selectedRole.value.id, { permissionIds: checkedPermissionIds.value });
    ElMessage.success('权限已保存，相关用户会话已下线');
  } catch (e: any) {
    ElMessage.error(e?.message ?? '保存失败');
  } finally {
    saving.value = false;
  }
}

onMounted(() => {
  loadPermissions();
  loadRoles();
});
</script>

<template>
  <div>
    <div class="toolbar">
      <h2 class="page-title">角色权限</h2>
      <el-button
        v-if="canGrant"
        type="primary"
        :disabled="!selectedRole"
        :loading="saving"
        @click="onSave"
      >
        保存权限
      </el-button>
    </div>

    <el-alert
      v-if="!canGrant"
      title="当前角色仅有查看权限，配置角色权限需超级管理员"
      type="info"
      :closable="false"
      class="tip"
    />

    <el-card shadow="never">
      <el-row :gutter="16">
        <el-col :span="7">
          <div class="role-list">
            <div
              v-for="r in roles"
              :key="r.id"
              class="role-item"
              :class="{ active: selectedRole?.id === r.id }"
              @click="selectRole(r)"
            >
              <div class="role-name">{{ r.name }}</div>
              <div class="role-code">{{ r.code }}</div>
            </div>
          </div>
        </el-col>
        <el-col :span="17">
          <div v-loading="loading" class="perm-panel">
            <template v-if="selectedRole">
              <h3 class="perm-title">{{ selectedRole.name }} 的权限点</h3>
              <el-checkbox-group v-model="checkedPermissionIds" :disabled="!canGrant" class="perm-group">
                <el-checkbox
                  v-for="p in allPermissions"
                  :key="p.id"
                  :value="Number(p.id)"
                  class="perm-item"
                >
                  <span class="perm-name">{{ p.name }}</span>
                  <span class="perm-code">{{ p.code }}</span>
                </el-checkbox>
              </el-checkbox-group>
            </template>
            <el-empty v-else description="请选择角色" />
          </div>
        </el-col>
      </el-row>
    </el-card>
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
.tip {
  margin-bottom: 16px;
}
.role-list {
  border-right: 1px solid #e5e7eb;
  min-height: 320px;
}
.role-item {
  padding: 14px 16px;
  cursor: pointer;
  border-radius: 6px;
  margin-bottom: 6px;
  transition: background 0.2s;
}
.role-item:hover {
  background: #f5f6fa;
}
.role-item.active {
  background: #ecf5ff;
}
.role-name {
  font-weight: 600;
}
.role-code {
  font-size: 12px;
  color: #9ca3af;
  margin-top: 2px;
}
.perm-panel {
  min-height: 320px;
  padding: 0 8px;
}
.perm-title {
  margin: 0 0 16px;
  font-size: 16px;
}
.perm-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.perm-item {
  height: auto;
  align-items: flex-start;
}
.perm-name {
  margin-right: 8px;
}
.perm-code {
  font-size: 12px;
  color: #9ca3af;
}
</style>
