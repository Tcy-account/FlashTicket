import { createApiClient, axios, setAccessToken } from '@flashticket/api-client';
import type { ApiResponse } from '@flashticket/shared-types';
import type {
  AdminLoginRequest,
  AdminLoginResponse,
  AdminResetPasswordRequest,
  AdminUser,
  AssignRolesRequest,
  AuditLog,
  Campaign,
  CampaignRequest,
  CampaignReviewRequest,
  Category,
  CategoryRequest,
  CategoryStatusRequest,
  CompensationTask,
  CouponTemplate,
  CouponTemplateRequest,
  CreateStaffRequest,
  EventSales,
  NotificationFailure,
  PlatformOverview,
  PurchaseConversion,
  ReconcileException,
  ReconcileJob,
  RiskRestriction,
  RestrictionRequest,
  RiskRule,
  RiskRuleRequest,
  RiskOperation,
  Event,
  EventCancelRequest,
  EventRequest,
  GrantRolePermissionsRequest,
  Inventory,
  InventoryLedger,
  Order,
  PageResult,
  Payment,
  Permission,
  Refund,
  Reservation,
  ReviewRefundRequest,
  ReviewRequest,
  Role,
  SalePauseRequest,
  SalePlan,
  SalePlanRequest,
  Session,
  SessionCancelRequest,
  SessionPostponeRequest,
  SessionRequest,
  TicketTier,
  TicketTierRequest,
  UpdateStaffRequest,
  UpdateUserStatusRequest,
  Venue,
  VenueRequest,
  VenueStatusRequest,
} from './types';

/** 后台 API 实例（/api/v1 前缀，Vite 代理到后端 8080）。 */
export const api = createApiClient({
  baseURL: '/api/v1',
  onUnauthorized: () => {
    setAccessToken(null);
    if (window.location.pathname !== '/login') {
      window.location.href = '/login';
    }
  },
  refreshAccessToken: async () => {
    const r = await refreshClient.post<ApiResponse<AdminLoginResponse>>('/admin/auth/refresh');
    const body = r.data;
    return body.code === 'SUCCESS' && body.data ? body.data.accessToken : null;
  },
});

/** 刷新专用裸 axios（后台 cookie，绕过拦截器避免 401 递归）。 */
const refreshClient = axios.create({ baseURL: '/api/v1', timeout: 10000, withCredentials: true });

/* ============ 后台认证 ============ */

export function adminLogin(payload: AdminLoginRequest): Promise<ApiResponse<AdminLoginResponse>> {
  return api.post('/admin/auth/login', payload).then((r) => r.data);
}

export function adminLogout(): Promise<ApiResponse<null>> {
  return api.post('/admin/auth/logout').then((r) => r.data);
}

/* ============ 用户管理 ============ */

export function listUsers(params: {
  page?: number;
  pageSize?: number;
  username?: string;
  status?: string;
  role?: string;
}): Promise<ApiResponse<PageResult<AdminUser>>> {
  return api.get('/admin/users', { params }).then((r) => r.data);
}

export function createStaff(payload: CreateStaffRequest): Promise<ApiResponse<AdminUser>> {
  return api.post('/admin/staff', payload).then((r) => r.data);
}

export function updateStaff(id: string, payload: UpdateStaffRequest): Promise<ApiResponse<AdminUser>> {
  return api.put(`/admin/staff/${id}`, payload).then((r) => r.data);
}

export function updateUserStatus(id: string, payload: UpdateUserStatusRequest): Promise<ApiResponse<null>> {
  return api.put(`/admin/users/${id}/status`, payload).then((r) => r.data);
}

export function assignRoles(id: string, payload: AssignRolesRequest): Promise<ApiResponse<null>> {
  return api.put(`/admin/users/${id}/roles`, payload).then((r) => r.data);
}

export function revokeSessions(id: string): Promise<ApiResponse<null>> {
  return api.post(`/admin/users/${id}/revoke-sessions`).then((r) => r.data);
}

export function resetPassword(id: string, payload: AdminResetPasswordRequest): Promise<ApiResponse<null>> {
  return api.post(`/admin/users/${id}/reset-password`, payload).then((r) => r.data);
}

/* ============ 角色与权限 ============ */

export function listRoles(): Promise<ApiResponse<Role[]>> {
  return api.get('/admin/roles').then((r) => r.data);
}

export function listPermissions(): Promise<ApiResponse<Permission[]>> {
  return api.get('/admin/permissions').then((r) => r.data);
}

export function getRolePermissions(roleId: string): Promise<ApiResponse<Permission[]>> {
  return api.get(`/admin/roles/${roleId}/permissions`).then((r) => r.data);
}

export function grantRolePermissions(
  roleId: string,
  payload: GrantRolePermissionsRequest,
): Promise<ApiResponse<null>> {
  return api.put(`/admin/roles/${roleId}/permissions`, payload).then((r) => r.data);
}

/* ============ 审计日志 ============ */

export function listAuditLogs(params: {
  page?: number;
  pageSize?: number;
}): Promise<ApiResponse<PageResult<AuditLog>>> {
  return api.get('/admin/audit-logs', { params }).then((r) => r.data);
}

/* ============ 演出分类（M04） ============ */

export function listCategories(): Promise<ApiResponse<Category[]>> {
  return api.get('/admin/categories').then((r) => r.data);
}

export function createCategory(payload: CategoryRequest): Promise<ApiResponse<Category>> {
  return api.post('/admin/categories', payload).then((r) => r.data);
}

export function updateCategory(id: string, payload: CategoryRequest): Promise<ApiResponse<Category>> {
  return api.put(`/admin/categories/${id}`, payload).then((r) => r.data);
}

export function updateCategoryStatus(
  id: string,
  payload: CategoryStatusRequest,
): Promise<ApiResponse<null>> {
  return api.put(`/admin/categories/${id}/status`, payload).then((r) => r.data);
}

/* ============ 演出场馆（M04） ============ */

export function listVenues(params: {
  page?: number;
  pageSize?: number;
  city?: string;
  status?: string;
  name?: string;
}): Promise<ApiResponse<PageResult<Venue>>> {
  return api.get('/admin/venues', { params }).then((r) => r.data);
}

export function createVenue(payload: VenueRequest): Promise<ApiResponse<Venue>> {
  return api.post('/admin/venues', payload).then((r) => r.data);
}

export function updateVenue(id: string, payload: VenueRequest): Promise<ApiResponse<Venue>> {
  return api.put(`/admin/venues/${id}`, payload).then((r) => r.data);
}

export function updateVenueStatus(id: string, payload: VenueStatusRequest): Promise<ApiResponse<null>> {
  return api.put(`/admin/venues/${id}/status`, payload).then((r) => r.data);
}

/* ============ 演出活动（M05） ============ */

export function listEvents(params: {
  page?: number;
  pageSize?: number;
  status?: string;
  categoryId?: string;
  keyword?: string;
}): Promise<ApiResponse<PageResult<Event>>> {
  return api.get('/admin/events', { params }).then((r) => r.data);
}

export function getEvent(id: string): Promise<ApiResponse<Event>> {
  return api.get(`/admin/events/${id}`).then((r) => r.data);
}

export function createEvent(payload: EventRequest): Promise<ApiResponse<Event>> {
  return api.post('/admin/events', payload).then((r) => r.data);
}

export function updateEvent(id: string, payload: EventRequest): Promise<ApiResponse<Event>> {
  return api.put(`/admin/events/${id}`, payload).then((r) => r.data);
}

export function cancelEvent(id: string, payload: EventCancelRequest): Promise<ApiResponse<Event>> {
  return api.post(`/admin/events/${id}/cancel`, payload).then((r) => r.data);
}

/* ============ 演出场次（M05） ============ */

export function listSessions(eventId: string): Promise<ApiResponse<Session[]>> {
  return api.get(`/admin/events/${eventId}/sessions`).then((r) => r.data);
}

export function createSession(eventId: string, payload: SessionRequest): Promise<ApiResponse<Session>> {
  return api.post(`/admin/events/${eventId}/sessions`, payload).then((r) => r.data);
}

export function updateSession(id: string, payload: SessionRequest): Promise<ApiResponse<Session>> {
  return api.put(`/admin/sessions/${id}`, payload).then((r) => r.data);
}

export function postponeSession(id: string, payload: SessionPostponeRequest): Promise<ApiResponse<Session>> {
  return api.post(`/admin/sessions/${id}/postpone`, payload).then((r) => r.data);
}

export function cancelSession(id: string, payload: SessionCancelRequest): Promise<ApiResponse<Session>> {
  return api.post(`/admin/sessions/${id}/cancel`, payload).then((r) => r.data);
}

/* ============ 票档与售票计划（M06） ============ */

export function listTiers(sessionId: string): Promise<ApiResponse<TicketTier[]>> {
  return api.get(`/admin/sessions/${sessionId}/tiers`).then((r) => r.data);
}

export function createTier(sessionId: string, payload: TicketTierRequest): Promise<ApiResponse<TicketTier>> {
  return api.post(`/admin/sessions/${sessionId}/tiers`, payload).then((r) => r.data);
}

export function updateTier(id: string, payload: TicketTierRequest): Promise<ApiResponse<TicketTier>> {
  return api.put(`/admin/tiers/${id}`, payload).then((r) => r.data);
}

export function configureSalePlan(id: string, payload: SalePlanRequest): Promise<ApiResponse<SalePlan>> {
  return api.put(`/admin/tiers/${id}/sale-plan`, payload).then((r) => r.data);
}

export function pauseTier(id: string, payload: SalePauseRequest): Promise<ApiResponse<SalePlan>> {
  return api.post(`/admin/tiers/${id}/pause`, payload).then((r) => r.data);
}

export function resumeTier(id: string): Promise<ApiResponse<SalePlan>> {
  return api.post(`/admin/tiers/${id}/resume`).then((r) => r.data);
}

/* ============ 审核发布（M06） ============ */

export function submitReview(id: string): Promise<ApiResponse<Event>> {
  return api.post(`/admin/events/${id}/submit-review`).then((r) => r.data);
}

export function reviewEvent(id: string, payload: ReviewRequest): Promise<ApiResponse<Event>> {
  return api.post(`/admin/events/${id}/review`, payload).then((r) => r.data);
}

export function publishEvent(id: string): Promise<ApiResponse<Event>> {
  return api.post(`/admin/events/${id}/publish`).then((r) => r.data);
}

/* ============ 库存总账（M07） ============ */

export function getInventory(tierId: string): Promise<ApiResponse<Inventory>> {
  return api.get(`/admin/inventory/${tierId}`).then((r) => r.data);
}

export function listReservations(tierId: string): Promise<ApiResponse<Reservation[]>> {
  return api.get(`/admin/inventory/${tierId}/reservations`).then((r) => r.data);
}

export function listLedger(tierId: string): Promise<ApiResponse<InventoryLedger[]>> {
  return api.get(`/admin/inventory/${tierId}/ledger`).then((r) => r.data);
}

/* ============ 订单查询（M08） ============ */

export function listOrders(params: {
  page?: number;
  pageSize?: number;
  status?: string;
  orderNo?: string;
  userId?: string;
}): Promise<ApiResponse<PageResult<Order>>> {
  return api.get('/admin/orders', { params }).then((r) => r.data);
}

export function getOrder(id: string): Promise<ApiResponse<Order>> {
  return api.get(`/admin/orders/${id}`).then((r) => r.data);
}

/* ============ 支付查询（M10） ============ */

export function listPayments(params: {
  page?: number;
  pageSize?: number;
  status?: string;
  paymentNo?: string;
}): Promise<ApiResponse<PageResult<Payment>>> {
  return api.get('/admin/payments', { params }).then((r) => r.data);
}

export function getPayment(id: string): Promise<ApiResponse<Payment>> {
  return api.get(`/admin/payments/${id}`).then((r) => r.data);
}

/* ============ 退款审核（M11） ============ */

export function listRefunds(params: {
  page?: number;
  pageSize?: number;
  status?: string;
  refundNo?: string;
}): Promise<ApiResponse<PageResult<Refund>>> {
  return api.get('/admin/refunds', { params }).then((r) => r.data);
}

export function getRefund(id: string): Promise<ApiResponse<Refund>> {
  return api.get(`/admin/refunds/${id}`).then((r) => r.data);
}

export function approveRefund(id: string, payload: ReviewRefundRequest): Promise<ApiResponse<Refund>> {
  return api.post(`/admin/refunds/${id}/approve`, payload).then((r) => r.data);
}

export function rejectRefund(id: string, payload: ReviewRefundRequest): Promise<ApiResponse<Refund>> {
  return api.post(`/admin/refunds/${id}/reject`, payload).then((r) => r.data);
}

export function retryRefund(id: string): Promise<ApiResponse<Refund>> {
  return api.post(`/admin/refunds/${id}/retry`).then((r) => r.data);
}

/* ============ 营销活动（M13） ============ */

export function listCampaigns(params: {
  page?: number;
  pageSize?: number;
  status?: string;
}): Promise<ApiResponse<PageResult<Campaign>>> {
  return api.get('/admin/campaigns', { params }).then((r) => r.data);
}

export function getCampaign(id: string): Promise<ApiResponse<Campaign>> {
  return api.get(`/admin/campaigns/${id}`).then((r) => r.data);
}

export function createCampaign(payload: CampaignRequest): Promise<ApiResponse<Campaign>> {
  return api.post('/admin/campaigns', payload).then((r) => r.data);
}

export function updateCampaign(id: string, payload: CampaignRequest): Promise<ApiResponse<Campaign>> {
  return api.put(`/admin/campaigns/${id}`, payload).then((r) => r.data);
}

export function submitCampaignReview(id: string): Promise<ApiResponse<null>> {
  return api.post(`/admin/campaigns/${id}/submit-review`).then((r) => r.data);
}

export function reviewCampaign(id: string, payload: CampaignReviewRequest): Promise<ApiResponse<null>> {
  return api.post(`/admin/campaigns/${id}/review`, payload).then((r) => r.data);
}

export function endCampaign(id: string): Promise<ApiResponse<null>> {
  return api.post(`/admin/campaigns/${id}/end`).then((r) => r.data);
}

export function cancelCampaign(id: string): Promise<ApiResponse<null>> {
  return api.post(`/admin/campaigns/${id}/cancel`).then((r) => r.data);
}

/* ============ 优惠券模板（M13） ============ */

export function listCouponTemplates(params: {
  page?: number;
  pageSize?: number;
  status?: string;
  campaignId?: string;
}): Promise<ApiResponse<PageResult<CouponTemplate>>> {
  return api.get('/admin/coupon-templates', { params }).then((r) => r.data);
}

export function getCouponTemplate(id: string): Promise<ApiResponse<CouponTemplate>> {
  return api.get(`/admin/coupon-templates/${id}`).then((r) => r.data);
}

export function createCouponTemplate(payload: CouponTemplateRequest): Promise<ApiResponse<CouponTemplate>> {
  return api.post('/admin/coupon-templates', payload).then((r) => r.data);
}

export function updateCouponTemplate(id: string, payload: CouponTemplateRequest): Promise<ApiResponse<CouponTemplate>> {
  return api.put(`/admin/coupon-templates/${id}`, payload).then((r) => r.data);
}

export function activateCouponTemplate(id: string): Promise<ApiResponse<CouponTemplate>> {
  return api.post(`/admin/coupon-templates/${id}/activate`).then((r) => r.data);
}

export function pauseCouponTemplate(id: string): Promise<ApiResponse<null>> {
  return api.post(`/admin/coupon-templates/${id}/pause`).then((r) => r.data);
}

export function resumeCouponTemplate(id: string): Promise<ApiResponse<null>> {
  return api.post(`/admin/coupon-templates/${id}/resume`).then((r) => r.data);
}

export function terminateCouponTemplate(id: string): Promise<ApiResponse<null>> {
  return api.post(`/admin/coupon-templates/${id}/terminate`).then((r) => r.data);
}

/* ============ 系统通知（M14） ============ */

export function listNotificationFailures(): Promise<ApiResponse<NotificationFailure[]>> {
  return api.get('/admin/notifications/failures').then((r) => r.data);
}

export function retryNotification(id: string): Promise<ApiResponse<null>> {
  return api.post(`/admin/notifications/${id}/retry`).then((r) => r.data);
}

/* ============ 风控（M15） ============ */

export function listRiskRestrictions(params?: {
  subjectType?: string;
  subjectId?: string;
  status?: string;
}): Promise<ApiResponse<RiskRestriction[]>> {
  return api.get('/admin/risk/restrictions', { params }).then((r) => r.data);
}

export function createRiskRestriction(payload: RestrictionRequest): Promise<ApiResponse<RiskRestriction>> {
  return api.post('/admin/risk/restrictions', payload).then((r) => r.data);
}

export function revokeRiskRestriction(id: string): Promise<ApiResponse<RiskRestriction>> {
  return api.post(`/admin/risk/restrictions/${id}/revoke`).then((r) => r.data);
}

export function listRiskRules(): Promise<ApiResponse<RiskRule[]>> {
  return api.get('/admin/risk/rules').then((r) => r.data);
}

export function createRiskRule(payload: RiskRuleRequest): Promise<ApiResponse<RiskRule>> {
  return api.post('/admin/risk/rules', payload).then((r) => r.data);
}

export function updateRiskRule(id: string, payload: RiskRuleRequest): Promise<ApiResponse<RiskRule>> {
  return api.put(`/admin/risk/rules/${id}`, payload).then((r) => r.data);
}

export function toggleRiskRule(id: string, enabled: boolean): Promise<ApiResponse<RiskRule>> {
  return api.post(`/admin/risk/rules/${id}/toggle`, null, { params: { enabled } }).then((r) => r.data);
}

export function listRiskOperations(): Promise<ApiResponse<RiskOperation[]>> {
  return api.get('/admin/risk/operations').then((r) => r.data);
}

/* ============ 运营统计（M16） ============ */

export function getOverview(days = 7): Promise<ApiResponse<PlatformOverview>> {
  return api.get('/admin/analytics/overview', { params: { days } }).then((r) => r.data);
}

export function getEventSales(): Promise<ApiResponse<EventSales[]>> {
  return api.get('/admin/analytics/events').then((r) => r.data);
}

export function getPurchaseConversion(): Promise<ApiResponse<PurchaseConversion>> {
  return api.get('/admin/analytics/purchases').then((r) => r.data);
}

/* ============ 系统对账与补偿（M16） ============ */

export function createReconcileJob(scope: string): Promise<ApiResponse<ReconcileJob>> {
  return api.post('/admin/reconciliation/jobs', null, { params: { scope } }).then((r) => r.data);
}

export function listReconcileJobs(params?: {
  page?: number;
  size?: number;
}): Promise<ApiResponse<PageResult<ReconcileJob>>> {
  return api.get('/admin/reconciliation/jobs', { params }).then((r) => r.data);
}

export function listReconcileExceptions(params?: {
  status?: string;
  page?: number;
  size?: number;
}): Promise<ApiResponse<PageResult<ReconcileException>>> {
  return api.get('/admin/reconciliation/exceptions', { params }).then((r) => r.data);
}

export function reviewReconcileException(
  id: string,
  payload: { resolution: string; comment?: string },
): Promise<ApiResponse<ReconcileException>> {
  return api.post(`/admin/reconciliation/exceptions/${id}/review`, payload).then((r) => r.data);
}

export function createCompensation(payload: {
  taskType: string;
  entityType: string;
  entityId: string;
  exceptionId?: string;
}): Promise<ApiResponse<CompensationTask>> {
  return api.post('/admin/compensations', payload).then((r) => r.data);
}

export function executeCompensation(id: string): Promise<ApiResponse<CompensationTask>> {
  return api.post(`/admin/compensations/${id}/execute`).then((r) => r.data);
}

export function listCompensations(params?: {
  page?: number;
  size?: number;
}): Promise<ApiResponse<PageResult<CompensationTask>>> {
  return api.get('/admin/compensations', { params }).then((r) => r.data);
}
