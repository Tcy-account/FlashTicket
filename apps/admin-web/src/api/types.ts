// admin-web 后台管理相关类型（契约以后端为准）

/** 当前后台用户资料（含权限点，用于侧边栏/按钮渲染）。 */
export interface AdminProfile {
  id: string;
  username: string;
  nickname: string;
  email: string;
  avatarUrl: string | null;
  roles: string[];
  permissions: string[];
}

/** 后台登录/刷新响应。 */
export interface AdminLoginResponse {
  accessToken: string;
  expiresIn: number;
  user: AdminProfile;
}

/** 后台登录请求。 */
export interface AdminLoginRequest {
  account: string;
  password: string;
}

/** 通用分页结构。 */
export interface PageResult<T> {
  items: T[];
  total: number;
  page: number;
  pageSize: number;
}

/** 用户列表项。 */
export interface AdminUser {
  id: string;
  username: string;
  email: string;
  nickname: string;
  status: 'ACTIVE' | 'DISABLED' | 'DELETION_PENDING' | 'DELETED';
  roles: string[];
  createdAt: string | null;
}

/** 角色。 */
export interface Role {
  id: string;
  code: string;
  name: string;
  status: string;
}

/** 权限点。 */
export interface Permission {
  id: string;
  code: string;
  name: string;
  type: string;
}

/** 审计日志项。 */
export interface AuditLog {
  id: string;
  operatorId: string;
  operatorUsername: string;
  action: string;
  targetType: string;
  targetId: string | null;
  detail: string | null;
  ip: string | null;
  createdAt: string;
}

/** 创建员工请求。 */
export interface CreateStaffRequest {
  username: string;
  email: string;
  nickname: string;
  password: string;
  roleIds: number[];
}

/** 修改员工请求。 */
export interface UpdateStaffRequest {
  nickname: string;
  email: string;
}

/** 启用/禁用请求。 */
export interface UpdateUserStatusRequest {
  status: 'ACTIVE' | 'DISABLED';
}

/** 分配角色请求。 */
export interface AssignRolesRequest {
  roleIds: number[];
}

/** 管理员重置密码请求。 */
export interface AdminResetPasswordRequest {
  newPassword: string;
}

/** 配置角色权限请求。 */
export interface GrantRolePermissionsRequest {
  permissionIds: number[];
}

/* ============ 演出分类（M04） ============ */

/** 演出分类。 */
export interface Category {
  id: string;
  code: string;
  name: string;
  description: string | null;
  sortOrder: number;
  status: 'ENABLED' | 'DISABLED';
  createdAt: string;
  updatedAt: string;
}

/** 分类创建/编辑请求。 */
export interface CategoryRequest {
  code: string;
  name: string;
  description?: string | null;
  sortOrder?: number;
}

/** 分类启停请求。 */
export interface CategoryStatusRequest {
  status: 'ENABLED' | 'DISABLED';
}

/* ============ 演出场馆（M04） ============ */

/** 演出场馆。 */
export interface Venue {
  id: string;
  name: string;
  countryCode: string;
  province: string | null;
  city: string;
  district: string | null;
  address: string;
  timezone: string;
  maxCapacity: number;
  description: string | null;
  status: 'ENABLED' | 'DISABLED';
  createdAt: string;
  updatedAt: string;
}

/** 场馆创建/编辑请求。 */
export interface VenueRequest {
  name: string;
  countryCode?: string;
  province?: string | null;
  city: string;
  district?: string | null;
  address: string;
  timezone?: string;
  maxCapacity: number;
  description?: string | null;
}

/** 场馆启停请求。 */
export interface VenueStatusRequest {
  status: 'ENABLED' | 'DISABLED';
}

/* ============ 演出活动（M05） ============ */

/** 活动生命周期状态。 */
export type EventStatus =
  | 'DRAFT'
  | 'PENDING_REVIEW'
  | 'REJECTED'
  | 'APPROVED'
  | 'PUBLISHED'
  | 'CANCELLED'
  | 'ARCHIVED';

/** 演出活动。 */
export interface Event {
  id: string;
  categoryId: string;
  title: string;
  subtitle: string | null;
  coverUrl: string | null;
  description: string | null;
  detailContent: string | null;
  organizerName: string | null;
  attendanceNotice: string | null;
  refundPolicy: string | null;
  status: EventStatus;
  publishedAt: string | null;
  cancelledAt: string | null;
  cancelReason: string | null;
  version: number;
  createdAt: string;
  updatedAt: string;
}

/** 活动创建/编辑请求。 */
export interface EventRequest {
  categoryId: string;
  title: string;
  subtitle?: string | null;
  coverUrl?: string | null;
  description?: string | null;
  detailContent?: string | null;
  organizerName?: string | null;
  attendanceNotice?: string | null;
  refundPolicy?: string | null;
  version?: number;
}

/** 取消活动请求。 */
export interface EventCancelRequest {
  reason: string;
}

/* ============ 演出场次（M05） ============ */

/** 场次状态。 */
export type SessionStatus = 'SCHEDULED' | 'POSTPONED' | 'CANCELLED' | 'COMPLETED';

/** 演出场次。 */
export interface Session {
  id: string;
  eventId: string;
  venueId: string;
  name: string;
  startAt: string;
  endAt: string;
  entryStartAt: string | null;
  saleCapacity: number;
  status: SessionStatus;
  postponedAt: string | null;
  cancelledAt: string | null;
  changeReason: string | null;
  version: number;
  createdAt: string;
  updatedAt: string;
}

/** 场次创建/编辑请求。 */
export interface SessionRequest {
  venueId: string;
  name: string;
  startAt: string;
  endAt: string;
  entryStartAt?: string | null;
  saleCapacity: number;
  version?: number;
}

/** 延期场次请求。 */
export interface SessionPostponeRequest {
  reason: string;
}

/** 取消场次请求。 */
export interface SessionCancelRequest {
  reason: string;
}

/* ============ 票档与售票计划（M06） ============ */

/** 票档。 */
export interface TicketTier {
  id: string;
  sessionId: string;
  name: string;
  tierType: string;
  unitPriceCent: number;
  initialQuantity: number;
  minPurchase: number;
  maxPurchase: number;
  perUserLimit: number;
  description: string | null;
  sortOrder: number;
  version: number;
  salePlan: SalePlan | null;
  createdAt: string;
  updatedAt: string;
}

/** 售票计划。 */
export interface SalePlan {
  id: string;
  tierId: string;
  saleStartAt: string;
  saleEndAt: string;
  saleStatus: 'ENABLED' | 'PAUSED';
  saleVersion: number;
  pausedAt: string | null;
  pauseReason: string | null;
  createdAt: string;
  updatedAt: string;
}

/** 票档创建/编辑请求。 */
export interface TicketTierRequest {
  name: string;
  tierType?: string;
  unitPriceCent: number;
  initialQuantity: number;
  minPurchase?: number;
  maxPurchase?: number;
  perUserLimit?: number;
  description?: string | null;
  sortOrder?: number;
  version?: number;
}

/** 售票计划配置请求。 */
export interface SalePlanRequest {
  saleStartAt: string;
  saleEndAt: string;
}

/** 暂停销售请求。 */
export interface SalePauseRequest {
  reason: string;
}

/** 审核请求。 */
export interface ReviewRequest {
  approve: boolean;
  comment?: string | null;
}

/* ============ 库存总账（M07） ============ */

/** 库存总账（三态守恒：total = available + reserved + sold）。 */
export interface Inventory {
  tierId: string;
  total: number;
  available: number;
  reserved: number;
  sold: number;
  version: number;
  generation: number;
  updatedAt: string;
}

/** 库存预占记录。 */
export interface Reservation {
  id: string;
  requestId: string;
  tierId: string;
  userId: string;
  quantity: number;
  status: 'RESERVED' | 'CONFIRMED' | 'RELEASED';
  expireAt: string;
  version: number;
  createdAt: string;
}

/** 库存变化流水。 */
export interface InventoryLedger {
  id: string;
  operationId: string;
  tierId: string;
  reservationId: string | null;
  operation: 'INIT' | 'RESERVE' | 'CONFIRM' | 'RELEASE';
  quantity: number;
  createdAt: string;
}

/* ============ 订单（M08） ============ */

/** 订单状态。 */
export type OrderStatus = 'PENDING_PAYMENT' | 'PAID' | 'CLOSED';

/** 订单明细交易快照。 */
export interface OrderItem {
  id: string;
  orderId: string;
  eventId: string;
  eventTitle: string;
  sessionId: string;
  sessionStartAt: string;
  venueName: string;
  tierId: string;
  tierName: string;
  unitPriceCent: number;
  quantity: number;
  amountCent: number;
}

/** 订单主体。 */
export interface Order {
  id: string;
  orderNo: string;
  requestId: string;
  userId: string;
  status: OrderStatus;
  originalAmountCent: number;
  discountAmountCent: number;
  payableAmountCent: number;
  paidAmountCent: number;
  expireAt: string;
  paidAt: string | null;
  closedAt: string | null;
  closeReason: string | null;
  version: number;
  createdAt: string;
  updatedAt: string;
  items: OrderItem[];
}

/* ============ 模拟支付（M10） ============ */

/** 支付单状态。 */
export type PaymentStatus = 'CREATED' | 'PROCESSING' | 'SUCCEEDED' | 'FAILED' | 'CLOSED';

/** 支付业务单。 */
export interface Payment {
  id: string;
  paymentNo: string;
  orderId: string;
  userId: string;
  channel: string;
  amountCent: number;
  currency: string;
  status: PaymentStatus;
  expireAt: string;
  succeededAt: string | null;
  createdAt: string;
}

/* ============ 全额退款（M11） ============ */

/** 退款状态。 */
export type RefundStatus =
  | 'PENDING_REVIEW'
  | 'APPROVED'
  | 'REJECTED'
  | 'PROCESSING'
  | 'SUCCEEDED'
  | 'FAILED'
  | 'CANCELLED';

/** 退款业务单。 */
export interface Refund {
  id: string;
  refundNo: string;
  refundRequestId: string;
  paymentId: string;
  orderId: string;
  userId: string;
  amountCent: number;
  currency: string;
  reasonCode: string;
  reasonDetail: string | null;
  status: RefundStatus;
  approvedBy: string | null;
  approvedAt: string | null;
  rejectedBy: string | null;
  rejectedAt: string | null;
  reviewComment: string | null;
  succeededAt: string | null;
  failedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

/** 退款审核请求。 */
export interface ReviewRefundRequest {
  comment?: string;
}

/* ============ 营销活动（M13） ============ */

/** 活动状态。 */
export type CampaignStatus = 'DRAFT' | 'PENDING_REVIEW' | 'PUBLISHED' | 'ENDED' | 'CANCELLED';

/** 活动类型。 */
export type CampaignType = 'FLASH_CLAIM' | 'NEW_USER' | 'EVENT_EXCLUSIVE';

/** 营销活动。 */
export interface Campaign {
  id: string;
  name: string;
  type: string;
  description: string | null;
  startAt: string;
  endAt: string;
  status: CampaignStatus;
  createdBy: string | null;
  reviewedBy: string | null;
  reviewedAt: string | null;
  version: number;
  createdAt: string;
}

/** 活动创建/编辑请求。 */
export interface CampaignRequest {
  name: string;
  type: string;
  description?: string | null;
  startAt: string;
  endAt: string;
}

/** 活动审核请求。 */
export interface CampaignReviewRequest {
  approve: boolean;
  comment?: string | null;
}

/* ============ 优惠券模板（M13） ============ */

/** 优惠券模板状态。 */
export type CouponTemplateStatus = 'DRAFT' | 'ACTIVE' | 'PAUSED' | 'TERMINATED';

/** 优惠券模板（含额度概览）。 */
export interface CouponTemplate {
  id: string;
  campaignId: string;
  name: string;
  type: 'FULL_REDUCTION' | 'PERCENTAGE';
  thresholdCent: number;
  discountCent: number;
  discountRate: number;
  maxDiscountCent: number;
  totalQuantity: number;
  perUserLimit: number;
  claimStartAt: string;
  claimEndAt: string;
  validityType: 'FIXED' | 'RELATIVE_DAYS';
  validFrom: string | null;
  validUntil: string | null;
  validDays: number | null;
  applicableScope: 'ALL' | 'EVENT' | 'TIER';
  scopeRefId: string | null;
  status: CouponTemplateStatus;
  version: number;
  quotaAvailable: number | null;
  quotaIssued: number | null;
  createdAt: string;
}

/** 优惠券模板创建/编辑请求。 */
export interface CouponTemplateRequest {
  campaignId: string;
  name: string;
  type: 'FULL_REDUCTION' | 'PERCENTAGE';
  thresholdCent?: number;
  discountCent?: number;
  discountRate?: number;
  maxDiscountCent?: number;
  totalQuantity: number;
  perUserLimit: number;
  claimStartAt: string;
  claimEndAt: string;
  validityType: 'FIXED' | 'RELATIVE_DAYS';
  validFrom?: string | null;
  validUntil?: string | null;
  validDays?: number | null;
  applicableScope: 'ALL' | 'EVENT' | 'TIER';
  scopeRefId?: string | null;
}

/* ============ 系统通知（M14） ============ */

/** 通知发送失败工单。 */
export interface NotificationFailure {
  id: string;
  eventId: string;
  eventType: string;
  aggregateType: string;
  aggregateId: string;
  attemptCount: number;
  lastError: string | null;
  createdAt: string;
}

/* ============ 风控（M15） ============ */

/** 风控临时限制/黑名单。 */
export interface RiskRestriction {
  id: string;
  subjectType: 'USER' | 'IP';
  subjectId: string;
  scope: 'PURCHASE' | 'CLAIM' | 'LOGIN' | 'ALL';
  reason: string;
  expireAt: string;
  status: 'ACTIVE' | 'EXPIRED' | 'REVOKED';
  createdBy: string;
  revokedBy: string | null;
  revokedAt: string | null;
  createdAt: string;
}

/** 创建风控限制请求。 */
export interface RestrictionRequest {
  subjectType: string;
  subjectId: string;
  scope: string;
  reason: string;
  expireAt: string;
}

/** 风控规则。 */
export interface RiskRule {
  id: string;
  ruleCode: string;
  name: string;
  scene: string;
  action: 'THROTTLE' | 'DENY';
  config: string | null;
  priority: number;
  status: 'ENABLED' | 'DISABLED';
  version: number;
  createdAt: string;
}

/** 创建/更新风控规则请求。 */
export interface RiskRuleRequest {
  ruleCode: string;
  name: string;
  scene: string;
  action: string;
  config?: string;
  priority: number;
}

/** 风控操作审计。 */
export interface RiskOperation {
  id: string;
  operatorId: string;
  targetType: string;
  targetId: string;
  action: string;
  reason: string | null;
  createdAt: string;
}

/* ============ 运营统计（M16） ============ */

/** 近 N 日实收趋势点。 */
export interface DailyTrendPoint {
  day: string;
  amountCent: number;
  orderCount: number;
}

/** 平台运营概览（模拟支付口径）。 */
export interface PlatformOverview {
  paidAmountCent: number;
  paidOrderCount: number;
  soldTicketCount: number;
  refundAmountCent: number;
  refundedOrderCount: number;
  orderCount: number;
  paymentRate: number;
  refundRate: number;
  netRevenueCent: number;
  dailyTrend: DailyTrendPoint[];
}

/** 演出销售排行行。 */
export interface EventSales {
  eventId: string;
  eventTitle: string;
  orderCount: number;
  ticketCount: number;
  amountCent: number;
}

/** 抢票链路转化统计。 */
export interface PurchaseConversion {
  acceptedCount: number;
  succeededCount: number;
  failedCount: number;
  processingCount: number;
  successRate: number;
}

/* ============ 系统对账与补偿（M16） ============ */

/** 对账任务。 */
export interface ReconcileJob {
  id: string;
  jobNo: string;
  scope: string;
  status: string;
  totalChecked: number;
  exceptionCount: number;
  startedAt: string;
  completedAt: string;
  errorMsg: string | null;
}

/** 对账异常工单。 */
export interface ReconcileException {
  id: string;
  exceptionNo: string;
  jobId: string;
  scope: string;
  checkType: string;
  severity: string;
  status: string;
  entityType: string;
  entityId: string;
  description: string;
  resolution: string | null;
  createdAt: string;
}

/** 补偿任务。 */
export interface CompensationTask {
  id: string;
  operationId: string;
  taskType: string;
  status: string;
  entityType: string;
  entityId: string;
  attemptCount: number;
  lastError: string | null;
  createdAt: string;
}
