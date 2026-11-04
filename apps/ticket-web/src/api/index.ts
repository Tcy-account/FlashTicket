import { createApiClient, axios, setAccessToken } from '@flashticket/api-client';
import type { ApiResponse } from '@flashticket/shared-types';
import type {
  Category,
  ChangePasswordRequest,
  CheckoutRequest,
  CheckoutResponse,
  Entitlement,
  Event,
  ForgotPasswordRequest,
  HealthDetail,
  LoginRequest,
  LoginResponse,
  MockPayRequest,
  Order,
  OrderOperation,
  PageResult,
  Payment,
  PurchaseStatusResponse,
  RegisterRequest,
  RegisterResponse,
  Refund,
  RefundRequest,
  ResetPasswordRequest,
  SendVerifyCodeRequest,
  Session,
  TicketTier,
  TierAvailability,
  UpdateProfileRequest,
  UserProfile,
  Venue,
  ClaimResult,
  CouponAvailable,
  UserCoupon,
  Notification,
  NotificationListResult,
  StreamTokenResponse,
} from './types';

/** 全局 API 实例，所有请求统一走 /api/v1 前缀（Vite 代理到后端 8080）。 */
export const api = createApiClient({
  baseURL: '/api/v1',
  // 刷新失败的最终兜底：清令牌并跳登录
  onUnauthorized: () => {
    setAccessToken(null);
    if (window.location.pathname !== '/login') {
      window.location.href = '/login';
    }
  },
  // 单飞刷新：走独立 axios（不走拦截器），用 HttpOnly Cookie 换取新 Access Token
  refreshAccessToken: async () => {
    const r = await refreshClient.post<ApiResponse<LoginResponse>>('/auth/refresh');
    const body = r.data;
    return body.code === 'SUCCESS' && body.data ? body.data.accessToken : null;
  },
});

/** 刷新专用裸 axios（带 cookie，绕过拦截器避免 401 递归）。 */
const refreshClient = axios.create({ baseURL: '/api/v1', timeout: 10000, withCredentials: true });

/* ============ 健康检查（M00 已可用） ============ */

export function getHealth(): Promise<ApiResponse<HealthDetail>> {
  return api.get('/health').then((r) => r.data);
}

export function getReadiness(): Promise<ApiResponse<HealthDetail>> {
  return api.get('/health/readiness').then((r) => r.data);
}

/* ============ 账号模块 ============ */

export function sendVerifyCode(payload: SendVerifyCodeRequest): Promise<ApiResponse<null>> {
  return api.post('/auth/verify-code/send', payload).then((r) => r.data);
}

export function register(payload: RegisterRequest): Promise<ApiResponse<RegisterResponse>> {
  return api.post('/auth/register', payload).then((r) => r.data);
}

export function login(payload: LoginRequest): Promise<ApiResponse<LoginResponse>> {
  return api.post('/auth/login', payload).then((r) => r.data);
}

export function logout(): Promise<ApiResponse<null>> {
  return api.post('/auth/logout').then((r) => r.data);
}

export function logoutAll(): Promise<ApiResponse<null>> {
  return api.post('/auth/logout-all').then((r) => r.data);
}

export function forgotPassword(payload: ForgotPasswordRequest): Promise<ApiResponse<null>> {
  return api.post('/auth/password/forgot', payload).then((r) => r.data);
}

export function resetPassword(payload: ResetPasswordRequest): Promise<ApiResponse<null>> {
  return api.post('/auth/password/reset', payload).then((r) => r.data);
}

/* ============ 个人中心 ============ */

export function getMe(): Promise<ApiResponse<UserProfile>> {
  return api.get('/users/me').then((r) => r.data);
}

export function updateProfile(payload: UpdateProfileRequest): Promise<ApiResponse<UserProfile>> {
  return api.put('/users/me', payload).then((r) => r.data);
}

export function changePassword(payload: ChangePasswordRequest): Promise<ApiResponse<null>> {
  return api.put('/users/me/password', payload).then((r) => r.data);
}

/* ============ 演出域公开查询（M04） ============ */

export function getCategories(): Promise<ApiResponse<Category[]>> {
  return api.get('/categories').then((r) => r.data);
}

export function getVenues(params?: {
  page?: number;
  pageSize?: number;
  city?: string;
}): Promise<ApiResponse<PageResult<Venue>>> {
  return api.get('/venues', { params }).then((r) => r.data);
}

/* ============ 演出域公开查询（M05） ============ */

export function getEvents(params?: {
  page?: number;
  pageSize?: number;
  categoryId?: string;
  keyword?: string;
}): Promise<ApiResponse<PageResult<Event>>> {
  return api.get('/events', { params }).then((r) => r.data);
}

export function getEvent(id: string): Promise<ApiResponse<Event>> {
  return api.get(`/events/${id}`).then((r) => r.data);
}

export function getEventSessions(id: string): Promise<ApiResponse<Session[]>> {
  return api.get(`/events/${id}/sessions`).then((r) => r.data);
}

/* ============ 演出域公开查询（M06：票档与可售状态） ============ */

export function getSessionTiers(id: string): Promise<ApiResponse<TicketTier[]>> {
  return api.get(`/sessions/${id}/tiers`).then((r) => r.data);
}

export function getTierAvailability(id: string): Promise<ApiResponse<TierAvailability>> {
  return api.get(`/tiers/${id}/availability`).then((r) => r.data);
}

/* ============ 抢票与订单（M08） ============ */

export function checkout(payload: CheckoutRequest): Promise<ApiResponse<CheckoutResponse>> {
  return api.post('/purchases', payload).then((r) => r.data);
}

/* ============ 异步抢票状态（M12） ============ */

export function getPurchaseStatus(requestId: string): Promise<ApiResponse<PurchaseStatusResponse>> {
  return api.get(`/purchases/${requestId}`).then((r) => r.data);
}

export function getMyPurchases(params?: {
  page?: number;
  pageSize?: number;
}): Promise<ApiResponse<PageResult<PurchaseStatusResponse>>> {
  return api.get('/purchases', { params }).then((r) => r.data);
}

export function cancelPurchase(requestId: string): Promise<ApiResponse<PurchaseStatusResponse>> {
  return api.post(`/purchases/${requestId}/cancel`).then((r) => r.data);
}

export function getMyOrders(params?: {
  page?: number;
  pageSize?: number;
  status?: string;
}): Promise<ApiResponse<PageResult<Order>>> {
  return api.get('/orders', { params }).then((r) => r.data);
}

export function getMyOrder(id: string): Promise<ApiResponse<Order>> {
  return api.get(`/orders/${id}`).then((r) => r.data);
}

export function cancelOrder(id: string): Promise<ApiResponse<Order>> {
  return api.post(`/orders/${id}/cancel`).then((r) => r.data);
}

export function getOrderOperations(id: string): Promise<ApiResponse<OrderOperation[]>> {
  return api.get(`/orders/${id}/operations`).then((r) => r.data);
}

/* ============ 模拟支付（M10） ============ */

export function createOrGetPayment(orderId: string): Promise<ApiResponse<Payment>> {
  return api.post(`/orders/${orderId}/payments`).then((r) => r.data);
}

export function getPayment(id: string): Promise<ApiResponse<Payment>> {
  return api.get(`/payments/${id}`).then((r) => r.data);
}

export function mockPay(id: string, payload: MockPayRequest): Promise<ApiResponse<Payment>> {
  return api.post(`/payments/${id}/mock-pay`, payload).then((r) => r.data);
}

/* ============ 票务权益（M10） ============ */

export function getMyEntitlements(): Promise<ApiResponse<Entitlement[]>> {
  return api.get('/entitlements').then((r) => r.data);
}

export function getMyEntitlement(id: string): Promise<ApiResponse<Entitlement>> {
  return api.get(`/entitlements/${id}`).then((r) => r.data);
}

/* ============ 全额退款（M11） ============ */

export function applyRefund(orderId: string, payload: RefundRequest): Promise<ApiResponse<Refund>> {
  return api.post(`/orders/${orderId}/refunds`, payload).then((r) => r.data);
}

export function getMyRefunds(params?: {
  page?: number;
  pageSize?: number;
  status?: string;
}): Promise<ApiResponse<PageResult<Refund>>> {
  return api.get('/refunds', { params }).then((r) => r.data);
}

export function getRefund(id: string): Promise<ApiResponse<Refund>> {
  return api.get(`/refunds/${id}`).then((r) => r.data);
}

export function cancelRefund(id: string): Promise<ApiResponse<Refund>> {
  return api.post(`/refunds/${id}/cancel`).then((r) => r.data);
}

/* ============ 优惠券（M13） ============ */

export function getAvailableCoupons(): Promise<ApiResponse<CouponAvailable[]>> {
  return api.get('/coupons/available').then((r) => r.data);
}

export function claimCoupon(requestId: string, templateId: string): Promise<ApiResponse<ClaimResult>> {
  return api.post('/coupons/claims', { requestId, templateId }).then((r) => r.data);
}

export function getClaimResult(requestId: string): Promise<ApiResponse<ClaimResult>> {
  return api.get(`/coupons/claims/${requestId}`).then((r) => r.data);
}

export function getMyCoupons(status?: string): Promise<ApiResponse<UserCoupon[]>> {
  return api.get('/users/me/coupons', { params: { status } }).then((r) => r.data);
}

/* ============ 系统通知（M14） ============ */

export function getNotifications(params?: {
  type?: string;
  cursor?: string;
  limit?: number;
}): Promise<ApiResponse<NotificationListResult>> {
  return api.get('/notifications', { params }).then((r) => r.data);
}

export function getUnreadCount(): Promise<ApiResponse<number>> {
  return api.get('/notifications/unread-count').then((r) => r.data);
}

export function getNotification(id: string): Promise<ApiResponse<Notification>> {
  return api.get(`/notifications/${id}`).then((r) => r.data);
}

export function markNotificationRead(id: string): Promise<ApiResponse<null>> {
  return api.put(`/notifications/${id}/read`).then((r) => r.data);
}

export function markAllNotificationsRead(): Promise<ApiResponse<null>> {
  return api.put('/notifications/read-all').then((r) => r.data);
}

export function getStreamToken(): Promise<ApiResponse<StreamTokenResponse>> {
  return api.post('/notifications/stream-token').then((r) => r.data);
}
