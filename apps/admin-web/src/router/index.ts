import { createRouter, createWebHistory } from 'vue-router';
import { useAuthStore } from '../stores/auth';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'login', component: () => import('../views/LoginView.vue') },
    {
      path: '/',
      component: () => import('../layouts/AdminLayout.vue'),
      children: [
        { path: '', name: 'dashboard', component: () => import('../views/DashboardView.vue') },
        { path: 'users', name: 'users', component: () => import('../views/UserManagementView.vue') },
        { path: 'roles', name: 'roles', component: () => import('../views/RolePermissionView.vue') },
        { path: 'audit', name: 'audit', component: () => import('../views/AuditLogView.vue') },
        { path: 'venues', name: 'venues', component: () => import('../views/VenueManagementView.vue') },
        { path: 'categories', name: 'categories', component: () => import('../views/CategoryManagementView.vue') },
        { path: 'events', name: 'events', component: () => import('../views/EventListView.vue') },
        { path: 'events/:eventId/sessions', name: 'sessions', component: () => import('../views/SessionManagementView.vue') },
        { path: 'sessions/:sessionId/tiers', name: 'tiers', component: () => import('../views/TicketTierManagementView.vue') },
        { path: 'campaigns', name: 'campaigns', component: () => import('../views/CampaignManagementView.vue') },
        { path: 'coupon-templates', name: 'coupon-templates', component: () => import('../views/CouponTemplateManagementView.vue') },
        { path: 'orders', name: 'orders', component: () => import('../views/OrderListView.vue') },
        { path: 'payments', name: 'payments', component: () => import('../views/PaymentListView.vue') },
        { path: 'refunds', name: 'refunds', component: () => import('../views/RefundListView.vue') },
        { path: 'notification-failures', name: 'notification-failures', component: () => import('../views/NotificationFailureView.vue') },
        { path: 'risk', name: 'risk', component: () => import('../views/RiskManagementView.vue') },
        { path: 'reconciliation', name: 'reconciliation', component: () => import('../views/ReconciliationView.vue') },
      ],
    },
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
});

// 登录守卫：未登录跳登录页，已登录访问登录页跳工作台
router.beforeEach((to) => {
  const auth = useAuthStore();
  if (to.path !== '/login' && !auth.isLoggedIn) {
    return { path: '/login' };
  }
  if (to.path === '/login' && auth.isLoggedIn) {
    return { path: '/' };
  }
  return true;
});

export default router;
