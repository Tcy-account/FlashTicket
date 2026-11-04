import { createRouter, createWebHistory } from 'vue-router';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'home', component: () => import('../views/HomeView.vue') },
    { path: '/login', name: 'login', component: () => import('../views/LoginView.vue') },
    { path: '/register', name: 'register', component: () => import('../views/RegisterView.vue') },
    { path: '/forgot', name: 'forgot', component: () => import('../views/ForgotPasswordView.vue') },
    { path: '/profile', name: 'profile', component: () => import('../views/ProfileView.vue') },
    { path: '/password', name: 'password', component: () => import('../views/ChangePasswordView.vue') },
    { path: '/events/:id', name: 'event-detail', component: () => import('../views/EventDetailView.vue') },
    { path: '/purchases/:requestId', name: 'purchase-queue', component: () => import('../views/PurchaseQueueView.vue') },
    { path: '/orders', name: 'orders', component: () => import('../views/OrderListView.vue') },
    { path: '/orders/:id', name: 'order-detail', component: () => import('../views/OrderDetailView.vue') },
    { path: '/entitlements', name: 'entitlements', component: () => import('../views/EntitlementListView.vue') },
    { path: '/entitlements/:id', name: 'entitlement-detail', component: () => import('../views/EntitlementDetailView.vue') },
    { path: '/coupons', name: 'coupons', component: () => import('../views/CouponCenterView.vue') },
    { path: '/coupons/mine', name: 'my-coupons', component: () => import('../views/MyCouponsView.vue') },
    { path: '/notifications', name: 'notifications', component: () => import('../views/MessageCenterView.vue') },
    { path: '/dev/api-lab', name: 'api-lab', component: () => import('../views/ApiLabView.vue') },
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
});

export default router;
