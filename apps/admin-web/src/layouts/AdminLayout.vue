<script setup lang="ts">
import { computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { adminLogout } from '../api';
import { useAuthStore } from '../stores/auth';

const route = useRoute();
const router = useRouter();
const auth = useAuthStore();

interface MenuItem {
  key: string;
  path: string;
  label: string;
  permission: string | null;
}

const allMenus: MenuItem[] = [
  { key: 'dashboard', path: '/', label: '工作台', permission: null },
  { key: 'users', path: '/users', label: '用户管理', permission: 'admin:user:list' },
  { key: 'roles', path: '/roles', label: '角色权限', permission: 'admin:role:list' },
  { key: 'audit', path: '/audit', label: '审计日志', permission: 'admin:audit:list' },
  { key: 'venues', path: '/venues', label: '场馆管理', permission: 'event:venue:list' },
  { key: 'categories', path: '/categories', label: '分类管理', permission: 'event:category:list' },
  { key: 'events', path: '/events', label: '演出管理', permission: 'event:event:list' },
  { key: 'campaigns', path: '/campaigns', label: '营销活动', permission: 'campaign:manage' },
  { key: 'coupon-templates', path: '/coupon-templates', label: '优惠券模板', permission: 'coupon:manage' },
  { key: 'orders', path: '/orders', label: '订单管理', permission: 'order:read' },
  { key: 'payments', path: '/payments', label: '支付管理', permission: 'payment:read' },
  { key: 'refunds', path: '/refunds', label: '退款管理', permission: 'refund:read' },
  { key: 'notification-failures', path: '/notification-failures', label: '通知工单', permission: 'notification:read' },
  { key: 'risk', path: '/risk', label: '风控管理', permission: 'risk:read' },
  { key: 'reconciliation', path: '/reconciliation', label: '对账补偿', permission: 'reconciliation:read' },
];

// 按当前用户权限点过滤菜单
const menus = computed(() => allMenus.filter((m) => !m.permission || auth.hasPermission(m.permission)));

const activeMenu = computed(() => {
  const current = allMenus.find((m) => m.path !== '/' && route.path.startsWith(m.path));
  return current?.key ?? 'dashboard';
});

async function onLogout() {
  try {
    await adminLogout();
  } catch {
    // 退出接口失败也继续清理本地登录态
  }
  auth.logout();
  ElMessage.success('已退出登录');
  router.push('/login');
}
</script>

<template>
  <el-container class="admin-layout">
    <el-aside width="220px" class="aside">
      <div class="logo">FlashTicket</div>
      <el-menu
        :default-active="activeMenu"
        router
        background-color="#19182f"
        text-color="#9ca3af"
        active-text-color="#ffffff"
      >
        <el-menu-item v-for="m in menus" :key="m.key" :index="m.path">
          {{ m.label }}
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <div class="flex-1" />
        <span class="username">{{ auth.user?.nickname ?? auth.user?.username ?? '' }}</span>
        <span class="roles">{{ (auth.user?.roles ?? []).join(' / ') }}</span>
        <el-button text @click="onLogout">退出登录</el-button>
      </el-header>
      <el-main><router-view /></el-main>
    </el-container>
  </el-container>
</template>

<style scoped lang="scss">
.admin-layout {
  min-height: 100vh;
}
.aside {
  background: #19182f;
  .logo {
    height: 60px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 18px;
    font-weight: 600;
    color: #fff;
  }
}
.header {
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
  display: flex;
  align-items: center;
  gap: 12px;
  .username {
    font-weight: 500;
  }
  .roles {
    font-size: 12px;
    color: #9ca3af;
  }
}
</style>
