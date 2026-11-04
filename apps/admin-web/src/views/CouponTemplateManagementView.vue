<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  activateCouponTemplate,
  createCouponTemplate,
  listCampaigns,
  listCouponTemplates,
  pauseCouponTemplate,
  resumeCouponTemplate,
  terminateCouponTemplate,
  updateCouponTemplate,
} from '../api';
import type { Campaign, CouponTemplate, CouponTemplateStatus } from '../api/types';
import { useAuthStore } from '../stores/auth';

const auth = useAuthStore();

const loading = ref(false);
const rows = ref<CouponTemplate[]>([]);
const total = ref(0);
const campaigns = ref<Campaign[]>([]);
const query = reactive({ page: 1, pageSize: 20, status: '', campaignId: '' });

const statusMap: Record<CouponTemplateStatus, { label: string; tag: string }> = {
  DRAFT: { label: '草稿', tag: 'info' },
  ACTIVE: { label: '生效中', tag: 'success' },
  PAUSED: { label: '已暂停', tag: 'warning' },
  TERMINATED: { label: '已终止', tag: 'danger' },
};

const campaignName = (id: string) => campaigns.value.find((c) => c.id === id)?.name ?? id;

const fmtCent = (c: number) => `¥${(c / 100).toFixed(2)}`;

function fmtTime(t: string | null) {
  if (!t) return '-';
  return t.replace('T', ' ').slice(0, 16);
}

/** 优惠规则文案。 */
function ruleText(row: CouponTemplate): string {
  if (row.type === 'FULL_REDUCTION') {
    const threshold = row.thresholdCent > 0 ? `满 ${fmtCent(row.thresholdCent)} ` : '';
    return `${threshold}减 ${fmtCent(row.discountCent)}`;
  }
  const off = (row.discountRate / 100).toFixed(1).replace(/\.0$/, '');
  const cap = row.maxDiscountCent > 0 ? `，最高优惠 ${fmtCent(row.maxDiscountCent)}` : '';
  return `${off} 折${cap}`;
}

async function loadCampaigns() {
  try {
    const r = await listCampaigns({ page: 1, pageSize: 200 });
    campaigns.value = r.data?.items ?? [];
  } catch {
    campaigns.value = [];
  }
}

async function load() {
  loading.value = true;
  try {
    const r = await listCouponTemplates({
      page: query.page,
      pageSize: query.pageSize,
      status: query.status || undefined,
      campaignId: query.campaignId || undefined,
    });
    rows.value = r.data?.items ?? [];
    total.value = r.data?.total ?? 0;
  } catch (e: any) {
    ElMessage.error(e?.message ?? '加载模板失败');
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
  campaignId: '',
  name: '',
  type: 'FULL_REDUCTION' as 'FULL_REDUCTION' | 'PERCENTAGE',
  thresholdYuan: 0,
  discountYuan: 0,
  discountOff: 9,
  maxDiscountYuan: 0,
  totalQuantity: 100,
  perUserLimit: 1,
  claimStartAt: '',
  claimEndAt: '',
  validityType: 'RELATIVE_DAYS' as 'FIXED' | 'RELATIVE_DAYS',
  validFrom: '',
  validUntil: '',
  validDays: 30,
  applicableScope: 'ALL' as 'ALL' | 'EVENT' | 'TIER',
  scopeRefId: '',
});

const isFixedValidity = computed(() => form.validityType === 'FIXED');

function openCreate() {
  isEdit.value = false;
  Object.assign(form, {
    id: '',
    campaignId: '',
    name: '',
    type: 'FULL_REDUCTION',
    thresholdYuan: 0,
    discountYuan: 0,
    discountOff: 9,
    maxDiscountYuan: 0,
    totalQuantity: 100,
    perUserLimit: 1,
    claimStartAt: '',
    claimEndAt: '',
    validityType: 'RELATIVE_DAYS',
    validFrom: '',
    validUntil: '',
    validDays: 30,
    applicableScope: 'ALL',
    scopeRefId: '',
  });
  formVisible.value = true;
}

function openEdit(row: CouponTemplate) {
  isEdit.value = true;
  Object.assign(form, {
    id: row.id,
    campaignId: row.campaignId,
    name: row.name,
    type: row.type,
    thresholdYuan: row.thresholdCent / 100,
    discountYuan: row.discountCent / 100,
    discountOff: row.discountRate / 100,
    maxDiscountYuan: row.maxDiscountCent / 100,
    totalQuantity: row.totalQuantity,
    perUserLimit: row.perUserLimit,
    claimStartAt: row.claimStartAt,
    claimEndAt: row.claimEndAt,
    validityType: row.validityType,
    validFrom: row.validFrom ?? '',
    validUntil: row.validUntil ?? '',
    validDays: row.validDays ?? 30,
    applicableScope: row.applicableScope,
    scopeRefId: row.scopeRefId ?? '',
  });
  formVisible.value = true;
}

async function onSubmit() {
  const payload = {
    campaignId: form.campaignId,
    name: form.name,
    type: form.type,
    thresholdCent: Math.round(form.thresholdYuan * 100),
    discountCent: Math.round(form.discountYuan * 100),
    discountRate: Math.round(form.discountOff * 100),
    maxDiscountCent: Math.round(form.maxDiscountYuan * 100),
    totalQuantity: form.totalQuantity,
    perUserLimit: form.perUserLimit,
    claimStartAt: form.claimStartAt,
    claimEndAt: form.claimEndAt,
    validityType: form.validityType,
    validFrom: isFixedValidity.value ? form.validFrom : null,
    validUntil: isFixedValidity.value ? form.validUntil : null,
    validDays: form.validityType === 'RELATIVE_DAYS' ? form.validDays : null,
    applicableScope: form.applicableScope,
    scopeRefId: form.scopeRefId || null,
  };
  try {
    if (isEdit.value) {
      await updateCouponTemplate(form.id, payload);
      ElMessage.success('模板修改成功');
    } else {
      await createCouponTemplate(payload);
      ElMessage.success('模板创建成功');
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

function onActivate(row: CouponTemplate) {
  ElMessageBox.confirm(`启用后将初始化发行额度并冻结规则字段，确认启用「${row.name}」？`, '启用模板', {
    type: 'warning',
    confirmButtonText: '确认启用',
    cancelButtonText: '取消',
  })
    .then(() => onAction(() => activateCouponTemplate(row.id), '模板已启用'))
    .catch(() => {});
}

function onPause(row: CouponTemplate) {
  ElMessageBox.confirm(`确认暂停「${row.name}」？暂停后用户无法继续领取。`, '暂停模板', { type: 'warning' })
    .then(() => onAction(() => pauseCouponTemplate(row.id), '模板已暂停'))
    .catch(() => {});
}

function onResume(row: CouponTemplate) {
  onAction(() => resumeCouponTemplate(row.id), '模板已恢复');
}

function onTerminate(row: CouponTemplate) {
  ElMessageBox.confirm(`终止后模板永久失效，不可恢复。确认终止「${row.name}」？`, '终止模板', { type: 'warning' })
    .then(() => onAction(() => terminateCouponTemplate(row.id), '模板已终止'))
    .catch(() => {});
}

onMounted(() => {
  loadCampaigns();
  load();
});
</script>

<template>
  <div>
    <div class="toolbar">
      <h2 class="page-title">优惠券模板管理</h2>
      <el-button v-if="auth.hasPermission('coupon:manage')" type="primary" @click="openCreate">
        新建模板
      </el-button>
    </div>

    <el-card shadow="never">
      <div class="mb-4 flex items-center gap-3">
        <el-select v-model="query.status" placeholder="模板状态" clearable style="width: 150px">
          <el-option label="草稿" value="DRAFT" />
          <el-option label="生效中" value="ACTIVE" />
          <el-option label="已暂停" value="PAUSED" />
          <el-option label="已终止" value="TERMINATED" />
        </el-select>
        <el-select v-model="query.campaignId" placeholder="关联活动" clearable filterable style="width: 200px">
          <el-option v-for="c in campaigns" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
        <el-button type="primary" @click="search">查询</el-button>
      </div>

      <el-table v-loading="loading" :data="rows" border stripe>
        <el-table-column prop="name" label="模板名称" min-width="160" show-overflow-tooltip />
        <el-table-column label="关联活动" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">{{ campaignName(row.campaignId) }}</template>
        </el-table-column>
        <el-table-column label="优惠规则" min-width="160">
          <template #default="{ row }">{{ ruleText(row) }}</template>
        </el-table-column>
        <el-table-column label="发行额度" width="110" align="center">
          <template #default="{ row }">{{ row.quotaIssued ?? 0 }} / {{ row.totalQuantity }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusMap[row.status as CouponTemplateStatus].tag" size="small">
              {{ statusMap[row.status as CouponTemplateStatus].label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="领取窗口" width="170">
          <template #default="{ row }">
            {{ fmtTime(row.claimStartAt) }}<br />{{ fmtTime(row.claimEndAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <template v-if="auth.hasPermission('coupon:manage')">
              <el-button v-if="row.status === 'DRAFT'" link type="primary" @click="openEdit(row)">编辑</el-button>
              <el-button v-if="row.status === 'DRAFT'" link type="success" @click="onActivate(row)">启用</el-button>
              <el-button v-if="row.status === 'ACTIVE'" link type="warning" @click="onPause(row)">暂停</el-button>
              <el-button v-if="row.status === 'PAUSED'" link type="success" @click="onResume(row)">恢复</el-button>
              <el-button
                v-if="row.status === 'ACTIVE' || row.status === 'PAUSED'"
                link
                type="danger"
                @click="onTerminate(row)"
              >
                终止
              </el-button>
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

    <el-dialog v-model="formVisible" :title="isEdit ? '编辑模板' : '新建模板'" width="640px">
      <el-form :model="form" label-width="120px">
        <el-form-item label="关联活动" required>
          <el-select v-model="form.campaignId" filterable placeholder="选择活动" style="width: 100%">
            <el-option v-for="c in campaigns" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="模板名称" required>
          <el-input v-model="form.name" placeholder="如：满 200 减 50 券" />
        </el-form-item>
        <el-form-item label="优惠类型" required>
          <el-radio-group v-model="form.type" :disabled="isEdit">
            <el-radio-button label="FULL_REDUCTION">满减券</el-radio-button>
            <el-radio-button label="PERCENTAGE">折扣券</el-radio-button>
          </el-radio-group>
        </el-form-item>

        <template v-if="form.type === 'FULL_REDUCTION'">
          <el-form-item label="门槛金额（元）">
            <el-input-number v-model="form.thresholdYuan" :min="0" :precision="2" :step="10" style="width: 100%" />
          </el-form-item>
          <el-form-item label="满减金额（元）" required>
            <el-input-number v-model="form.discountYuan" :min="0" :precision="2" :step="5" style="width: 100%" />
          </el-form-item>
        </template>
        <template v-else>
          <el-form-item label="折扣（折）" required>
            <el-input-number v-model="form.discountOff" :min="0.1" :max="10" :precision="1" :step="0.5" style="width: 100%" />
            <div class="hint">8.5 表示 85 折，上限 10 折</div>
          </el-form-item>
          <el-form-item label="最高优惠（元）">
            <el-input-number v-model="form.maxDiscountYuan" :min="0" :precision="2" :step="10" style="width: 100%" />
            <div class="hint">0 表示不封顶</div>
          </el-form-item>
        </template>

        <el-form-item label="发行总量" required>
          <el-input-number v-model="form.totalQuantity" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="每人限领" required>
          <el-input-number v-model="form.perUserLimit" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="领取开始" required>
          <el-date-picker v-model="form.claimStartAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="领取结束" required>
          <el-date-picker v-model="form.claimEndAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" />
        </el-form-item>

        <el-form-item label="有效期类型" required>
          <el-radio-group v-model="form.validityType">
            <el-radio-button label="RELATIVE_DAYS">领取后 N 天</el-radio-button>
            <el-radio-button label="FIXED">固定区间</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="form.validityType === 'RELATIVE_DAYS'" label="有效天数" required>
          <el-input-number v-model="form.validDays" :min="1" style="width: 100%" />
        </el-form-item>
        <template v-else>
          <el-form-item label="生效时间" required>
            <el-date-picker v-model="form.validFrom" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" />
          </el-form-item>
          <el-form-item label="失效时间" required>
            <el-date-picker v-model="form.validUntil" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" />
          </el-form-item>
        </template>

        <el-form-item label="适用范围" required>
          <el-radio-group v-model="form.applicableScope">
            <el-radio-button label="ALL">全场通用</el-radio-button>
            <el-radio-button label="EVENT">指定活动</el-radio-button>
            <el-radio-button label="TIER">指定票档</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="form.applicableScope !== 'ALL'" label="关联 ID">
          <el-input v-model="form.scopeRefId" placeholder="活动/票档 ID（雪花 ID）" />
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
