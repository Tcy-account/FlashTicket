<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import {
  createRiskRestriction,
  createRiskRule,
  listRiskOperations,
  listRiskRestrictions,
  listRiskRules,
  revokeRiskRestriction,
  toggleRiskRule,
} from '../api';
import type { RiskOperation, RiskRestriction, RiskRule } from '../api/types';

const activeTab = ref('restrictions');

const restrictions = ref<RiskRestriction[]>([]);
const rules = ref<RiskRule[]>([]);
const operations = ref<RiskOperation[]>([]);
const loading = ref(false);

const restrictionDialog = ref(false);
const restrictionForm = ref({
  subjectType: 'USER',
  subjectId: '',
  scope: 'PURCHASE',
  reason: '',
  expireAt: '',
});

const ruleDialog = ref(false);
const ruleForm = ref({
  ruleCode: '',
  name: '',
  scene: 'PURCHASE',
  action: 'THROTTLE',
  config: '{}',
  priority: 0,
});

async function loadRestrictions() {
  loading.value = true;
  try {
    const r = await listRiskRestrictions();
    restrictions.value = r.data;
  } catch (e: any) {
    ElMessage.error(e?.message ?? '加载限制失败');
  } finally {
    loading.value = false;
  }
}

async function loadRules() {
  try {
    const r = await listRiskRules();
    rules.value = r.data;
  } catch (e: any) {
    ElMessage.error(e?.message ?? '加载规则失败');
  }
}

async function loadOperations() {
  try {
    const r = await listRiskOperations();
    operations.value = r.data;
  } catch (e: any) {
    ElMessage.error(e?.message ?? '加载审计失败');
  }
}

async function submitRestriction() {
  try {
    await createRiskRestriction({
      subjectType: restrictionForm.value.subjectType,
      subjectId: restrictionForm.value.subjectId,
      scope: restrictionForm.value.scope,
      reason: restrictionForm.value.reason,
      expireAt: new Date(restrictionForm.value.expireAt).toISOString().slice(0, 19),
    });
    ElMessage.success('限制已创建');
    restrictionDialog.value = false;
    await loadRestrictions();
  } catch (e: any) {
    ElMessage.error(e?.message ?? '创建失败');
  }
}

async function revoke(row: RiskRestriction) {
  try {
    await revokeRiskRestriction(row.id);
    ElMessage.success('已解除');
    await loadRestrictions();
  } catch (e: any) {
    ElMessage.error(e?.message ?? '解除失败');
  }
}

async function submitRule() {
  try {
    await createRiskRule({
      ruleCode: ruleForm.value.ruleCode,
      name: ruleForm.value.name,
      scene: ruleForm.value.scene,
      action: ruleForm.value.action,
      config: ruleForm.value.config,
      priority: ruleForm.value.priority,
    });
    ElMessage.success('规则已创建');
    ruleDialog.value = false;
    await loadRules();
  } catch (e: any) {
    ElMessage.error(e?.message ?? '创建失败');
  }
}

async function toggleRule(row: RiskRule) {
  try {
    await toggleRiskRule(row.id, row.status !== 'ENABLED');
    ElMessage.success('已切换');
    await loadRules();
  } catch (e: any) {
    ElMessage.error(e?.message ?? '操作失败');
  }
}

function fmtTime(t: string | null) {
  if (!t) return '-';
  return new Date(t).toLocaleString('zh-CN', { hour12: false });
}

onMounted(() => {
  void loadRestrictions();
  void loadRules();
  void loadOperations();
});
</script>

<template>
  <div>
    <h2 class="mb-4 text-lg font-semibold">风控管理</h2>

    <el-tabs v-model="activeTab">
      <!-- 黑名单 / 临时限制 -->
      <el-tab-pane label="临时限制" name="restrictions">
        <div class="mb-4">
          <el-button type="primary" @click="restrictionDialog = true">新建限制</el-button>
        </div>
        <el-table v-loading="loading" :data="restrictions" border stripe>
          <el-table-column prop="subjectType" label="主体类型" width="100" />
          <el-table-column prop="subjectId" label="主体标识" width="200" show-overflow-tooltip />
          <el-table-column prop="scope" label="业务范围" width="110" />
          <el-table-column prop="reason" label="原因" show-overflow-tooltip />
          <el-table-column label="到期时间" width="170">
            <template #default="{ row }">{{ fmtTime(row.expireAt) }}</template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="100" />
          <el-table-column label="操作" width="100" fixed="right">
            <template #default="{ row }">
              <el-button
                v-if="row.status === 'ACTIVE'"
                type="danger"
                size="small"
                @click="revoke(row)"
              >
                解除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- 风控规则 -->
      <el-tab-pane label="风控规则" name="rules">
        <div class="mb-4">
          <el-button type="primary" @click="ruleDialog = true">新建规则</el-button>
        </div>
        <el-table :data="rules" border stripe>
          <el-table-column prop="ruleCode" label="规则码" width="180" />
          <el-table-column prop="name" label="名称" width="160" />
          <el-table-column prop="scene" label="场景" width="110" />
          <el-table-column prop="action" label="动作" width="100" />
          <el-table-column prop="priority" label="优先级" width="90" />
          <el-table-column prop="status" label="状态" width="100" />
          <el-table-column label="操作" width="100" fixed="right">
            <template #default="{ row }">
              <el-button size="small" @click="toggleRule(row)">
                {{ row.status === 'ENABLED' ? '停用' : '启用' }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- 操作审计 -->
      <el-tab-pane label="操作审计" name="operations">
        <el-table :data="operations" border stripe>
          <el-table-column prop="operatorId" label="操作人" width="200" />
          <el-table-column prop="targetType" label="目标类型" width="120" />
          <el-table-column prop="action" label="动作" width="110" />
          <el-table-column prop="reason" label="原因" show-overflow-tooltip />
          <el-table-column label="时间" width="170">
            <template #default="{ row }">{{ fmtTime(row.createdAt) }}</template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <!-- 新建限制对话框 -->
    <el-dialog v-model="restrictionDialog" title="新建临时限制" width="480px">
      <el-form label-width="90px">
        <el-form-item label="主体类型">
          <el-select v-model="restrictionForm.subjectType">
            <el-option label="用户" value="USER" />
            <el-option label="IP" value="IP" />
          </el-select>
        </el-form-item>
        <el-form-item label="主体标识">
          <el-input v-model="restrictionForm.subjectId" placeholder="用户 ID 或 IP 地址" />
        </el-form-item>
        <el-form-item label="业务范围">
          <el-select v-model="restrictionForm.scope">
            <el-option label="抢票" value="PURCHASE" />
            <el-option label="领券" value="CLAIM" />
            <el-option label="登录" value="LOGIN" />
            <el-option label="全部" value="ALL" />
          </el-select>
        </el-form-item>
        <el-form-item label="原因">
          <el-input v-model="restrictionForm.reason" placeholder="限制原因" />
        </el-form-item>
        <el-form-item label="到期时间">
          <el-date-picker
            v-model="restrictionForm.expireAt"
            type="datetime"
            placeholder="选择到期时间"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="restrictionDialog = false">取消</el-button>
        <el-button type="primary" @click="submitRestriction">确定</el-button>
      </template>
    </el-dialog>

    <!-- 新建规则对话框 -->
    <el-dialog v-model="ruleDialog" title="新建风控规则" width="480px">
      <el-form label-width="90px">
        <el-form-item label="规则码">
          <el-input v-model="ruleForm.ruleCode" placeholder="唯一规则码" />
        </el-form-item>
        <el-form-item label="名称">
          <el-input v-model="ruleForm.name" />
        </el-form-item>
        <el-form-item label="场景">
          <el-select v-model="ruleForm.scene">
            <el-option label="抢票" value="PURCHASE" />
            <el-option label="领券" value="CLAIM" />
            <el-option label="登录" value="LOGIN" />
            <el-option label="查询" value="QUERY" />
          </el-select>
        </el-form-item>
        <el-form-item label="动作">
          <el-select v-model="ruleForm.action">
            <el-option label="限速" value="THROTTLE" />
            <el-option label="拒绝" value="DENY" />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级">
          <el-input-number v-model="ruleForm.priority" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="ruleDialog = false">取消</el-button>
        <el-button type="primary" @click="submitRule">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>
