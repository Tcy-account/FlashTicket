// 账号与健康检查相关请求/响应类型（契约以后端为准，见 doc/M00-工程初始化.md）

/** 健康检查详情。 */
export interface HealthDetail {
  status: 'UP' | 'DOWN';
  db?: string;
  redis?: string;
}

/** 用户资料摘要（雪花 ID 以字符串传输）。 */
export interface UserProfile {
  id: string;
  username: string;
  nickname: string;
  email: string;
  avatarUrl: string | null;
  roles: string[];
  permissions: string[];
}

/** 登录请求：用户名或已验证邮箱 + 密码。 */
export interface LoginRequest {
  account: string;
  password: string;
}

/** 登录/刷新响应：Access Token + 资料（Refresh Token 在 HttpOnly Cookie）。 */
export interface LoginResponse {
  accessToken: string;
  expiresIn: number;
  user: UserProfile;
}

/** 注册成功响应。 */
export interface RegisterResponse {
  userId: string;
  username: string;
  email: string;
  nickname: string;
}

/** 注册请求：用户名 + 邮箱 + 验证码 + 密码。 */
export interface RegisterRequest {
  username: string;
  email: string;
  verifyCode: string;
  password: string;
}

/** 发送邮箱验证码请求。 */
export interface SendVerifyCodeRequest {
  email: string;
  /** 用途隔离：REGISTER / RESET_PASSWORD / CHANGE_EMAIL */
  purpose: 'REGISTER' | 'RESET_PASSWORD' | 'CHANGE_EMAIL';
}

/** 修改资料请求。 */
export interface UpdateProfileRequest {
  nickname: string;
  avatarUrl?: string;
}

/** 修改密码请求（已登录）。 */
export interface ChangePasswordRequest {
  oldPassword: string;
  newPassword: string;
}

/** 找回密码第一步：发送重置验证码。 */
export interface ForgotPasswordRequest {
  email: string;
}

/** 找回密码第二步：验证码 + 新密码。 */
export interface ResetPasswordRequest {
  email: string;
  verifyCode: string;
  newPassword: string;
}

/* ============ 演出域公开查询（M04） ============ */

/** 通用分页结构。 */
export interface PageResult<T> {
  items: T[];
  total: number;
  page: number;
  pageSize: number;
}

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

/* ============ 演出活动与场次（M05） ============ */

/** 演出活动（公开视图，仅已发布）。 */
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
  status: 'DRAFT' | 'PENDING_REVIEW' | 'REJECTED' | 'APPROVED' | 'PUBLISHED' | 'CANCELLED' | 'ARCHIVED';
  publishedAt: string | null;
  cancelledAt: string | null;
  cancelReason: string | null;
  version: number;
  createdAt: string;
  updatedAt: string;
}

/** 演出场次（公开视图，仅正常场次）。 */
export interface Session {
  id: string;
  eventId: string;
  venueId: string;
  name: string;
  startAt: string;
  endAt: string;
  entryStartAt: string | null;
  saleCapacity: number;
  status: 'SCHEDULED' | 'POSTPONED' | 'CANCELLED' | 'COMPLETED';
  postponedAt: string | null;
  cancelledAt: string | null;
  changeReason: string | null;
  version: number;
  createdAt: string;
  updatedAt: string;
}

/* ============ 票档与可售状态（M06） ============ */

/** 售票计划（公开视图）。 */
export interface SalePlan {
  id: string;
  tierId: string;
  saleStartAt: string;
  saleEndAt: string;
  saleStatus: 'ENABLED' | 'PAUSED';
  saleVersion: number;
  pausedAt: string | null;
  pauseReason: string | null;
}

/** 票档（公开视图）。 */
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

/** 票档可售状态（三层销售状态 + 资格结论 + 库存余量）。 */
export interface TierAvailability {
  tierId: string;
  sessionId: string;
  eventId: string | null;
  tierName: string;
  unitPriceCent: number;
  saleStatus: 'ENABLED' | 'PAUSED' | null;
  windowStatus: 'NOT_STARTED' | 'IN_PROGRESS' | 'ENDED' | null;
  eligible: boolean;
  saleStartAt: string | null;
  saleEndAt: string | null;
  serverNow: string;
  availableQuantity: number | null;
  totalQuantity: number | null;
}

/* ============ 抢票与订单（M08） ============ */

/** 抢票请求。 */
export interface CheckoutRequest {
  requestId: string;
  tierId: string;
  quantity: number;
  /** 选用的用户优惠券 ID（可空，不选券则不传）。 */
  selectedCouponId?: string;
}

/** 抢票请求状态（M12 起异步排队，权威状态见 ft_purchase_request）。 */
export type PurchaseStatus =
  | 'QUEUED'
  | 'PROCESSING'
  | 'RECOVERING'
  | 'SUCCEEDED'
  | 'FAILED'
  | 'CANCELLED';

/** 抢票受理结果（POST /purchases）。 */
export interface CheckoutResponse {
  requestId: string;
  status: PurchaseStatus;
  order: Order | null;
}

/** 抢票状态（GET /purchases/{requestId}，排队页轮询依据）。 */
export interface PurchaseStatusResponse {
  requestId: string;
  status: PurchaseStatus;
  failReason: string | null;
  orderId: string | null;
  order: Order | null;
  attemptCount: number;
  createdAt: string;
  completedAt: string | null;
}

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

/** 订单操作流水（状态变更历史）。 */
export interface OrderOperation {
  id: string;
  orderId: string;
  operation: 'ORDER_CREATED' | 'USER_CANCELLED' | 'TIMEOUT_CLOSED' | 'PAYMENT_CONFIRMED' | 'REFUND_REQUESTED' | 'REFUND_SUCCEEDED';
  fromStatus: OrderStatus | null;
  toStatus: OrderStatus;
  actorId: string | null;
  reason: string | null;
  operationId: string;
  createdAt: string;
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

/** 模拟执行支付请求。 */
export interface MockPayRequest {
  attemptRequestId: string;
}

/* ============ 票务权益（M10） ============ */

/** 票务权益状态。 */
export type EntitlementStatus = 'VALID' | 'USED' | 'REVOKED';

/** 票务权益（门票凭证）。 */
export interface Entitlement {
  id: string;
  entitlementNo: string;
  orderId: string;
  orderItemId: string;
  userId: string;
  eventId: string;
  eventTitle: string;
  sessionId: string;
  sessionStartAt: string;
  venueName: string;
  tierId: string;
  tierName: string;
  ticketIndex: number;
  status: EntitlementStatus;
  issuedAt: string;
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

/** 退款原因。 */
export type RefundReason = 'PERSONAL' | 'EVENT_POSTPONED' | 'EVENT_CANCELLED' | 'OTHER';

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

/** 退款申请请求。 */
export interface RefundRequest {
  refundRequestId: string;
  reasonCode: RefundReason;
  reasonDetail?: string;
}

/* ============ 优惠券（M13） ============ */

/** 用户优惠券状态。 */
export type UserCouponStatus = 'AVAILABLE' | 'LOCKED' | 'USED' | 'EXPIRED' | 'VOID';

/** 优惠券类型。 */
export type CouponType = 'FULL_REDUCTION' | 'PERCENTAGE';

/** 优惠券适用范围。 */
export type CouponApplicableScope = 'ALL' | 'EVENT' | 'TIER';

/** 优惠券有效期类型。 */
export type CouponValidityType = 'FIXED' | 'RELATIVE_DAYS';

/** 用户优惠券（含模板摘要，供我的券/下单选券展示）。 */
export interface UserCoupon {
  id: string;
  templateId: string;
  campaignId: string;
  couponNo: string;
  status: UserCouponStatus;
  validFrom: string;
  validUntil: string;
  templateName: string;
  type: CouponType;
  thresholdCent: number;
  discountCent: number;
  discountRate: number;
  maxDiscountCent: number;
  applicableScope: CouponApplicableScope;
  usedAt: string | null;
  lockedAt: string | null;
  createdAt: string;
}

/** 可领优惠券（领券中心列表项）。 */
export interface CouponAvailable {
  templateId: string;
  campaignId: string;
  campaignName: string;
  name: string;
  type: CouponType;
  thresholdCent: number;
  discountCent: number;
  discountRate: number;
  maxDiscountCent: number;
  totalQuantity: number;
  availableQuantity: number;
  perUserLimit: number;
  claimStartAt: string;
  claimEndAt: string;
  validityType: CouponValidityType;
  validFrom: string | null;
  validUntil: string | null;
  validDays: number | null;
  applicableScope: CouponApplicableScope;
  scopeRefId: string | null;
}

/** 领券结果。 */
export interface ClaimResult {
  requestId: string;
  status: string;
  userCouponId: string | null;
  failReason: string | null;
  coupon: UserCoupon | null;
}

/* ============ 系统通知（M14） ============ */

/** 通知类型。 */
export type NotificationType = 'TRANSACTION' | 'EVENT' | 'MARKETING' | 'SYSTEM';

/** 用户站内消息。 */
export interface Notification {
  id: string;
  type: NotificationType;
  title: string;
  content: string;
  bizType: string | null;
  bizId: string | null;
  read: boolean;
  receivedAt: string;
}

/** 消息游标分页结果。 */
export interface NotificationListResult {
  items: Notification[];
  nextCursor: string | null;
  unreadCount: number;
}

/** SSE 流连接凭证。 */
export interface StreamTokenResponse {
  token: string;
  expiresInSeconds: number;
}
