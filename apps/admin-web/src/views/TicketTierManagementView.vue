<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import type { Inventory, InventoryLedger, SalePlan, TicketTier, TicketTierRequest } from '../api/types';
import {
  configureSalePlan,
  createTier,
  getInventory,
  listLedger,
  listTiers,
  pauseTier,
  resumeTier,
  updateTier,
} from '../api';
import { useAuthStore } from '../stores/auth';

const route = useRoute();
const router = useRouter();
const auth = useAuthStore();

const sessionId = route.params.sessionId as string;
const sessionName = (route.query.name as string) ?? '';

const loading = ref(false);
const rows = ref<TicketTier[]>([]);

const fmtCent = (c: number) => `¥${(c / 100).toFixed(2)}`;

function fmtTime(t: string | null) {
  if (!t) return '-';
  return t.replace('T', ' ').slice(0, 16);
}

async function load() {
  loading.value = true;
  try {
    const r = await listTiers(sessionId);
    rows.value = r.data ?? [];
  } finally {
    loading.value = false;
  }
}

// ===== 新建/编辑票档 =====
const dialogVisible = ref(false);
const editingId = ref<string | null>(null);
const saving = ref(false);
const form = reactive<TicketTierRequest>({
  name: '',
  tierType: 'GENERAL_ADMISSION',
  unitPriceCent: 0,
  initialQuantity: 0,
  minPurchase: 1,
  maxPurchase: 2,
  perUserLimit: 2,
  description: '',
  sortOrder: 0,
  version: 0,
});

function openCreate() {
  editingId.value = null;
  Object.assign(form, {
    name: '',
    tierType: 'GENERAL_ADMISSION',
    unitPriceCent: 0,
    initialQuantity: 0,
    minPurchase: 1,
    maxPurchase: 2,
    perUserLimit: 2,
    description: '',
    sortOrder: 0,
    version: 0,
  });
  dialogVisible.value = true;
}

function openEdit(row: TicketTier) {
  editingId.value = row.id;
  Object.assign(form, {
    name: row.name,
    tierType: row.tierType,
    unitPriceCent: row.unitPriceCent,
    initialQuantity: row.initialQuantity,
    minPurchase: row.minPurchase,
    maxPurchase: row.maxPurchase,
    perUserLimit: row.perUserLimit,
    description: row.description ?? '',
    sortOrder: row.sortOrder,
    version: row.version,
  });
  dialogVisible.value = true;
}

async function submit() {
  saving.value = true;
  try {
    const payload: TicketTierRequest = {
      name: form.name,
      tierType: form.tierType || 'GENERAL_ADMISSION',
      unitPriceCent: form.unitPriceCent,
      initialQuantity: form.initialQuantity,
      minPurchase: form.minPurchase,
      maxPurchase: form.maxPurchase,
      perUserLimit: form.perUserLimit,
      description: form.description || null,
      sortOrder: form.sortOrder,
      version: form.version,
    };
    if (editingId.value) {
      await updateTier(editingId.value, payload);
      ElMessage.success('票档已更新');
    } else {
      await createTier(sessionId, payload);
      ElMessage.success('票档已创建');
    }
    dialogVisible.value = false;
    load();
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message ?? '操作失败');
  } finally {
    saving.value = false;
  }
}

// ===== 售票计划 =====
const saleDialogVisible = ref(false);
const saleTier = ref<TicketTier | null>(null);
const saleSaving = ref(false);
const saleForm = reactive({ saleStartAt: '', saleEndAt: '' });

function openSalePlan(row: TicketTier) {
  saleTier.value = row;
  Object.assign(saleForm, {
    saleStartAt: row.salePlan?.saleStartAt ?? '',
    saleEndAt: row.salePlan?.saleEndAt ?? '',
  });
  saleDialogVisible.value = true;
}

async function submitSalePlan() {
  if (!saleForm.saleStartAt || !saleForm.saleEndAt) {
    ElMessage.warning('请填写开售与结束时间');
    return;
  }
  saleSaving.value = true;
  try {
    await configureSalePlan(saleTier.value!.id, {
      saleStartAt: saleForm.saleStartAt,
      saleEndAt: saleForm.saleEndAt,
    });
    ElMessage.success('售票计划已保存');
    saleDialogVisible.value = false;
    load();
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message ?? '操作失败');
  } finally {
    saleSaving.value = false;
  }
}

// ===== 暂停/恢复 =====
async function onPause(row: TicketTier) {
  const { value } = await ElMessageBox.prompt('请输入暂停原因', '暂停销售', {
    confirmButtonText: '确认暂停',
    cancelButtonText: '取消',
    inputValidator: (v: string) => (v && v.trim() ? true : '暂停原因不能为空'),
  });
  try {
    await pauseTier(row.id, { reason: value.trim() });
    ElMessage.success('已暂停销售');
    load();
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message ?? '暂停失败');
  }
}

async function onResume(row: TicketTier) {
  try {
    await resumeTier(row.id);
    ElMessage.success('已恢复销售');
    load();
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message ?? '恢复失败');
  }
}

function saleStatusTag(p: SalePlan | null) {
  if (!p) return 'info';
  return p.saleStatus === 'PAUSED' ? 'danger' : 'success';
}

// ===== 库存查看（M07） =====
const inventoryVisible = ref(false);
const inventoryTier = ref<TicketTier | null>(null);
const inventory = ref<Inventory | null>(null);
const ledger = ref<InventoryLedger[]>([]);
const inventoryLoading = ref(false);

const operationLabel: Record<string, string> = {
  INIT: '初始化',
  RESERVE: '预占',
  CONFIRM: '确认已售',
  RELEASE: '释放',
};

async function openInventory(row: TicketTier) {
  inventoryTier.value = row;
  inventoryVisible.value = true;
  inventory.value = null;
  ledger.value = [];
  inventoryLoading.value = true;
  try {
    const [invResp, ledgerResp] = await Promise.all([getInventory(row.id), listLedger(row.id)]);
    inventory.value = invResp.data;
    ledger.value = ledgerResp.data ?? [];
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message ?? '库存加载失败');
  } finally {
    inventoryLoading.value = false;
  }
}

onMounted(load);
</script>

<template>
  <div class="page">
    <div class="toolbar">
      <el-button @click="router.back()">返回</el-button>
      <span class="title">票档管理{{ sessionName ? `：${sessionName}` : '' }}</span>
      <div class="flex-1" />
      <el-button v-if="auth.hasPermission('event:tier:create')" type="primary" @click="openCreate">
        新建票档
      </el-button>
    </div>

    <el-table v-loading="loading" :data="rows" stripe>
      <el-table-column prop="name" label="票档名称" min-width="120" />
      <el-table-column label="票价" width="110">
        <template #default="{ row }">{{ fmtCent(row.unitPriceCent) }}</template>
      </el-table-column>
      <el-table-column prop="initialQuantity" label="库存" width="80" />
      <el-table-column label="限购" width="130">
        <template #default="{ row }">每单 {{ row.minPurchase }}-{{ row.maxPurchase }} / 每人 {{ row.perUserLimit }}</template>
      </el-table-column>
      <el-table-column label="开售时间" width="150">
        <template #default="{ row }">{{ fmtTime(row.salePlan?.saleStartAt ?? null) }}</template>
      </el-table-column>
      <el-table-column label="结束时间" width="150">
        <template #default="{ row }">{{ fmtTime(row.salePlan?.saleEndAt ?? null) }}</template>
      </el-table-column>
      <el-table-column label="销售状态" width="100">
        <template #default="{ row }">
          <el-tag :type="saleStatusTag(row.salePlan)">
            {{ row.salePlan ? (row.salePlan.saleStatus === 'PAUSED' ? '已暂停' : '销售中') : '未配置' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="360" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="auth.hasPermission('inventory:read')"
            link
            type="info"
            @click="openInventory(row)"
          >
            库存
          </el-button>
          <el-button
            v-if="auth.hasPermission('event:tier:update')"
            link
            type="primary"
            @click="openEdit(row)"
          >
            编辑
          </el-button>
          <el-button
            v-if="auth.hasPermission('event:sale-plan:update')"
            link
            type="primary"
            @click="openSalePlan(row)"
          >
            售票计划
          </el-button>
          <el-button
            v-if="auth.hasPermission('event:sale-plan:pause') && row.salePlan?.saleStatus === 'ENABLED'"
            link
            type="warning"
            @click="onPause(row)"
          >
            暂停
          </el-button>
          <el-button
            v-if="auth.hasPermission('event:sale-plan:resume') && row.salePlan?.saleStatus === 'PAUSED'"
            link
            type="success"
            @click="onResume(row)"
          >
            恢复
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 票档编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑票档' : '新建票档'"
      width="560px"
      :close-on-click-modal="false"
    >
      <el-form label-width="110px">
        <el-form-item label="票档名称" required>
          <el-input v-model="form.name" maxlength="64" placeholder="如 VIP / 内场 / 看台" />
        </el-form-item>
        <el-form-item label="单价（元）" required>
          <el-input-number v-model="form.unitPriceCent" :min="0" :step="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="初始库存" required>
          <el-input-number v-model="form.initialQuantity" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="每单最小购买">
          <el-input-number v-model="form.minPurchase" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="每单最大购买">
          <el-input-number v-model="form.maxPurchase" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="每用户限购">
          <el-input-number v-model="form.perUserLimit" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="排序权重">
          <el-input-number v-model="form.sortOrder" style="width: 100%" />
        </el-form-item>
        <el-form-item label="票档说明">
          <el-input v-model="form.description" type="textarea" :rows="2" maxlength="500" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 售票计划弹窗 -->
    <el-dialog v-model="saleDialogVisible" title="配置售票计划" width="520px" :close-on-click-modal="false">
      <el-form label-width="90px">
        <el-form-item label="开售时间" required>
          <el-date-picker
            v-model="saleForm.saleStartAt"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            placeholder="开售时间（UTC）"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="结束时间" required>
          <el-date-picker
            v-model="saleForm.saleEndAt"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            placeholder="售票结束（UTC）"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="saleDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saleSaving" @click="submitSalePlan">保存</el-button>
      </template>
    </el-dialog>

    <!-- 库存查看弹窗（M07） -->
    <el-dialog
      v-model="inventoryVisible"
      :title="`库存账本${inventoryTier ? `：${inventoryTier.name}` : ''}`"
      width="620px"
      :close-on-click-modal="false"
    >
      <div v-loading="inventoryLoading">
        <div v-if="inventory" class="grid grid-cols-4 gap-3">
          <div class="rounded-lg bg-gray-50 p-4 text-center">
            <div class="text-2xl font-bold text-[var(--brand)]">{{ inventory.total }}</div>
            <div class="mt-1 text-xs text-gray-500">总库存</div>
          </div>
          <div class="rounded-lg bg-gray-50 p-4 text-center">
            <div class="text-2xl font-bold text-green-600">{{ inventory.available }}</div>
            <div class="mt-1 text-xs text-gray-500">可售</div>
          </div>
          <div class="rounded-lg bg-gray-50 p-4 text-center">
            <div class="text-2xl font-bold text-amber-500">{{ inventory.reserved }}</div>
            <div class="mt-1 text-xs text-gray-500">预占中</div>
          </div>
          <div class="rounded-lg bg-gray-50 p-4 text-center">
            <div class="text-2xl font-bold text-blue-500">{{ inventory.sold }}</div>
            <div class="mt-1 text-xs text-gray-500">已售</div>
          </div>
        </div>
        <el-empty v-else-if="!inventoryLoading" description="库存尚未初始化（活动未发布）" />

        <el-divider content-position="left">库存流水</el-divider>
        <el-table :data="ledger" size="small" max-height="260" stripe>
          <el-table-column label="操作" width="100">
            <template #default="{ row }">{{ operationLabel[row.operation] ?? row.operation }}</template>
          </el-table-column>
          <el-table-column prop="quantity" label="数量" width="80" />
          <el-table-column label="时间" min-width="160">
            <template #default="{ row }">{{ fmtTime(row.createdAt) }}</template>
          </el-table-column>
        </el-table>
      </div>
      <template #footer>
        <el-button @click="inventoryVisible = false">关闭</el-button>
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
