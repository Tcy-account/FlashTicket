FlashTicket 数据库详细设计 01：用户与权限模块
我们先确定第一批 6 张核心表，本轮只做字段、类型、索引、约束与 ER 关系设计，不直接编写 DDL。
一、六张表的关系
#chatgpt-mermaid-_r_1af_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-_r_1af_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-_r_1af_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-_r_1af_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-_r_1af_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-_r_1af_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-_r_1af_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-_r_1af_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-_r_1af_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-_r_1af_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-_r_1af_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-_r_1af_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-_r_1af_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-_r_1af_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";font-size:16px;}#chatgpt-mermaid-_r_1af_ p{margin:0;}#chatgpt-mermaid-_r_1af_ .entityBox{fill:rgb(19, 33, 54);stroke:rgb(31, 78, 148);}#chatgpt-mermaid-_r_1af_ .relationshipLabelBox{fill:rgb(48, 48, 48);opacity:0.7;background-color:rgb(48, 48, 48);}#chatgpt-mermaid-_r_1af_ .relationshipLabelBox rect{opacity:0.5;}#chatgpt-mermaid-_r_1af_ .labelBkg{background-color:rgba(48, 48, 48, 0.5);}#chatgpt-mermaid-_r_1af_ .edgeLabel{background-color:rgb(14, 14, 14);}#chatgpt-mermaid-_r_1af_ .edgeLabel .label rect{fill:rgb(14, 14, 14);}#chatgpt-mermaid-_r_1af_ .edgeLabel .label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-_r_1af_ .edgeLabel .label{fill:rgb(31, 78, 148);font-size:14px;}#chatgpt-mermaid-_r_1af_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";color:rgb(237, 237, 237);}#chatgpt-mermaid-_r_1af_ .edge-pattern-dashed{stroke-dasharray:8,8;}#chatgpt-mermaid-_r_1af_ .node rect,#chatgpt-mermaid-_r_1af_ .node circle,#chatgpt-mermaid-_r_1af_ .node ellipse,#chatgpt-mermaid-_r_1af_ .node polygon{fill:rgb(19, 33, 54);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-_r_1af_ .relationshipLine{stroke:rgb(175, 175, 175);stroke-width:1px;fill:none;}#chatgpt-mermaid-_r_1af_ .marker{fill:none!important;stroke:rgb(175, 175, 175)!important;stroke-width:1;}#chatgpt-mermaid-_r_1af_ [data-look=neo].labelBkg{background-color:rgba(48, 48, 48, 0.5);}#chatgpt-mermaid-_r_1af_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-_r_1af_ [data-look="neo"].node rect,#chatgpt-mermaid-_r_1af_ [data-look="neo"].cluster rect,#chatgpt-mermaid-_r_1af_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-_r_1af_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_1af_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-_r_1af_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-_r_1af_-gradient);stroke-width:1px;}#chatgpt-mermaid-_r_1af_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_1af_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-_r_1af_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-_r_1af_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_1af_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-_r_1af_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-_r_1af_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_1af_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-_r_1af_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_1af_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";}ft_userft_user_profileft_user_roleft_roleft_role_permissionft_permissionhasassignedcontainsgrantsassigned






表名	中文名称	主要职责
ft_user	用户账号表	统一身份、登录凭证、账号状态
ft_user_profile	用户资料表	昵称、头像、个人展示信息
ft_role	角色表	USER、OPERATOR 等角色
ft_permission	权限表	定义具体操作权限
ft_user_role	用户角色关联表	用户与角色多对多
ft_role_permission	角色权限关联表	角色与权限多对多
二、统一字段规范
所有有独立主键的表，默认遵循：
字段	类型	规则
id	BIGINT	雪花主键，NOT NULL
created_at	DATETIME(3)	创建时间，UTC，NOT NULL
updated_at	DATETIME(3)	更新时间，UTC，NOT NULL
DATETIME(3) 本身不存储时区，因此 Java 应用、JDBC 会话及数据库写入必须统一 UTC 约定。
账号和角色不采用通用软删除字段。需要保留历史交易引用的账号，注销时通过专门的状态、脱敏及保留策略处理。
三、ft_user：用户账号表
字段	类型	约束	说明
id	BIGINT	PK	用户 ID
username	VARCHAR(32)	NOT NULL，UNIQUE	登录用户名
email	VARCHAR(254)	NOT NULL，UNIQUE	已验证邮箱
password_hash	VARCHAR(255)	NOT NULL	BCrypt 密码哈希
status	VARCHAR(24)	NOT NULL	账号状态
password_changed_at	DATETIME(3)	NULL	最近修改密码时间
last_login_at	DATETIME(3)	NULL	最近成功登录时间
created_at	DATETIME(3)	NOT NULL	创建时间
updated_at	DATETIME(3)	NOT NULL	更新时间
version	BIGINT	NOT NULL，默认 0	并发修改版本
账号状态统一使用：
ACTIVE、DISABLED、DELETION_PENDING、DELETED。
唯一性设计： 用户名只允许 ASCII 字母、数字及下划线，在写入前统一小写。邮箱保留用户展示形式，但建议另有规范化的邮箱唯一键，以免依赖数据库比较规则处理邮箱大小写及等价关系。
MySQL 8.4 的 utf8mb4_0900_ai_ci 是大小写不敏感、重音不敏感排序规则。对于用户名这类经过规范化的 ASCII 标识，可以使用二进制比较语义来确保行为可预测。

MySQL
+1



四、ft_user_profile：用户资料表
字段	类型	约束	说明
user_id	BIGINT	PK，FK	对应用户 ID
nickname	VARCHAR(32)	NOT NULL	昵称
avatar_url	VARCHAR(512)	NULL	头像地址
created_at	DATETIME(3)	NOT NULL	创建时间
updated_at	DATETIME(3)	NOT NULL	更新时间
这里建议不再增加独立 id，直接使用 user_id 作为主键。
原因是一个用户只能拥有一份基础资料，主键本身就能保证一对一关系。
昵称不需要全局唯一，用户可以重名。
五、ft_role：角色表
字段	类型	约束	说明
id	BIGINT	PK	角色 ID
code	VARCHAR(32)	NOT NULL，UNIQUE	角色标识
name	VARCHAR(64)	NOT NULL	角色名称
description	VARCHAR(255)	NULL	角色说明
status	VARCHAR(16)	NOT NULL	ENABLED / DISABLED
system_builtin	BOOLEAN	NOT NULL，默认 FALSE	是否内置角色
created_at	DATETIME(3)	NOT NULL	创建时间
updated_at	DATETIME(3)	NOT NULL	更新时间
预置五个角色：
code	name
USER	普通用户
OPERATOR	运营人员
FINANCE	财务人员
ADMIN	系统管理员
SUPER_ADMIN	超级管理员
内置角色不允许随意删除。SUPER_ADMIN 的权限变更和成员变更必须经过专门校验。
六、ft_permission：权限表
字段	类型	约束	说明
id	BIGINT	PK	权限 ID
code	VARCHAR(100)	NOT NULL，UNIQUE	权限标识
name	VARCHAR(64)	NOT NULL	权限名称
module	VARCHAR(32)	NOT NULL	所属业务模块
description	VARCHAR(255)	NULL	权限说明
status	VARCHAR(16)	NOT NULL	ENABLED / DISABLED
created_at	DATETIME(3)	NOT NULL	创建时间
updated_at	DATETIME(3)	NOT NULL	更新时间
权限示例：
module	code	name
event	event:create	创建演出
event	event:publish	发布演出
inventory	inventory:adjust	调整库存
order	order:read	查询运营订单
refund	refund:approve	审核退款
risk	risk:rule:manage	管理风控规则
system	role:manage	管理角色权限
建议权限只描述服务端业务操作，不将前端菜单和按钮也混入同一权限实体。
前端可以根据返回的权限标识控制展示，但最终授权必须由 Spring Security 和业务服务校验。
七、ft_user_role：用户角色关联表
字段	类型	约束
user_id	BIGINT	PK 组成部分，FK
role_id	BIGINT	PK 组成部分，FK
created_at	DATETIME(3)	NOT NULL
created_by	BIGINT	NULL，授权操作员 ID
采用联合主键：
PRIMARY KEY(user_id, role_id)
并增加 INDEX(role_id, user_id)，用于查询某角色的所有用户。
用户可以拥有多个角色，例如一个运营人员同时拥有 USER 和 OPERATOR。
八、ft_role_permission：角色权限关联表
字段	类型	约束
role_id	BIGINT	PK 组成部分，FK
permission_id	BIGINT	PK 组成部分，FK
created_at	DATETIME(3)	NOT NULL
created_by	BIGINT	NULL，授权操作员 ID
采用联合主键：
PRIMARY KEY(role_id, permission_id)
再增加 INDEX(permission_id, role_id)，用于反向查询哪些角色拥有某项权限。
这两张关联表不需要再生成雪花 ID，因为联合主键已经能够表示唯一的业务关系。
MySQL InnoDB 外键要求引用列具备合适索引。用户、角色、权限这类相对稳定的数据，很适合使用物理外键维护关联完整性。

MySQL



九、统一索引与外键方案
表	主键	其他核心索引
ft_user	id	UNIQUE(username)、UNIQUE(规范化邮箱键)
ft_user_profile	user_id	无额外索引
ft_role	id	UNIQUE(code)
ft_permission	id	UNIQUE(code)、INDEX(module)
ft_user_role	user_id + role_id	INDEX(role_id, user_id)
ft_role_permission	role_id + permission_id	INDEX(permission_id, role_id)
外键采用 RESTRICT 删除策略，禁止直接删除仍有授权关系的角色、权限及用户。
用户注销走业务状态与数据处理流程，而非通过级联删除清空身份和历史交易关系。
十、需要同时确定的安全规则
这些不完全属于表结构，但会直接影响持久化设计。
- 首次注册统一赋予 USER 角色，不接受客户端传入角色 ID。
- 后台员工由有权限的管理员创建，不能通过普通注册接口获取后台角色。
- 普通 ADMIN 不能授予自己或他人 SUPER_ADMIN。
- 不能禁用或移除最后一个有效超级管理员。
- 权限变更与角色变更应在本地事务中完成，并记录可靠审计事件。
- 账号禁用、角色撤销后，后台敏感接口必须立即校验新的授权状态，不能只信任旧 JWT 中的角色声明。
- Redis 中存储 Refresh Token 会话；不在 ft_user 保存明文令牌或短期验证码。
十一、阶段结论
用户与权限数据库模型 V1.0


6 张核心表： 用户、资料、角色、权限、用户角色关系、角色权限关系。
主键： 4 张实体表采用雪花 ID 或用户 ID，2 张关系表采用联合主键。
一致性： MySQL 唯一索引、物理外键、本地事务及服务端权限检查。
账号体系： 统一用户主体，普通购票和后台管理入口分离。
扩展边界： 登录审计、操作审计、账号注销、持久化会话记录在后续迁移阶段添加。



FlashTicket 数据库详细设计 02：演出活动与场次域
Event · Venue · Session · Ticket Tier · Sale Plan · Review | MySQL 8.4 LTS
本轮继续全局数据库设计的第二批，详细确定 7 张核心表的字段、数据类型、默认值、索引、唯一约束、外键关系及业务状态。
沿用前面已经确定的技术栈：
- Java 21 + Spring Boot 3.5.x + MyBatis-Plus。
- MySQL 8.4 LTS + InnoDB + Flyway。
- 雪花算法 BIGINT 主键，默认 REPEATABLE READ。
- 核心业务 SQL 手写。
- 第一版仅支持数量型票档，不指定具体座位。
这一轮只做数据库设计，不编写 DDL 或 Java 代码。
一、数据库表清单
表名	中文名称	主要职责
ft_event_category	演出分类表	管理演唱会、音乐节等分类
ft_event	演出活动表	活动基础信息与发布状态
ft_venue	场馆表	场馆地址、时区、容纳人数
ft_session	演出场次表	场次时间、场馆、生命周期
ft_ticket_tier	票档表	票价、限购、核定票量
ft_sale_plan	售票计划表	开售时间、停售时间、暂停控制
ft_event_review	活动审核记录表	提交审核、审核结果及历史
核心 ER 关系
#chatgpt-mermaid-_r_1cu_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-_r_1cu_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-_r_1cu_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-_r_1cu_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-_r_1cu_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-_r_1cu_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-_r_1cu_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-_r_1cu_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-_r_1cu_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-_r_1cu_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-_r_1cu_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-_r_1cu_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-_r_1cu_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-_r_1cu_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";font-size:16px;}#chatgpt-mermaid-_r_1cu_ p{margin:0;}#chatgpt-mermaid-_r_1cu_ .entityBox{fill:rgb(19, 33, 54);stroke:rgb(31, 78, 148);}#chatgpt-mermaid-_r_1cu_ .relationshipLabelBox{fill:rgb(48, 48, 48);opacity:0.7;background-color:rgb(48, 48, 48);}#chatgpt-mermaid-_r_1cu_ .relationshipLabelBox rect{opacity:0.5;}#chatgpt-mermaid-_r_1cu_ .labelBkg{background-color:rgba(48, 48, 48, 0.5);}#chatgpt-mermaid-_r_1cu_ .edgeLabel{background-color:rgb(14, 14, 14);}#chatgpt-mermaid-_r_1cu_ .edgeLabel .label rect{fill:rgb(14, 14, 14);}#chatgpt-mermaid-_r_1cu_ .edgeLabel .label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-_r_1cu_ .edgeLabel .label{fill:rgb(31, 78, 148);font-size:14px;}#chatgpt-mermaid-_r_1cu_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";color:rgb(237, 237, 237);}#chatgpt-mermaid-_r_1cu_ .edge-pattern-dashed{stroke-dasharray:8,8;}#chatgpt-mermaid-_r_1cu_ .node rect,#chatgpt-mermaid-_r_1cu_ .node circle,#chatgpt-mermaid-_r_1cu_ .node ellipse,#chatgpt-mermaid-_r_1cu_ .node polygon{fill:rgb(19, 33, 54);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-_r_1cu_ .relationshipLine{stroke:rgb(175, 175, 175);stroke-width:1px;fill:none;}#chatgpt-mermaid-_r_1cu_ .marker{fill:none!important;stroke:rgb(175, 175, 175)!important;stroke-width:1;}#chatgpt-mermaid-_r_1cu_ [data-look=neo].labelBkg{background-color:rgba(48, 48, 48, 0.5);}#chatgpt-mermaid-_r_1cu_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-_r_1cu_ [data-look="neo"].node rect,#chatgpt-mermaid-_r_1cu_ [data-look="neo"].cluster rect,#chatgpt-mermaid-_r_1cu_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-_r_1cu_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_1cu_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-_r_1cu_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-_r_1cu_-gradient);stroke-width:1px;}#chatgpt-mermaid-_r_1cu_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_1cu_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-_r_1cu_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-_r_1cu_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_1cu_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-_r_1cu_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-_r_1cu_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_1cu_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-_r_1cu_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_1cu_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";}ft_event_categoryft_eventft_sessionft_venueft_ticket_tierft_sale_planft_event_reviewft_userft_inventoryclassifiescontainshostscontainscontrolsreview_historyreviewsinventory





票档创建时可以暂时没有售票计划和库存记录，但活动正式发布及开放销售前，必须完成相关初始化。
三个设计原则：
1. 演出、场次、票档是不同实体，不将所有信息堆在一张表里。
2. 票档价格和总发行配置属于票务域，实际可售、预占、已售库存属于库存域。
3. 活动发布状态、场次状态、售票时间及库存状态相互独立，不使用一个大状态枚举覆盖全部业务。

二、ft_event_category：演出分类表
用于用户端分类筛选以及后台活动分类管理。
字段设计
字段	MySQL 类型	约束及默认值	说明
id	BIGINT	PK	分类 ID
code	VARCHAR(32)	NOT NULL，UNIQUE	分类标识
name	VARCHAR(64)	NOT NULL	分类名称
description	VARCHAR(255)	NULL	分类说明
sort_order	INT	NOT NULL，默认 0	排序
status	VARCHAR(16)	NOT NULL，默认 ENABLED	是否启用
created_at	DATETIME(3)	NOT NULL	创建时间
updated_at	DATETIME(3)	NOT NULL	更新时间
分类初始化数据
code	name
CONCERT	演唱会
MUSIC_FESTIVAL	音乐节
THEATER	话剧演出
SPORTS	体育赛事
COMEDY	脱口秀
EXHIBITION	展览活动
索引及规则
- PRIMARY KEY(id)
- UNIQUE KEY uk_event_category_code(code)
- INDEX idx_event_category_status_sort(status, sort_order)
code 是稳定的业务标识，不建议修改。
已经被演出活动引用的分类不允许物理删除，可以停用。停用分类后，已有活动的分类关系仍保留。

三、ft_event：演出活动主表
这张表是用户端演出详情、后台活动管理以及运营审核的核心实体。
字段设计
字段	MySQL 类型	约束及默认值	说明
id	BIGINT	PK	活动 ID
category_id	BIGINT	NOT NULL	分类 ID
title	VARCHAR(100)	NOT NULL	演出名称
subtitle	VARCHAR(200)	NULL	副标题
cover_url	VARCHAR(512)	NULL	封面地址
description	VARCHAR(500)	NULL	简介
detail_content	MEDIUMTEXT	NULL	演出详细说明
organizer_name	VARCHAR(100)	NULL	主办方名称
attendance_notice	TEXT	NULL	观演须知
refund_policy	TEXT	NULL	退票规则描述
status	VARCHAR(24)	NOT NULL，默认 DRAFT	活动生命周期
published_at	DATETIME(3)	NULL	最后一次发布生效时间
cancelled_at	DATETIME(3)	NULL	取消时间
cancel_reason	VARCHAR(500)	NULL	取消原因
created_by	BIGINT	NOT NULL	创建人
updated_by	BIGINT	NOT NULL	最近修改人
version	BIGINT	NOT NULL，默认 0	乐观并发版本
created_at	DATETIME(3)	NOT NULL	创建时间
updated_at	DATETIME(3)	NOT NULL	更新时间
活动状态
状态	含义
DRAFT	草稿
PENDING_REVIEW	待审核
REJECTED	审核驳回
PUBLISHED	已发布
CANCELLED	已取消
ARCHIVED	已归档
不增加 SELLING、SOLD_OUT 等状态，这些是由场次、售票计划、库存推导的展示状态。
索引
索引	字段	用途
PK	id	主键查询
idx_event_category_status	(category_id, status, id)	分类浏览
idx_event_status_created	(status, created_at, id)	后台活动列表
idx_event_created_by	(created_by, created_at)	创建人管理活动
活动名称搜索第一版可以使用普通搜索及简单查询，但若大量使用 %关键词% 模糊匹配，普通 B-Tree 索引并不能很好地优化。暂时不为此额外引入 Elasticsearch。
特别重要：审核快照
审核需要保证审核人看到的版本与最终发布的版本一致。
建议 ft_event_review 保存提交时的配置版本与审核内容快照，不能只记录“审核通过”四个字。
ft_event.version 每次关键配置变更递增；审核通过及发布时必须验证待审核版本没有被其他运营操作修改。
第一版约定：待审核期间禁止修改提交范围内的关键配置。 如果需要修改，应撤回审核并重新提交。
已发布活动的关键内容变更应产生新的待审核版本，不能直接覆盖线上有效配置。完整的线上发布版本和草稿并行编辑由后续 ft_event_revision 进一步支持。
四、ft_venue：演出场馆表
场馆独立于活动，一个场馆可以举办多个活动及场次。
字段设计
字段	类型	约束及默认值	说明
id	BIGINT	PK	场馆 ID
name	VARCHAR(100)	NOT NULL	场馆名称
country_code	CHAR(2)	NOT NULL，默认 CN	ISO 国家代码
province	VARCHAR(64)	NULL	省份
city	VARCHAR(64)	NOT NULL	城市
district	VARCHAR(64)	NULL	区县
address	VARCHAR(255)	NOT NULL	详细地址
timezone	VARCHAR(64)	NOT NULL，默认 Asia/Shanghai	IANA 时区
max_capacity	INT	NOT NULL	最大核定容量
description	VARCHAR(1000)	NULL	场馆简介
status	VARCHAR(16)	NOT NULL，默认 ENABLED	场馆状态
created_at	DATETIME(3)	NOT NULL	创建时间
updated_at	DATETIME(3)	NOT NULL	更新时间
version	BIGINT	NOT NULL，默认 0	配置并发版本
索引
- PRIMARY KEY(id)
- INDEX idx_venue_city_status(city, status, id)
- INDEX idx_venue_name(name)
第一版不强制场馆名称全局唯一，因为不同城市可能存在同名体育馆。
容量规则
- max_capacity > 0。
- 场次的可售容量不得超过对应场馆容量。
- 已创建场次后，减少场馆容量不能使已有有效场次变为非法。
- 场馆停用不自动取消已有演出。
- 已经被场次引用的场馆不得物理删除。
max_capacity > 0 可以用数据库 CHECK 约束验证，但涉及不同表的“场次容量不能超过场馆容量”，必须由事务中的业务校验维护。MySQL CHECK 不支持查询其他表。

MySQL




五、ft_session：演出场次表
一个 Event 可以包含多个 Session，但一条 Session 只能归属于一个 Event。
字段设计
字段	类型	约束及默认值	说明
id	BIGINT	PK	场次 ID
event_id	BIGINT	NOT NULL	活动 ID
venue_id	BIGINT	NOT NULL	场馆 ID
name	VARCHAR(64)	NOT NULL	场次名称
start_at	DATETIME(3)	NOT NULL	演出开始时间（UTC）
end_at	DATETIME(3)	NOT NULL	预计结束时间（UTC）
entry_start_at	DATETIME(3)	NULL	开始入场时间
sale_capacity	INT	NOT NULL	场次核定可售量
status	VARCHAR(24)	NOT NULL，默认 SCHEDULED	场次状态
postponed_at	DATETIME(3)	NULL	延期记录时间
cancelled_at	DATETIME(3)	NULL	取消时间
change_reason	VARCHAR(500)	NULL	延期或取消原因
created_at	DATETIME(3)	NOT NULL	创建时间
updated_at	DATETIME(3)	NOT NULL	更新时间
version	BIGINT	NOT NULL，默认 0	并发版本
场次状态
状态	含义
SCHEDULED	正常场次
POSTPONED	已延期，暂停新销售
CANCELLED	场次取消
COMPLETED	演出完成
场次的状态不直接使用 SELLING 或 SOLD_OUT，由售票计划与库存决定具体展示。
索引
索引	字段	用途
idx_session_event_start	(event_id, start_at, id)	演出详情中的场次列表
idx_session_venue_time	(venue_id, start_at, end_at)	场馆排期查询
idx_session_status_start	(status, start_at, id)	场次运营查询
数据约束
- end_at > start_at。
- sale_capacity >= 0。
- entry_start_at 如果存在，必须早于演出开始时间。
- 有销售记录的场次不能直接物理删除。
- 场次取消后不得接受新的抢票请求。
- 演出结束不是简单根据时间就认定 COMPLETED，应根据运营确认或经过业务确认的自动流程完成。
核心并发问题：场馆时间冲突
例如同一场馆已有：
场次	占用时间
场次 A	19:00–21:00
场次 B	20:30–22:00
场次 B 与 A 冲突，不能创建。
但更难的问题是：
两个运营人员并发创建两个各自看起来没有冲突、合起来却冲突的场次。
仅靠普通 SELECT 查询冲突再插入，可能产生竞态。
推荐在创建或修改场次时：
1. 开启 MySQL 本地事务。
2. 获取对应 ft_venue 行的排他锁，作为该场馆排期的串行化入口。
3. 检查包括清场缓冲时间在内的排期区间是否重叠。
4. 插入或修改场次。
5. 提交事务。
所有可能改变占用时间的操作，包括延期、取消、恢复，也应遵循同一场馆排期锁定协议。
如果场次更换场馆，则按场馆 ID 的固定顺序获取相关锁，降低死锁风险。
这种设计比使用 Redisson 对场馆名称加锁更适合单数据库单体架构。
六、ft_ticket_tier：票档表
这是整个 FlashTicket 高频库存链路的上游配置表。
字段设计
字段	类型	约束及默认值	说明
id	BIGINT	PK	票档 ID
session_id	BIGINT	NOT NULL	所属场次
name	VARCHAR(64)	NOT NULL	票档名称
ticket_type	VARCHAR(32)	NOT NULL，默认 GENERAL_ADMISSION	票档类型
price_cent	BIGINT	NOT NULL	单价（分）
initial_quantity	INT	NOT NULL	初始化发行量
min_per_order	INT	NOT NULL，默认 1	单笔最小购买数量
max_per_order	INT	NOT NULL，默认 2	单笔最大购买数量
max_per_user	INT	NOT NULL，默认 2	每人累计限购
description	VARCHAR(500)	NULL	票档说明
sort_order	INT	NOT NULL，默认 0	展示顺序
status	VARCHAR(16)	NOT NULL，默认 ENABLED	配置启用状态
created_at	DATETIME(3)	NOT NULL	创建时间
updated_at	DATETIME(3)	NOT NULL	更新时间
version	BIGINT	NOT NULL，默认 0	并发版本
票档类型
第一版只允许：
GENERAL_ADMISSION
代表不指定座位的数量型门票。
未来支持指定座位时，才增加 RESERVED_SEATING 等类型及座位占用数据表。
索引与约束
约束 / 索引	字段	说明
PK	id	票档主键
UNIQUE	(session_id, name)	同一场次票档名称唯一
INDEX	(session_id, status, sort_order)	场次票档列表
关键规则：
\[
1 \le minPerOrder \le maxPerOrder \le maxPerUser
\]

此外：
- price_cent >= 0。
- initial_quantity >= 0。
- 同一场次所有票档初始数量合计不得超过 ft_session.sale_capacity。
- 发布后已经产生有效抢票或订单记录的票档，不能直接修改其历史成交价格。
- 新增或减少可售额度必须经库存中心执行调整。
- 票档已有订单关联后禁止物理删除。
特别要区分 initial_quantity 和 ft_inventory.total。
initial_quantity 是初始核定票量的配置快照；ft_inventory.total 是当前库存总账的核定容量。后续经过合法审批增发或减少票量时，两者可以不同。
票档表绝不能和库存表分别维护两个实时剩余票数。

七、ft_sale_plan：售票计划表
一个票档可以预先配置一个售票计划；活动发布前必须配置完成。
第一版保持“一票档一售票窗口”，不引入复杂预售批次。
字段设计
字段	类型	约束及默认值	说明
id	BIGINT	PK	售票计划 ID
tier_id	BIGINT	NOT NULL，UNIQUE	对应票档
sale_start_at	DATETIME(3)	NOT NULL	开售时间
sale_end_at	DATETIME(3)	NOT NULL	销售截止时间
control_status	VARCHAR(16)	NOT NULL，默认 ENABLED	销售控制状态
paused_at	DATETIME(3)	NULL	最近暂停时间
pause_reason	VARCHAR(500)	NULL	暂停原因
config_version	BIGINT	NOT NULL，默认 0	销售配置版本
created_at	DATETIME(3)	NOT NULL	创建时间
updated_at	DATETIME(3)	NOT NULL	更新时间
约束及索引
- UNIQUE KEY uk_sale_plan_tier(tier_id)
- CHECK(sale_end_at > sale_start_at)
- INDEX idx_sale_plan_window(sale_start_at, sale_end_at)
时间窗口规则
必须满足：
\[
saleStartAt < saleEndAt
\]

第一版还要求：
\[
saleStartAt < sessionStartAt
\]

对于 saleEndAt，建议不晚于演出开始时间。实际运营有特殊入场前销售场景时，再通过明确业务配置扩展。
销售状态如何计算？
售票资格不是只查一个 control_status。
条件	必须满足
活动	PUBLISHED
场次	SCHEDULED
票档	ENABLED
售票计划	ENABLED
时间	sale_start_at <= now < sale_end_at
库存	有可发放资格
风控	当前不处于禁止准入状态
因此 ft_sale_plan 不存 NOT_STARTED、SELLING、SOLD_OUT 这些可由时间和库存推导的冗余状态。
销售配置版本与 Redis 关联
Redis 预占模块需要知道当前票档使用的是哪个销售配置版本。
建议当发生以下操作时递增 config_version：
- 修改开售和截止时间。
- 暂停销售。
- 恢复销售。
- 改变正式生效的售票规则。
高并发抢票在关键状态校验时应识别配置版本，不能因为 Redis 缓存里仍是旧的 ENABLED 就允许暂停后的请求继续创建订单。
还要结合前面库存模块规划的销售代次，区分普通配置更新和需要冻结、重建库存准入预算的恢复操作。两者不可混为一个版本号。
八、ft_event_review：活动审核记录表
这是整个演出发布流程的审计与审核依据。
字段设计
字段	类型	约束及默认值	说明
id	BIGINT	PK	审核记录 ID
event_id	BIGINT	NOT NULL	目标活动
submitted_by	BIGINT	NOT NULL	提交审核人
submitted_at	DATETIME(3)	NOT NULL	提交时间
reviewed_by	BIGINT	NULL	审核人
reviewed_at	DATETIME(3)	NULL	审核时间
status	VARCHAR(24)	NOT NULL，默认 PENDING	审核状态
review_comment	VARCHAR(1000)	NULL	审核意见
event_version	BIGINT	NOT NULL	提交时活动版本
configuration_snapshot	JSON	NOT NULL	提交审核的配置快照
created_at	DATETIME(3)	NOT NULL	创建时间
updated_at	DATETIME(3)	NOT NULL	更新时间
审核状态
状态	含义
PENDING	等待审核
APPROVED	审核通过
REJECTED	审核拒绝
WITHDRAWN	提交人撤回
configuration_snapshot 应包含审核范围内的活动、场次、票档、售票计划及相关重要规则，并带有内部快照格式版本。
不能只保存活动标题和活动版本，因为子表的配置也可能发生变化。
关键索引
索引	字段	用途
idx_review_event_time	(event_id, submitted_at, id)	活动审核历史
idx_review_status_time	(status, submitted_at, id)	待审核列表
idx_review_reviewer	(reviewed_by, reviewed_at)	审核人记录
并发审核约束
同一个活动同一时刻最多存在一个待处理审核申请。
这一约束不能仅依赖“先查询有没有 PENDING 再插入”。
建议统一通过 ft_event 的状态条件更新控制提交和领取审核资格。在同一事务中创建审核记录并修改活动状态。
审核时还要确认：
- 审核记录仍为 PENDING。
- 活动配置版本仍符合提交的快照。
- 当前审核人不是提交人。
- 活动仍处于合法待审核状态。
- 相关售票配置及容量约束仍成立。
审核通过和活动发布是两个动作。第一版可以由同一有权限的管理员连续执行，但需要分别记录。
一个需要提前确定的发布策略
为了避免 ft_event_review 记录了审核快照，却在发布时引用已经被修改的场次和票档，建议第一版采用：
关键配置在提交审核后冻结，审核通过后到发布生效前继续保持冻结；只有发布成功或审核流程终结后，才能解冻相应操作。
已经发布的活动若要修改核心配置，优先走新的审核版本，不允许直接绕过审核写入线上表。
对于“线上正常销售，同时编辑下一版草稿”，应在 P1 正式引入 ft_event_revision 的独立发布版本机制。第一版不做复杂的双版本并行编辑。
九、七张表的完整索引汇总
表名	主键	关键唯一索引	普通索引
ft_event_category	id	code	status + sort_order
ft_event	id	—	category_id + status；status + created_at
ft_venue	id	—	city + status；name
ft_session	id	—	event_id + start_at；venue_id + start_at
ft_ticket_tier	id	session_id + name	session_id + status + sort_order
ft_sale_plan	id	tier_id	sale_start_at + sale_end_at
ft_event_review	id	通过活动状态控制单条待审核	event_id + submitted_at；status + submitted_at
索引初步按最常见的列表查询、详情查询、审核及场馆排期校验设计。后续必须用实际 SQL 和 EXPLAIN ANALYZE 检查，不能认为索引越多越好。
十、外键关系设计
这一批数据主要是相对稳定的运营配置，因此建议使用物理外键。
子表字段	引用表	删除策略
ft_event.category_id	ft_event_category.id	RESTRICT
ft_session.event_id	ft_event.id	RESTRICT
ft_session.venue_id	ft_venue.id	RESTRICT
ft_ticket_tier.session_id	ft_session.id	RESTRICT
ft_sale_plan.tier_id	ft_ticket_tier.id	RESTRICT
ft_event_review.event_id	ft_event.id	RESTRICT
ft_event_review.submitted_by	ft_user.id	RESTRICT
ft_event_review.reviewed_by	ft_user.id	RESTRICT
InnoDB 会检查外键所引用的父表数据，并要求使用合适的索引。外键还可能增加相关表的锁定与写入成本，因此本项目主要用于稳定配置表，而不是强制所有高频交易表都建立外键。

MySQL



十一、跨表业务约束
这是本批数据库设计真正需要重点落实的部分。
业务约束	实现方式
同场馆场次不可非法重叠	锁定场馆行 + 排期校验
场次容量不得超过场馆容量	本地事务 + 父记录锁定
同场次票档容量合计不得超过场次容量	场次行锁 + 汇总校验
活动发布必须有有效场次、票档、销售计划	发布前完整性校验
已成交票档不得直接改变历史订单价格	订单快照 + 修改限制
已有库存的票档不得重新初始化	ft_inventory.tier_id 唯一约束
暂停售票后不得产生非法新订单	销售配置版本 + 库存准入控制
同活动不可存在并行待审核流程	活动状态条件更新
已售票场次取消后必须处理订单	取消状态 + 可靠业务事件 + 补偿
这些涉及多张表的规则，不能仅依赖 CHECK 或普通唯一索引实现，需要在事务及业务服务中维护。
推荐的锁定顺序
为了降低运营配置变更时的死锁风险，建议明确统一加锁顺序：
场馆 → 活动 → 场次 → 票档 → 售票计划。
但需要注意，这并不意味着任何事务都必须把全部对象锁一遍。只有在修改涉及这些资源的共享约束时才按统一顺序加锁。
尤其不能在抢票核心链路中为了读取演出配置，频繁锁定所有这些运营记录，否则会制造新的热点。
十二、库存初始化与售票资格边界
这一批运营配置表需要和后续的库存模块形成明确约定。
活动、场次、票档配置完成


提交审核并通过


初始化 MySQL 库存账本
每个票档只创建一份 ft_inventory


发布活动与销售配置


Redis 预热通过后允许高速抢票


这里建议进一步明确销售就绪状态：
活动已发布，并不必然代表库存准入已经就绪。
如果 Redis 初始化失败或库存检查未通过，系统不能错误放行高并发抢票请求。应由库存或准入模块持久化维护就绪状态，活动表不额外增加一份容易产生分歧的 stock_ready 字段。
十三、Flyway 迁移规划
这一批七张表建议分为两个逻辑迁移阶段。
迁移阶段	内容
演出基础结构	分类、活动、场馆、场次
售票与审核结构	票档、销售计划、审核记录
迁移时按照父表先于子表的顺序创建；种子数据初始化演出分类，而不是在生产环境硬编码演出活动。
需要注意，Flyway 负责数据结构迁移，不应被当作大量运营活动数据的日常管理工具。
十四、数据库设计验收清单
演出与场次数据库验收
0/16 已检查



[ ] 演出分类与活动是一对多关系
[ ] 一个活动支持多个场次
[ ] 一个场馆可以承载不同时间的多个场次
[ ] 同一场馆并发排期不能产生非法冲突
[ ] 场次容量不得超过场馆核定容量
[ ] 同场次票档发行总量不得超过可售容量
[ ] 每个票档最多一个 V1 售票计划
[ ] 销售状态不重复存储时间推导结果
[ ] 售票计划版本变化能使旧准入配置失效
[ ] 票档初始票量不等同于实时可售库存
[ ] 审核历史保存完整配置快照
[ ] 待审核配置受并发修改保护
[ ] 发布时不会使用未经审核的新配置
[ ] 已有订单的场次和票档不能物理删除
[ ] 所有雪花 ID 和时间字段遵守统一规范
[ ] 索引覆盖核心后台和用户端查询场景
此处用于后续迁移和集成测试验收，不表示数据库表已经建立。
十五、本批数据库设计结论
演出活动与场次域 V1.0


7 张核心表： 分类、活动、场馆、场次、票档、售票计划、审核记录。
领域关系： Event → Session → TicketTier → SalePlan，场次独立关联 Venue。
业务状态： 活动发布、场次有效性、票档启用、销售控制分别管理。
并发安全： 场馆排期串行化、容量约束、活动版本校验和审核配置冻结。
库存边界： 票档保存初始发行配置，库存中心维护真正的剩余、预占及已售账本。
审核与历史： 审核快照、订单交易快照、关键配置变更受控。
第一版范围： 数量型票档、单轮公开售票，不支持指定座位或多轮预售。

至此，数据库详细设计已完成前两批，共 13 张核心表。



FlashTicket 数据库详细设计 03：库存与票档预占域
Inventory & Reservation · MySQL 8.4 LTS · 详细表结构 V1.0
库存与票档预占域是 FlashTicket 整个高并发交易系统中，对数据正确性要求最高的部分之一。
本轮确定 3 张核心表：
表名	中文名称	核心职责
ft_inventory	票档库存总账表	维护可售、预占、已售数量
ft_reservation	库存预占记录表	维护每笔抢票请求的库存占用
ft_inventory_ledger	库存变更流水表	追踪库存每一次增减和状态转换
另有一张跨模块关联表 ft_purchase_request，归属于第四批“抢票请求域”，本轮只确定它与库存的关联约束，不重复建表。
一、核心 ER 关系
#chatgpt-mermaid-_r_15a_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-_r_15a_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-_r_15a_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-_r_15a_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-_r_15a_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-_r_15a_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-_r_15a_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-_r_15a_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-_r_15a_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-_r_15a_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-_r_15a_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-_r_15a_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-_r_15a_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-_r_15a_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";font-size:16px;}#chatgpt-mermaid-_r_15a_ p{margin:0;}#chatgpt-mermaid-_r_15a_ .entityBox{fill:rgb(19, 33, 54);stroke:rgb(31, 78, 148);}#chatgpt-mermaid-_r_15a_ .relationshipLabelBox{fill:rgb(48, 48, 48);opacity:0.7;background-color:rgb(48, 48, 48);}#chatgpt-mermaid-_r_15a_ .relationshipLabelBox rect{opacity:0.5;}#chatgpt-mermaid-_r_15a_ .labelBkg{background-color:rgba(48, 48, 48, 0.5);}#chatgpt-mermaid-_r_15a_ .edgeLabel{background-color:rgb(14, 14, 14);}#chatgpt-mermaid-_r_15a_ .edgeLabel .label rect{fill:rgb(14, 14, 14);}#chatgpt-mermaid-_r_15a_ .edgeLabel .label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-_r_15a_ .edgeLabel .label{fill:rgb(31, 78, 148);font-size:14px;}#chatgpt-mermaid-_r_15a_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";color:rgb(237, 237, 237);}#chatgpt-mermaid-_r_15a_ .edge-pattern-dashed{stroke-dasharray:8,8;}#chatgpt-mermaid-_r_15a_ .node rect,#chatgpt-mermaid-_r_15a_ .node circle,#chatgpt-mermaid-_r_15a_ .node ellipse,#chatgpt-mermaid-_r_15a_ .node polygon{fill:rgb(19, 33, 54);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-_r_15a_ .relationshipLine{stroke:rgb(175, 175, 175);stroke-width:1px;fill:none;}#chatgpt-mermaid-_r_15a_ .marker{fill:none!important;stroke:rgb(175, 175, 175)!important;stroke-width:1;}#chatgpt-mermaid-_r_15a_ [data-look=neo].labelBkg{background-color:rgba(48, 48, 48, 0.5);}#chatgpt-mermaid-_r_15a_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-_r_15a_ [data-look="neo"].node rect,#chatgpt-mermaid-_r_15a_ [data-look="neo"].cluster rect,#chatgpt-mermaid-_r_15a_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-_r_15a_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_15a_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-_r_15a_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-_r_15a_-gradient);stroke-width:1px;}#chatgpt-mermaid-_r_15a_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_15a_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-_r_15a_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-_r_15a_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_15a_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-_r_15a_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-_r_15a_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_15a_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-_r_15a_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_15a_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";}ft_ticket_tierft_inventoryft_reservationft_inventory_ledgerft_purchase_requestft_orderhasallocatesrecordsproducesobtainsbacks





这是核心业务关系。每个已初始化的票档恰好有一份库存总账；未初始化票档可以暂时没有总账。流水可以用于初始化和人工调整，此时不一定对应某条预占记录。
本批设计的六条硬性约束
1. 每个票档只能拥有一条库存总账。
2. 每个抢票请求最多只能形成一条正式库存预占。
3. MySQL 的 reserved 只统计已提交数据库事务的有效预占。
4. 每次库存账目变化必须与对应流水在同一个事务中提交。
5. 已确认的预占不能再次释放，已释放的预占不能再次确认。
6. Redis 临时占用资格不能直接算作 MySQL 已确认预占。

二、ft_inventory：票档库存总账表
2.1 字段设计
字段	MySQL 类型	约束 / 默认值	说明
tier_id	BIGINT	PK，NOT NULL	票档 ID，同时关联 ft_ticket_tier.id
total	INT UNSIGNED	NOT NULL	当前核定总库存
available	INT UNSIGNED	NOT NULL	可售库存
reserved	INT UNSIGNED	NOT NULL，默认 0	已落库预占数量
sold	INT UNSIGNED	NOT NULL，默认 0	已售占用容量
status	VARCHAR(16)	NOT NULL，默认 ACTIVE	库存总账状态
generation	BIGINT UNSIGNED	NOT NULL，默认 1	Redis 准入恢复代次
version	BIGINT UNSIGNED	NOT NULL，默认 0	数据库账本版本
initialized_at	DATETIME(3)	NOT NULL	初始化时间
updated_at	DATETIME(3)	NOT NULL	最近账目变更时间
库存表不新增独立雪花 id。因为每个票档只对应一份库存账本，使用 tier_id 作为主键即可。
2.2 库存状态
状态	含义	是否允许新预占
ACTIVE	账本正常	还需验证售票与风控状态
FROZEN	运营或系统冻结	否
REBUILDING	Redis 资格重建、核对中	否
这里 ACTIVE 不是“正在售票”，而是“库存账本可以参与正常业务”。
即使状态为 ACTIVE，活动尚未开售、票档被暂停或库存为零时，也不能预占。
2.3 数据库约束
必须保证：
\[
total=available+reserved+sold
\]

此外：
\[
total,\ available,\ reserved,\ sold \geq 0
\]

虽然 UNSIGNED 能阻止存储负数，但仍建议使用 CHECK 强化总账守恒约束。
2.4 索引设计
索引	字段	作用
PRIMARY KEY	tier_id	根据票档操作库存
可选 idx_inventory_status	(status, tier_id)	批量查找被冻结或重建中的库存
第一版只需要主键即可支撑核心抢票更新。
不建议为了每秒高频变动的 available、reserved、sold 单独建普通索引，会增加无用的索引维护成本。
2.5 重要字段说明
version 和 generation 不是同一件事。
- version：每次数据库库存账目发生变化时递增，服务于并发更新与审计。
- generation：Redis 库存准入资格的恢复代次，在安全重建或切换准入代次时递增。
例如 MySQL 当前账本版本为 800，但 Redis 准入代次可能只有 3。
不能把两者混用，否则每次普通库存扣减都会触发 Redis 全量失效。

三、ft_reservation：库存预占记录表
这是每笔成功抢票请求对应的正式库存占用凭证。
3.1 字段设计
字段	MySQL 类型	约束 / 默认值	说明
id	BIGINT	PK	预占 ID，雪花算法
request_id	VARCHAR(64)	NOT NULL，UNIQUE	抢票幂等请求编号
user_id	BIGINT	NOT NULL	购票用户
tier_id	BIGINT	NOT NULL	票档 ID
quantity	INT UNSIGNED	NOT NULL	预占数量
status	VARCHAR(24)	NOT NULL	预占状态
order_id	BIGINT	NULL，UNIQUE	关联订单
expire_at	DATETIME(3)	NOT NULL	正式预占的支付截止时间
confirmed_at	DATETIME(3)	NULL	确认已售时间
released_at	DATETIME(3)	NULL	释放时间
release_reason	VARCHAR(32)	NULL	释放原因
generation	BIGINT UNSIGNED	NULL	来源 Redis 准入代次
version	BIGINT UNSIGNED	NOT NULL，默认 0	预占并发版本
created_at	DATETIME(3)	NOT NULL	创建时间
updated_at	DATETIME(3)	NOT NULL	更新时间
3.2 预占状态
建议正式预占表只保留三个稳定状态：
状态	含义	是否终态
RESERVED	已经在 MySQL 事务中占用库存	否
CONFIRMED	支付成功，库存转为已售	是
RELEASED	预占取消或超时，库存已归还	是
特别说明： 之前设计的 INIT、REDIS_HELD、RECOVERING 等状态，不放到这张正式预占记录表中。
这些属于抢票请求及 Redis 预占协调过程，统一由后续 ft_purchase_request 和 Redis 准入记录管理。
这样可以避免出现已经在 ft_reservation 表中写了 REDIS_HELD，但实际上 MySQL 库存根本没有扣减，导致账目语义混乱的问题。
3.3 状态机
RESERVED
MySQL 已确认预占，等待交易结果


CONFIRMED
支付成功，转入已售


RELEASED
关单取消，归还可售



两个终态互斥。重复确认或重复释放返回已存在的业务结果，不能再次改变库存计数。
3.4 唯一约束与索引
索引	字段	用途
PRIMARY KEY	id	预占 ID
uk_reservation_request	request_id	一个请求最多一份正式预占
uk_reservation_order	order_id	一笔订单最多对应一份预占
idx_reservation_tier_status	(tier_id, status, id)	按票档对账
idx_reservation_status_expire	(status, expire_at, id)	查找过期预占
idx_reservation_user_time	(user_id, created_at, id)	查询用户预占记录（按需）
order_id 允许 NULL；MySQL 唯一索引允许多个 NULL，因此可在正式订单关联前暂时为空。
不过第一版我们已经确定：MySQL 库存预占与创建订单在同一个本地事务中完成。 因此正常已提交的 RESERVED 记录应该存在合法订单，不应长时间保留 order_id = NULL。
3.5 释放原因
建议定义：
release_reason	含义
USER_CANCEL	用户取消待支付订单
PAYMENT_TIMEOUT	订单过期
ORDER_CREATE_FAILED	订单创建确定失败
SYSTEM_CANCEL	系统按业务规则关闭
EVENT_CANCELLED	活动取消
其中 ORDER_CREATE_FAILED 通常是 Redis 准入或请求协调层需要处理的原因。在正常的 MySQL 同事务建单方案中，如果订单创建失败，数据库预占应整体回滚，不会留下需要正式释放的 RESERVED 记录。
这个原因主要为未来拆分处理步骤或处理特殊异常预留。
四、ft_inventory_ledger：库存变更流水表
这是库存域的第三张核心表。
它负责记录每一次已经提交的库存变化，是库存对账、异常排查和高并发一致性验证的重要依据。
4.1 字段设计
字段	MySQL 类型	约束 / 默认值	说明
id	BIGINT	PK	雪花主键
operation_id	VARCHAR(64)	NOT NULL，UNIQUE	业务操作幂等 ID
tier_id	BIGINT	NOT NULL	票档 ID
reservation_id	BIGINT	NULL	对应预占记录
order_id	BIGINT	NULL	关联订单
operation_type	VARCHAR(32)	NOT NULL	库存操作类型
quantity	INT UNSIGNED	NOT NULL	本次操作的票数
available_delta	INT	NOT NULL，默认 0	可售库存变化量
reserved_delta	INT	NOT NULL，默认 0	预占库存变化量
sold_delta	INT	NOT NULL，默认 0	已售占用变化量
total_delta	INT	NOT NULL，默认 0	总库存变化量
version_before	BIGINT UNSIGNED	NOT NULL	操作前库存版本
version_after	BIGINT UNSIGNED	NOT NULL	操作后库存版本
reason	VARCHAR(255)	NULL	操作原因
operator_type	VARCHAR(16)	NOT NULL	SYSTEM / USER / ADMIN
operator_id	BIGINT	NULL	操作主体 ID
created_at	DATETIME(3)	NOT NULL	流水产生时间
delta 字段使用有符号 INT，因为库存变化既可能增加，也可能减少。
4.2 库存操作类型
操作类型	含义	可售变化	预占变化	已售变化
INITIALIZE	初始化库存	+100	0	0
RESERVE	预占 2 张	-2	+2	0
CONFIRM	支付确认 2 张	0	-2	+2
RELEASE	释放 2 张	+2	-2	0
INCREASE	增发 10 张	+10	0	0
DECREASE	减少 10 张未售票	-10	0	0
REFUND_RESTOCK	经授权重新上架 2 张	+2	0	-2
其中 INITIALIZE 的 total_delta 为 +100，INCREASE 为 +10，DECREASE 为 -10。
流水记录不应该只有操作类型和数量。 记录四个库存分量的变化和操作前后版本，更容易进行完整对账。
4.3 关键约束
- quantity > 0。
- operation_id 全局唯一。
- 每条流水满足：
\[
totalDelta=availableDelta+reservedDelta+soldDelta
\]

- version_after = version_before + 1。
- 同一票档、同一库存版本不能出现两条已提交的账目变更流水。
- 每一次真正改变库存计数的操作，在同一个事务内写入对应流水。
- INITIALIZE 只允许执行一次，重试时返回原初始化结果。
建议增加：
UNIQUE(tier_id, version_after)
这既便于追踪库存修改顺序，也可以辅助发现重复流水。
4.4 索引设计
索引	字段	用途
PRIMARY KEY	id	主键
uk_inventory_operation	operation_id	操作幂等
uk_inventory_version	(tier_id, version_after)	库存版本唯一
idx_ledger_tier_time	(tier_id, created_at, id)	票档流水查询
idx_ledger_reservation	(reservation_id, id)	查看一笔预占的变化历史
idx_ledger_order	(order_id, id)	订单库存流水查询
流水表属于高频写入表。第一版建议保留上述核心索引，等 JMeter 压测时再根据写入开销和实际查询需求做调整。
4.5 为什么采用不可变流水？
库存流水不允许普通业务进行修改或删除。
假设预占 2 张票，正常情况下会生成：
同一笔预占的库存流水示例
操作	可售	预占	已售
RESERVE	-2	+2	0
CONFIRM	0	-2	+2
如果订单超时，第二条应为 RELEASE：可售 +2、预占 -2、已售 0。CONFIRM 和 RELEASE 不可能对同一笔预占都成为有效终态。

对于需要纠正的历史记录，应追加新的、经过审批的冲正或调整流水，而不是修改旧记录。

五、三张表之间的核心事务规则
MySQL 8.4 InnoDB 的 SELECT ... FOR UPDATE 能够对相关记录获取排他锁；普通一致性读不能替代需要保护并发修改的锁定读取。

MySQL
+1



我们需要确保以下操作不会在高并发下产生不一致。
5.1 预占库存事务
顺序	操作	预期效果
1	验证请求幂等与处理资格	避免同一请求重复预占
2	对 ft_inventory 按主键进行条件扣减	available - q、reserved + q
3	创建 ft_reservation	状态 RESERVED
4	创建 ft_inventory_ledger	操作 RESERVE
5	创建待支付订单、更新请求及限购占用	形成完整交易
6	提交 MySQL 事务	所有操作一起成功或回滚
第一版建议上述操作在一个 MySQL 本地事务里完成。
因此，库存预占记录和订单要么同时存在，要么均不提交。
5.2 支付确认事务
操作	变化
校验订单及支付状态	仅允许合法确认
锁定订单、预占及相关库存记录	防止并发修改
预占变为 CONFIRMED	确认已售
库存账本变化	reserved - q，sold + q
创建 CONFIRM 流水	记录确认操作
订单及支付状态更新	转为成功
写入 Outbox	后续通知
提交事务	原子完成
5.3 未支付订单关闭事务
操作	变化
校验订单仍为待支付	防止关掉已支付订单
获取订单状态转换资格	防止多线程重复关单
预占转为 RELEASED	不可再次确认
库存账本变化	available + q，reserved - q
创建 RELEASE 流水	记录释放
释放用户限购、优惠券占用	同一事务
写入 Outbox	后续通知
提交事务	原子完成
建议统一加锁顺序，例如先订单，再预占，再库存，然后处理需要关联的限购和优惠券记录。所有竞争路径必须采用一致的锁顺序，必要时针对抢票建单路径制定全局顺序，以减少死锁。即使如此，仍需为 MySQL 死锁和锁等待超时设计有界重试。
六、Redis 预占与 MySQL 库存的关系
这一部分需要格外注意，因为很多高并发项目容易在这里产生逻辑漏洞。
6.1 两种预占不能混为一谈
类型	保存位置	业务含义
Redis 临时资格	Redis + MySQL 请求恢复记录	暂时占用高速准入预算
正式库存预占	ft_reservation	数据库已经确认库存占用
已售库存	ft_inventory.sold	支付已确认的票务容量
例如库存 100 张，当前数据库状态：
90
MySQL 可售


6
MySQL 预占


4
MySQL 已售



另有 Redis 临时资格 3 张，尚未落库

87
理论上剩余可发放的 Redis 资格

这里假设全部数字处于同一个一致性检查点。真实系统存在并发操作和恢复代次，不能直接使用一次普通查询就重置 Redis。
在一致性检查点上，可以采用：
\[
Redis可发放资格=MySQL可售-未落库的有效Redis预占
\]

但要注意：Redis 资格不能和 MySQL 预占重复扣减。消费者完成数据库预占后，需要按协议将 Redis 资格从“在途”转为“已落库”，保证预算总量正确。
6.2 generation 的作用
Redis 库存临时资格必须关联发放时的 generation。
例如：
- 第一次销售代次：generation = 1。
- Redis 重启造成数据不可信，进入 REBUILDING。
- 暂停新的准入，处理在途资格。
- MySQL 将 generation 升至 2。
- 重建 Redis 预算后恢复销售。
旧 generation = 1 的资格不得直接在 generation = 2 的账本上完成正式库存预占。
要做到这一点，正式落库路径还需在同一事务内校验 MySQL 库存行的当前代次。仅在 Java 内存里比较版本不够。
6.3 为什么不能只依赖 Redis TTL？
Redis 资格可能过期，但对应 Java 消费任务仍在执行。
如果资格到期时直接 INCR 恢复库存，同时旧消费任务继续在 MySQL 落库，就有可能多发资格。
因此：
- Redis TTL 只用于识别可能超时的临时资格。
- 释放前需要判断该请求是否已经落库。
- 不能确定结果时进入恢复流程。
- 只有确定不会有旧请求继续生效，才能安全归还资格。
- 所有归还动作必须按 requestId 幂等。

七、数据库约束和索引总表
表	主键	唯一约束	核心查询索引
ft_inventory	tier_id	主键即票档唯一约束	主键
ft_reservation	id	request_id、order_id	(status, expire_at, id)、(tier_id, status, id)
ft_inventory_ledger	id	operation_id、(tier_id, version_after)	(tier_id, created_at, id)、(reservation_id, id)
外键选择
建议采用：
关联	推荐方案	理由
Inventory → TicketTier	物理外键 RESTRICT	配置与库存一对一
Reservation → Inventory	逻辑关联或物理外键	根据压测再定
Reservation → Order	逻辑关联	避免建单时产生循环外键依赖
Ledger → Inventory	逻辑关联	高频流水插入减少关联维护
Ledger → Reservation	逻辑关联	允许初始化和调整流水无预占
不允许删除已经发生交易的票档，也不允许因为订单状态变化就物理删除预占及库存流水。
CHECK 约束建议
MySQL 8.4 可以强制执行 CHECK，但约束表达式只能验证允许引用的本行字段，不能跨表统计预占数量。

MySQL



建议检查：
- 库存三态守恒。
- 预占数量大于零。
- CONFIRMED 对应确认时间非空。
- RELEASED 对应释放时间非空。
- 流水数量大于零。
- 流水分量变化满足守恒。
- 流水前后版本连续。
跨表的“有效预占记录总量是否等于库存 reserved”需要使用业务事务与对账检查，不能简单通过 CHECK 约束实现。
八、需要提前解决的三个数据库设计细节
8.1 INT UNSIGNED 与 Java 类型映射
库存数量采用 MySQL INT UNSIGNED，其最大值超过 Java Integer.MAX_VALUE。
建议 Java 持久化模型使用 Long 接收这些字段，或者直接将 MySQL 库存字段调整为有符号 INT 并通过 CHECK >= 0 约束。
我最终更推荐后者：有符号 INT + CHECK。
这样能统一 Java Integer 映射，避免无符号整数的边界问题，同时普通演出票档也不需要超过约 21 亿的库存容量。
这是对本轮前面字段草案的小幅修订：正式 DDL 采用有符号 INT，所有库存字段通过 CHECK 保证非负。
8.2 预占与订单的双向关联
ft_reservation.order_id 和 ft_order 中可能存在的 reservation_id 不建议同时维护。
第一版推荐：
- ft_reservation.order_id：关联正式订单。
- ft_order.request_id：关联抢票请求。
- ft_reservation.request_id：关联同一个抢票请求。
通过 request_id 可以稳定追踪整个交易链路。
两个表不需要互相保存对方的外键，避免重复关联和更新时的一致性问题。
8.3 Redis 临时资格的恢复记录
仅靠本轮三张库存表，还不能完整实现 Redis 预占后的崩溃恢复。
第四批 ft_purchase_request 必须补充：
- Redis 资格处理阶段。
- generation。
- 预占操作标识。
- 处理者租约与处理超时。
- 最后一次处理错误。
- 恢复与补偿状态。
还需持久化足够的信息来识别 Redis Lua 已成功但数据库尚未记录成功的窗口；对于操作结果未知的请求，必须查询 Redis 令牌和数据库事实并执行受控恢复，而不是直接重新扣减。

九、开发阶段与验收标准
库存域验收清单
0/16 项已验证



库存初始化与账本0/4[ ] 同一票档只能初始化一次库存
[ ] 初始库存与票档配置数量一致
[ ] 账本始终满足 total = available + reserved + sold
[ ] 每次账目变化均有唯一流水



库存预占与确认0/4

Redis 与故障恢复0/4

数据一致性0/4
这些是后续集成测试及并发测试的验收条件，不代表当前已经完成实现。
十、第三批数据库设计定稿
库存与票档预占域 V1.0


三张核心表：
ft_inventory、ft_reservation、ft_inventory_ledger
库存模型：
total = available + reserved + sold
正式预占状态：
RESERVED → CONFIRMED / RELEASED
高并发控制：
MySQL 条件更新、行锁、唯一约束、幂等操作和本地事务。
Redis 协调：
高速临时资格与 MySQL 正式预占分离；使用准入代次、在途恢复和安全重建。
库存流水：
每次账目变化追加不可变流水，记录四项数量变化及前后版本。
金额与订单：
库存域不负责订单价格、支付金额或退款审批；正式数据库库存确认由订单和支付编排服务调用。



FlashTicket 数据库详细设计 04：抢票请求与异步排队域
Purchase Request & Async Queue · MySQL 8.4 LTS · 详细表结构 V1.0
这是 FlashTicket 第四批数据库设计，主要解决抢票请求可靠受理、请求幂等、RocketMQ 重复消费、处理租约、Redis 临时资格恢复、用户限购等问题。
本轮确定两张 P0 核心表，以及一张 P1 扩展表。
表名	中文名称	主要职责	优先级
ft_purchase_request	抢票请求表	记录受理、处理、结果和异常恢复	P0
ft_purchase_limit	用户票档限购表	原子维护未支付预占和已支付限购数量	P0
ft_purchase_attempt	抢票处理尝试表	记录消息消费、重试、故障与耗时	P1
同时复用已经规划的 ft_reservation、ft_order、ft_outbox_event、ft_message_inbox。
本轮只进行字段及数据库关系设计，不编写 DDL 或 Java 代码。
一、领域关系与事务边界
#chatgpt-mermaid-_r_1io_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-_r_1io_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-_r_1io_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-_r_1io_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-_r_1io_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-_r_1io_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-_r_1io_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-_r_1io_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-_r_1io_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-_r_1io_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-_r_1io_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-_r_1io_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-_r_1io_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-_r_1io_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";font-size:16px;}#chatgpt-mermaid-_r_1io_ p{margin:0;}#chatgpt-mermaid-_r_1io_ .entityBox{fill:rgb(19, 33, 54);stroke:rgb(31, 78, 148);}#chatgpt-mermaid-_r_1io_ .relationshipLabelBox{fill:rgb(48, 48, 48);opacity:0.7;background-color:rgb(48, 48, 48);}#chatgpt-mermaid-_r_1io_ .relationshipLabelBox rect{opacity:0.5;}#chatgpt-mermaid-_r_1io_ .labelBkg{background-color:rgba(48, 48, 48, 0.5);}#chatgpt-mermaid-_r_1io_ .edgeLabel{background-color:rgb(14, 14, 14);}#chatgpt-mermaid-_r_1io_ .edgeLabel .label rect{fill:rgb(14, 14, 14);}#chatgpt-mermaid-_r_1io_ .edgeLabel .label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-_r_1io_ .edgeLabel .label{fill:rgb(31, 78, 148);font-size:14px;}#chatgpt-mermaid-_r_1io_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";color:rgb(237, 237, 237);}#chatgpt-mermaid-_r_1io_ .edge-pattern-dashed{stroke-dasharray:8,8;}#chatgpt-mermaid-_r_1io_ .node rect,#chatgpt-mermaid-_r_1io_ .node circle,#chatgpt-mermaid-_r_1io_ .node ellipse,#chatgpt-mermaid-_r_1io_ .node polygon{fill:rgb(19, 33, 54);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-_r_1io_ .relationshipLine{stroke:rgb(175, 175, 175);stroke-width:1px;fill:none;}#chatgpt-mermaid-_r_1io_ .marker{fill:none!important;stroke:rgb(175, 175, 175)!important;stroke-width:1;}#chatgpt-mermaid-_r_1io_ [data-look=neo].labelBkg{background-color:rgba(48, 48, 48, 0.5);}#chatgpt-mermaid-_r_1io_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-_r_1io_ [data-look="neo"].node rect,#chatgpt-mermaid-_r_1io_ [data-look="neo"].cluster rect,#chatgpt-mermaid-_r_1io_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-_r_1io_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_1io_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-_r_1io_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-_r_1io_-gradient);stroke-width:1px;}#chatgpt-mermaid-_r_1io_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_1io_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-_r_1io_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-_r_1io_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_1io_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-_r_1io_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-_r_1io_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_1io_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-_r_1io_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_1io_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";}ft_userft_purchase_requestft_ticket_tierft_purchase_limitft_purchase_attemptft_reservationft_orderft_outbox_eventsubmitstargetsownslimitsattemptsreservescreatesproduces





逻辑 ER 关系。Outbox 采用业务聚合标识关联请求，不要求直接建立到抢票请求表的物理外键。
其中有三条必须坚持的原则：
1. 抢票请求是业务事实，不是 RocketMQ 消息本身。 MQ 可以重复投递，用户的同一次抢票操作仍然只是一条请求。
2. 正式库存预占与订单创建使用同一个 MySQL 本地事务。 抢票请求的成功状态也在同一事务中提交。
3. Redis 临时资格只是高速准入机制。 即使 Redis 临时资格已经发放，数据库中也不能提前把请求标记为抢票成功。

二、ft_purchase_request：抢票请求表
这是整个抢票异步链路的核心表。
2.1 字段设计
基础标识与业务参数
字段	MySQL 类型	约束 / 默认值	说明
id	BIGINT	PK，NOT NULL	雪花主键
request_id	VARCHAR(64)	NOT NULL，UNIQUE	全局幂等请求号
user_id	BIGINT	NOT NULL	发起请求的用户
event_id	BIGINT	NOT NULL	活动 ID，便于追踪与统计
session_id	BIGINT	NOT NULL	场次 ID
tier_id	BIGINT	NOT NULL	目标票档 ID
quantity	INT	NOT NULL	购买数量
selected_coupon_id	BIGINT	NULL	用户选择的优惠券
status	VARCHAR(24)	NOT NULL，默认 QUEUED	抢票状态
failure_code	VARCHAR(64)	NULL	最终失败原因码
created_at	DATETIME(3)	NOT NULL	请求受理时间
updated_at	DATETIME(3)	NOT NULL	最近更新时间
completed_at	DATETIME(3)	NULL	进入终态的时间
第一版一条请求只能购买一个票档，不能同时包含多个场次或多个票档。
event_id、session_id 属于查询冗余字段，其值必须在首次受理时由服务端根据 tier_id 确认，不能无条件相信客户端提交的关联 ID。
异步处理、租约及恢复字段
字段	MySQL 类型	约束 / 默认值	说明
attempt_count	INT	NOT NULL，默认 0	已开始的处理尝试次数
lease_owner	VARCHAR(64)	NULL	当前处理者的唯一令牌
lease_until	DATETIME(3)	NULL	当前处理租约到期时间
next_retry_at	DATETIME(3)	NULL	下次允许恢复时间
last_error_code	VARCHAR(64)	NULL	最近一次技术故障码
last_error_at	DATETIME(3)	NULL	最近异常发生时间
processing_started_at	DATETIME(3)	NULL	最近一次开始处理的时间
accepted_deadline_at	DATETIME(3)	NOT NULL	本次请求允许开始处理的最晚时间
version	BIGINT	NOT NULL，默认 0	状态并发版本
这里需要区分两个不同时间：
- accepted_deadline_at：排队请求必须开始处理的截止时间，由受理规则确定。
- 订单 expire_at：创建订单之后的 15 分钟支付期限。
两者不能混用。 用户排队 10 秒，不应因此损失订单的 15 分钟支付时间。
Redis 高速预占协作字段
字段	MySQL 类型	约束 / 默认值	说明
admission_mode	VARCHAR(16)	NOT NULL，默认 MYSQL	准入模式
admission_state	VARCHAR(24)	NOT NULL，默认 NOT_REQUIRED	Redis 准入处理状态
admission_generation	BIGINT	NULL	Redis 准入代次
admission_operation_id	VARCHAR(64)	NULL，UNIQUE	Redis 临时资格操作 ID
admission_updated_at	DATETIME(3)	NULL	最近准入状态更新时间
admission_error_code	VARCHAR(64)	NULL	准入异常码
采用 admission_mode，是为了让 MySQL 基线版和 Redis 优化版能够共存并进行压测对比，而不是让所有业务流程强制依赖 Redis 预占。
模式	说明
MYSQL	直接使用数据库条件库存扣减
REDIS	先获得 Redis 临时资格，再执行数据库正式预占
admission_operation_id 是临时资格的稳定业务标识，重试时不能重新生成另一个 ID 造成重复预占。
2.2 抢票请求主状态
建议使用六种状态。
状态	含义	是否终态
QUEUED	已受理，等待处理	否
PROCESSING	已领取处理资格	否
RECOVERING	存在待恢复异常或处理结果不确定	否
SUCCEEDED	订单及正式库存预占已提交	是
FAILED	业务确定失败	是
CANCELLED	在允许阶段撤销请求	是
QUEUED
可靠受理成功


PROCESSING
消费者取得处理资格


SUCCEEDED
订单创建成功


FAILED
明确业务失败



RECOVERING
暂时不确定，恢复后返回处理或终结

CANCELLED 是尚未取得执行权时允许的另一终态。对 RECOVERING 的处理必须依照请求、Redis 准入和数据库事务的实际结果判断。
2.3 主状态与 Redis 准入状态必须分开
例如抢票请求可能是 PROCESSING，同时 Redis 准入为 HELD。
推荐如下状态：
admission_state	含义
NOT_REQUIRED	MySQL 基线模式，不使用 Redis 资格
PENDING	计划获取 Redis 临时资格
UNKNOWN	Redis Lua 调用结果暂时无法确定
HELD	已确认临时资格存在
COMMITTED	临时资格对应的正式 MySQL 预占已提交
RELEASE_PENDING	需要安全归还临时资格
RELEASED	临时资格已经归还
REJECTED	Redis 明确拒绝发放资格
特别需要注意一个事实：
Redis Lua 成功后，Java 进程可能在写入 admission_state = HELD 之前崩溃。
因此，admission_state 是恢复控制状态，不是 Redis 事实的绝对证明。必须结合 admission_operation_id、Redis 令牌、准入代次和 MySQL 正式预占记录判断。
这正是 UNKNOWN 状态的价值。
2.4 请求终态和订单状态的关系
抢票状态	订单可能状态	是否允许
QUEUED	不存在	是
PROCESSING	不存在	是
SUCCEEDED	PENDING_PAYMENT	是
SUCCEEDED	PAID	是
SUCCEEDED	CLOSED	是
FAILED	存在有效订单	不允许
CANCELLED	存在有效订单	不允许
订单后续取消或者退款，都不能将原抢票请求从 SUCCEEDED 改回 FAILED。
2.5 索引与唯一约束
名称	字段	用途
PRIMARY KEY	id	主键
uk_purchase_request_id	request_id	全局请求幂等
uk_purchase_admission_op	admission_operation_id	Redis 准入操作唯一
idx_purchase_user_time	(user_id, created_at, id)	我的抢票记录
idx_purchase_tier_status	(tier_id, status, created_at)	票档请求查询
idx_purchase_recovery	(status, next_retry_at, id)	可恢复请求扫描
idx_purchase_lease	(status, lease_until, id)	超时租约扫描
idx_purchase_deadline	(status, accepted_deadline_at, id)	过期排队请求扫描
request_id 和 admission_operation_id 不建议使用会产生意外大小写等价的默认字符排序规则；可以对它们选用明确的二进制比较语义。
高频请求表会持续写入，以上是完整的候选索引集合。正式落地时应按实际查询需求收敛索引，避免无谓增加插入和状态更新成本。
三、ft_purchase_limit：用户票档限购表
这张表用于解决一个典型的并发问题：
用户最多允许购买 2 张票，但同时发起两个各购买 2 张的请求，如何保证不会最终占用 4 张？

不能使用“查询已购买数量 → 判断是否超限 → 创建订单”的普通流程，因为两个并发事务可能同时检查通过。
建议为每一个 user_id + tier_id 维护独立限购账本。
3.1 字段设计
字段	MySQL 类型	约束 / 默认值	说明
user_id	BIGINT	联合 PK，NOT NULL	用户 ID
tier_id	BIGINT	联合 PK，NOT NULL	票档 ID
reserved_quantity	INT	NOT NULL，默认 0	待支付订单占用票数
purchased_quantity	INT	NOT NULL，默认 0	累计已支付且计入限购的票数
version	BIGINT	NOT NULL，默认 0	并发版本
created_at	DATETIME(3)	NOT NULL	创建时间
updated_at	DATETIME(3)	NOT NULL	更新时间
采用联合主键：
PRIMARY KEY(user_id, tier_id)
不增加独立雪花 ID。
3.2 限购数量怎么算？
\[
当前限购占用=reservedQuantity+purchasedQuantity
\]

假设每个用户最多购买 2 张：
用户限购规则模拟
上限 2 张

已有未支付占用1 张




累计已支付票数0 张




本次申请购买1 张




本次购买后的总占用
2 张

是否满足限购
允许


只展示限购数量计算，真实系统还必须在 MySQL 事务中原子占用额度。
3.3 限购账目如何变化？
假设用户限购上限为 2 张，每次操作涉及 1 张门票。
业务操作	reserved_quantity	purchased_quantity
初始状态	0	0
抢票成功、订单待支付	+1	不变
支付成功	-1	+1
未支付订单关闭	-1	不变
已支付订单退款	不变	不变
退款后重新上架（未来扩展）	按独立限购规则决定	按规则调整
V1 采用：退款后不自动恢复用户限购额度。
需要特别明确，purchased_quantity 在这个模型中不是财务净购买数量，而是累计计入限购的已支付票量。
因为本项目已经决定退款后门票默认不重新上架，所以保留限购占用与库存政策一致。
3.4 数据库约束
建议：
- reserved_quantity >= 0。
- purchased_quantity >= 0。
- version >= 0。
- (user_id, tier_id) 联合唯一。
- 用户的限购上限来自对应票档的有效规则快照，不应由前端传入。
跨表的限购上限无法通过当前表的普通 CHECK 直接验证，必须通过 MySQL 本地事务和业务校验实施。
3.5 并发处理策略
推荐流程：
1. 在事务内确保该用户票档的限购账本行存在，依赖联合主键处理并发首次创建。
2. 使用锁定读取或原子条件更新取得限购占用资格。
3. 验证本次数量不会超过有效限购上限。
4. 增加 reserved_quantity。
5. 同事务执行库存预占、订单创建、请求状态更新及流水记录。
6. 事务提交后才认为限购占用正式生效。
MySQL InnoDB 对唯一键查找和锁定读取提供对应的记录锁行为，适合维护这种用户与票档维度的并发账本。

MySQL



3.6 索引设计
索引	字段	用途
PRIMARY KEY	(user_id, tier_id)	用户某票档的限购检查
idx_purchase_limit_tier	(tier_id, user_id)	票档维度限购对账
P0 可以先只建联合主键。如果后续确实需要按票档扫描全部用户限购占用，再增加反向索引。
3.7 一个容易忽略的业务问题
ft_purchase_limit 当前只按票档统计，而不同票档可能属于同一个场次。
因此第一版支持的是：
每用户、每票档限购。
如果未来出现“同一场次所有票档合计最多买 2 张”的规则，仅靠此表无法原子保证跨票档总限购，需要增加场次级限购账本或统一的场次级锁定策略。
本轮不增加该复杂度。

四、ft_purchase_attempt：抢票处理尝试表（P1）
P0 阶段已经通过 ft_purchase_request.attempt_count 和故障字段具备基础恢复能力。
P1 增加这张表，主要用于面试中的故障分析、MQ 重复消费与处理耗时排查。
4.1 字段设计
字段	MySQL 类型	约束 / 默认值	说明
id	BIGINT	PK	雪花主键
request_id	VARCHAR(64)	NOT NULL	关联抢票请求
attempt_no	INT	NOT NULL	第几次处理
message_event_id	VARCHAR(64)	NULL	对应 MQ 事件标识
worker_token	VARCHAR(64)	NOT NULL	本次处理资格令牌
result	VARCHAR(24)	NOT NULL	处理结果
error_code	VARCHAR(64)	NULL	错误码
error_message	VARCHAR(500)	NULL	脱敏后的错误摘要
started_at	DATETIME(3)	NOT NULL	开始时间
finished_at	DATETIME(3)	NULL	结束时间
duration_ms	BIGINT	NULL	处理耗时
created_at	DATETIME(3)	NOT NULL	记录创建时间
4.2 尝试结果
result	含义
RUNNING	当前尝试处理中
SUCCEEDED	当前尝试完成业务提交
BUSINESS_FAILED	明确业务失败
TECHNICAL_FAILED	技术错误
LOST_LEASE	处理资格失效
UNKNOWN	结果待恢复
4.3 索引
索引	字段
PK	id
UNIQUE	(request_id, attempt_no)
INDEX	(request_id, started_at)
INDEX	(result, started_at)
注意：这张表记录的是处理尝试历史，不能作为最终订单成功与否的权威来源。
例如一个旧处理者返回 LOST_LEASE，但后续新的处理者成功创建订单，抢票请求最终仍然是 SUCCEEDED。
五、处理租约和 RocketMQ 消费幂等
这里需要统一三种经常被混淆的概念：
机制	解决的问题	存储位置
request_id	同一次用户抢票不重复创建业务	MySQL 抢票请求表
处理租约	多个消费者争抢同一请求的执行权	MySQL 抢票请求表
Inbox	同一事件重复投递的消费去重	ft_message_inbox
RocketMQ 的消息投递本身不能代替业务执行权控制。
推荐的消费者执行流程
RocketMQ 收到抢票消息


根据 requestId 查询权威状态


通过条件更新领取处理租约


取得资格
校验业务、执行预占与建单事务


没有资格
查询终态，或等待当前处理者完成




状态提交 + 消费记录 + Outbox
需要提交的业务数据保持本地事务一致


租约具体规则
- 每次成功领取都生成新的不可预测 lease_owner。
- attempt_count 在领取成功时递增。
- 领取时写入 lease_until。
- 提交业务事务时必须确认当前仍持有有效的处理资格。
- 旧处理者即使恢复运行，也不能凭过期令牌继续提交。
- 租约过期不是“原任务一定失败”，只是允许受控恢复检查。
- 对运行中的长事务不能依赖固定短租约进行武断抢占；应设置合理的事务超时和恢复屏障。
特别强调：校验租约不能只在事务开始前做一次。 最终提交路径必须通过数据库锁定行或状态条件更新保护，防止旧消费者在租约失效后仍完成写入。
MySQL 支持 FOR UPDATE NOWAIT 和 SKIP LOCKED，但它们适合不同的并发场景。SKIP LOCKED 适合并发扫描待处理队列，不适合用来证明一般交易查询具备一致性。

MySQL



Inbox 消费确认的边界
需要区分两种情形：
- 业务进入终态： 在对应 MySQL 事务中记录 Inbox 完成标记，然后确认 MQ 消费。
- 其他处理者仍持有有效租约： 不能简单写入 Inbox 完成记录并直接视为业务成功。应让恢复机制确保请求继续推进，或使用适当的延期重试策略。
如果业务事务已经成功提交，但消费者确认 MQ 消息前崩溃，下一次消费通过请求终态和唯一索引安全返回即可。

六、请求幂等的完整约束
UNIQUE(request_id) 仅解决相同请求号重复插入问题，但还需要校验请求号与业务内容是否一致。
建议首次受理后冻结以下字段：
- user_id
- tier_id
- quantity
- selected_coupon_id
- 关联场次与活动
- 抢票操作类型
相同 requestId 再次提交时：
重试情况	处理
同一用户、完全相同业务参数	返回原请求的当前状态
不同用户使用同一请求号	拒绝，不能泄露原用户请求信息
同一用户但改变购买数量	幂等参数冲突
同一用户但更换票档	幂等参数冲突
同一请求已经创建订单	返回原成功结果
同一请求已经失败	返回原失败结果，不重新发起另一笔业务
可以在 P1 增加 request_fingerprint 字段，存储规范化业务参数摘要，用于高效比较。但最终不能只看哈希是否相同，还应确保用户身份和核心业务参数校验正确。
另一个重要区别是：
请求幂等不等于用户限购。
一个用户可以生成两个不同 requestId，但限购账本仍必须阻止其超过上限。
七、Redis 临时预占的恢复设计
这是第四批数据库设计与第三批库存域衔接的关键。
我们需要处理这样一个故障窗口：
MySQL 已持久化请求


Redis Lua 预占成功


应用进程崩溃
MySQL 还没来得及记录 HELD


推荐恢复方案
1. 初次创建请求时预先持久化 admission_operation_id。
2. Redis Lua 使用这个稳定的操作 ID 发放资格。
3. 执行结果未知时，将请求作为恢复候选，而不是生成新操作 ID。
4. 恢复程序检查数据库正式预占、请求终态、Redis 资格记录及准入代次。
5. 如果 Redis 资格仍有效且请求满足条件，允许受控继续处理。
6. 如果请求确认失败，执行幂等资格释放。
7. 如果 Redis 状态丢失或旧代次无法安全确定结果，则进入票档级恢复屏障，不直接将额度归还。
跨模块关联字段
字段	所在表	用途
request_id	ft_purchase_request	抢票业务唯一标识
admission_operation_id	ft_purchase_request	Redis 临时资格唯一操作
admission_generation	ft_purchase_request	临时资格准入代次
generation	ft_inventory	当前有效库存准入代次
request_id	ft_reservation	正式预占的请求关联
generation	ft_reservation	正式预占来源代次
request_id	ft_order	订单来源与幂等约束
恢复安全性依赖所有正式库存确认路径都执行代次校验。
如果旧请求绕过代次校验，Redis 重建就有可能再次分发已被旧请求占用的资格。
恢复流程的设计边界
Redis 优化版需要支持临时资格的安全协调，但并不意味着成功的订单必须永久依赖 Redis 才能完成支付。
一旦 MySQL 事务已经创建订单和正式预占，后续支付确认、关单、库存释放应以 MySQL 交易状态为依据。Redis 准入记录主要负责对账、释放资格与预算同步。

八、状态、约束与索引汇总
核心数据库约束
业务要求	数据库实现依据
请求号全局唯一	ft_purchase_request.request_id 唯一
一个请求最多一个订单	ft_order.request_id 唯一
一个请求最多一个正式预占	ft_reservation.request_id 唯一
Redis 操作唯一	admission_operation_id 唯一
用户票档限购账本唯一	(user_id, tier_id) 联合主键
处理尝试不重复编号	(request_id, attempt_no) 唯一（P1）
请求处理版本防覆盖	version + 处理令牌条件更新
限购数量不可为负	CHECK 约束
购买数量合法	CHECK quantity > 0 与业务上限校验
外键策略
关联	推荐方式	原因
PurchaseRequest → User	逻辑外键	历史请求需要长期保留
PurchaseRequest → TicketTier	逻辑外键	高频写入与交易历史独立保存
PurchaseLimit → User	逻辑关联	业务事务维护
PurchaseLimit → TicketTier	逻辑关联	业务事务维护
PurchaseAttempt → PurchaseRequest	逻辑关联	高频重试记录
PurchaseRequest → Order	通过 request_id	避免请求表与订单表双向保存冗余外键
虽然这些关联不全部建立物理外键，但不意味着允许产生孤儿数据。系统需要通过事务、唯一约束和对账测试保证关联正确。
九、开发与验收顺序
阶段	主要内容	核心验收
P0-A	抢票请求表、请求状态、幂等	同请求重复提交不重复创建
P0-B	用户票档限购账本	并发下无法绕过限购
P0-C	RocketMQ 消费、租约、恢复	多次投递与崩溃恢复正确
P0-D	库存预占与订单同事务	不超卖、不重复订单
P1-A	Redis 准入恢复字段及流程	Redis 预占故障窗口可恢复
P1-B	抢票处理尝试日志	可追踪重试、处理耗时与故障
注意，P0、P1 的实现顺序是按开发阶段划分，而不是代表 P0 可以缺少恢复安全保障。一旦正式启用 Redis 准入模式，就必须同时完成对应的故障恢复机制。
十、第四批数据库设计定稿
抢票请求与异步排队域 V1.0


P0 两张表： ft_purchase_request、ft_purchase_limit。
P1 一张表： ft_purchase_attempt。
抢票请求状态： QUEUED / PROCESSING / RECOVERING / SUCCEEDED / FAILED / CANCELLED。
Redis 临时准入： 独立的准入模式、操作 ID、代次和恢复状态。
用户限购： (user_id, tier_id) 联合主键，待支付占用与累计已支付数量分开维护。
异步可靠性： MySQL Outbox + RocketMQ PushConsumer + Inbox + 数据库处理租约。
事务规则： 抢票请求成功状态、正式库存预占、限购占用及订单创建同事务提交。
业务历史： 抢票成功后，即使订单后来取消或退款，也不回退原抢票成功状态。

至此，前四批详细数据库设计已经覆盖：
1. 用户与权限域：6 张表。
2. 演出活动与场次域：7 张表。
3. 库存与预占域：3 张表。
4. 抢票请求与排队域：2 张核心表 + 1 张扩展表。


FlashTicket 数据库详细设计 05：订单与交易状态机域
Order & Transaction Lifecycle · MySQL 8.4 LTS · 详细表结构 V1.0
本轮继续第五批数据库设计，围绕订单创建、交易快照、状态流转、15 分钟支付期限、超时关单、并发幂等及操作审计展开。
延续前四批的设计，本轮确定 3 张核心表：
表名	中文名称	核心职责
ft_order	订单主表	订单身份、金额、状态、支付期限
ft_order_item	订单明细表	票档、数量、价格与演出快照
ft_order_operation	订单操作流水表	状态变化、操作主体、审计历史
本轮只设计字段、数据类型、索引、约束和事务规则，不编写 DDL 或 Java 代码。
一、核心 ER 关系
#chatgpt-mermaid-_r_1p1_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-_r_1p1_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-_r_1p1_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-_r_1p1_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-_r_1p1_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-_r_1p1_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-_r_1p1_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-_r_1p1_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-_r_1p1_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-_r_1p1_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-_r_1p1_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-_r_1p1_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-_r_1p1_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-_r_1p1_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";font-size:16px;}#chatgpt-mermaid-_r_1p1_ p{margin:0;}#chatgpt-mermaid-_r_1p1_ .entityBox{fill:rgb(19, 33, 54);stroke:rgb(31, 78, 148);}#chatgpt-mermaid-_r_1p1_ .relationshipLabelBox{fill:rgb(48, 48, 48);opacity:0.7;background-color:rgb(48, 48, 48);}#chatgpt-mermaid-_r_1p1_ .relationshipLabelBox rect{opacity:0.5;}#chatgpt-mermaid-_r_1p1_ .labelBkg{background-color:rgba(48, 48, 48, 0.5);}#chatgpt-mermaid-_r_1p1_ .edgeLabel{background-color:rgb(14, 14, 14);}#chatgpt-mermaid-_r_1p1_ .edgeLabel .label rect{fill:rgb(14, 14, 14);}#chatgpt-mermaid-_r_1p1_ .edgeLabel .label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-_r_1p1_ .edgeLabel .label{fill:rgb(31, 78, 148);font-size:14px;}#chatgpt-mermaid-_r_1p1_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";color:rgb(237, 237, 237);}#chatgpt-mermaid-_r_1p1_ .edge-pattern-dashed{stroke-dasharray:8,8;}#chatgpt-mermaid-_r_1p1_ .node rect,#chatgpt-mermaid-_r_1p1_ .node circle,#chatgpt-mermaid-_r_1p1_ .node ellipse,#chatgpt-mermaid-_r_1p1_ .node polygon{fill:rgb(19, 33, 54);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-_r_1p1_ .relationshipLine{stroke:rgb(175, 175, 175);stroke-width:1px;fill:none;}#chatgpt-mermaid-_r_1p1_ .marker{fill:none!important;stroke:rgb(175, 175, 175)!important;stroke-width:1;}#chatgpt-mermaid-_r_1p1_ [data-look=neo].labelBkg{background-color:rgba(48, 48, 48, 0.5);}#chatgpt-mermaid-_r_1p1_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-_r_1p1_ [data-look="neo"].node rect,#chatgpt-mermaid-_r_1p1_ [data-look="neo"].cluster rect,#chatgpt-mermaid-_r_1p1_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-_r_1p1_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_1p1_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-_r_1p1_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-_r_1p1_-gradient);stroke-width:1px;}#chatgpt-mermaid-_r_1p1_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_1p1_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-_r_1p1_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-_r_1p1_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_1p1_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-_r_1p1_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-_r_1p1_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_1p1_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-_r_1p1_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_1p1_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";}ft_userft_orderft_purchase_requestft_order_itemft_ticket_tierft_order_operationft_reservationft_paymentft_refundft_coupon_usageownsgeneratescontainsreferencesrecordsreservespaysrefund_historyuses





逻辑关系图包含后续支付与优惠券域的关联。V1 中每笔订单只有一个票档明细和一个支付业务单；暂未创建支付单时允许为空。
本批统一的业务规则
- 一个 requestId 最多创建一个订单。
- 一笔订单只能归属于一个用户。
- 一笔订单只包含一个场次、一个票档，数量 1–2 张。
- 订单创建成功时，必须已经完成 MySQL 正式库存预占。
- 订单创建后，价格、票档与场次快照不能被运营修改覆盖。
- 未支付订单有效期为 15 分钟。
- 已支付订单不能通过普通取消或超时关单释放库存。
- 订单状态转换、库存及优惠券相关变化必须在合法的 MySQL 本地事务内完成。

二、ft_order：订单主表
订单主表只负责交易身份、金额、生命周期与并发控制，不直接保存完整演出详情。
2.1 订单标识与关联字段
字段	MySQL 类型	约束 / 默认值	说明
id	BIGINT	PK，NOT NULL	MyBatis-Plus 雪花主键
order_no	VARCHAR(40)	NOT NULL，UNIQUE	用户可见的订单编号
request_id	VARCHAR(64)	NOT NULL，UNIQUE	抢票请求幂等编号
user_id	BIGINT	NOT NULL	下单用户
event_id	BIGINT	NOT NULL	关联演出活动
session_id	BIGINT	NOT NULL	关联场次
tier_id	BIGINT	NOT NULL	关联票档
quantity	INT	NOT NULL	购买数量
user_coupon_id	BIGINT	NULL	本订单使用的用户优惠券
这里特意保留了 event_id、session_id、tier_id。
虽然它们可以经 ft_order_item 查询，但订单列表、演出统计和票档对账会频繁使用，属于有明确用途的冗余字段。
这些字段必须在订单创建时由服务端依据已验证的抢票请求生成，不能信任客户端自行提供的活动关联。
2.2 金额字段
全部使用 BIGINT，单位为人民币分。
字段	类型	默认值 / 约束	说明
original_amount_cent	BIGINT	NOT NULL	优惠前商品金额
discount_amount_cent	BIGINT	NOT NULL，默认 0	订单优惠金额
payable_amount_cent	BIGINT	NOT NULL	实际应付金额
paid_amount_cent	BIGINT	NOT NULL，默认 0	已确认支付金额
currency	CHAR(3)	NOT NULL，默认 CNY	币种
V1 必须满足：
\[
payableAmount=originalAmount-discountAmount
\]

并且：
\[
0\leq discountAmount\leq originalAmount
\]

第一版暂不支持零元订单，因此：
\[
payableAmount>0
\]

如果将来需要免费票或全额抵扣，单独设计零元订单确认流程，不要伪造成功支付流水。
金额计算由服务端完成，订单创建后原价、优惠金额和应付金额不可修改。
2.3 订单状态与时间字段
字段	类型	约束 / 默认值	说明
status	VARCHAR(24)	NOT NULL，默认 PENDING_PAYMENT	订单主状态
expire_at	DATETIME(3)	NOT NULL	支付截止时间
paid_at	DATETIME(3)	NULL	支付最终确认时间
closed_at	DATETIME(3)	NULL	关单时间
close_reason	VARCHAR(32)	NULL	订单关闭原因
created_at	DATETIME(3)	NOT NULL	数据库创建时间
updated_at	DATETIME(3)	NOT NULL	最近更新时间
version	BIGINT	NOT NULL，默认 0	乐观并发版本
2.4 V1 订单状态
状态	含义	是否终态
PENDING_PAYMENT	待支付	否
PAID	已支付	对支付/关单竞争是终态
CLOSED	已取消或超时关闭	是
退款单独使用 ft_refund 管理。
例如已支付订单申请退款时：
- 订单仍然为 PAID。
- 退款单状态为 PENDING_REVIEW。
- 退款完成后，用户端可以派生展示为“已退款”。
- 不直接把订单主状态改成 CLOSED。
2.5 关闭原因
close_reason	业务含义
USER_CANCEL	用户主动取消
PAYMENT_TIMEOUT	超过支付期限
EVENT_CANCELLED	演出取消
SYSTEM_CANCEL	系统依据业务规则关闭
对于已经支付的订单，活动取消也不能直接使用上述普通关单流程，而是必须走退款和票务权益处理。
三、ft_order 的数据库约束与索引
3.1 唯一约束
约束名	字段	目的
PRIMARY KEY	id	唯一订单主键
uk_order_no	order_no	业务订单号不能重复
uk_order_request	request_id	同一抢票请求只能生成一个订单
其中 request_id 的比较规则需要与 ft_purchase_request.request_id 完全统一。建议采用大小写敏感的二进制比较语义。
3.2 普通索引
索引名	字段	用途
idx_order_user_created	(user_id, created_at, id)	我的订单分页
idx_order_status_expire	(status, expire_at, id)	超时关单扫描
idx_order_event_created	(event_id, created_at, id)	活动订单统计
idx_order_session_created	(session_id, created_at, id)	场次订单查询
idx_order_tier_created	(tier_id, created_at, id)	票档订单查询
这是一组候选索引。第一版建议优先保留前三个，其他统计索引根据实际 SQL 与 EXPLAIN ANALYZE 再决定。
频繁更新 status 的订单表会维护相关二级索引，不应该为了所有管理端筛选条件建立大量索引。
3.3 CHECK 约束
建议至少覆盖：
约束	规则
订单数量	quantity BETWEEN 1 AND 2
金额非负	original_amount_cent >= 0、discount_amount_cent >= 0
应付金额	payable_amount_cent = original_amount_cent - discount_amount_cent
支付金额	paid_amount_cent >= 0 AND paid_amount_cent <= payable_amount_cent
已支付状态	status = PAID 时 paid_at 非空
已关闭状态	status = CLOSED 时 closed_at、close_reason 非空
支付期限	expire_at > created_at
版本	version >= 0
对于 V1 不支持零元订单，正式确认支付时 paid_amount_cent 应等于 payable_amount_cent。
MySQL 8.4 支持表级 CHECK 约束，但这些约束只能验证本行数据的条件，无法直接验证对应支付单或库存预占是否存在。跨表约束仍需通过事务、唯一索引和对账实现。

MySQL




四、订单状态机与并发约束
订单不允许自由修改 status，必须经过明确的业务操作。
PENDING_PAYMENT
订单创建成功，库存已预占


PAID
支付确认成功


CLOSED
订单取消或超时



两个方向互斥，不允许 PAID → CLOSED、CLOSED → PAID，也不允许订单重新回到待支付。
状态转换要求
原状态	业务动作	新状态	必须一起完成的业务
无订单	抢票建单成功	PENDING_PAYMENT	库存预占、限购占用、请求成功
PENDING_PAYMENT	模拟支付确认	PAID	支付单成功、库存确认、券核销
PENDING_PAYMENT	用户取消	CLOSED	库存释放、限购释放、券解锁
PENDING_PAYMENT	超时关单	CLOSED	库存释放、限购释放、券解锁
PAID	重复支付通知	PAID	不重复变化
CLOSED	重复关单	CLOSED	不重复变化
并发互斥原则
支付成功和关单必须争夺同一订单的合法状态转换权。
推荐使用订单行作为串行化入口，结合条件状态更新与固定加锁顺序。
MySQL InnoDB 的锁定读 SELECT ... FOR UPDATE 可以对匹配的记录加锁，阻止其他事务在当前事务提交前修改相同记录。

MySQL



不要仅靠：
“先 SELECT 查询订单状态，再根据查询结果 UPDATE。”
因为在并发条件下，两个事务可能在不同时间读取到相同的旧状态。尤其在默认 RR 下，普通一致性读与锁定读对数据库可见版本的处理不同。

MySQL



关于支付截止时间
以数据库权威时间判断：
\[
createdAt < expireAt
\]

对于 15 分钟支付期限：
\[
expireAt = createdAt + 15\text{分钟}
\]

同时规定：
- 支付最终确认必须在订单有效期内取得合法确认资格。
- 已经到期的订单即使关单任务尚未执行，也不能继续确认新的模拟支付成功。
- 到期关单只允许关闭仍为 PENDING_PAYMENT 的订单。
- 已支付订单即使延迟关单消息到达，也不能关闭。
- 订单是否过期由服务端或数据库判断，不由浏览器倒计时决定。
订单创建和到期时间应采用一致的时间基准。系统虽统一以 UTC 保存 DATETIME(3)，但应进一步确保 JDBC 会话时区和数据库时间函数不会混用不同的时间基准。
五、ft_order_item：订单明细表
第一版虽然每笔订单只有一个票档，但我仍然建议保留独立的订单明细表。
这有利于：
- 保存不可变的演出与价格快照。
- 将来扩展多票档购买。
- 减少订单主表的文本字段。
- 支持退款、统计与历史详情查询。
5.1 字段设计
字段	MySQL 类型	约束 / 默认值	说明
id	BIGINT	PK，NOT NULL	订单明细雪花主键
order_id	BIGINT	NOT NULL	所属订单
event_id	BIGINT	NOT NULL	演出 ID
event_title	VARCHAR(100)	NOT NULL	下单时演出名称
session_id	BIGINT	NOT NULL	场次 ID
session_name	VARCHAR(64)	NOT NULL	下单时场次名称
session_start_at	DATETIME(3)	NOT NULL	下单时演出开始时间
venue_id	BIGINT	NOT NULL	场馆 ID
venue_name	VARCHAR(100)	NOT NULL	下单时场馆名称
venue_address	VARCHAR(255)	NOT NULL	下单时场馆地址
tier_id	BIGINT	NOT NULL	票档 ID
tier_name	VARCHAR(64)	NOT NULL	下单时票档名称
unit_price_cent	BIGINT	NOT NULL	下单时单价
quantity	INT	NOT NULL	购买数量
original_amount_cent	BIGINT	NOT NULL	本明细优惠前金额
created_at	DATETIME(3)	NOT NULL	明细创建时间
5.2 交易快照原则
例如用户在 2026 年购买：
字段	下单时内容
演出	星光音乐节
场次	2027-05-01 19:30
场馆	星光体育馆
票档	VIP
单价	¥999
数量	2
原价	¥1,998
即使运营人员后来将票价调整为 ¥1,099，订单明细仍然保留原来的 ¥999。
如果演出发生延期，用户端应能够另外展示最新演出安排，但不应覆盖原交易快照。
5.3 唯一约束与索引
索引	字段	作用
PRIMARY KEY	id	明细主键
uk_order_item_order	order_id	V1 每订单最多一条明细
idx_order_item_tier	(tier_id, order_id)	票档订单关联查询
这里需要做一个设计取舍。
如果长期只做一单一票档，UNIQUE(order_id) 很合适。
如果未来支持一单多个票档，则需要通过 Flyway 迁移删除这个约束并调整订单、库存与退款逻辑。
V1 推荐先保持 UNIQUE(order_id)，不要为了尚未实现的复杂订单提前放宽业务约束。
5.4 CHECK 约束
- quantity BETWEEN 1 AND 2。
- unit_price_cent >= 0。
- original_amount_cent = unit_price_cent * quantity。
- 订单明细不允许为空。
- 明细创建后不允许修改价格、数量及演出交易快照。
最后两项属于跨表完整性及不可变业务规则，不能仅通过普通 CHECK 保证。
订单主表和明细表的金额一致性，也需要在创建订单的本地事务内检查。

六、优惠券与订单的关联设计
上一批已经确定 ft_purchase_request.selected_coupon_id，而订单模块也保留 ft_order.user_coupon_id。
两个字段职责不同：
字段	语义
ft_purchase_request.selected_coupon_id	用户提交抢票时希望使用的券
ft_order.user_coupon_id	订单创建时实际成功锁定的券
如果用户选择的优惠券在排队期间失效，V1 默认使这次抢票请求业务失败，不自动按原价创建订单。
订单创建时必须在同一事务内：
- 验证用户券归属和有效期。
- 将用户券从 AVAILABLE 转为 LOCKED。
- 计算最终优惠金额。
- 保存订单金额。
- 创建订单与库存预占。
建议增加优惠规则快照
在 ft_order 增加两个可空或具默认值的字段：
字段	类型	说明
coupon_template_id	BIGINT，NULL	优惠券所属模板
promotion_snapshot	JSON，NULL	使用时的优惠规则快照
如果订单没有使用优惠券，这些字段为空。
promotion_snapshot 是成交事实的一部分，不用于重新计算已经生成的历史订单金额。
退款时只退实际支付金额，不退优惠前原价。
七、ft_order_operation：订单操作流水表
这张表记录所有重要订单状态变更，用于业务审计、异常分析和对账。
7.1 字段设计
字段	MySQL 类型	约束 / 默认值	说明
id	BIGINT	PK，NOT NULL	雪花主键
operation_id	VARCHAR(64)	NOT NULL，UNIQUE	操作幂等编号
order_id	BIGINT	NOT NULL	关联订单
operation_type	VARCHAR(32)	NOT NULL	业务操作类型
from_status	VARCHAR(24)	NULL	操作前订单状态
to_status	VARCHAR(24)	NOT NULL	操作后订单状态
reason_code	VARCHAR(64)	NULL	业务原因码
remark	VARCHAR(500)	NULL	脱敏后的操作说明
operator_type	VARCHAR(16)	NOT NULL	操作主体类型
operator_id	BIGINT	NULL	用户或管理员 ID
request_id	VARCHAR(64)	NULL	关联请求
trace_id	VARCHAR(64)	NULL	链路追踪 ID
version_before	BIGINT	NOT NULL	订单操作前版本
version_after	BIGINT	NOT NULL	操作后版本
created_at	DATETIME(3)	NOT NULL	操作时间
7.2 操作类型
类型	说明	订单状态变化
CREATE	抢票成功建单	无 → PENDING_PAYMENT
PAYMENT_CONFIRM	模拟支付确认	PENDING_PAYMENT → PAID
USER_CANCEL	用户取消	PENDING_PAYMENT → CLOSED
TIMEOUT_CLOSE	超时自动关闭	PENDING_PAYMENT → CLOSED
SYSTEM_CLOSE	系统合法关单	PENDING_PAYMENT → CLOSED
REFUND_REQUEST	用户申请退款	PAID → PAID
REFUND_COMPLETE	退款完成通知或权益处理	PAID → PAID
对于退款事件，订单主状态未变化，因此 version_after 是否递增，需要统一约定。
最终推荐：订单操作流水只记录确实发生的订单业务操作，但不是每条记录都必须改变订单主版本。 因此采用：
- 状态改变时：version_after = version_before + 1。
- 只记录退款关联事件且订单状态不变时：可保持两个版本相同。
- 订单退款详情的权威状态仍以 ft_refund 为准。
7.3 索引与唯一约束
索引	字段	用途
PRIMARY KEY	id	流水主键
uk_order_operation	operation_id	操作幂等
idx_order_operation_time	(order_id, created_at, id)	订单操作历史
idx_order_operation_type	(operation_type, created_at, id)	运营审计筛选
订单操作流水应采用追加写入，已提交的流水不能被普通业务修改或删除。
若发现历史操作错误，应创建新的审计事件及补偿记录，而不是覆盖旧流水。

八、三张表之间的关联约束
关系	V1 约束	保障方式
PurchaseRequest → Order	1 : 0..1	ft_order.request_id 唯一
Order → OrderItem	1 : 1	ft_order_item.order_id 唯一 + 创建事务
Order → Reservation	1 : 1（成功建单后）	ft_reservation.order_id 唯一
Order → Operation	1 : N	关联订单 ID
Order → Payment	1 : 0..1	后续 ft_payment.order_id 唯一
Order → CouponUsage	1 : 0..1	后续券使用记录
User → Order	1 : N	user_id
物理外键选择
推荐：
- ft_order_item.order_id → ft_order.id：使用物理外键，删除规则 RESTRICT。
- ft_order_operation.order_id → ft_order.id：逻辑关联为主。
- ft_order.request_id → ft_purchase_request.request_id：逻辑关联。
- ft_order.user_id → ft_user.id：逻辑关联。
- ft_order.tier_id → ft_ticket_tier.id：逻辑关联。
对于用户、票档等历史业务实体，禁止直接物理删除。
MySQL 外键可以保护引用完整性，但需要关联索引并会参与数据库锁与约束检查，所以高频交易表应明确取舍，而不是全部无差别使用外键。

MySQL




九、订单创建、支付和关单的事务设计
这是三张表真正发挥作用的地方。
9.1 事务 A：抢票成功创建订单
RocketMQ 抢票消费者取得合法执行权


MySQL 本地事务
1. 锁定并验证抢票请求的处理资格和租约令牌。
2. 确认活动、票档与限购规则仍然有效。
3. 原子占用用户限购额度。
4. 通过库存模块执行正式库存预占。
5. 创建 ft_order 和 ft_order_item。
6. 关联 ft_reservation.order_id，设置支付截止时间。
7. 更新抢票请求为 SUCCEEDED。
8. 创建 ft_order_operation 和 Outbox 事件。


事务提交 → 订单正式成立


正常情况下，ft_reservation.expire_at 应与 ft_order.expire_at 一致。
创建订单和正式库存预占属于同一个事务，因此不会留下已经提交的订单却没有库存预占的合法结果。
9.2 事务 B：支付确认
需要一起提交：
数据对象	变化
ft_order	PENDING_PAYMENT → PAID
ft_payment	进入最终成功状态
ft_reservation	RESERVED → CONFIRMED
ft_inventory	reserved 减少，sold 增加
ft_purchase_limit	未支付限购转为累计已支付限购
ft_user_coupon	LOCKED → USED
ft_inventory_ledger	新增 CONFIRM 流水
ft_order_operation	新增 PAYMENT_CONFIRM
ft_outbox_event	新增支付成功业务事件
如果任一步发生数据库错误，整个事务回滚。
9.3 事务 C：取消或超时关单
数据对象	变化
ft_order	PENDING_PAYMENT → CLOSED
ft_reservation	RESERVED → RELEASED
ft_inventory	reserved 减少，available 增加
ft_purchase_limit	释放待支付限购
ft_user_coupon	根据有效期解锁或转为过期
ft_inventory_ledger	新增 RELEASE 流水
ft_order_operation	新增关闭操作
ft_outbox_event	新增关单业务事件
重复关单应幂等返回，不得第二次增加可售库存。
9.4 固定锁顺序
为降低死锁风险，建议定义统一的交易锁顺序：
抢票请求 / 订单主状态 → 用户限购 → 库存总账 → 正式预占 → 优惠券占用。
实际实现中，部分记录创建前并不存在，因此需要使用条件插入、唯一键冲突处理和按业务操作设计的加锁协议。支付与关单都要遵循一致的已存在记录加锁顺序。
固定顺序只能降低死锁概率，不能保证完全不发生死锁。对于 MySQL 识别出的死锁，应回滚整个事务，在业务幂等前提下进行有限次数的重新尝试。

MySQL



十、超时关单的数据查询设计
第一版采用：
RocketMQ Delay + Spring Scheduling 数据库扫描兜底。
10.1 扫描条件
只有满足以下条件的订单才能进入超时关单处理：
- status = PENDING_PAYMENT
- expire_at <= 当前数据库业务时间
依托：
idx_order_status_expire(status, expire_at, id)
建议按 expire_at, id 顺序分页扫描，不做大批量全表加载。
10.2 扫描任务不能直接批量修改库存
推荐流程为：
1. 扫描到期订单 ID。
2. 按限定批量领取处理任务。
3. 对每笔订单调用统一关单业务服务。
4. 在订单事务中重新确认状态、支付有效性及过期条件。
5. 原子关闭订单并释放库存。
6. 记录操作结果。
这比一个 UPDATE ft_order SET status='CLOSED' WHERE ... 更安全，因为订单关闭还涉及预占、限购、优惠券和 Outbox。
10.3 不需要额外建立超时任务表
此前我们将 ft_order_timeout_task 列为可选表。
本轮正式确定：V1 不创建该表。
原因是当前已有订单过期时间、可靠延迟事件和数据库扫描兜底，增加独立任务表会带来额外的状态同步工作。

十一、支付与超时关单的并发竞争实例
以一笔订单为例：
模拟两个事务同时竞争订单状态

支付先成功关单先成功

PENDING_PAYMENT
数据库当前订单状态

支付事务
获得转换资格


关单事务
不能再关闭



PAID
库存由预占转为已售，支付与订单共同提交

这是状态机示意，不代表真实数据库竞态模拟。实际支付胜出还必须满足支付期限与模拟渠道确认条件，不能仅靠线程抢到锁就视为有效。
这里有一个特别容易出错的边界：
事务先拿到锁，不一定代表业务就可以成功。
例如订单已经在 10:15:00 到期，支付事务在 10:15:01 获得锁，也不能因为关单消费者还没执行，就继续让模拟支付成功。
因此状态检查必须同时验证有效截止时间，而不只是验证当前状态。
对于 V1 的内部模拟支付，我们确定：
- 支付确认有效时刻不晚于 expire_at（边界按严格小于截止时刻实现）。
- 到期之后不能产生新的模拟成功支付。
- 关单取得合法转换资格后，支付不能再变成成功。
- 对结果不确定的模拟渠道尝试，先完成状态确认，不直接假定失败。

十二、异常恢复与订单对账
异常情况	数据库应如何处理
抢票消息重复投递	查询 request_id 对应订单，避免重复创建
订单创建事务回滚	请求恢复处理，库存和订单不能出现半提交
订单已经创建但 MQ 消费确认失败	根据已提交订单和请求终态幂等返回
延迟关单事件重复	已关闭或已支付订单不再改变
RocketMQ 延迟事件遗漏	扫描过期待支付订单
订单状态为 PAID 但支付单未成功	生成支付对账异常
订单状态为 CLOSED 但库存仍 RESERVED	生成库存释放异常
用户优惠券仍 LOCKED，但订单已关闭	生成返券异常
订单金额和明细金额不一致	生成交易金额异常
订单对账必须验证的关系
对于一个有效订单：
- ft_purchase_request.status = SUCCEEDED。
- ft_reservation.request_id 与 ft_order.request_id 一致。
- ft_reservation.order_id = ft_order.id。
- ft_order.quantity 与明细及预占数量一致。
- ft_order.original_amount_cent 与明细原价合计一致。
- 优惠金额与创建时保存的优惠快照一致。
- 订单 PAID 时，对应支付成功并且库存预占已经确认。
- 订单 CLOSED 时，对应预占已经释放。
这些属于跨表业务一致性规则，不能全部依靠 MySQL CHECK 表达。
十三、测试验收清单
订单域验收计划
0/16 项



订单创建0/4 已检查[ ] 相同 requestId 重复消费 100 次只生成一笔订单
[ ] 订单、订单明细与库存预占同事务提交
[ ] 每笔订单只有一条 V1 订单明细
[ ] 订单原价、优惠额、应付金额始终符合规则



并发状态机0/4 已检查

可靠性与恢复0/4 已检查

数据与权限0/4 已检查
本清单为后续集成与并发测试的验收标准，不代表当前测试已经执行。
十四、本批数据库设计最终定稿
订单与交易状态机域 V1.0


三张核心表： ft_order、ft_order_item、ft_order_operation。
订单主状态： PENDING_PAYMENT → PAID / CLOSED，退款使用独立状态机。
订单模型： 一笔订单包含一个场次、一个票档、1–2 张票；订单明细存储不可变交易快照。
金额模型： 原价、优惠金额、应付金额、实付金额全部使用整数分。
支付期限： 订单创建后 15 分钟，以服务端统一的数据库时间基准判断。
并发安全： 订单状态转换权、MySQL 条件更新、行锁、幂等操作 ID。
关单机制： RocketMQ Delay + Spring Scheduling 扫描兜底，不另建超时任务表。
事务原则： 订单创建与库存预占同事务；支付确认与库存已售同事务；关单与库存、限购、优惠券释放同事务。
历史审计： 所有订单状态变化追加操作记录，不覆盖原始交易快照。

至此，FlashTicket 前五批数据库详细设计已经覆盖 21 张 P0 核心表（用户 6、演出 7、库存 3、抢票 2、订单 3），并额外规划了抢票处理尝试表等扩展实体。



FlashTicket 数据库详细设计 06：模拟支付与退款域
Payment & Refund · MySQL 8.4 LTS · 详细表结构 V1.0
本轮进入第六批数据库详细设计，继续沿用前五批的规范：
- Java 21 + Spring Boot 3.5.x + MyBatis-Plus。
- MySQL 8.4 LTS + InnoDB + Flyway。
- BIGINT 雪花主键，金额使用 BIGINT 整数分。
- MySQL 默认 REPEATABLE READ。
- 核心交易通过本地事务、条件更新、唯一索引保证一致性。
- RocketMQ 5.x + Transactional Outbox + Inbox 负责可靠异步消息。
- 只使用模拟支付，不接入真实支付渠道。
本轮只确定字段、状态、索引、约束、关联与事务边界，不编写 DDL 或 Java 代码。
一、本批数据库表清单
5 张
P0 核心表

1 张
P1 可选审计表


表名	中文名称	职责
ft_payment	支付业务单	一个订单对应一个支付业务
ft_payment_attempt	支付执行尝试	记录每次模拟支付执行
ft_payment_callback	支付通知记录	回调幂等、处理状态
ft_refund	退款业务单	退款申请、审核、金额及状态
ft_refund_attempt	退款执行尝试	模拟退款执行与恢复
ft_payment_operation	支付操作审计	P1，扩展审计记录

本批需要保证的六项核心规则
1. 一个订单在 V1 中只能拥有一个支付业务单，但允许多次受控支付尝试。
2. 同一订单最多只能确认一次成功支付。
3. 重复的模拟渠道回调不能重复扣款、确认库存或核销优惠券。
4. 支付成功与订单关闭不能同时成为合法交易结果。
5. 同一笔支付最多发生一次最终有效的全额退款。
6. 退款成功后，门票和优惠券默认不重新上架、不返券。
二、核心 ER 关系
#chatgpt-mermaid-_r_2lc_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-_r_2lc_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-_r_2lc_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-_r_2lc_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-_r_2lc_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-_r_2lc_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-_r_2lc_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-_r_2lc_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-_r_2lc_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-_r_2lc_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-_r_2lc_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-_r_2lc_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-_r_2lc_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-_r_2lc_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";font-size:16px;}#chatgpt-mermaid-_r_2lc_ p{margin:0;}#chatgpt-mermaid-_r_2lc_ .entityBox{fill:rgb(19, 33, 54);stroke:rgb(31, 78, 148);}#chatgpt-mermaid-_r_2lc_ .relationshipLabelBox{fill:rgb(48, 48, 48);opacity:0.7;background-color:rgb(48, 48, 48);}#chatgpt-mermaid-_r_2lc_ .relationshipLabelBox rect{opacity:0.5;}#chatgpt-mermaid-_r_2lc_ .labelBkg{background-color:rgba(48, 48, 48, 0.5);}#chatgpt-mermaid-_r_2lc_ .edgeLabel{background-color:rgb(14, 14, 14);}#chatgpt-mermaid-_r_2lc_ .edgeLabel .label rect{fill:rgb(14, 14, 14);}#chatgpt-mermaid-_r_2lc_ .edgeLabel .label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-_r_2lc_ .edgeLabel .label{fill:rgb(31, 78, 148);font-size:14px;}#chatgpt-mermaid-_r_2lc_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";color:rgb(237, 237, 237);}#chatgpt-mermaid-_r_2lc_ .edge-pattern-dashed{stroke-dasharray:8,8;}#chatgpt-mermaid-_r_2lc_ .node rect,#chatgpt-mermaid-_r_2lc_ .node circle,#chatgpt-mermaid-_r_2lc_ .node ellipse,#chatgpt-mermaid-_r_2lc_ .node polygon{fill:rgb(19, 33, 54);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-_r_2lc_ .relationshipLine{stroke:rgb(175, 175, 175);stroke-width:1px;fill:none;}#chatgpt-mermaid-_r_2lc_ .marker{fill:none!important;stroke:rgb(175, 175, 175)!important;stroke-width:1;}#chatgpt-mermaid-_r_2lc_ [data-look=neo].labelBkg{background-color:rgba(48, 48, 48, 0.5);}#chatgpt-mermaid-_r_2lc_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-_r_2lc_ [data-look="neo"].node rect,#chatgpt-mermaid-_r_2lc_ [data-look="neo"].cluster rect,#chatgpt-mermaid-_r_2lc_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-_r_2lc_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_2lc_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-_r_2lc_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-_r_2lc_-gradient);stroke-width:1px;}#chatgpt-mermaid-_r_2lc_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_2lc_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-_r_2lc_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-_r_2lc_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_2lc_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-_r_2lc_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-_r_2lc_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_2lc_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-_r_2lc_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_2lc_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";}ft_userft_orderft_paymentft_payment_attemptft_payment_callbackft_refundft_refund_attemptft_reservationft_outbox_eventownshasattemptscallbacksrefundsattemptssecurespublishes





订单创建时可以还没有支付单。正式支付和退款成功后，相关订单、库存、优惠券与异步事件仍通过各自业务表记录。
最重要的是区分三个实体：
概念	业务意义	示例
Payment	一笔订单的支付业务	支付订单 FT2026XXXX
PaymentAttempt	一次尝试执行支付	第一次失败，第二次成功
PaymentCallback	模拟渠道发送的一次结果通知	同一个成功通知重复发送 3 次
支付业务单不能等同于支付尝试，也不能等同于回调消息。
这也是支付系统面试中经常被追问的建模问题。

三、ft_payment：支付业务单表
这张表保存一笔订单的支付业务身份、金额、最终结果和生命周期。
3.1 业务关联与金额字段
字段	MySQL 类型	约束 / 默认值	说明
id	BIGINT	PK，NOT NULL	雪花主键
payment_no	VARCHAR(40)	NOT NULL，UNIQUE	支付业务编号
order_id	BIGINT	NOT NULL，UNIQUE	所属订单
user_id	BIGINT	NOT NULL	支付用户
channel	VARCHAR(24)	NOT NULL，默认 MOCK_WALLET	模拟渠道
amount_cent	BIGINT	NOT NULL	应付金额，单位分
currency	CHAR(3)	NOT NULL，默认 CNY	币种
status	VARCHAR(24)	NOT NULL，默认 CREATED	支付单状态
expire_at	DATETIME(3)	NOT NULL	支付截止时间
version	BIGINT	NOT NULL，默认 0	并发控制版本
金额来源只能是服务端已经持久化的 ft_order.payable_amount_cent。
普通用户不能通过请求参数改变支付金额。
3.2 时间与结果字段
字段	类型	约束 / 默认值	说明
current_attempt_id	BIGINT	NULL	当前有效支付尝试
successful_attempt_id	BIGINT	NULL	最终成功尝试
processing_started_at	DATETIME(3)	NULL	开始处理时间
succeeded_at	DATETIME(3)	NULL	最终确认成功时间
failed_at	DATETIME(3)	NULL	确定失败时间
closed_at	DATETIME(3)	NULL	支付关闭时间
failure_code	VARCHAR(64)	NULL	最终失败码
created_at	DATETIME(3)	NOT NULL	创建时间
updated_at	DATETIME(3)	NOT NULL	更新时间
这里使用 successful_attempt_id 是为了让支付单能够直接追溯究竟由哪一次执行尝试确认成功。
它不能只依赖回调记录反推。
3.3 支付单状态
状态	含义	是否终态
CREATED	支付单已创建，尚未执行	否
PROCESSING	存在正在执行的支付尝试	否
SUCCEEDED	支付最终成功确认	是
FAILED	支付业务确定失败且不再允许本支付单重试	是
CLOSED	订单关闭或支付过期	是
这里对之前的支付需求做一处必要的细化：
一次支付尝试失败，不等于整张支付业务单必须变成 FAILED。
如果用户还有合法的支付时间，可以允许：
- 当前 Attempt 进入 FAILED；
- Payment 回到 CREATED，等待新的执行尝试；
- 新的 Attempt 使用新的唯一尝试编号。
只有支付业务整体确定无法继续时，才把 Payment 标记为 FAILED。
3.4 支付单索引
索引名	字段	用途
PRIMARY KEY	id	支付单主键
uk_payment_no	payment_no	支付业务编号幂等
uk_payment_order	order_id	一订单一支付业务
idx_payment_user_created	(user_id, created_at, id)	用户支付记录
idx_payment_status_expire	(status, expire_at, id)	支付过期和恢复扫描
首版核心索引建议优先保留前三项与过期扫描索引；用户列表可以优先通过订单查询关联支付信息。
3.5 数据库约束
建议设置：
- amount_cent > 0。
- version >= 0。
- status = SUCCEEDED 时，succeeded_at 和 successful_attempt_id 必须非空。
- status = CLOSED 时，closed_at 必须非空。
- status = FAILED 时，failed_at 必须非空。
- expire_at > created_at。
MySQL 8.4 可以通过 CHECK 强制验证同一行中的金额、状态与时间字段，但不能通过 CHECK 验证另一张订单表中的金额是否相等。

MySQL



跨表必须通过本地事务和对账保障：
\[
payment.amountCent=order.payableAmountCent
\]

而且：
\[
payment.currency=order.currency
\]


四、ft_payment_attempt：支付执行尝试表
这张表专门用于模拟“支付请求发送后发生什么”。
例如用户第一次支付失败，第二次支付成功，那么：
- ft_payment 仍然只有一条。
- ft_payment_attempt 可以存在两条。
4.1 字段设计
字段	MySQL 类型	约束 / 默认值	说明
id	BIGINT	PK	雪花主键
payment_id	BIGINT	NOT NULL	所属支付业务
attempt_no	INT	NOT NULL	当前是第几次尝试
attempt_request_id	VARCHAR(64)	NOT NULL，UNIQUE	客户端支付幂等请求
channel_request_no	VARCHAR(64)	NOT NULL，UNIQUE	模拟渠道执行编号
status	VARCHAR(24)	NOT NULL	尝试状态
amount_cent	BIGINT	NOT NULL	本次尝试金额
result_code	VARCHAR(64)	NULL	模拟渠道结果码
result_message	VARCHAR(255)	NULL	脱敏结果描述
started_at	DATETIME(3)	NOT NULL	开始执行时间
finished_at	DATETIME(3)	NULL	完成时间
created_at	DATETIME(3)	NOT NULL	创建时间
updated_at	DATETIME(3)	NOT NULL	更新时间
4.2 尝试状态
状态	含义
CREATED	尝试已登记
PROCESSING	模拟渠道处理中
SUCCEEDED	本次尝试最终确认成功
FAILED	本次尝试确定失败
CLOSED	本次尝试被合法关闭
UNKNOWN	暂时无法判断结果，需要恢复
UNKNOWN 非常重要。
例如内部模拟渠道已经执行成功，但消费者在记录结果之前崩溃，就不能把这次 Attempt 简单认定为失败并直接创建另一笔执行尝试。
恢复程序必须先查询该 channel_request_no 的持久化执行结果。
4.3 索引与约束
索引	字段	用途
PK	id	主键
UNIQUE	(payment_id, attempt_no)	同一支付业务尝试序号唯一
UNIQUE	attempt_request_id	HTTP 幂等
UNIQUE	channel_request_no	模拟渠道幂等
INDEX	(payment_id, created_at, id)	查询尝试历史
INDEX	(status, updated_at, id)	异常尝试恢复
正式 DDL 不必把所有候选索引无差别加入。特别是 status 频繁更新的尝试表，需要通过实际扫描查询确定索引收益。
4.4 多次支付的边界
建议 V1 保持：
同一 Payment 同时最多一个非终态 Attempt。
不能允许：
- Attempt A 正在执行；
- 用户重复点击又创建 Attempt B；
- 两个尝试分别得到成功结果。
这个限制应由支付单行上的事务串行化、current_attempt_id 和尝试状态联合保障。应用可以返回已有的处理中尝试，而不是盲目创建新的记录。
五、ft_payment_callback：模拟支付回调表
这是支付幂等的重要表。
虽然 FlashTicket 不接入真实支付宝或微信支付，但仍然设计“模拟渠道产生结果通知”的过程，以便验证重复回调、乱序和延迟到达。
5.1 字段设计
字段	MySQL 类型	约束 / 默认值	说明
id	BIGINT	PK	雪花主键
channel	VARCHAR(24)	NOT NULL	渠道
channel_event_id	VARCHAR(64)	NOT NULL	渠道事件唯一编号
channel_request_no	VARCHAR(64)	NOT NULL	对应执行尝试
payment_id	BIGINT	NOT NULL	对应支付单
attempt_id	BIGINT	NOT NULL	对应尝试
event_type	VARCHAR(32)	NOT NULL	通知事件类型
payload	JSON	NOT NULL	模拟渠道通知数据
payload_hash	CHAR(64)	NOT NULL	通知内容摘要
process_status	VARCHAR(24)	NOT NULL	通知处理状态
error_code	VARCHAR(64)	NULL	处理异常
received_at	DATETIME(3)	NOT NULL	接收时间
processed_at	DATETIME(3)	NULL	处理完成时间
created_at	DATETIME(3)	NOT NULL	创建时间
5.2 回调处理状态
状态	含义
RECEIVED	已接收，待处理
PROCESSED	处理成功
DUPLICATE	重复回调，无需重复执行业务
REJECTED	非法或不匹配的回调
REVIEW_REQUIRED	结果与业务状态冲突，需要复核
这里有一个约束需要明确：
如果同一 channel_event_id 被重复提交，数据库唯一约束阻止重复插入。因此 DUPLICATE 通常体现在接口处理结果或独立接入日志，不要求为每次重复 HTTP 通知都插入另一条相同业务事件。
5.3 唯一索引
索引	字段	用途
PK	id	主键
uk_payment_callback_event	(channel, channel_event_id)	回调去重
idx_payment_callback_payment	(payment_id, received_at, id)	支付单回调历史
idx_payment_callback_pending	(process_status, received_at, id)	待处理与异常查询
5.4 回调校验规则
收到模拟支付成功事件时，必须验证：
- 对应支付单存在。
- 对应执行尝试存在。
- channel_request_no 和尝试记录一致。
- 回调金额和币种与支付业务一致。
- 事件类型合法。
- 当前订单与支付状态允许此次转换。
- 通知来自可信的内部模拟渠道入口。
绝对不能因为回调里写了 SUCCESS，就直接更新订单为 PAID。
模拟回调接口应限制为内部或受控测试入口，不能允许任何已登录普通用户自行构造成功通知。

六、ft_refund：退款业务单表
退款与支付采用相同的核心原则：
Refund 表示一项退款业务，RefundAttempt 表示某次执行。
第一版只支持全额退款。
6.1 业务标识与金额字段
字段	MySQL 类型	约束 / 默认值	说明
id	BIGINT	PK	雪花主键
refund_no	VARCHAR(40)	NOT NULL，UNIQUE	退款业务号
refund_request_id	VARCHAR(64)	NOT NULL，UNIQUE	退款申请幂等号
payment_id	BIGINT	NOT NULL	原支付单
order_id	BIGINT	NOT NULL	原订单
user_id	BIGINT	NOT NULL	退款申请人
amount_cent	BIGINT	NOT NULL	退款金额
currency	CHAR(3)	NOT NULL，默认 CNY	币种
reason_code	VARCHAR(32)	NOT NULL	退款原因
reason_detail	VARCHAR(500)	NULL	补充说明
status	VARCHAR(24)	NOT NULL	退款状态
version	BIGINT	NOT NULL，默认 0	并发版本
6.2 审核与完成字段
字段	类型	约束 / 默认值	说明
approved_by	BIGINT	NULL	审核人
approved_at	DATETIME(3)	NULL	审核通过时间
rejected_by	BIGINT	NULL	审核拒绝人
rejected_at	DATETIME(3)	NULL	拒绝时间
review_comment	VARCHAR(500)	NULL	审核说明
current_attempt_id	BIGINT	NULL	当前退款执行尝试
succeeded_at	DATETIME(3)	NULL	退款完成时间
failed_at	DATETIME(3)	NULL	退款最终失败时间
created_at	DATETIME(3)	NOT NULL	创建时间
updated_at	DATETIME(3)	NOT NULL	更新时间
6.3 退款状态
状态	含义	业务说明
PENDING_REVIEW	待审核	用户提交申请
APPROVED	审核通过	等待执行
REJECTED	审核拒绝	本次申请终结
PROCESSING	执行中	模拟退款正在处理
SUCCEEDED	退款成功	逆向交易已确认
FAILED	退款最终失败	已确认不能继续自动执行
CANCELLED	已撤销	审核前合法撤销
对可重试的执行失败，建议保持 Refund 业务处于 APPROVED 或受控待重试状态，而不是直接进入不可逆的 FAILED。
退款失败的具体尝试历史由 ft_refund_attempt 记录。
6.4 退款唯一性约束
第一版只支持全额退款，因此推荐：
UNIQUE(order_id)
它意味着一笔订单在 V1 最多创建一项退款业务，审核拒绝后的再次申请需通过明确的重开或复核流程，而不是直接创建第二笔退款记录。
这是为了让数据库约束更加直接，避免多笔全额退款累计金额超过原支付。
如果以后支持多次部分退款，就需要迁移这个约束，并增加累计已退款金额的原子占用账本。
6.5 退款索引
索引	字段	用途
PK	id	主键
UNIQUE	refund_no	退款业务号
UNIQUE	refund_request_id	申请幂等
UNIQUE	order_id	V1 一订单一退款业务
INDEX	(user_id, created_at, id)	我的退款记录
INDEX	(status, created_at, id)	财务审核队列
INDEX	(payment_id, id)	支付退款关联
6.6 退款金额约束
必须满足：
\[
0<refund.amountCent\leq payment.amountCent
\]

V1 只允许全额退款：
\[
refund.amountCent=payment.amountCent
\]

同时要求：
- 原支付状态为 SUCCEEDED。
- 原订单状态为 PAID。
- 金额和币种来自服务端支付记录。
- 申请人拥有对应订单。
- 不允许重复退款成功。
这里金额与原支付是否相等属于跨表约束，不能仅靠本表 CHECK 实现，需要事务与对账保障。
七、ft_refund_attempt：退款执行尝试表
这一张表主要用来解决：
退款请求已经提交，但处理过程中应用崩溃，系统究竟有没有退款成功？

即使是模拟渠道，也应保留每一次执行尝试的确定结果或不确定状态。
7.1 字段设计
字段	MySQL 类型	约束 / 默认值	说明
id	BIGINT	PK	雪花主键
refund_id	BIGINT	NOT NULL	退款业务 ID
attempt_no	INT	NOT NULL	尝试次数
channel_request_no	VARCHAR(64)	NOT NULL，UNIQUE	模拟退款渠道执行编号
status	VARCHAR(24)	NOT NULL	执行状态
amount_cent	BIGINT	NOT NULL	执行金额
result_code	VARCHAR(64)	NULL	执行结果码
result_message	VARCHAR(255)	NULL	脱敏的结果描述
started_at	DATETIME(3)	NOT NULL	开始时间
finished_at	DATETIME(3)	NULL	完成时间
created_at	DATETIME(3)	NOT NULL	创建时间
updated_at	DATETIME(3)	NOT NULL	更新时间
7.2 状态定义
状态	含义
CREATED	尝试已创建
PROCESSING	模拟退款处理中
SUCCEEDED	本次执行成功
FAILED	确认本次执行失败
UNKNOWN	执行结果暂时未知
CLOSED	本次执行终止
7.3 索引设计
索引	字段	用途
PK	id	主键
UNIQUE	(refund_id, attempt_no)	尝试序号唯一
UNIQUE	channel_request_no	模拟渠道执行幂等
INDEX	(refund_id, created_at, id)	查询执行记录
INDEX	(status, updated_at, id)	异常恢复扫描
建议 V1 保持同一退款业务同时最多一个有效执行尝试。
如果已经存在 PROCESSING 或 UNKNOWN 的尝试，不能直接生成新的 Attempt，应先查询原结果。

八、支付成功与超时关单的事务一致性
这是本批数据库设计最重要的技术问题。
订单模块此前已经确定：
PENDING_PAYMENT → PAID / CLOSED
支付模块必须服从这个订单状态机。
8.1 原子支付确认
模拟渠道产生可信的支付成功结果


MySQL 本地事务
1. 取得订单状态转换资格，校验尚未过期。
2. 验证 Payment 与 Attempt 的关联及当前状态。
3. 将 Payment 和对应 Attempt 确认为成功。
4. 订单转换为 PAID，记录实付金额和时间。
5. 库存预占由 RESERVED 转为 CONFIRMED。
6. 库存总账由 reserved 转入 sold。
7. 用户限购由未支付占用转为已支付累计。
8. 若使用优惠券，正式核销。
9. 写入交易流水、Outbox 业务事件。


事务成功提交 → 支付最终确认


由于所有这些核心业务记录都在同一个 MySQL 数据库中，因此优先使用本地事务。
Spring 声明式事务支持在同一事务管理器下协调 JDBC 数据库操作，但事务上下文不会自动跨异步线程或外部网络调用传播。

Home
+1



8.2 支付与关单的互斥条件
条件	支付确认	超时关单
订单仍为待支付	必须	必须
当前时间符合期限	必须未过期	必须已过期，或合法主动取消
支付单存在不确定结果	需要先查明	不可盲目关闭可能已经成功的支付
库存预占仍有效	必须	必须
更新成功后	订单 PAID	订单 CLOSED
重要边界： 支付事务先拿到订单锁，不等于一定能成功。仍必须检查支付截止时间与支付结果是否合法。
在 V1 的内部模拟渠道中，应把模拟支付的最终确认和数据库状态转换设计成受同一个本地事务控制的操作，避免产生无法协调的外部资金结果。

九、退款执行与交易一致性
退款不允许删除原支付记录，也不能直接将订单改为 CLOSED。
9.1 退款申请事务
操作	处理要求
查询原订单	必须属于申请用户且已支付
查询原支付	必须为 SUCCEEDED
检查退票规则	根据订单创建时的退票政策快照
判断退款重复	同一订单不能创建第二个退款业务
创建 Refund	状态为 PENDING_REVIEW
保存审计事件	记录退款申请人与原因
这里不改变库存、优惠券和原支付状态。
9.2 审核事务
财务人员审核时：
- 只能处理 PENDING_REVIEW。
- 审核人不能审核自己提交的申请。
- 审核通过进入 APPROVED。
- 审核拒绝进入 REJECTED。
- 必须记录审核人、时间和意见。
- 重复审核只能返回既有结果，不能再次推动状态。
9.3 退款成功事务
退款申请已审核通过


模拟退款执行
唯一 channel_request_no · 幂等处理


MySQL 本地事务
确认退款尝试 · Refund 成功 · 撤销票务权益 · Outbox


退款完成
订单保持 PAID，前台派生展示“已退款”


第一版退款成功后的处理规则：
对象	处理
ft_payment	仍为 SUCCEEDED
ft_refund	更新为 SUCCEEDED
ft_refund_attempt	对应尝试更新为 SUCCEEDED
ft_order	保持 PAID
ft_inventory	sold 不变
ft_user_coupon	保持 USED
票务权益	撤销后不能使用
ft_outbox_event	生成退款成功事件
需要补充一个跨模块设计约束： 前面五批尚未建立独立的票券权益表。为了避免“退款成功但购票凭证仍有效”，建议第七批以后增加 ft_ticket_entitlement（票务权益表），或者在第一版订单明细中建立受控权益状态。两种方案中，更推荐前者，后续便于扩展一笔订单两张票、票券核验和部分退款。
9.4 退款重试
同一退款业务允许多次执行尝试，但必须满足：
- 上一次尝试已经确定失败，才允许新建下一次。
- UNKNOWN 状态必须先查询或恢复原执行结果。
- 已成功的退款不得再次执行。
- 管理员重试必须复用原退款业务单。
- 退款完成时再次检查订单和原支付的有效性。
不能因为 MQ 再次投递一条退款执行消息，就自动创建新的退款尝试。

十、全局唯一约束与索引汇总
数据表	主键	必需唯一约束	主要查询索引
ft_payment	id	payment_no、order_id	(status, expire_at, id)
ft_payment_attempt	id	attempt_request_id、channel_request_no、(payment_id, attempt_no)	(payment_id, created_at, id)
ft_payment_callback	id	(channel, channel_event_id)	(payment_id, received_at, id)
ft_refund	id	refund_no、refund_request_id、order_id	(status, created_at, id)
ft_refund_attempt	id	channel_request_no、(refund_id, attempt_no)	(refund_id, created_at, id)
关于并发中的“唯一有效尝试”
上面的唯一索引能阻止相同尝试编号重复插入，但不能单独证明不存在两个不同编号的处理中尝试。
因此最终设计增加两条数据库事务规则：
1. 创建支付或退款尝试之前，必须锁定对应 Payment 或 Refund 主记录。
2. 在该主记录的事务保护下，检查 current_attempt_id 所指尝试已经进入允许新建的状态，才能创建下一次尝试。
模拟渠道处理也必须使用稳定的 channel_request_no，并在执行最终确认时取得数据库状态转换资格。
外键策略
关联	方案
Payment → Order	逻辑关联 + UNIQUE(order_id)
PaymentAttempt → Payment	逻辑关联
PaymentCallback → PaymentAttempt	逻辑关联
Refund → Payment	逻辑关联
RefundAttempt → Refund	逻辑关联
这些是高频交易和历史流水表，不建议物理级联删除。
MySQL 唯一索引允许多个 NULL 值，所以 current_attempt_id 等可空引用不应单独用于证明唯一成功交易；核心唯一性仍以支付业务主记录和事务约束保证。

MySQL




十一、与前五批数据库的事务关系
事务	涉及的数据表	原子性目标
创建支付单	ft_order、ft_payment	同订单仅一支付业务
创建支付尝试	ft_payment、ft_payment_attempt	同时只有一个有效尝试
支付最终确认	Payment、Attempt、Order、Reservation、Inventory、PurchaseLimit、Coupon、Ledger、Outbox	不重复支付、不重复库存确认
订单超时关闭	Order、Payment、Attempt、Reservation、Inventory、PurchaseLimit、Coupon、Outbox	不允许支付与关闭双重生效
申请退款	Order、Payment、Refund	不重复申请
审核退款	Refund、审计记录、Outbox	审核状态合法
退款最终确认	Refund、RefundAttempt、票务权益、Outbox	不重复退款及权益撤销
统一加锁约定： 所有支付与关单竞争流程首先锁定订单主记录，再按稳定顺序访问支付、预占、库存、限购和优惠券记录。退款则锁定原订单和退款业务记录。加锁协议需在具体实现阶段与抢票创建事务一并检查，避免不同路径出现循环等待。
所有数据库操作应在一个明确的事务边界内完成；数据库事务外的 SSE、邮件和 RocketMQ 发送使用 Outbox 驱动。
十二、故障与并发验收标准
支付与退款域验收清单
0/16 项已验证



支付业务0/4[ ] 同一订单并发创建支付单只成功创建一条
[ ] 重复支付请求不会生成多条有效执行尝试
[ ] 支付回调重复 100 次仅确认一次订单
[ ] 支付成功与超时关单并发只产生一个合法终态



退款业务0/4

事务一致性0/4

故障与安全0/4
验收项是后续开发与集成测试的目标，不代表功能已经实现。
十三、本批数据库设计定稿
模拟支付与退款域 V1.0


五张核心表： ft_payment、ft_payment_attempt、ft_payment_callback、ft_refund、ft_refund_attempt。
支付模型： 一订单一 Payment，多次受控 Attempt；回调记录独立去重。
支付状态： CREATED / PROCESSING / SUCCEEDED / FAILED / CLOSED。
退款模型： 一订单最多一项 V1 全额退款业务，多次受控执行尝试。
退款状态： PENDING_REVIEW / APPROVED / REJECTED / PROCESSING / SUCCEEDED / FAILED / CANCELLED。
金额规范： 所有金额以 BIGINT 整数分保存，支付金额来自订单，退款金额来自原支付。
交易一致性： 支付、订单、库存、限购、优惠券在同一 MySQL 本地事务中最终确认。
退款权益： 不自动返券、不自动重新上架，需撤销对应购票权益。
可靠性： 唯一键、状态条件更新、操作幂等、Outbox/Inbox、异常恢复及对账。


FlashTicket 数据库详细设计 07：优惠券与营销活动域
Coupon & Marketing · MySQL 8.4 LTS · 详细表结构 V1.0
本轮进入第七批数据库设计，延续前六批已确定的技术规范：
- MySQL 8.4 LTS + InnoDB，默认 REPEATABLE READ。
- MyBatis-Plus 雪花算法 BIGINT 主键。
- 金额使用 BIGINT，单位为人民币分。
- 日期时间统一使用 DATETIME(3)，应用以 UTC 读写。
- Redis Lua 用于高并发准入，MySQL 负责最终持久化。
- RocketMQ + Transactional Outbox + Inbox 用于可靠异步发券。
本轮只设计表结构、状态、约束、索引和事务边界，不编写 DDL 或 Java 代码。
一、本批数据库表清单
这一批需要特别区分三件事：发行额度、用户领取额度、用户优惠券使用状态，它们不能混在同一张表里。
表名	中文名称	主要职责
ft_campaign	营销活动表	营销活动及审核发布
ft_coupon_template	优惠券模板表	定义面额、门槛、适用范围及有效期
ft_coupon_quota	优惠券发行额度表	模板总量、预留、已发行数量
ft_coupon_claim_request	优惠券领取请求表	幂等、异步处理、Redis 准入恢复
ft_coupon_user_quota	用户限领额度表	控制每人最多领取的数量
ft_user_coupon	用户优惠券表	用户持有的实际优惠券和状态
ft_coupon_usage	优惠券使用记录表	订单锁券、核销和返券历史
本批表规模
7 张核心表

2
活动与规则


3
额度与请求


2
持券与使用




此外，为了满足之前确定的每次发行变化可追溯要求，建议把 ft_coupon_ledger 从原来的 P1 调整为 P0 审计配套表。
因此本批最终为 7 张主要业务表 + 1 张不可变发行流水表，共 8 张。此前全局统计的 P0 表数量需要在最终 ER 总表中相应修订。
二、核心 ER 关系
#chatgpt-mermaid-_r_30a_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-_r_30a_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-_r_30a_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-_r_30a_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-_r_30a_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-_r_30a_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-_r_30a_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-_r_30a_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-_r_30a_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-_r_30a_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-_r_30a_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-_r_30a_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-_r_30a_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-_r_30a_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";font-size:16px;}#chatgpt-mermaid-_r_30a_ p{margin:0;}#chatgpt-mermaid-_r_30a_ .entityBox{fill:rgb(19, 33, 54);stroke:rgb(31, 78, 148);}#chatgpt-mermaid-_r_30a_ .relationshipLabelBox{fill:rgb(48, 48, 48);opacity:0.7;background-color:rgb(48, 48, 48);}#chatgpt-mermaid-_r_30a_ .relationshipLabelBox rect{opacity:0.5;}#chatgpt-mermaid-_r_30a_ .labelBkg{background-color:rgba(48, 48, 48, 0.5);}#chatgpt-mermaid-_r_30a_ .edgeLabel{background-color:rgb(14, 14, 14);}#chatgpt-mermaid-_r_30a_ .edgeLabel .label rect{fill:rgb(14, 14, 14);}#chatgpt-mermaid-_r_30a_ .edgeLabel .label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-_r_30a_ .edgeLabel .label{fill:rgb(31, 78, 148);font-size:14px;}#chatgpt-mermaid-_r_30a_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";color:rgb(237, 237, 237);}#chatgpt-mermaid-_r_30a_ .edge-pattern-dashed{stroke-dasharray:8,8;}#chatgpt-mermaid-_r_30a_ .node rect,#chatgpt-mermaid-_r_30a_ .node circle,#chatgpt-mermaid-_r_30a_ .node ellipse,#chatgpt-mermaid-_r_30a_ .node polygon{fill:rgb(19, 33, 54);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-_r_30a_ .relationshipLine{stroke:rgb(175, 175, 175);stroke-width:1px;fill:none;}#chatgpt-mermaid-_r_30a_ .marker{fill:none!important;stroke:rgb(175, 175, 175)!important;stroke-width:1;}#chatgpt-mermaid-_r_30a_ [data-look=neo].labelBkg{background-color:rgba(48, 48, 48, 0.5);}#chatgpt-mermaid-_r_30a_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-_r_30a_ [data-look="neo"].node rect,#chatgpt-mermaid-_r_30a_ [data-look="neo"].cluster rect,#chatgpt-mermaid-_r_30a_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-_r_30a_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_30a_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-_r_30a_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-_r_30a_-gradient);stroke-width:1px;}#chatgpt-mermaid-_r_30a_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_30a_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-_r_30a_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-_r_30a_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_30a_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-_r_30a_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-_r_30a_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_30a_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-_r_30a_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_30a_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";}ft_campaignft_coupon_templateft_coupon_quotaft_coupon_claim_requestft_coupon_user_quotaft_user_couponft_userft_coupon_usageft_orderft_coupon_ledgercontainshasreceiveslimitsissuessubmitsownsholdsgeneratesusage_historyusesrecordsrelated





营销活动和券模板是一对多关系；用户券是领取后真正产生的权益。成功领券请求最多生成一张用户券；同一用户券可以有多次历史锁券记录，但只能同时存在一个有效占用。
本批必须满足的核心约束： 不超发、不超领、不重复发券、不重复核销、不允许两笔有效订单同时占用同一张券，以及退款后按 V1 规则不自动返券。

三、ft_campaign：营销活动表
营销活动控制一次营销计划的整体生命周期，例如“周年庆限量抢券”。
3.1 字段设计
字段	MySQL 类型	约束 / 默认值	说明
id	BIGINT	PK	雪花主键
name	VARCHAR(100)	NOT NULL	活动名称
campaign_type	VARCHAR(32)	NOT NULL	营销活动类型
description	VARCHAR(1000)	NULL	活动说明
start_at	DATETIME(3)	NOT NULL	活动开始时间
end_at	DATETIME(3)	NOT NULL	活动结束时间
status	VARCHAR(24)	NOT NULL，默认 DRAFT	活动生命周期
control_status	VARCHAR(16)	NOT NULL，默认 ENABLED	手动启停控制
submitted_by	BIGINT	NULL	审核提交人
reviewed_by	BIGINT	NULL	审核人
reviewed_at	DATETIME(3)	NULL	审核时间
review_comment	VARCHAR(500)	NULL	审核意见
published_at	DATETIME(3)	NULL	发布时间
created_by	BIGINT	NOT NULL	创建人
updated_by	BIGINT	NOT NULL	修改人
version	BIGINT	NOT NULL，默认 0	配置版本
created_at	DATETIME(3)	NOT NULL	创建时间
updated_at	DATETIME(3)	NOT NULL	更新时间
3.2 营销类型
类型	说明	开发阶段
FLASH_CLAIM	限时抢券	P0
EVENT_EXCLUSIVE	指定演出专属券	P0
NEW_USER	新用户券	P1
3.3 活动状态
DRAFT → PENDING_REVIEW → PUBLISHED → ENDED
审核拒绝返回 REJECTED，运营可以修改后重新提交；已发布活动可以进入 CANCELLED。
control_status 单独使用 ENABLED / PAUSED，避免把临时暂停与业务生命周期混为一谈。
活动是否可以领券，由发布状态、控制状态、时间窗口及券模板规则共同判断。
3.4 索引与约束
名称	字段	用途
PK	id	主键查询
idx_campaign_status_time	(status, start_at, id)	前台营销活动列表
idx_campaign_creator	(created_by, created_at, id)	运营人员活动查询
字段级约束：
- end_at > start_at。
- version >= 0。
- PUBLISHED 活动必须有审核通过的配置。
- 审核人不能审核自己提交的活动。
- 已发布活动的关键配置不得直接覆盖既有券权益。
最后三项需要业务事务或审核历史记录保证，不能仅通过单行 CHECK 实现。
本批取舍： 第一版不单独建立营销审核表，但应通过共享后台审计设施保存审核快照。正式发布前需验证活动和模板的配置版本一致，不能只依赖 ft_campaign.version 而忽略子模板修改。
四、ft_coupon_template：优惠券模板表
这张表定义“什么券可以领、有什么优惠、能用在哪里、能用多久”。
第一版支持满减券，折扣券作为 P1 扩展，但字段结构提前预留。
4.1 基础与规则字段
字段	类型	约束 / 默认值	说明
id	BIGINT	PK	模板 ID
campaign_id	BIGINT	NOT NULL	所属营销活动
code	VARCHAR(48)	NOT NULL，UNIQUE	稳定模板业务编号
name	VARCHAR(100)	NOT NULL	优惠券名称
coupon_type	VARCHAR(24)	NOT NULL	FULL_REDUCTION / PERCENTAGE
threshold_cent	BIGINT	NOT NULL，默认 0	满减门槛
discount_cent	BIGINT	NULL	固定满减金额
discount_rate_bp	INT	NULL	折扣率，万分比基点
max_discount_cent	BIGINT	NULL	折扣优惠上限
per_user_limit	INT	NOT NULL，默认 1	单用户限领
applicable_scope	VARCHAR(24)	NOT NULL，默认 ALL	优惠范围
applicable_event_id	BIGINT	NULL	指定活动
applicable_tier_id	BIGINT	NULL	指定票档
status	VARCHAR(24)	NOT NULL，默认 DRAFT	模板状态
version	BIGINT	NOT NULL，默认 0	规则版本
这里 discount_rate_bp = 9000 代表按原价支付 90%，即九折；10000 代表原价。必须统一采用应付比例而不是有的字段表示折扣额、有的字段表示支付比例。
4.2 时间字段
字段	类型	约束 / 默认值	说明
claim_start_at	DATETIME(3)	NOT NULL	允许领取开始时间
claim_end_at	DATETIME(3)	NOT NULL	允许领取结束时间
validity_type	VARCHAR(24)	NOT NULL，默认 FIXED	有效期类型
valid_from	DATETIME(3)	NOT NULL（V1）	券使用开始时间
valid_to	DATETIME(3)	NOT NULL（V1）	券使用截止时间
valid_days	INT	NULL	领取后 N 天（P1）
created_by	BIGINT	NOT NULL	创建人
updated_by	BIGINT	NOT NULL	修改人
created_at	DATETIME(3)	NOT NULL	创建时间
updated_at	DATETIME(3)	NOT NULL	更新时间
total_quantity 不放在模板表里作为重复发行账本，而是由 ft_coupon_quota.total 统一管理。
4.3 模板业务规则
条件	约束
满减金额	discount_cent > 0
满减门槛	threshold_cent >= discount_cent（V1）
折扣比例	1–10000 基点
用户限领	per_user_limit >= 1
领取窗口	claim_start_at < claim_end_at
使用窗口	valid_from < valid_to
作用范围	指定活动或票档必须真实存在
applicable_scope 推荐：
- ALL：所有允许使用优惠券的演出。
- EVENT：指定演出。
- TIER：指定票档。
当 applicable_scope = EVENT 时要求 applicable_event_id 非空；当为 TIER 时要求 applicable_tier_id 非空。不同范围不应同时填写互相冲突的目标 ID。
4.4 索引
索引	字段
PK	id
UNIQUE	code
INDEX	(campaign_id, status, id)
INDEX	(status, claim_start_at, claim_end_at)
INDEX（按需）	(applicable_event_id, status)
已发布且产生用户券的模板，优惠规则必须版本化或在用户券和订单中保留规则快照。

五、ft_coupon_quota：优惠券发行额度表
这张表是营销域的库存总账，类似前面票务域的 ft_inventory。
5.1 字段设计
字段	类型	约束 / 默认值	说明
template_id	BIGINT	PK	优惠券模板 ID
total	INT	NOT NULL	核定总额度
available	INT	NOT NULL	尚可发行数量
reserved	INT	NOT NULL，默认 0	MySQL 已持久化的待发券额度
issued	INT	NOT NULL，默认 0	已成功发行的用户券数量
status	VARCHAR(16)	NOT NULL，默认 ACTIVE	额度总账状态
generation	BIGINT	NOT NULL，默认 1	Redis 发券准入代次
version	BIGINT	NOT NULL，默认 0	账本版本
initialized_at	DATETIME(3)	NOT NULL	初始化时间
updated_at	DATETIME(3)	NOT NULL	更新时间
5.2 额度约束
\[
total=available+reserved+issued
\]

三个额度分量必须非负。
reserved 只统计 MySQL 中有可靠持久化凭证的预留额度，不包括仅在 Redis 中取得的临时资格。
5.3 额度状态
状态	含义
ACTIVE	可以根据营销规则继续发行
FROZEN	运营或异常冻结
REBUILDING	Redis 额度重建、恢复中
5.4 额度变化
操作	available	reserved	issued
初始化 1000 张	+1000	0	0
持久化预留 1 张	-1	+1	0
发券成功	0	-1	+1
预留失败释放	+1	-1	0
直接 MySQL 原子发券	-1	0	+1
已使用券或过期券	0	0	0
已发行的券无论被使用、过期还是退款，都不自动恢复发行额度。
5.5 索引
第一版只需：
PRIMARY KEY(template_id)
不对高频更新的 available、reserved、issued 创建二级索引。
MySQL 8.4 的 CHECK 可以验证同一行内的守恒关系，但无法跨表计算实际已发行券数。后者必须由事务及对账保证。

MySQL



六、ft_coupon_claim_request：优惠券领取请求表
这张表负责记录用户发起的一次领券请求。
它与第四批 ft_purchase_request 设计类似，但两个领域不能共用同一张表。
6.1 基础字段
字段	类型	约束 / 默认值	说明
id	BIGINT	PK	雪花主键
request_id	VARCHAR(64)	NOT NULL，UNIQUE	领券幂等编号
campaign_id	BIGINT	NOT NULL	营销活动
template_id	BIGINT	NOT NULL	目标模板
user_id	BIGINT	NOT NULL	领取用户
status	VARCHAR(24)	NOT NULL，默认 QUEUED	领取请求状态
issued_coupon_id	BIGINT	NULL，UNIQUE	成功发放的用户券
failure_code	VARCHAR(64)	NULL	最终业务失败原因
accepted_deadline_at	DATETIME(3)	NOT NULL	最晚开始处理时间
completed_at	DATETIME(3)	NULL	请求终结时间
created_at	DATETIME(3)	NOT NULL	受理时间
updated_at	DATETIME(3)	NOT NULL	更新时间
version	BIGINT	NOT NULL，默认 0	并发版本
6.2 异步消费字段
字段	类型	约束 / 默认值	用途
attempt_count	INT	NOT NULL，默认 0	消费尝试次数
lease_owner	VARCHAR(64)	NULL	当前处理令牌
lease_until	DATETIME(3)	NULL	租约结束时间
next_retry_at	DATETIME(3)	NULL	下次恢复时间
last_error_code	VARCHAR(64)	NULL	最近异常
last_error_at	DATETIME(3)	NULL	最近异常时间
6.3 Redis 准入字段
字段	类型	约束 / 默认值	说明
admission_mode	VARCHAR(16)	NOT NULL，默认 MYSQL	MYSQL / REDIS
admission_state	VARCHAR(24)	NOT NULL，默认 NOT_REQUIRED	资格处理阶段
admission_generation	BIGINT	NULL	Redis 发行资格代次
admission_operation_id	VARCHAR(64)	NULL，UNIQUE	稳定操作 ID
admission_updated_at	DATETIME(3)	NULL	最近状态变化时间
6.4 请求状态
与抢票请求统一采用：
QUEUED / PROCESSING / RECOVERING / SUCCEEDED / FAILED / CANCELLED
SUCCEEDED 只有在用户券真正持久化成功后才能写入。
不能因为 Redis Lua 返回成功就立即把请求标记为 SUCCEEDED。
6.5 数据库约束与索引
索引	字段	用途
PK	id	主键
UNIQUE	request_id	请求幂等
UNIQUE	issued_coupon_id	用户券结果不重复关联
UNIQUE	admission_operation_id	Redis 临时资格唯一
INDEX	(user_id, created_at, id)	我的领券历史
INDEX	(template_id, status, created_at)	模板请求列表
INDEX	(status, next_retry_at, id)	恢复扫描
INDEX	(status, lease_until, id)	租约超时恢复
6.6 一个关键取舍：持久化预留是否需要独立凭证？
ft_coupon_quota.reserved 如果需要实际大于零，就应存在能够解释每一份持久化预留的明细。
本轮建议使用 ft_coupon_claim_request 作为预留凭证来源，并在它上面补充：
字段	类型	含义
quota_hold_state	VARCHAR(24)	NONE / RESERVED / ISSUED / RELEASED
quota_reserved_at	DATETIME(3)	额度持久化预留时间
每张券只占用 1 个发行额度。
若采用 MySQL 直接发券基线，可以在一个事务中直接 available - 1、issued + 1，无需经过长期存在的 RESERVED 状态。
这使 MySQL 同步基线和 Redis 异步发券模式都可以在同一套数据库模型中实现。

七、ft_coupon_user_quota：用户领取额度表
这张表用于解决同一个用户并发领取同一模板时绕过限领的问题。
7.1 字段设计
字段	类型	约束 / 默认值	说明
user_id	BIGINT	联合 PK	用户 ID
template_id	BIGINT	联合 PK	模板 ID
reserved_quantity	INT	NOT NULL，默认 0	已落库、尚未完成的领券额度占用
issued_quantity	INT	NOT NULL，默认 0	历史成功领取次数
version	BIGINT	NOT NULL，默认 0	并发版本
created_at	DATETIME(3)	NOT NULL	创建时间
updated_at	DATETIME(3)	NOT NULL	更新时间
采用：
PRIMARY KEY(user_id, template_id)
7.2 限领约束
\[
reservedQuantity+issuedQuantity\leq perUserLimit
\]

perUserLimit 来自优惠券模板，不能信任用户提交的参数。
当请求成功领取后：
- reserved_quantity 减少 1（如果之前确实已预留）。
- issued_quantity 增加 1。
当预留失败并确定释放后，只减少本次真实持有的 reserved_quantity。
7.3 特别注意
优惠券使用、过期、退款都不减少 issued_quantity，因为 V1 采用的是每用户累计领取次数限制，而不是当前持有有效优惠券的数量限制。
例如一人限领 1 张，就算他的那张券已经使用，仍然不能重新领取第二张。
7.4 索引
第一版仅需联合主键。
若后台需要按模板统计全部领取用户，可根据实际查询增加：
INDEX(template_id, user_id)。
这里跨表的每人领取上限仍然由 MySQL 事务内的条件判断维护。对用户限领账本行进行锁定或条件更新，可以避免并发请求都看到同一个旧额度。

MySQL



八、ft_user_coupon：用户优惠券表
这是实际归属于用户的一张优惠券，也是订单锁券与支付核销时操作的核心对象。
8.1 字段设计
字段	类型	约束 / 默认值	说明
id	BIGINT	PK	用户优惠券 ID
coupon_no	VARCHAR(40)	NOT NULL，UNIQUE	用户可见券号
claim_request_id	VARCHAR(64)	NOT NULL，UNIQUE	来源领券请求
user_id	BIGINT	NOT NULL	持有人
template_id	BIGINT	NOT NULL	模板 ID
status	VARCHAR(24)	NOT NULL，默认 AVAILABLE	用户券状态
valid_from	DATETIME(3)	NOT NULL	使用开始时间
valid_to	DATETIME(3)	NOT NULL	使用截止时间
locked_order_id	BIGINT	NULL	当前锁定订单
locked_at	DATETIME(3)	NULL	最近锁券时间
used_order_id	BIGINT	NULL	最终核销订单
used_at	DATETIME(3)	NULL	核销时间
expired_at	DATETIME(3)	NULL	标记过期时间
voided_at	DATETIME(3)	NULL	作废时间
rule_snapshot	JSON	NOT NULL	发行时的优惠规则快照
version	BIGINT	NOT NULL，默认 0	乐观并发版本
created_at	DATETIME(3)	NOT NULL	发券时间
updated_at	DATETIME(3)	NOT NULL	更新时间
rule_snapshot 建议保存券种、门槛、面额、折扣比例、适用范围、模板版本、有效期规则，以及对应的快照格式版本。
8.2 用户券状态机
AVAILABLE · 可用


LOCKED
订单占用


EXPIRED
未锁定且过期




USED
支付核销后终态


AVAILABLE / EXPIRED
未支付关单后返券



LOCKED 的券在订单支付成功时转为 USED；在未支付关单时按返券时刻的有效期判断恢复可用或过期。作废 VOID 是额外终态。
8.3 状态规则
原状态	业务动作	目标状态
AVAILABLE	订单创建并锁券	LOCKED
AVAILABLE	有效期结束	EXPIRED
AVAILABLE	合法作废	VOID
LOCKED	订单支付成功	USED
LOCKED	订单关闭，券未过期	AVAILABLE
LOCKED	订单关闭，券已过期	EXPIRED
USED	已支付退款	保持 USED
USED	重复核销	幂等返回
不能在订单仍待支付的情况下，因为用户券原始 valid_to 已经过期，就把 LOCKED 强行转换成 EXPIRED。
已经在有效期内合法锁定的券，应允许订单在自己的 15 分钟支付期限内完成支付。
8.4 索引及 CHECK
索引	字段	用途
PK	id	用户券 ID
UNIQUE	coupon_no	券号唯一
UNIQUE	claim_request_id	一请求最多发行一券
INDEX	(user_id, status, valid_to, id)	我的优惠券列表
INDEX	(template_id, status, id)	模板发券与状态统计
INDEX	(locked_order_id)	当前锁券订单
INDEX	(used_order_id)	核销订单
建议检查：
- valid_from < valid_to。
- version >= 0。
- LOCKED 时 locked_order_id 非空。
- USED 时 used_order_id 与 used_at 非空。
- EXPIRED 时有合法过期依据。
注意，一张券的当前状态只能由事务变更。单纯给 locked_order_id 建普通索引并不能保证不重复锁券；必须对用户券当前状态做原子条件更新。
九、ft_coupon_usage：优惠券使用记录表
本表不是简单记录“优惠了多少钱”，而是记录优惠券与订单之间的占用生命周期。
9.1 字段设计
字段	类型	约束 / 默认值	说明
id	BIGINT	PK	雪花主键
operation_id	VARCHAR(64)	NOT NULL，UNIQUE	使用流程幂等操作号
user_coupon_id	BIGINT	NOT NULL	被使用的用户券
order_id	BIGINT	NOT NULL，UNIQUE	订单
user_id	BIGINT	NOT NULL	订单所属用户
status	VARCHAR(24)	NOT NULL	使用记录状态
discount_cent	BIGINT	NOT NULL	本次订单优惠金额
rule_snapshot	JSON	NOT NULL	使用时的优惠规则
locked_at	DATETIME(3)	NOT NULL	锁定时间
used_at	DATETIME(3)	NULL	正式核销时间
released_at	DATETIME(3)	NULL	释放时间
release_reason	VARCHAR(32)	NULL	释放原因
version	BIGINT	NOT NULL，默认 0	状态版本
9.2 使用记录状态
状态	含义
LOCKED	当前订单持有这张券
USED	支付成功，已核销
RELEASED	订单关闭，已经返券或判为过期
9.3 唯一性取舍
我们已经确定：
- 一笔订单最多使用一张券。
- 一张券可以在未支付订单关闭后，被重新用于下一笔新订单。
- 一张券最多被成功核销一次。
因此不能给 user_coupon_id 加全局唯一索引，否则用户在一次订单取消后无法重新使用该券。
采用以下约束：
约束	目的
UNIQUE(order_id)	一订单最多一条使用记录
UNIQUE(operation_id)	业务操作幂等
INDEX(user_coupon_id, locked_at)	查询用户券使用历史
同一张券是否还能锁定，由 ft_user_coupon.status = AVAILABLE 的原子状态转换保护。
如果需要数据库层进一步保证同一券最多一个有效使用记录，可以在下一步 DDL 中考虑基于生成列的条件唯一键，例如仅在 LOCKED 或 USED 时生成用于唯一性约束的券 ID。但这属于加强型约束，不代替当前状态事务。
MySQL 的唯一索引允许多个 NULL，因而可以利用生成列实现部分状态下的唯一性约束；必须完整审查生成表达式和状态集合。

MySQL



9.4 订单、券状态及使用记录之间的关系
订单状态	UserCoupon	CouponUsage
PENDING_PAYMENT	LOCKED	LOCKED
PAID	USED	USED
CLOSED	AVAILABLE 或 EXPIRED	RELEASED
PAID + 退款成功	USED	USED
ft_order.user_coupon_id 与 ft_coupon_usage.user_coupon_id 必须匹配。
若发现不匹配，应进入营销对账异常，而不是自动修改已支付订单金额。

十、ft_coupon_ledger：优惠券发行流水表
本表属于本批新增的 P0 配套表。
它用于解释每一次优惠券额度变化，方便审计、故障恢复及对账。
10.1 字段设计
字段	类型	约束 / 默认值	说明
id	BIGINT	PK	雪花主键
operation_id	VARCHAR(64)	NOT NULL，UNIQUE	业务操作编号
template_id	BIGINT	NOT NULL	模板 ID
claim_request_id	VARCHAR(64)	NULL	关联领取请求
user_coupon_id	BIGINT	NULL	关联用户券
operation_type	VARCHAR(32)	NOT NULL	操作类型
quantity	INT	NOT NULL	操作数量
total_delta	INT	NOT NULL，默认 0	发行总额度变化
available_delta	INT	NOT NULL，默认 0	剩余额度变化
reserved_delta	INT	NOT NULL，默认 0	预留额度变化
issued_delta	INT	NOT NULL，默认 0	已发放额度变化
version_before	BIGINT	NOT NULL	操作前版本
version_after	BIGINT	NOT NULL	操作后版本
operator_type	VARCHAR(16)	NOT NULL	SYSTEM / ADMIN
operator_id	BIGINT	NULL	管理员 ID
reason	VARCHAR(255)	NULL	业务原因
created_at	DATETIME(3)	NOT NULL	流水时间
10.2 流水类型
类型	available_delta	reserved_delta	issued_delta
INITIALIZE，初始化 1000 张	+1000	0	0
RESERVE，预留一张	-1	+1	0
ISSUE_FROM_RESERVE，完成发券	0	-1	+1
ISSUE_DIRECT，直接发券	-1	0	+1
RELEASE_RESERVE，取消预留	+1	-1	0
INCREASE，新增发行量	+100	0	0
DECREASE，减少未发行量	-100	0	0
INITIALIZE 的 total_delta = +1000；新增或减少发行量的操作同样改变 total_delta。
使用优惠券或优惠券过期，不写发行额度变化流水，因为发行额度没有变化。券状态变化由 ft_coupon_usage 和用户券状态记录追踪。
10.3 约束与索引
建议满足：
\[
totalDelta=availableDelta+reservedDelta+issuedDelta
\]

并且每次账本修改：
\[
versionAfter=versionBefore+1
\]

索引	字段	用途
PK	id	主键
UNIQUE	operation_id	幂等
UNIQUE	(template_id, version_after)	模板额度版本唯一
INDEX	(template_id, created_at, id)	发行流水查询
INDEX	(claim_request_id, id)	关联领券过程
流水采用追加写入，不允许普通业务修改已提交记录。发行总账更新与对应流水插入必须在同一个 MySQL 事务里完成。
十一、最重要的四个本地事务
优惠券模块的数据库设计必须和订单、支付状态机保持一致。
11.1 事务 A：领券成功
已受理的领券请求


MySQL 本地事务
1. 锁定并验证领券请求处理资格。
2. 校验活动、模板、领取时间和限领规则。
3. 原子占用用户领取额度。
4. 原子转移模板发行额度。
5. 创建唯一 ft_user_coupon。
6. 更新领券请求为 SUCCEEDED。
7. 写入发行流水及 Outbox 事件。


事务提交 → 真正发券成功


如果已经存在数据库持久化预留，则在事务中把额度从 reserved 转为 issued；如果采用直接发券基线，则从 available 转为 issued。
不能两个路径同时扣减一份额度。
11.2 事务 B：抢票建单并锁券
在上一批已经确定的抢票建单事务中增加营销模块操作：
对象	必须满足的变化
抢票请求	当前拥有有效处理资格
用户优惠券	AVAILABLE → LOCKED
使用记录	创建 ft_coupon_usage，状态 LOCKED
库存	原子正式预占
用户限购	占用指定购票数量
订单	创建待支付订单
金额	记录原价、折扣、应付金额
Outbox	记录订单创建事件
所有操作同一个数据库事务提交或回滚。
特别要注意：在锁券时，检查的是券是否在锁定时刻有效，不是简单要求券能够覆盖整个订单 15 分钟支付窗口。
11.3 事务 C：订单支付成功
对象	变化
订单	PENDING_PAYMENT → PAID
支付业务	最终确认成功
预占库存	RESERVED → CONFIRMED
用户优惠券	LOCKED → USED
使用记录	LOCKED → USED
Outbox	支付成功事件
这几个动作必须在同一个 MySQL 事务内完成。
如果用户券已经被非法释放，支付确认不得忽略这个异常并继续提交；应进入安全失败或恢复路径。
11.4 事务 D：订单取消或超时关闭
对象	变化
订单	PENDING_PAYMENT → CLOSED
正式预占	RESERVED → RELEASED
用户限购	释放未支付占用
用户优惠券	LOCKED → AVAILABLE / EXPIRED
使用记录	LOCKED → RELEASED
Outbox	关单通知事件
重复关单不得重复返券。
若用户券在订单锁定期间过期，那么订单正常支付成功时仍可核销；若订单关闭，则根据关闭时刻决定是否恢复为可用券。
退款成功后 V1 不自动返券，也不自动重新上架门票。

十二、并发问题与唯一性设计
12.1 发行总额并发超发
模板剩余 100 张，同时有 10,000 个领取请求。
数据库侧必须使用条件更新保证发行额度足够，不能先查询 available 再无条件写入。
Redis Lua 可以提前筛掉没有资格的请求，但最终仍以数据库事务为准。
12.2 用户重复领券
一个用户限领 1 张，同时发送 10 次请求，且使用不同 requestId。
ft_coupon_claim_request.request_id 唯一只能防同请求重复，不能防不同请求绕过限领。
真正的最终保护是：
ft_coupon_user_quota (user_id, template_id) 联合主键，以及事务内原子占用。
12.3 同一张券用于两个订单
用户同时对两个票档发起抢票，并选同一张优惠券。
只有一个订单事务能够将券从 AVAILABLE 转为 LOCKED，另一个必须失败或按预设业务规则处理。
不建议先把券信息查出来，再不带状态条件地更新它。
12.4 支付与关单竞争
支付成功要核销券，关单要释放券，两者都必须依赖订单状态转换权。
不能在订单主状态尚未确定的情况下，让两个不同的 MQ 消费者分别直接处理优惠券的 USED 和 AVAILABLE 转换。
MySQL InnoDB 的锁定读及条件更新适用于这类并发控制；在 RR 隔离级别下，普通快照读不能替代当前状态的锁定检查。

MySQL



12.5 Redis 临时资格恢复
优惠券发行同样需要区分两种额度：
- Redis 临时准入资格。
- MySQL 正式发行额度。
假设 MySQL available = 100，Redis 已发放 8 份尚未落库资格，则在同一受控检查点下，Redis 仍可发放的额度不应该超过 92。
Redis 重启后不能简单把 ft_coupon_quota.available 完整写回 Redis，再发放一遍。
应使用领券请求的稳定操作 ID、准入代次和在途处理恢复记录，结合 MySQL 发行额度，确认旧资格不会继续落库后才安全重建。
十三、全局索引与约束汇总
表	主键	必要唯一约束	关键查询索引
ft_campaign	id	—	(status, start_at, id)
ft_coupon_template	id	code	(campaign_id, status, id)
ft_coupon_quota	template_id	主键即唯一	主键
ft_coupon_claim_request	id	request_id、issued_coupon_id、admission_operation_id	(status, next_retry_at, id)
ft_coupon_user_quota	(user_id, template_id)	联合主键	视需求增加反向索引
ft_user_coupon	id	coupon_no、claim_request_id	(user_id, status, valid_to, id)
ft_coupon_usage	id	order_id、operation_id	(user_coupon_id, locked_at)
ft_coupon_ledger	id	operation_id、(template_id, version_after)	(template_id, created_at, id)
外键策略
关联	推荐方案
Template → Campaign	物理外键，RESTRICT
Quota → Template	物理外键，RESTRICT
ClaimRequest → User / Template	逻辑关联，事务与唯一键保证
UserQuota → User / Template	逻辑关联
UserCoupon → User / Template	逻辑关联，禁止物理删除
CouponUsage → Order / UserCoupon	逻辑关联
CouponLedger → Template	逻辑关联，流水不可变
需要特别留意的两个设计点
第一，ft_coupon_usage.order_id 全局唯一是 V1 的主动约束。 以后如果订单支持多张优惠券，需要迁移约束并重新设计优惠分摊，而不只是删掉唯一索引。
第二，ft_user_coupon 与 ft_coupon_usage 共同记录锁券信息是有目的的冗余。 前者表示当前状态，后者表示历史订单使用过程，两者必须在同一事务中修改，并通过对账检查一致性。
另外，ft_coupon_usage 如果未来允许同一张券反复锁定和释放，需要明确每次新锁券的 operation_id 不重复、使用记录的历史状态不能被直接覆盖。
十四、验收标准
营销域验收清单
0/16 已检查



发行额度0/4[ ] 模板限量 1000 张，面对 10000 次并发请求最终不超发
[ ] 重复 requestId 不重复发券
[ ] 模板发行总额、预留、已发放始终守恒
[ ] Redis 额度丢失后可以安全恢复，不重复发放资格



用户限领0/4

订单核销0/4

对账与恢复0/4
本清单用于后续数据库集成测试和并发测试，当前尚未执行。
十五、第七批设计定稿
优惠券与营销活动域 V1.0


8 张 P0 表： 7 张主要业务表，加 ft_coupon_ledger 发行流水表。
营销规则： 满减券 P0、折扣券 P1；单订单最多一张优惠券。
发行账本： total = available + reserved + issued。
用户限领： (user_id, template_id) 联合主键，累计领取次数不因使用或过期恢复。
领取请求： 独立请求状态、消费租约、幂等键、Redis 准入代次及恢复字段。
用户券状态： AVAILABLE → LOCKED → USED，关单后按有效期恢复可用或过期。
订单协同： 锁券、建单及库存预占同事务；支付核销及关单返券同事务。
退款策略： V1 全额退款成功后不自动返券。
可靠性： MySQL 条件更新、不可变发行流水、Outbox/Inbox、Redis 预占恢复及对账。



FlashTicket 数据库详细设计 08：风险控制与防刷域
Risk Control & Anti-Abuse · MySQL 8.4 LTS · 详细表结构 V1.0
本轮进入第八批数据库详细设计，继续沿用前七批已经确定的规范：
- MySQL 8.4 LTS + InnoDB，默认 REPEATABLE READ。
- MyBatis-Plus 雪花算法 BIGINT 主键。
- 时间统一采用 DATETIME(3)，应用以 UTC 读写。
- Redis 7.4 + Lettuce + Lua 实现高频限流和临时计数。
- RocketMQ 5.x + Outbox/Inbox 处理异步风险事件。
- Spring Security 6 + JWT + RBAC 负责身份与权限校验。
本轮只确定表结构、字段、索引、状态、约束和事务边界，不编写 DDL 或 Java 代码。
一、本批数据库表清单
我建议使用 3 张 P0 核心表 + 3 张 P1 扩展表。
表名	中文名称	核心职责	优先级
ft_risk_rule	风控规则表	定义限流、风险识别和处置规则	P0
ft_risk_restriction	风险限制表	持久化黑名单及临时业务限制	P0
ft_risk_event	风险事件表	记录重要风险命中与业务上下文	P0
ft_risk_rule_version	风控规则版本表	保存历史发布配置	P1
ft_risk_decision	风险决策表	记录完整规则评估结果	P1
ft_risk_operation	风控操作审计表	记录管理员对规则和限制的操作	P1
与订单、库存模块不同，风控模块绝大多数请求不会产生 MySQL 写入。
正常请求只需完成 Redis 限流和规则判断。重要的拒绝、临时限制及管理员操作才进行持久化。
这是为了避免 10 万用户同时抢票时，风控日志反过来成为数据库性能瓶颈。
二、核心 ER 关系
#chatgpt-mermaid-_r_33o_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-_r_33o_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-_r_33o_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-_r_33o_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-_r_33o_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-_r_33o_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-_r_33o_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-_r_33o_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-_r_33o_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-_r_33o_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-_r_33o_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-_r_33o_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-_r_33o_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-_r_33o_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";font-size:16px;}#chatgpt-mermaid-_r_33o_ p{margin:0;}#chatgpt-mermaid-_r_33o_ .entityBox{fill:rgb(19, 33, 54);stroke:rgb(31, 78, 148);}#chatgpt-mermaid-_r_33o_ .relationshipLabelBox{fill:rgb(48, 48, 48);opacity:0.7;background-color:rgb(48, 48, 48);}#chatgpt-mermaid-_r_33o_ .relationshipLabelBox rect{opacity:0.5;}#chatgpt-mermaid-_r_33o_ .labelBkg{background-color:rgba(48, 48, 48, 0.5);}#chatgpt-mermaid-_r_33o_ .edgeLabel{background-color:rgb(14, 14, 14);}#chatgpt-mermaid-_r_33o_ .edgeLabel .label rect{fill:rgb(14, 14, 14);}#chatgpt-mermaid-_r_33o_ .edgeLabel .label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-_r_33o_ .edgeLabel .label{fill:rgb(31, 78, 148);font-size:14px;}#chatgpt-mermaid-_r_33o_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";color:rgb(237, 237, 237);}#chatgpt-mermaid-_r_33o_ .edge-pattern-dashed{stroke-dasharray:8,8;}#chatgpt-mermaid-_r_33o_ .node rect,#chatgpt-mermaid-_r_33o_ .node circle,#chatgpt-mermaid-_r_33o_ .node ellipse,#chatgpt-mermaid-_r_33o_ .node polygon{fill:rgb(19, 33, 54);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-_r_33o_ .relationshipLine{stroke:rgb(175, 175, 175);stroke-width:1px;fill:none;}#chatgpt-mermaid-_r_33o_ .marker{fill:none!important;stroke:rgb(175, 175, 175)!important;stroke-width:1;}#chatgpt-mermaid-_r_33o_ [data-look=neo].labelBkg{background-color:rgba(48, 48, 48, 0.5);}#chatgpt-mermaid-_r_33o_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-_r_33o_ [data-look="neo"].node rect,#chatgpt-mermaid-_r_33o_ [data-look="neo"].cluster rect,#chatgpt-mermaid-_r_33o_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-_r_33o_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_33o_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-_r_33o_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-_r_33o_-gradient);stroke-width:1px;}#chatgpt-mermaid-_r_33o_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_33o_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-_r_33o_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-_r_33o_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_33o_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-_r_33o_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-_r_33o_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_33o_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-_r_33o_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_33o_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";}ft_risk_ruleft_risk_rule_versionft_risk_eventft_risk_decisionft_risk_restrictionft_risk_operationft_userhas_versionstriggersevaluatesevidence_forassessed_byauditedauditedassociated_with





这是逻辑关系图。风险主体既可能是用户，也可能是 IP 或业务资源，因此不能把所有风险限制都设计为只能关联 ft_user。
本批的六项设计原则
1. 风控规则存 MySQL，Redis 保存规则缓存和高频限流状态。
2. 风控限制必须可追溯、可解除、可到期，不能依赖一个永久 Redis Key。
3. 正常请求不逐条同步写 MySQL 风控事件。
4. 风控放行不能替代库存、订单及优惠券模块的业务校验。
5. 风控规则发布要有版本号，避免部分实例使用旧规则。
6. 风控故障时，应按接口风险级别进行明确降级，而不是统一无条件放行。

三、ft_risk_rule：风控规则表
这是风控模块最重要的配置表。
负责定义在哪个业务场景下、满足什么条件时、执行什么风险处置动作。
3.1 基础字段
字段	MySQL 类型	约束 / 默认值	说明
id	BIGINT	PK	雪花主键
rule_code	VARCHAR(64)	NOT NULL，UNIQUE	稳定规则编号
name	VARCHAR(100)	NOT NULL	规则名称
description	VARCHAR(500)	NULL	规则说明
scene	VARCHAR(32)	NOT NULL	业务场景
rule_type	VARCHAR(32)	NOT NULL	规则类型
action	VARCHAR(24)	NOT NULL	命中后的处置动作
priority	INT	NOT NULL，默认 100	执行优先级
status	VARCHAR(16)	NOT NULL，默认 DRAFT	规则发布状态
config	JSON	NOT NULL	规则参数
config_version	BIGINT	NOT NULL，默认 0	当前规则配置版本
published_at	DATETIME(3)	NULL	最近发布时间
created_by	BIGINT	NOT NULL	创建人
updated_by	BIGINT	NOT NULL	最后修改人
created_at	DATETIME(3)	NOT NULL	创建时间
updated_at	DATETIME(3)	NOT NULL	更新时间
version	BIGINT	NOT NULL，默认 0	数据库乐观锁版本
这里特别区分两个版本：
- version：MySQL 记录更新版本，防止管理员并发修改覆盖。
- config_version：业务规则发布版本，用于 Redis 缓存和应用实例同步。
例如规则修改了三次草稿，但只发布一次，两个版本就可能不同。
3.2 业务场景 scene
场景	含义	示例
LOGIN	登录	密码错误频率
REGISTER	注册	同 IP 大量注册
CAPTCHA	验证码	高频发送邮箱验证码
EVENT_QUERY	演出查询	高频访问公开活动
PURCHASE	抢票	单用户、票档限流
COUPON_CLAIM	领券	模板级抢券保护
PAYMENT	模拟支付	同订单重复发起支付
ADMIN_API	管理端	敏感接口调用频率
V1 使用这八个固定场景即可，不需要允许后台管理员随意创建未知的场景字符串。
3.3 规则类型 rule_type
类型	作用	V1
RATE_LIMIT	请求速率限制	P0
LOGIN_FAILURE	登录失败限制	P0
REPEAT_OPERATION	重复业务操作识别	P0
UNPAID_OCCUPANCY	长期占票不支付	P0
SUBJECT_RESTRICTION	主体限制名单匹配	P0
RISK_SCORE	多规则评分	P1
RESOURCE_PRESSURE	系统容量保护规则	P1
3.4 风险动作 action
动作	含义
ALLOW	允许继续业务处理
THROTTLE	限速或拒绝当前超额请求
CHALLENGE	要求额外验证
DENY	拒绝当前业务操作
这里不把 BLOCK_ACCOUNT 直接设计为普通规则动作。
如果需要对用户实施一段时间的抢票限制，应创建 ft_risk_restriction，记录原因、作用范围和过期时间。
3.5 JSON 规则配置
不同规则需要不同参数，因此使用 MySQL JSON 比设计大量稀疏字段更合适。
规则	需要配置的参数
用户限流	窗口长度、最大次数、主体维度
IP 限流	IP 窗口、最大次数、处理动作
登录失败保护	错误次数、统计窗口、锁定时间
占票不支付	观察窗口、超时订单次数、限制时间
风险评分	风险分值、命中条件、阈值
但必须注意：不能直接将任意 JSON 配置当作可执行表达式。
建议 Java 中定义明确的规则类型及对应 DTO，对 config 进行 Schema 校验、范围校验和白名单校验。
不允许通过 JSON 上传任意 Java 类名、SpEL 表达式、SQL 或脚本执行内容。
3.6 规则状态
状态	含义
DRAFT	草稿，未生效
PUBLISHED	已发布并可参与评估
DISABLED	已停用
ARCHIVED	已归档
其中规则是否命中用户请求，应由发布状态、规则类型和匹配条件决定。
DRAFT 规则不得出现在运行中的限流判断中。
3.7 索引设计
索引	字段	用途
PRIMARY KEY	id	主键
uk_risk_rule_code	rule_code	规则码全局唯一
idx_risk_rule_scene_status	(scene, status, priority, id)	获取场景有效规则
idx_risk_rule_updated	(updated_at, id)	规则同步和后台查询
不建议直接对整个 JSON 配置创建索引。
规则数量预计远少于抢票请求数量，正常运行时从本地已发布规则缓存读取，不需要每次请求查询 MySQL。
四、ft_risk_restriction：风险限制表
这张表是临时黑名单和业务限制的权威存储。
例如：
- 某用户连续占票不支付，限制其 30 分钟内再次抢票。
- 某 IP 高频尝试登录，临时限制登录频率。
- 某个账号被限制参加特定演出的抢票活动。
4.1 字段设计
字段	MySQL 类型	约束 / 默认值	说明
id	BIGINT	PK	雪花主键
restriction_no	VARCHAR(64)	NOT NULL，UNIQUE	限制业务编号
subject_type	VARCHAR(24)	NOT NULL	限制主体类型
subject_key	VARCHAR(128)	NOT NULL	主体标识或安全摘要
scope_type	VARCHAR(24)	NOT NULL	限制业务范围
scope_id	BIGINT	NULL	具体活动、票档等 ID
restriction_type	VARCHAR(32)	NOT NULL	限制原因分类
source_rule_id	BIGINT	NULL	来源规则 ID
source_event_id	BIGINT	NULL	来源风险事件
reason_code	VARCHAR(64)	NOT NULL	原因码
reason_detail	VARCHAR(500)	NULL	脱敏原因说明
status	VARCHAR(16)	NOT NULL，默认 ACTIVE	限制状态
effective_at	DATETIME(3)	NOT NULL	生效时间
expire_at	DATETIME(3)	NOT NULL	到期时间
revoked_at	DATETIME(3)	NULL	提前解除时间
revoked_by	BIGINT	NULL	解除人员
revoke_reason	VARCHAR(500)	NULL	解除原因
created_by	BIGINT	NULL	创建人员，系统创建时可空
version	BIGINT	NOT NULL，默认 0	并发版本
created_at	DATETIME(3)	NOT NULL	创建时间
updated_at	DATETIME(3)	NOT NULL	更新时间
4.2 主体类型
subject_type	含义	subject_key 建议内容
USER	用户账号	用户 ID 的规范化字符串
IP	网络来源	经过规范化的 IP 或受保护摘要
SESSION	登录会话	会话 ID 的安全摘要
DEVICE	设备标识	设备标识的安全摘要，P1
V1 重点支持 USER 和 IP。
不能把 IP 字段简单定义为 BIGINT，因为还需要支持 IPv6。
如果后续需要对 IP 网段进行限制，应单独设计网段匹配规则，不能直接通过字符串前缀判断。
4.3 作用范围
scope_type	含义	是否需要 scope_id
GLOBAL	全平台相关受限操作	否
LOGIN	登录	否
PURCHASE	全部新抢票请求	否
EVENT	指定活动抢票	是
TIER	指定票档抢票	是
COUPON_CLAIM	全部领券	否
COUPON_TEMPLATE	指定模板领券	是
需要明确：风控 GLOBAL 限制不等于账号主状态 DISABLED。
限制某用户参与新抢票，也不能默认阻止他查询已支付订单或处理合法退款。
4.4 风险限制状态机
ACTIVE
限制生效中


EXPIRED
达到过期时间


REVOKED
管理员提前解除




实际限制是否生效，不能只判断 status = ACTIVE，还要满足：
\[
effectiveAt\leq now<expireAt
\]

并且未被撤销。
限制到期时，即使 Spring 定时任务还未把状态更新为 EXPIRED，业务判断也必须自动认为限制失效。
4.5 唯一性与重复限制
不能简单添加：
UNIQUE(subject_type, subject_key, scope_type, scope_id)
因为一个主体可能有多条历史限制记录，而且不同原因可能在不同时间段产生限制。
V1 推荐：
- restriction_no 全局唯一。
- 相同来源事件不能重复创建同一种限制。
- 有效限制可以共存，但最终决策采用匹配范围内更严格的限制。
- 同一条限制若需要延长时间，采用受控更新并记录审计。
- 已过期或已撤销的历史记录保留，不直接覆盖。
对于来源于同一事件的自动处置，建议增加：
source_operation_id VARCHAR(64) NULL UNIQUE
用于保证同一个自动处置动作不会因消息重试创建多条相同限制。
4.6 索引设计
索引	字段	用途
PRIMARY KEY	id	主键
uk_restriction_no	restriction_no	限制编号唯一
uk_restriction_source_op	source_operation_id	自动处置幂等
idx_restriction_subject	(subject_type, subject_key, status, expire_at)	主体限制查询
idx_restriction_expire	(status, expire_at, id)	到期扫描
idx_restriction_scope	(scope_type, scope_id, status)	指定活动限制查询
当 scope_id 为 NULL 时，应用需要按照 scope_type 判断范围，不能把 NULL 误认为数字 0。
MySQL 唯一索引允许多个 NULL 值，因此上述可空 source_operation_id 能保留多条没有自动来源操作号的人工限制记录。

MySQL



五、ft_risk_event：风险事件表
这张表用于记录具有追溯价值的风险命中。
例如某个用户在一分钟内频繁提交抢票请求，或者因为长期占票不支付而被触发限制。
5.1 字段设计
字段	MySQL 类型	约束 / 默认值	说明
id	BIGINT	PK	雪花主键
event_no	VARCHAR(64)	NOT NULL，UNIQUE	风险事件业务编号
rule_id	BIGINT	NULL	命中的主规则
rule_version	BIGINT	NULL	判断时的发布规则版本
scene	VARCHAR(32)	NOT NULL	业务场景
risk_code	VARCHAR(64)	NOT NULL	风险原因码
risk_level	VARCHAR(16)	NOT NULL	风险等级
action	VARCHAR(24)	NOT NULL	实际处置动作
subject_type	VARCHAR(24)	NOT NULL	主体类型
subject_key	VARCHAR(128)	NOT NULL	主体标识
user_id	BIGINT	NULL	相关用户
request_id	VARCHAR(64)	NULL	关联业务请求
biz_type	VARCHAR(32)	NULL	关联业务类型
biz_id	VARCHAR(64)	NULL	关联业务编号
ip_hash	CHAR(64)	NULL	IP 安全摘要
session_id_hash	CHAR(64)	NULL	会话安全摘要
risk_score	INT	NULL	风险分值，P1 使用
context_snapshot	JSON	NULL	必要的脱敏特征快照
occurred_at	DATETIME(3)	NOT NULL	风险发生时间
created_at	DATETIME(3)	NOT NULL	写入数据库时间
5.2 风险等级
等级	含义
LOW	轻度异常
MEDIUM	需要限流或观察
HIGH	严重异常，需要挑战或限制
CRITICAL	明确严重风险，需拒绝与审计
风险等级不直接等于处置动作。
例如 HIGH 风险可以要求验证，而不必立即永久封禁。
5.3 风险事件记录策略
这是整套数据库设计的重要性能取舍。
事件类型	是否写 MySQL
正常允许请求	不逐条写，仅做指标聚合
一般限流命中	Redis 计数 + 按需采样
多次明显异常抢票	持久化重要事件
业务临时限制创建	必须可靠持久化
严重风险拒绝	持久化关键事件
管理员风控操作	必须审计
普通 HTTP 429	不需要全部写入数据库
建议通过 RocketMQ 异步记录可丢失少量细节的普通风险事件，但如果风险事件将用于创建强制业务限制，则限制本身必须可靠入库。
不能因为 MQ 发送失败，就让已经生效的管理员限制只存在于 Redis 中而没有持久化依据。
5.4 JSON 快照内容
context_snapshot 可以保存：
- 请求次数及统计窗口。
- 命中的阈值。
- 相关票档或活动 ID。
- 最近未支付关闭次数。
- 风控配置版本。
- 必要的风险判断特征。
不能保存：
- 用户明文密码。
- Access Token 或 Refresh Token。
- 邮箱验证码。
- 未经必要性评估的完整浏览器指纹。
- 与风险判断无关的敏感个人资料。
5.5 索引设计
索引	字段	用途
PRIMARY KEY	id	主键
uk_risk_event_no	event_no	事件唯一
idx_risk_event_user	(user_id, occurred_at, id)	用户风险历史
idx_risk_event_scene	(scene, occurred_at, id)	场景事件查询
idx_risk_event_level	(risk_level, occurred_at, id)	严重风险列表
idx_risk_event_request	(request_id, id)	关联抢票或领券请求
这些索引是候选集合。P0 优先保留用户、场景和请求维度查询索引；其他索引应结合实际后台查询和写入性能评估。
风险事件表不建议每次插入时同步更新大量聚合统计字段。统计交给 Prometheus 和运营统计模块。

六、P1 扩展：三张表
6.1 ft_risk_rule_version：规则历史版本
这是完整规则版本与回滚的重要基础。
字段	类型	说明
id	BIGINT	雪花主键
rule_id	BIGINT	对应规则
config_version	BIGINT	发布版本
config_snapshot	JSON	本次发布规则配置
action_snapshot	VARCHAR(24)	发布时处置动作
priority_snapshot	INT	发布时优先级
checksum	CHAR(64)	规范化配置摘要
published_by	BIGINT	发布人
published_at	DATETIME(3)	发布时间
change_reason	VARCHAR(500)	变更原因
关键唯一约束：
UNIQUE(rule_id, config_version)
发布后的版本记录不可修改。
这里建议进一步明确：P0 就应保留最基本的发布快照，P1 才增加完整的版本查询、版本差异和回滚管理页面。 否则仅有 ft_risk_rule 的可变 JSON 配置，无法可靠复现一次历史风险决策使用的规则。
6.2 ft_risk_decision：完整风险决策记录
用于记录某一次业务风险评估最终得出了什么结果。
字段	类型	说明
id	BIGINT	主键
decision_id	VARCHAR(64)	唯一决策编号
scene	VARCHAR(32)	业务场景
request_id	VARCHAR(64)	业务请求号，可空
user_id	BIGINT	用户，可空
action	VARCHAR(24)	最终风险动作
risk_score	INT	总风险分值，可空
matched_rules	JSON	命中规则及版本
reason_codes	JSON	决策原因
evaluated_at	DATETIME(3)	决策时间
duration_ms	INT	评估耗时
created_at	DATETIME(3)	入库时间
索引建议：
- UNIQUE(decision_id)
- (user_id, evaluated_at, id)
- (scene, action, evaluated_at, id)
- (request_id, id)
不建议对所有 ALLOW 请求都创建 ft_risk_decision，只持久化重要拒绝和需要调查的结果。
6.3 ft_risk_operation：风控操作审计
字段	类型	说明
id	BIGINT	主键
operation_id	VARCHAR(64)	唯一操作 ID
operator_id	BIGINT	管理员
target_type	VARCHAR(24)	RULE / RESTRICTION
target_id	BIGINT	目标记录
operation_type	VARCHAR(32)	CREATE / UPDATE / PUBLISH / REVOKE 等
before_snapshot	JSON	操作前关键信息
after_snapshot	JSON	操作后关键信息
reason	VARCHAR(500)	操作原因
trace_id	VARCHAR(64)	调用链标识
created_at	DATETIME(3)	操作时间
建议：
- UNIQUE(operation_id)。
- (target_type, target_id, created_at) 索引。
- (operator_id, created_at) 索引。
风控操作审计应追加写入，不允许普通业务删除或覆盖历史记录。
七、Redis 与 MySQL 如何协同？
这里需要把“规则配置”“限流计数”“持久化限制”三类状态明确分开。
数据	MySQL	Redis / JVM
已发布风控规则	权威配置	本地缓存及 Redis 版本标记
登录失败计数	不逐次保存	Redis 计数
抢票请求频率	不逐次保存	Redis Lua
用户临时限制	权威持久化	Redis 限制缓存
重要风险事件	选择性持久化	异步处理
管理员操作	审计记录	可通知缓存失效
下游过载状态	必要时记录操作	Redis 或应用实时指标
7.1 规则发布流程
管理员修改规则草稿


校验配置类型、参数及合法范围


MySQL 事务发布新版本
当前规则 + 版本快照 + Outbox 事件


可靠通知应用实例刷新缓存


使用新的完整规则快照进行风险评估


规则变更必须按完整版本切换，不能先更新部分阈值，再更新部分处置动作，让运行中的请求看到混合版本。
对于重要规则，可采用：
- JVM 内存不可变规则快照。
- 事件通知触发刷新。
- 定期校验数据库已发布版本。
- 关键限制执行时额外验证持久化状态或有界的版本新鲜度。
需要明确：普通异步缓存失效不能保证规则修改在所有实例上毫秒级立即生效。
如果某项风控规则要求严格实时禁止抢票，应使用单独的强制准入版本或受控暂停屏障，不能仅依赖 Caffeine 的最终一致性缓存。
7.2 临时限制生效流程
管理员创建一条限制时：
1. 在 MySQL 中创建 ft_risk_restriction。
2. 记录唯一业务操作及有效期。
3. 同事务写入 Outbox 事件。
4. 提交后刷新 Redis 限制缓存。
5. 抢票、领券等入口根据限制范围判断是否允许继续。
如果 Redis 更新失败，必须有恢复刷新机制。对于必须即时生效的禁止规则，在缓存状态未确认之前应进入受控安全降级，而不是默认放行。
7.3 Redis 限流 Key
继续沿用已确定的多维限流设计。
Key	类型	主要职责
ft:risk:rate:user:{userId}:{scene}	Hash	用户令牌桶
ft:risk:rate:ip:{ipHash}:{scene}	Hash	IP 限流
ft:risk:rate:event:{eventId}	Hash	活动限流
ft:risk:rate:tier:{tierId}	Hash	票档准入限流
ft:risk:rate:coupon:{templateId}	Hash	优惠券模板限流
ft:risk:login:fail:{accountHash}	String	登录失败计数
ft:risk:restriction:user:{userId}	Hash / String	用户限制缓存
ft:risk:rule:version	String	规则全局版本标记
Redis 限流值应有合理 TTL，避免长期占用内存。
但关键临时限制不得只依赖 Redis TTL：MySQL 的 expire_at 才是持久化限制的业务到期依据。
八、风险规则与限制的状态一致性
8.1 规则版本一致性
假设管理员发布了：
- 版本 1：用户每 5 秒最多抢票一次。
- 版本 2：用户每 10 秒最多抢票一次。
如果两个应用实例仍分别使用版本 1 和版本 2，在切换期间可能出现不同的风控结果。
因此需要定义明确的业务语义：
一般限流规则允许短时间的版本传播延迟，但关键冻结与强制限制必须满足更严格的生效要求。
建议风险事件保存 rule_id + rule_version，以便解释当时为什么作出该决策。
8.2 重复限制的处理
假设同一个用户同时命中：
限制	生效范围	截止时间
A	全部抢票	10:30
B	指定票档	11:00
用户在 10:45 访问该指定票档时：
- 限制 A 已过期。
- 限制 B 仍有效。
- 仍然禁止该票档的新抢票请求。
不能只查询最近一条限制记录就作出决定。
8.3 风控结果不能回滚已完成交易
例如：
- 用户抢票成功，订单已创建。
- 系统之后判定该用户请求过于频繁。
- 临时限制用户继续抢票。
此时可以限制新的抢票请求，但不能直接删除其已创建订单或释放库存。
任何已受理订单的取消，都需要进入订单模块的合法状态转换流程。

九、数据库约束与索引总表
数据表	主键	必须唯一	主要索引
ft_risk_rule	id	rule_code	(scene, status, priority, id)
ft_risk_restriction	id	restriction_no、source_operation_id	(subject_type, subject_key, status, expire_at)
ft_risk_event	id	event_no	(user_id, occurred_at, id)、(scene, occurred_at, id)
ft_risk_rule_version	id	(rule_id, config_version)	(rule_id, published_at)
ft_risk_decision	id	decision_id	(scene, action, evaluated_at)
ft_risk_operation	id	operation_id	(target_type, target_id, created_at)
建议的 CHECK 约束
表	约束
ft_risk_rule	priority >= 0、version >= 0、config_version >= 0
ft_risk_restriction	expire_at > effective_at、version >= 0
ft_risk_event	risk_score 为空或非负
ft_risk_decision	duration_ms >= 0
ft_risk_rule_version	config_version > 0
MySQL 8.4 会执行受支持的 CHECK 约束，但无法在 CHECK 中跨表查找有效角色、风险规则或限制记录。

MySQL



外键设计
关系	建议方案
RiskRuleVersion → RiskRule	物理外键 RESTRICT
RiskEvent → RiskRule	逻辑关联，允许规则变化后保留历史
RiskRestriction → RiskEvent	逻辑关联
RiskEvent → User	逻辑关联，支持账号注销后的脱敏保留
RiskOperation → RiskRule / Restriction	多态逻辑关联
由于 ft_risk_restriction.subject_type 可能是 USER、IP、SESSION 等，subject_key 不能统一设计为外键指向用户表。

十、风控数据库的事务边界
事务 A：规则发布
同一 MySQL 事务内：
1. 验证管理员权限与规则配置。
2. 检查规则当前版本。
3. 写入新规则发布版本。
4. 更新 ft_risk_rule 的当前配置及 config_version。
5. 写入风控操作审计记录。
6. 写入 Outbox 规则变更事件。
必须原子提交。
不能出现规则表显示版本 5，而规则版本历史中只有版本 4 的情况。
事务 B：创建临时限制
同一事务内：
1. 确认风险事件或管理员操作来源。
2. 使用 source_operation_id 保证幂等。
3. 创建风险限制记录。
4. 写入审计操作。
5. 写入 Outbox 限制生效事件。
Redis 限制缓存在事务提交后更新，并且具备版本检查和补偿机制。
事务 C：提前解除限制
同一事务内：
1. 验证限制当前仍可解除。
2. 通过状态条件更新 ACTIVE → REVOKED。
3. 记录管理员、解除时间及原因。
4. 写入审计和 Outbox。
重复解除应幂等返回，不能覆盖第一次解除的历史信息。
事务 D：风险事件写入
普通风险事件不加入抢票或订单核心事务。
推荐异步批量写入或按重要性持久化。
但用于直接产生强制限制的严重风险事件，应保证限制有可靠来源；不能因为日志异步写入失败而失去处置审计。
十一、跨模块关联与权限
风控模块会接触前面多个业务域。
业务模块	关联方式	约束
用户与权限	user_id、角色与账号状态	风控不能绕过 Spring Security
演出与场次	event_id、tier_id	只负责准入，不管理活动发布
抢票请求	request_id	风控拒绝和抢票业务失败分开统计
优惠券领取	领券请求 ID、模板 ID	不重复扣减发行额度
订单	订单 ID、超时关闭行为	不直接修改订单状态
支付	支付频率、异常重复尝试	不直接认定支付成功
通知中心	风险限制事件	异步通知用户
统计对账	风险事件、规则版本	统计拒绝率、命中率和处理记录
建议风控模块内部通过 RiskService 提供风险决策能力，但风险数据访问仍由风控模块自身维护，不允许业务模块随意读取或修改风控表。
RBAC 权限
权限标识	作用
risk:read	查询风险事件
risk:rule:manage	创建、编辑规则
risk:rule:publish	发布规则
risk:restriction:manage	新增及解除限制
risk:audit:read	查询审计
risk:admission:manage	管理业务准入保护
这些权限需加入第一批已经设计的 ft_permission，并通过 ft_role_permission 分配。
十二、性能与数据保留策略
风控日志往往增长很快，因此应提前定义保留和采样策略。
数据	建议处理
风控规则	长期保留有效及必要历史
规则版本	长期保留
有效临时限制	持续保存至失效并完成处理
已过期限制	按审计与隐私保留政策清理
普通限流命中	Redis 计数或聚合指标
严重风险事件	持久化，并控制保留期限
风险决策详情	按价值采样，不能无限增长
管理员操作审计	独立保护、不可随意删除
对于测试项目，可以先设定风险事件 90 天、普通限制历史 180 天的实验保留策略；正式环境应依据实际合规与业务需要确定，不将这些示例期限视为法律要求。
高并发下建议监控：
- Redis Lua 执行延迟。
- 用户/IP/票档限流命中次数。
- 风控决策 P95/P99 延迟。
- 规则缓存版本落后时间。
- 风险事件异步写入积压。
- 限制生效传播延迟。
- 错误限制与人工撤销数量。
十三、异常与并发验收
风控域验收清单
0/16 项



规则与版本0/4 项[ ] 并发修改同一规则不会静默覆盖
[ ] 同一规则版本不能重复发布
[ ] 历史风险事件能追溯到命中的规则版本
[ ] 规则缓存更新失败后可以恢复正确版本



限制与处置0/4 项

高并发与可靠性0/4 项

安全与审计0/4 项
以上是后续集成、并发与故障测试的验收目标，不代表当前已经通过测试。
十四、第八批数据库设计定稿
风险控制与防刷域 V1.0


P0 核心表： ft_risk_rule、ft_risk_restriction、ft_risk_event。
P1 扩展表： ft_risk_rule_version、ft_risk_decision、ft_risk_operation。其中规则发布快照和关键管理员操作审计应在 P0 具备最小实现。
规则体系： 业务场景 + 规则类型 + JSON 参数 + 处置动作 + 发布版本。
风险限制： 用户、IP 等主体，支持业务范围、有效期、撤销及来源幂等。
限流架构： Redis Lua 高频判断，MySQL 不记录每一次正常请求。
一致性： MySQL 保存权威规则与限制，Redis/Caffeine 保存缓存与临时计数。
可靠性： 关键风控修改与审计、Outbox 同事务，普通风险事件异步处理。
面试重点： 高并发限流、Redis Lua、规则版本一致性、黑名单缓存失效、系统降级、日志采样、并发幂等。



FlashTicket 数据库详细设计 09：系统通知与消息中心域
Notification & Message Center · MySQL 8.4 LTS · 详细表结构 V1.0
本轮进入第九批数据库详细设计，继续沿用前八批的规范：
- MySQL 8.4 LTS + InnoDB，默认 REPEATABLE READ。
- MyBatis-Plus 雪花算法 BIGINT 主键。
- 时间使用 DATETIME(3)，应用统一按 UTC 读写。
- RocketMQ 5.x + Transactional Outbox + Inbox 负责可靠事件。
- Redis 7.4 + Caffeine 用于缓存和实时通知辅助。
- Spring MVC + SSE 实现用户在线通知。
- Vue 3 + Naive UI 实现用户消息中心。
本轮只确定数据库表结构、关系、索引、状态与事务规则，不编写 DDL 或 Java 代码。
一、本批数据库表清单
建议采用 2 张 P0 核心表 + 3 张 P1 扩展表。
表名	中文名称	主要职责	优先级
ft_notification	通知内容表	保存通知正文、业务来源、幂等标识	P0
ft_notification_recipient	通知接收记录表	保存接收用户、已读及隐藏状态	P0
ft_notification_template	通知模板表	管理通知内容模板与版本	P1
ft_announcement	系统公告表	管理公告审核、发布与有效期	P1
ft_notification_batch	批量通知任务表	管理接收人分发、进度及恢复	P1
这里将前期草案中的 ft_announcement_delivery 调整为更通用的 ft_notification_batch，用于支撑活动取消、延期以及系统公告的大规模通知。
另外，已读记录不等于消息实际推送记录。第一版不会为每次 SSE 推送建立一张数据库流水表。
二、核心 ER 关系
#chatgpt-mermaid-_r_384_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-_r_384_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-_r_384_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-_r_384_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-_r_384_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-_r_384_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-_r_384_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-_r_384_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-_r_384_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-_r_384_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-_r_384_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-_r_384_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-_r_384_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-_r_384_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";font-size:16px;}#chatgpt-mermaid-_r_384_ p{margin:0;}#chatgpt-mermaid-_r_384_ .entityBox{fill:rgb(19, 33, 54);stroke:rgb(31, 78, 148);}#chatgpt-mermaid-_r_384_ .relationshipLabelBox{fill:rgb(48, 48, 48);opacity:0.7;background-color:rgb(48, 48, 48);}#chatgpt-mermaid-_r_384_ .relationshipLabelBox rect{opacity:0.5;}#chatgpt-mermaid-_r_384_ .labelBkg{background-color:rgba(48, 48, 48, 0.5);}#chatgpt-mermaid-_r_384_ .edgeLabel{background-color:rgb(14, 14, 14);}#chatgpt-mermaid-_r_384_ .edgeLabel .label rect{fill:rgb(14, 14, 14);}#chatgpt-mermaid-_r_384_ .edgeLabel .label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-_r_384_ .edgeLabel .label{fill:rgb(31, 78, 148);font-size:14px;}#chatgpt-mermaid-_r_384_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";color:rgb(237, 237, 237);}#chatgpt-mermaid-_r_384_ .edge-pattern-dashed{stroke-dasharray:8,8;}#chatgpt-mermaid-_r_384_ .node rect,#chatgpt-mermaid-_r_384_ .node circle,#chatgpt-mermaid-_r_384_ .node ellipse,#chatgpt-mermaid-_r_384_ .node polygon{fill:rgb(19, 33, 54);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-_r_384_ .relationshipLine{stroke:rgb(175, 175, 175);stroke-width:1px;fill:none;}#chatgpt-mermaid-_r_384_ .marker{fill:none!important;stroke:rgb(175, 175, 175)!important;stroke-width:1;}#chatgpt-mermaid-_r_384_ [data-look=neo].labelBkg{background-color:rgba(48, 48, 48, 0.5);}#chatgpt-mermaid-_r_384_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-_r_384_ [data-look="neo"].node rect,#chatgpt-mermaid-_r_384_ [data-look="neo"].cluster rect,#chatgpt-mermaid-_r_384_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-_r_384_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_384_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-_r_384_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-_r_384_-gradient);stroke-width:1px;}#chatgpt-mermaid-_r_384_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_384_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-_r_384_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-_r_384_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_384_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-_r_384_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-_r_384_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_384_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-_r_384_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_384_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";}ft_notificationft_notification_recipientft_userft_notification_templateft_announcementft_notification_batchdelivered_toreceivesrenderspublishesdistributed_bycreates





包含 P1 扩展关系。个人通知可没有模板或公告；同一通知内容可以对应多个用户接收记录。接收人记录也允许源于某个批量分发任务。
核心关系
关系	基数	业务说明
Notification → Recipient	1:N	一条通知发给多个用户
User → Recipient	1:N	用户可以收到多条通知
Template → Notification	1:N	同一模板渲染不同通知
Announcement → Notification	0..1:0..1	公告可生成一份公共通知内容
Notification → Batch	1:N	大规模受众可拆成多批
Batch → Recipient	1:N	每批生成对应用户接收记录
一条个人交易通知通常只有一个 Recipient；一条演出取消公告可能对应数千个 Recipient。
六项硬性原则
1. 同一业务通知不能因 MQ 重复消费而产生重复消息。
2. 每个用户对同一通知只能有一条接收记录。
3. 用户消息的阅读状态必须持久化。
4. SSE 推送失败不影响已创建通知。
5. 用户不能查询、标记或隐藏其他用户的消息。
6. 批量通知中途崩溃后可以恢复，不能重复生成接收记录。

三、ft_notification：通知内容表
这张表负责保存真正的通知内容、业务来源及可跳转目标。
3.1 基础字段设计
字段	MySQL 类型	约束 / 默认值	说明
id	BIGINT	PK，NOT NULL	雪花主键
notification_no	VARCHAR(64)	NOT NULL，UNIQUE	通知业务编号
notification_type	VARCHAR(32)	NOT NULL	具体通知类型
category	VARCHAR(24)	NOT NULL	消息分类
title	VARCHAR(200)	NOT NULL	通知标题
content	TEXT	NOT NULL	通知正文
content_format	VARCHAR(16)	NOT NULL，默认 PLAIN_TEXT	正文格式
priority	VARCHAR(16)	NOT NULL，默认 NORMAL	优先级
template_id	BIGINT	NULL	来源模板（P1）
template_version	BIGINT	NULL	渲染时使用的模板版本
created_at	DATETIME(3)	NOT NULL	生成时间
3.2 业务来源与幂等字段
字段	类型	约束 / 默认值	说明
source_event_id	VARCHAR(64)	NULL	RocketMQ 业务事件 ID
dedup_key	VARCHAR(160)	NOT NULL，UNIQUE	业务通知去重键
biz_type	VARCHAR(32)	NULL	关联业务类型
biz_id	VARCHAR(64)	NULL	关联业务编号
action_type	VARCHAR(32)	NULL	目标跳转类型
action_target_id	VARCHAR(64)	NULL	目标实体 ID
payload_snapshot	JSON	NULL	必要的业务展示快照
schema_version	INT	NOT NULL，默认 1	通知数据结构版本
expires_at	DATETIME(3)	NULL	展示截止时间（可选）
3.3 为什么需要 dedup_key？
只使用 source_event_id 不一定能够识别语义上重复的通知。
例如同一笔订单的 PURCHASE_SUCCEEDED 与 ORDER_CREATED 可能来自不同事件，但我们之前已经决定只生成一条“抢票成功，请支付”的通知。
因此需要稳定的业务去重键。
业务通知	去重键的逻辑组成
抢票成功待支付	PURCHASE_SUCCESS + orderId
支付成功	PAYMENT_SUCCESS + paymentId
订单关闭	ORDER_CLOSED + orderId
退款结果	REFUND_RESULT + refundId + 结果类型
优惠券发放	COUPON_ISSUED + userCouponId
场次延期	SESSION_POSTPONED + sessionId + 变更版本
建议使用规范化字符串或其固定长度摘要构建 dedup_key，不能直接用可能过长的任意正文拼接。
source_event_id 用于追踪事件，dedup_key 用于防止重复业务通知，二者职责不同。
3.4 消息分类
category	中文	示例
TRANSACTION	交易通知	抢票、订单、支付、退款
EVENT	演出通知	延期、场次变更、取消
MARKETING	营销通知	领券成功、优惠券即将到期
SYSTEM	系统通知	账号安全、平台公告
notification_type 比 category 更具体，例如：
PURCHASE_SUCCESS、PAYMENT_SUCCESS、ORDER_CLOSED、REFUND_SUCCESS、COUPON_ISSUED、EVENT_CANCELLED。
3.5 消息优先级
优先级	说明
NORMAL	一般消息
IMPORTANT	支付成功、演出延期等
URGENT	演出取消或重大账号安全事项
优先级用于界面展示和推送策略，不影响订单、库存和支付事务。
3.6 索引设计
索引名	字段	主要用途
PRIMARY KEY	id	通知内容主键
uk_notification_no	notification_no	通知编号唯一
uk_notification_dedup	dedup_key	业务通知幂等
idx_notification_biz	(biz_type, biz_id, created_at)	关联业务通知查询
idx_notification_created	(created_at, id)	后台历史查询
source_event_id 可增加普通索引用于链路排查，但不建议全局唯一，因为一个业务事件可能合理地产生不同通知类型。
3.7 内容设计规则
建议 V1 只支持 PLAIN_TEXT，暂不开放后台任意 HTML。
同时规定：
- 通知标题、正文在创建时形成快照。
- 通知业务跳转目标使用白名单业务类型。
- 不直接保存任意未经验证的外部 URL。
- 不保存密码、Access Token、Refresh Token、验证码。
- 用户个人敏感数据不能放在多人共用的通知正文中。
- 已创建的通知不因模板修改而改变历史内容。
尤其对批量通知，应确保共享的 ft_notification.content 不包含某一个用户的私有订单金额、账号信息等内容。
四、ft_notification_recipient：通知接收记录表
这张表是用户消息中心查询、已读状态和未读计数的权威数据来源。
4.1 字段设计
字段	MySQL 类型	约束 / 默认值	说明
id	BIGINT	PK，NOT NULL	雪花主键
notification_id	BIGINT	NOT NULL	通知内容 ID
user_id	BIGINT	NOT NULL	接收用户 ID
batch_id	BIGINT	NULL	来源批量任务（P1）
read_at	DATETIME(3)	NULL	首次阅读时间
hidden_at	DATETIME(3)	NULL	用户隐藏时间（P1）
created_at	DATETIME(3)	NOT NULL	通知入箱时间
第一版不必另存 UNREAD / READ 状态字段。
直接通过：
- read_at IS NULL：未读。
- read_at IS NOT NULL：已读。
这样能够避免 status = READ 但 read_at 为空的冗余状态不一致。
4.2 为什么接收记录使用独立雪花 ID？
虽然可以把 (notification_id, user_id) 作为联合主键，但用户端会频繁根据接收记录 ID 进行查询和标记已读。
使用 id BIGINT 更方便构建 API，也便于消息列表使用游标分页。
同时增加 (notification_id, user_id) 唯一约束防重。
4.3 必须设置的索引
索引名	字段	用途
PRIMARY KEY	id	接收记录主键
uk_notification_recipient	(notification_id, user_id)	同一用户不重复接收
idx_recipient_user_list	(user_id, created_at DESC, id DESC)	我的消息游标分页
idx_recipient_user_unread	(user_id, read_at, created_at DESC, id DESC)	未读消息列表及计数
idx_recipient_batch	(batch_id, id)	批量任务排查（P1）
第一版只需主键、唯一约束及两个用户维度索引。batch_id 的索引可随批量通知功能加入。
4.4 单条已读操作
业务规则：
1. 根据当前登录用户查询接收记录。
2. 确认这条通知属于该用户。
3. 仅在 read_at IS NULL 时写入阅读时间。
4. 重复标记已读时直接返回已有结果。
5. 不重复扣减 Redis 未读数缓存。
标记已读必须是幂等操作。
两台设备同时将同一条消息标记已读，只能发生一次“未读 → 已读”的有效状态变化。
4.5 全部已读操作
这里建议定义明确的截止边界：
将用户执行操作时已经入箱的通知标记已读，而不是把未来新产生的消息也算进去。
可以采用本次请求的服务端时间或稳定的接收记录水位线。
例如用户点击“全部已读”时，系统已经有 50 条消息；操作期间又新增 2 条通知，这 2 条新通知应继续保持未读。
建议以接收记录 ID 或持久化批次水位线结合数据库查询边界实现，不能在多个分页请求中无条件反复标记当前全部消息。
4.6 隐藏与删除
P0 暂不提供物理删除。
P1 可以添加“隐藏消息”操作：
- 设置 hidden_at。
- 普通列表不再展示。
- 不删除原通知内容和接收关系。
- 隐藏未读消息时，必须明确未读数量是否计入。
推荐：隐藏的消息不再计入用户可见未读数量。
隐藏操作同样需要幂等，并在同一数据库状态口径下更新或失效 Redis 缓存。

五、两个 P0 表之间的事务设计
5.1 业务事件生成站内通知
订单 / 支付 / 退款业务事务提交
业务事实 + Outbox 同事务


RocketMQ 通知事件


Notification Consumer 的 MySQL 事务
1. 检查 Inbox 幂等记录
2. 根据业务规则计算 dedup_key
3. 创建或识别已有通知内容
4. 创建或识别已有接收人记录
5. 提交 Inbox 消费完成记录


事务提交成功
通知持久化成立 · 在线推送可以开始


当消费端提交数据库成功，但在 RocketMQ 消费 ACK 前崩溃，消息可能被再次投递。
此时：
- Inbox 可以识别重复事件。
- dedup_key 避免相同业务语义重复创建通知。
- (notification_id, user_id) 避免重复建立接收记录。
三层幂等的作用不同，不能互相替代。
5.2 SSE 推送不能纳入数据库事务
通知入库成功后才推送 SSE。
SSE 连接失败、用户离线或浏览器突然关闭时：
- 不回滚通知。
- 不重试订单支付事务。
- 不将通知状态设为已读。
- 用户下次访问通过 REST 查询未读消息。
用户真正查看后，再调用标记已读接口。
六、P1：ft_notification_template 通知模板表
用于运营管理不同通知类型的展示内容，避免所有通知模板硬编码在 Java 业务流程里。
6.1 字段设计
字段	MySQL 类型	约束 / 默认值	说明
id	BIGINT	PK	模板 ID
template_code	VARCHAR(64)	NOT NULL，UNIQUE	稳定模板标识
name	VARCHAR(100)	NOT NULL	模板名称
category	VARCHAR(24)	NOT NULL	通知类别
title_template	VARCHAR(300)	NOT NULL	标题模板
content_template	TEXT	NOT NULL	正文模板
variables_schema	JSON	NOT NULL	允许填充的变量定义
status	VARCHAR(16)	NOT NULL，默认 DRAFT	草稿 / 已发布 / 停用
published_version	BIGINT	NOT NULL，默认 0	当前发布版本
created_by	BIGINT	NOT NULL	创建人员
updated_by	BIGINT	NOT NULL	修改人员
created_at	DATETIME(3)	NOT NULL	创建时间
updated_at	DATETIME(3)	NOT NULL	更新时间
version	BIGINT	NOT NULL，默认 0	乐观锁版本
6.2 模板版本规则
- template_code 创建后保持稳定。
- 未发布草稿允许编辑。
- 生产消息只使用已发布版本。
- 消息生成后正文不可因模板变更而改变。
- 模板变量必须符合 variables_schema。
- 已经引用的版本不得直接修改历史内容。
如果要求支持真正的历史模板版本查看和回滚，P1 的完整实现应增加 ft_notification_template_version 子表，或者采用不可变版本实体设计。
单纯保存一个 published_version 数字，并不能重建旧版本模板内容。因此本次先将模板历史版本表列为按需扩展，不在核心五张表里重复计数。
6.3 索引
索引	字段
PK	id
UNIQUE	template_code
INDEX	(category, status, id)

七、P1：ft_announcement 系统公告表
系统公告与普通个人通知不同。
它有发布、展示、撤回和受众范围等运营概念。
7.1 字段设计
字段	MySQL 类型	约束 / 默认值	说明
id	BIGINT	PK	公告 ID
announcement_no	VARCHAR(64)	NOT NULL，UNIQUE	公告编号
title	VARCHAR(200)	NOT NULL	公告标题
content	TEXT	NOT NULL	公告正文
audience_type	VARCHAR(32)	NOT NULL	受众类型
audience_ref_id	BIGINT	NULL	活动或场次 ID
status	VARCHAR(24)	NOT NULL，默认 DRAFT	公告状态
publish_at	DATETIME(3)	NULL	发布时间
expire_at	DATETIME(3)	NULL	展示截止时间
notification_id	BIGINT	NULL，UNIQUE	关联已生成的通知
created_by	BIGINT	NOT NULL	创建人
approved_by	BIGINT	NULL	审核人
approved_at	DATETIME(3)	NULL	审核时间
recalled_at	DATETIME(3)	NULL	撤回时间
version	BIGINT	NOT NULL，默认 0	并发版本
created_at	DATETIME(3)	NOT NULL	创建时间
updated_at	DATETIME(3)	NOT NULL	更新时间
7.2 受众类型
audience_type	含义
ALL	全平台用户
EVENT_PURCHASERS	指定演出有效购票用户
SESSION_PURCHASERS	指定场次有效购票用户
EVENT_FOLLOWERS	关注某活动的用户（未来扩展）
第一版优先支持 ALL、EVENT_PURCHASERS 和 SESSION_PURCHASERS。
7.3 公告状态
DRAFT → PENDING_REVIEW → PUBLISHED → EXPIRED
并允许已发布公告因合法运营操作进入 RECALLED。
需要区分：
- 公告已过期：不再对新访问用户展示。
- 公告已撤回：运营主动撤销其展示。
- 已经送达的历史消息：是否隐藏或保留，需要按公告撤回规则处理。
不能直接物理删除已经发送给用户的重要演出变更通知。
7.4 索引
索引	字段
PK	id
UNIQUE	announcement_no
UNIQUE	notification_id
INDEX	(status, publish_at, id)
INDEX	(audience_type, audience_ref_id, id)
7.5 审核规则
涉及演出取消或延期的重要公告，应该关联相应的真实业务操作与有效业务版本。
管理员不得仅通过创建一条公告就改变场次状态或自动取消订单。
公告是通知事实，不代替演出、订单、退款的业务操作。
八、P1：ft_notification_batch 批量分发任务表
这张表是整个通知模块最值得深入研究的高并发数据模型。
例如某场演出取消，需要通知 20,000 名已支付用户。
系统不能直接在订单取消事务中插入 20,000 条接收人记录，也不能只向 Redis Pub/Sub 广播一次。
8.1 字段设计
字段	MySQL 类型	约束 / 默认值	说明
id	BIGINT	PK	批次任务 ID
batch_no	VARCHAR(64)	NOT NULL，UNIQUE	批次业务编号
notification_id	BIGINT	NOT NULL	要分发的通知内容
audience_type	VARCHAR(32)	NOT NULL	受众来源类型
audience_ref_id	BIGINT	NULL	活动或场次 ID
snapshot_watermark	VARCHAR(128)	NULL	受众快照或稳定查询水位线
status	VARCHAR(24)	NOT NULL，默认 PENDING	任务状态
last_processed_user_id	BIGINT	NULL	当前扫描游标
expected_count	BIGINT	NULL	预计接收人数
processed_count	BIGINT	NOT NULL，默认 0	已处理人数
inserted_count	BIGINT	NOT NULL，默认 0	实际新增接收记录数量
attempt_count	INT	NOT NULL，默认 0	执行尝试次数
lease_owner	VARCHAR(64)	NULL	当前任务处理者
lease_until	DATETIME(3)	NULL	租约过期时间
next_retry_at	DATETIME(3)	NULL	下次重试时间
last_error_code	VARCHAR(64)	NULL	最近异常码
started_at	DATETIME(3)	NULL	开始处理时间
finished_at	DATETIME(3)	NULL	完成时间
created_at	DATETIME(3)	NOT NULL	创建时间
updated_at	DATETIME(3)	NOT NULL	更新时间
version	BIGINT	NOT NULL，默认 0	任务并发版本
8.2 状态机
PENDING
任务待执行


RUNNING
分批生成接收人记录


SUCCEEDED
接收人分发完成


RETRY_WAIT
等待恢复重试



无法自动修复时进入 FAILED / MANUAL_REVIEW。执行成功表示分发完成，不代表所有用户已阅读消息。
8.3 核心索引
索引名	字段	用途
PK	id	任务主键
UNIQUE	batch_no	批次幂等
INDEX	(status, next_retry_at, id)	可执行任务扫描
INDEX	(status, lease_until, id)	超时租约恢复
INDEX	(notification_id, id)	通知批次查询
8.4 受众快照与稳定分页
这部分有一个容易被忽视的问题：
假设任务在 10:00 创建，需要通知当时所有已支付用户，但在任务扫描过程中，有新的用户继续支付成功。
如果只根据当前订单实时分页查询，就可能不断有新的用户进入受众范围，甚至产生漏发或重复。
最终推荐：以明确业务截止时间和稳定用户集合定义受众。
对于需要严格复现“当时应该通知谁”的重要公告，建议新增持久化受众快照明细表 ft_notification_batch_target，记录：
- batch_id
- user_id
- status
- created_at
并设置 UNIQUE(batch_id, user_id)。
这张表属于 P1 严格受众快照增强，不强制第一版就实现。若不引入它，则需证明使用的查询水位线和筛选条件在重试期间能够得到稳定结果；单纯保存 last_processed_user_id 不足以解决受众资格后来变化的问题。
8.5 批量执行事务
每批建议按照可调数量处理，例如 100–500 个接收人，实际值由压测决定。
单批事务应：
1. 取得当前批次合法处理权。
2. 获取稳定的本批受众。
3. 批量插入接收记录，依赖唯一约束去重。
4. 更新任务处理游标及真实插入数量。
5. 提交事务。
游标推进与本批接收人写入必须在同一个数据库事务中提交。
这样即使应用在事务提交前崩溃，也不会发生“进度已经前进，但这一页接收人尚未创建”的漏发。
九、跨表索引与外键策略
9.1 全局索引汇总
表	主键	唯一约束	核心查询索引
ft_notification	id	notification_no、dedup_key	(biz_type, biz_id, created_at)
ft_notification_recipient	id	(notification_id, user_id)	(user_id, created_at DESC, id DESC)、未读索引
ft_notification_template	id	template_code	(category, status, id)
ft_announcement	id	announcement_no、notification_id	(status, publish_at, id)
ft_notification_batch	id	batch_no	(status, next_retry_at, id)
9.2 外键选择
关联	推荐方式	理由
Recipient → Notification	物理外键 RESTRICT	防止无效通知关联
Recipient → User	逻辑关联	用户注销、匿名化策略
Notification → Template	逻辑关联	模板版本变化不影响旧通知
Announcement → Notification	逻辑关联 + 唯一约束	公告可能尚未正式发布
Batch → Notification	物理外键 RESTRICT	批次必须对应持久化通知
BatchTarget → Batch	物理外键（P1 增强）	明确快照归属
MySQL InnoDB 外键会参与约束与索引维护，因此高频接收记录表的外键数量应有明确取舍。对于已选用的外键，不允许级联删除重要消息历史。
9.3 CHECK 约束
建议检查：
- 模板版本非负。
- 消息 Schema 版本大于零。
- 公告展示截止时间晚于发布时间。
- processed_count >= 0。
- inserted_count >= 0。
- inserted_count <= processed_count。
- 批次任务租约、重试次数和版本非负。
- 已结束的批次记录完成时间。
对于通知接收人与业务订单的归属一致性，属于跨表授权与事务约束，不能用普通单表 CHECK 完成。
MySQL 8.4 唯一索引允许可空列存在多个 NULL，因此 ft_announcement.notification_id 可为空且保持非空值唯一。

MySQL




十、Redis 未读缓存设计
沿用前面确定的 Redis 7.4 + Lettuce + Caffeine。
建议：
Redis Key	类型	用途
ft:notification:unread:{userId}	String	可见未读数量缓存
ft:notification:unread:version:{userId}	String	缓存版本辅助（按需）
ft:notification:template:{code}	String	已发布模板缓存
ft:notification:announcement:{id}	String	公告内容缓存
未读数为什么不能只用 Redis INCR / DECR？
考虑如下并发：
- 设备 A 将一条通知标记已读。
- 设备 B 同时也标记同一条已读。
- 两者如果都无条件 DECR，未读数就可能减少两次。
推荐以 MySQL 条件更新实际影响行数作为是否发生已读转换的依据。
缓存处理策略第一版选择：提交 MySQL 事务后删除对应用户未读数缓存，下次读取从 MySQL 重新计算并缓存。
这比直接在多个异步事件中增减 Redis 计数更容易保证正确性。
不过简单的“读数据库后回填 Redis”仍可能和其他线程的失效操作发生竞态。因此第一版应使用短 TTL，并在需要严格准确的页面或异常情况下重新查询 MySQL；后续再引入版本校验方案防止陈旧回填。
多设备一致性
一条消息在电脑端被标记已读后：
- MySQL read_at 发生变更。
- 当前用户未读数缓存失效。
- SSE 向在线会话推送“未读数已变化”的提示。
- 手机或其他浏览器重新查询权威未读数量。
SSE 提示是最佳努力，不参与 MySQL 事务，也不代替数据库已读状态。
十一、消息可靠性与故障恢复
11.1 不同故障的处理
故障	风险	处理方式
RocketMQ 重复消费	重复创建通知	Inbox + dedup_key + Recipient 唯一约束
通知入库后 ACK 丢失	消息重新投递	根据已有通知幂等成功
MySQL 临时不可用	无法保存通知	MQ 重试
通知创建成功，SSE 失败	用户没有实时收到	REST 补查
用户同时在多设备标记已读	未读数重复减少	数据库条件更新
Redis 未读缓存丢失	未读徽标暂时不准确	MySQL 重新计算
批量分发事务回滚	部分接收人未入库	原游标重试
批量任务租约过期	两个工作者可能竞争	处理令牌与状态条件更新
公告发布后系统崩溃	部分用户未收到	持久化任务恢复
模板被修改	历史通知内容变化风险	通知保存最终渲染快照
11.2 通知的业务交付保证
第一版建议明确三层保证：
层次	能提供的保证
业务事件	Outbox 持久化，失败可重发
站内通知	幂等落库，离线可查询
SSE 实时推送	尽力推送，允许丢失和重复，查询兜底
不能承诺浏览器一定收到每一个 SSE 事件，也不能把“消息写入用户信箱”描述为“用户已阅读”。
11.3 批量通知的资源隔离
系统通知消费者和批量公告分发任务应该限制自己的并发度。
不能因为某场演出取消要通知十万人，就占满全部 HikariCP 连接，导致支付确认或订单关单无法执行。
建议：
- 批量任务使用独立的有界执行器。
- 限制单次批量写入规模。
- 使用任务租约避免多个实例重复执行同一批。
- 依据 MySQL 负载动态减小分发速率。
- 高峰抢票期间可以降低普通营销通知优先级。
其中交易状态通知可以延迟，但订单自身状态和支付结果必须保持权威可查询。
十二、数据库验收标准
通知与消息域验收清单
0/16 项已检查



通知幂等0/4[ ] 同一 RocketMQ 事件重复投递不会重复创建通知
[ ] 同一业务语义产生重复事件时 dedup_key 能阻止重复通知
[ ] 同一用户对一份通知最多拥有一条接收记录
[ ] Inbox 完成记录和通知写入在同一事务提交



已读与用户隔离0/4

批量通知0/4

实时通知及审计0/4
上述项目用于后续数据库集成、消息重复消费和故障恢复测试，当前不表示已经通过。
十三、第九批数据库设计定稿
系统通知与消息中心域 V1.0


P0 两张表： ft_notification、ft_notification_recipient。
P1 三张主扩展表： ft_notification_template、ft_announcement、ft_notification_batch。
可选增强： 不可变模板版本表、批量通知受众快照表。
通知幂等： dedup_key + (notification_id,user_id) 唯一约束 + Inbox。
已读模型： 使用 read_at，不重复存储阅读状态枚举。
批量分发： 稳定受众快照、任务租约、分批写入、持久化游标。
实时推送： SSE 只负责在线提示，MySQL 才是消息记录权威来源。
缓存： Redis 缓存未读数量，失败或丢失时从 MySQL 重建。
性能隔离： 通知和批量公告限制处理并发，不阻塞抢票、支付与库存交易。

前九批数据库设计进度
批次	业务域	P0 表数
01	用户与权限	6
02	演出与场次	7
03	库存与预占	3
04	抢票与排队	2
05	订单状态机	3
06	模拟支付与退款	5
07	优惠券与营销	8
08	风控与防刷	3
09	通知与消息	2
合计	前九批	39
以上统计反映目前的详细设计，尚未包括运营统计、对账补偿和共享 Outbox/Inbox 表。第七批已将优惠券发行流水提升为 P0，因此会在最终全局表清单中统一校正早期估算。


FlashTicket 数据库详细设计 10：运营数据统计与系统对账域
Analytics & Reconciliation · MySQL 8.4 LTS · 详细表结构 V1.0
本轮进入第十批数据库详细设计，也是 FlashTicket 十大业务域的最后一批。
这一批要解决两个不同的问题：
- 运营统计：订单量、抢票成功率、演出销量、模拟支付收入、退款金额等数据如何高效查询。
- 系统对账：订单、支付、库存、退款出现不一致时，如何发现问题、保存证据并安全补偿。
继续沿用已确定的 MySQL 8.4、MyBatis-Plus、RocketMQ Outbox/Inbox、Spring Scheduling 和 Redis/Caffeine 技术栈。
本轮只设计表结构、字段、索引、约束、事务和关联关系，不写 DDL 或 Java 代码。
一、本批数据库表清单
建议采用 5 张 P0 核心表 + 6 张 P1 扩展表。
表名	中文名称	优先级
ft_stats_platform_daily	平台每日统计表	P0
ft_stats_event_daily	演出每日统计表	P0
ft_reconcile_job	对账任务表	P0
ft_reconcile_exception	对账异常表	P0
ft_compensation_task	补偿任务表	P0
ft_stats_tier_hourly	票档小时统计表	P1
ft_stats_coupon_daily	优惠券每日统计表	P1
ft_stats_aggregation_job	统计聚合任务表	P1
ft_stats_processed_event	增量统计事件去重表	P1
ft_reconcile_batch	对账分批进度表	P1
ft_reconcile_action	对账处理审计表	P1
2
P0 统计表


3
P0 对账与补偿表


6
P1 扩展表



重要设计决定： MySQL 原始交易记录是权威数据，统计汇总表是可重建的派生数据。对账表负责发现、记录和处理异常，不能直接取代原始库存或资金流水。

二、核心 ER 关系
#chatgpt-mermaid-_r_3ca_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-_r_3ca_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-_r_3ca_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-_r_3ca_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-_r_3ca_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-_r_3ca_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-_r_3ca_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-_r_3ca_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-_r_3ca_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-_r_3ca_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-_r_3ca_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-_r_3ca_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-_r_3ca_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-_r_3ca_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";font-size:16px;}#chatgpt-mermaid-_r_3ca_ p{margin:0;}#chatgpt-mermaid-_r_3ca_ .entityBox{fill:rgb(19, 33, 54);stroke:rgb(31, 78, 148);}#chatgpt-mermaid-_r_3ca_ .relationshipLabelBox{fill:rgb(48, 48, 48);opacity:0.7;background-color:rgb(48, 48, 48);}#chatgpt-mermaid-_r_3ca_ .relationshipLabelBox rect{opacity:0.5;}#chatgpt-mermaid-_r_3ca_ .labelBkg{background-color:rgba(48, 48, 48, 0.5);}#chatgpt-mermaid-_r_3ca_ .edgeLabel{background-color:rgb(14, 14, 14);}#chatgpt-mermaid-_r_3ca_ .edgeLabel .label rect{fill:rgb(14, 14, 14);}#chatgpt-mermaid-_r_3ca_ .edgeLabel .label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-_r_3ca_ .edgeLabel .label{fill:rgb(31, 78, 148);font-size:14px;}#chatgpt-mermaid-_r_3ca_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";color:rgb(237, 237, 237);}#chatgpt-mermaid-_r_3ca_ .edge-pattern-dashed{stroke-dasharray:8,8;}#chatgpt-mermaid-_r_3ca_ .node rect,#chatgpt-mermaid-_r_3ca_ .node circle,#chatgpt-mermaid-_r_3ca_ .node ellipse,#chatgpt-mermaid-_r_3ca_ .node polygon{fill:rgb(19, 33, 54);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-_r_3ca_ .relationshipLine{stroke:rgb(175, 175, 175);stroke-width:1px;fill:none;}#chatgpt-mermaid-_r_3ca_ .marker{fill:none!important;stroke:rgb(175, 175, 175)!important;stroke-width:1;}#chatgpt-mermaid-_r_3ca_ [data-look=neo].labelBkg{background-color:rgba(48, 48, 48, 0.5);}#chatgpt-mermaid-_r_3ca_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-_r_3ca_ [data-look="neo"].node rect,#chatgpt-mermaid-_r_3ca_ [data-look="neo"].cluster rect,#chatgpt-mermaid-_r_3ca_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-_r_3ca_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_3ca_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-_r_3ca_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-_r_3ca_-gradient);stroke-width:1px;}#chatgpt-mermaid-_r_3ca_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_3ca_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-_r_3ca_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-_r_3ca_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_3ca_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-_r_3ca_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-_r_3ca_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_3ca_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-_r_3ca_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_3ca_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";}ft_eventft_stats_event_dailyft_reconcile_jobft_reconcile_exceptionft_reconcile_batchft_compensation_taskft_reconcile_actionft_coupon_templateft_stats_coupon_dailyft_ticket_tierft_stats_tier_hourlyft_stats_aggregation_jobft_stats_processed_eventsummarized_bydetectscontainsrequiresaudited_byrecorded_bysummarized_bysummarized_bytracks





这是逻辑 ER 图。平台每日统计是全局聚合，不直接隶属于单个 Event；补偿任务允许关联某个异常，也允许由超时扫描等恢复流程直接创建。
数据一致性等级
数据类别	一致性要求	允许延迟
订单、支付、退款	交易级正确性	不允许出现非法已提交状态
库存、预占、流水	账目守恒	不允许无依据超卖或重复释放
Outbox、补偿任务	可靠持久化、幂等恢复	允许异步处理延迟
对账检查结果	基于明确截止水位线	允许定时检查
运营统计	最终一致、允许重算	目标秒级至分钟级
不能简单要求运营看板和核心交易表在同一毫秒内完全一致。

三、ft_stats_platform_daily：平台每日统计表
负责平台级运营看板，例如每日支付订单数、售票数量和模拟交易收入。
3.1 字段设计
字段	MySQL 类型	约束 / 默认值	说明
stat_date	DATE	PK，NOT NULL	统计日期
purchase_accepted_count	BIGINT	NOT NULL，默认 0	当日受理抢票请求数
purchase_succeeded_count	BIGINT	NOT NULL，默认 0	当日成功抢票请求数
purchase_failed_count	BIGINT	NOT NULL，默认 0	当日失败请求数
order_created_count	BIGINT	NOT NULL，默认 0	当日创建订单数
order_paid_count	BIGINT	NOT NULL，默认 0	当日确认支付订单数
ticket_paid_quantity	BIGINT	NOT NULL，默认 0	当日支付确认票数
order_closed_count	BIGINT	NOT NULL，默认 0	当日关闭订单数
original_amount_cent	BIGINT	NOT NULL，默认 0	支付订单原价金额合计
discount_amount_cent	BIGINT	NOT NULL，默认 0	已支付订单优惠金额合计
payment_amount_cent	BIGINT	NOT NULL，默认 0	模拟成功支付金额
refund_succeeded_count	BIGINT	NOT NULL，默认 0	成功退款笔数
refund_amount_cent	BIGINT	NOT NULL，默认 0	成功退款金额
updated_at	DATETIME(3)	NOT NULL	最近统计更新时间
calculated_at	DATETIME(3)	NOT NULL	最近完成计算时间
revision	BIGINT	NOT NULL，默认 0	统计修订版本
3.2 为什么不用雪花主键？
因为一天只应该有一条平台每日统计。
直接采用：
PRIMARY KEY(stat_date)
更加符合业务唯一性，也便于按日期范围查询。
3.3 统一统计口径
这是必须写清楚的细节。
指标	归属日期
抢票受理数	ft_purchase_request.created_at 所属日期
抢票成功数	ft_purchase_request.completed_at 所属日期
订单创建数	ft_order.created_at 所属日期
订单支付数	ft_order.paid_at 所属日期
订单关闭数	ft_order.closed_at 所属日期
退款成功数	ft_refund.succeeded_at 所属日期
模拟实收金额	支付确认成功日期
模拟退款金额	退款最终确认成功日期
统一采用 UTC 日期作为底层统计分桶；管理端按北京时间展示每日运营数据时，应显式采用 Asia/Shanghai 的业务日重新聚合，不能把 UTC 日期直接改一个标签就叫北京时间日报。
对于中国大陆票务平台，更推荐最终按配置的业务时区生成运营日统计。正式 DDL 可将 stat_date 明确约定为 Asia/Shanghai 业务日期，以上时间戳只用于确定各指标的事实发生时刻。
3.4 核心索引
第一版仅需主键 stat_date。
日期范围查询可直接使用主键范围扫描，不需要再为同一日期字段建普通索引。
3.5 数据约束
- 所有数量和金额非负。
- revision >= 0。
- 同一日期只能有一条有效统计。
- 允许对尚未结算稳定的历史日期重算并修订。
- 统计表禁止被订单、支付等核心模块用来判断业务状态。
对于这些非负约束，MySQL 8.4 可以通过本行 CHECK 实现。

MySQL



四、ft_stats_event_daily：演出每日统计表
负责统计某个演出活动在某个业务日期的表现。
4.1 字段设计
字段	类型	约束 / 默认值	说明
event_id	BIGINT	联合 PK	演出活动 ID
stat_date	DATE	联合 PK	业务日期
purchase_accepted_count	BIGINT	NOT NULL，默认 0	抢票受理数
purchase_succeeded_count	BIGINT	NOT NULL，默认 0	抢票成功数
order_created_count	BIGINT	NOT NULL，默认 0	订单创建数
order_paid_count	BIGINT	NOT NULL，默认 0	支付订单数
ticket_paid_quantity	BIGINT	NOT NULL，默认 0	已确认支付票数
order_closed_count	BIGINT	NOT NULL，默认 0	关闭订单数
payment_amount_cent	BIGINT	NOT NULL，默认 0	模拟实收金额
discount_amount_cent	BIGINT	NOT NULL，默认 0	优惠金额
refund_amount_cent	BIGINT	NOT NULL，默认 0	成功退款金额
refund_succeeded_count	BIGINT	NOT NULL，默认 0	成功退款笔数
calculated_at	DATETIME(3)	NOT NULL	计算完成时间
updated_at	DATETIME(3)	NOT NULL	更新时间
revision	BIGINT	NOT NULL，默认 0	修订版本
4.2 主键与索引
采用：
PRIMARY KEY(event_id, stat_date)
另外根据后台报表查询需求增加：
INDEX(stat_date, payment_amount_cent, event_id)
这个二级索引主要用于按某一天或日期范围查找演出统计并辅助排序。是否保留金额列，要通过实际 SQL 和数据量验证。
4.3 与平台汇总的关系
同一业务日期：
\[
平台已支付订单数=\sum 演出已支付订单数
\]

前提是所有订单都能唯一归属一个活动、统计口径一致，并且两份汇总处于相同计算水位线。
不建议以这一等式的瞬时不成立就立即触发严重账务告警。 增量更新期间两张汇总表可能暂时不一致，应根据统计任务的稳定检查点进行比较。
4.4 演出取消与退款
演出取消后，退款可能在后续几天陆续完成。
因此，退款金额应计入退款真实成功的日期，而不是一律追溯到原订单支付日期。
这样运营看板能反映每天实际发生的模拟退款量。

五、ft_reconcile_job：对账任务表
这张表是整个对账系统的入口，用于管理手动、定时、故障恢复触发的对账任务。
5.1 字段设计
字段	类型	约束 / 默认值	说明
id	BIGINT	PK	雪花主键
job_no	VARCHAR(64)	NOT NULL，UNIQUE	对账任务业务编号
reconcile_type	VARCHAR(32)	NOT NULL	对账类型
scope_type	VARCHAR(24)	NOT NULL	对账范围类型
scope_id	BIGINT	NULL	票档、活动或其他业务 ID
window_start_at	DATETIME(3)	NOT NULL	检查范围开始时间
window_end_at	DATETIME(3)	NOT NULL	检查范围结束时间
snapshot_cutoff_at	DATETIME(3)	NOT NULL	本次权威数据截止水位线
trigger_type	VARCHAR(24)	NOT NULL	SCHEDULED / MANUAL / RECOVERY
status	VARCHAR(24)	NOT NULL，默认 PENDING	任务状态
progress_cursor	VARCHAR(128)	NULL	已处理的稳定游标
checked_count	BIGINT	NOT NULL，默认 0	已检查对象数量
difference_count	BIGINT	NOT NULL，默认 0	发现的差异数量
attempt_count	INT	NOT NULL，默认 0	执行尝试次数
lease_owner	VARCHAR(64)	NULL	任务领取令牌
lease_until	DATETIME(3)	NULL	处理租约截止时间
next_retry_at	DATETIME(3)	NULL	下次重试时间
started_at	DATETIME(3)	NULL	开始时间
finished_at	DATETIME(3)	NULL	完成时间
last_error_code	VARCHAR(64)	NULL	最近执行异常
created_by	BIGINT	NULL	手工创建人
created_at	DATETIME(3)	NOT NULL	创建时间
updated_at	DATETIME(3)	NOT NULL	更新时间
version	BIGINT	NOT NULL，默认 0	并发版本
5.2 对账类型
reconcile_type	对账内容
INVENTORY	库存总账、预占、已售与流水
ORDER	抢票请求、订单、库存关联
PAYMENT	订单、支付结果和金额
REFUND	支付、退款、权益
COUPON	券额度、用户券、锁券核销
OUTBOX	待发送消息与异常状态
PURCHASE_RECOVERY	抢票处理中与恢复状态
5.3 对账任务状态机
PENDING
等待调度


RUNNING
持有租约，按批次检查


SUCCEEDED
检查完整执行


RETRY_WAIT
等待重试



重试仍失败后进入 FAILED。任务 SUCCEEDED 只表示完成检查，不代表 difference_count 为零。
5.4 关键约束与索引
索引	字段	用途
PK	id	主键
UNIQUE	job_no	任务幂等
INDEX	(status, next_retry_at, id)	待运行任务扫描
INDEX	(status, lease_until, id)	租约超时恢复
INDEX	(reconcile_type, created_at, id)	管理端任务列表
需要验证：
- window_start_at < window_end_at。
- snapshot_cutoff_at 与业务检查范围匹配。
- checked_count >= 0、difference_count >= 0。
- version >= 0。
5.5 多实例并发领取
即使 FlashTicket 是单体，后续也可能运行多个相同的应用实例。
建议使用 MySQL 任务租约控制执行权。
MySQL InnoDB 支持 FOR UPDATE SKIP LOCKED，适合多个任务执行器并发领取队列式记录，但它不适合普通业务查询的一致性判断。

MySQL



租约领取必须通过事务条件更新取得执行权，不能仅根据 lease_until 查询结果自行假设已经拥有任务。
六、ft_reconcile_exception：对账异常表
这张表记录“哪个业务对象违反了什么一致性规则，以及有哪些证据”。
6.1 字段设计
字段	类型	约束 / 默认值	说明
id	BIGINT	PK	雪花主键
exception_no	VARCHAR(64)	NOT NULL，UNIQUE	异常业务编号
job_id	BIGINT	NOT NULL	首次发现的对账任务
reconcile_type	VARCHAR(32)	NOT NULL	对账领域
exception_code	VARCHAR(64)	NOT NULL	差异类型
severity	VARCHAR(16)	NOT NULL	严重程度
biz_type	VARCHAR(32)	NOT NULL	异常对象类型
biz_id	VARCHAR(64)	NOT NULL	异常对象编号
dedup_key	VARCHAR(160)	NOT NULL	相同未解决异常归并键
expected_snapshot	JSON	NULL	预期账本或约束信息
actual_snapshot	JSON	NULL	实际检查结果
evidence_snapshot	JSON	NULL	相关版本、操作与流水证据
status	VARCHAR(24)	NOT NULL，默认 OPEN	异常处理状态
resolution_type	VARCHAR(32)	NULL	解决方式
resolution_note	VARCHAR(1000)	NULL	解决说明
assigned_to	BIGINT	NULL	处理人员
first_detected_at	DATETIME(3)	NOT NULL	首次发现时间
last_detected_at	DATETIME(3)	NOT NULL	最后发现时间
detected_count	INT	NOT NULL，默认 1	被重复发现次数
resolved_at	DATETIME(3)	NULL	解决时间
closed_at	DATETIME(3)	NULL	工单关闭时间
created_at	DATETIME(3)	NOT NULL	创建时间
updated_at	DATETIME(3)	NOT NULL	更新时间
version	BIGINT	NOT NULL，默认 0	并发版本
6.2 异常等级
等级	典型场景
LOW	一般统计延迟
MEDIUM	可确认的通知或 Outbox 异常
HIGH	正式预占和库存总账不一致
CRITICAL	支付金额不一致、疑似超卖或重复退款
6.3 异常状态
OPEN → INVESTIGATING → RESOLVED → CLOSED
若确定为误报：
- 保留原异常。
- 将 resolution_type 记录为 FALSE_POSITIVE。
- 保存复核依据。
- 通过合法状态流转完成关闭。
6.4 去重设计
例如 ft_inventory 中票档 20001 的预占数量与正式预占明细不一致，连续三次对账都发现。
不应生成三张互不关联的未解决异常工单。
建议使用稳定的：
dedup_key = 对账域 + 异常类型 + 业务对象
但同一异常在处理完成后，未来可能再次发生。因此：
不直接给 dedup_key 加永久全局唯一约束。
正式 DDL 可增加一个可空的 active_dedup_key，仅对尚未解决的异常生成值，并建立唯一索引；关闭后将该字段清空，允许未来同类问题产生新工单。
这个方案还需要将状态转移与去重键修改放在同一个事务中。
6.5 索引
索引	字段	用途
PK	id	主键
UNIQUE	exception_no	异常编号
UNIQUE（条件模拟）	active_dedup_key	防止重复未关闭异常
INDEX	(job_id, id)	某次任务差异
INDEX	(status, severity, created_at, id)	异常工作台
INDEX	(biz_type, biz_id, id)	业务对象排查
INDEX	(assigned_to, status, id)	我的待处理异常
6.6 异常证据设计
expected_snapshot、actual_snapshot 并不意味着把整张订单或支付表序列化进去。
只保存最小必要证据，例如：
- 库存总账 version。
- 对应预占及订单 ID。
- 应有与实际数量。
- 对应支付或退款编号。
- 相关操作 ID。
- 发现异常的检查时间、水位线和规则版本。
涉及用户身份和支付数据时需脱敏，不能在后台异常表中无限制复制个人资料。

七、ft_compensation_task：补偿任务表
这是本批最有 Java 后端面试价值的一张表。
它负责在不破坏原始账本的前提下，执行已确认安全的补偿操作。
7.1 字段设计
字段	类型	约束 / 默认值	说明
id	BIGINT	PK	雪花主键
task_no	VARCHAR(64)	NOT NULL，UNIQUE	补偿任务编号
operation_id	VARCHAR(64)	NOT NULL，UNIQUE	最终业务幂等操作号
exception_id	BIGINT	NULL	关联的异常工单
compensation_type	VARCHAR(32)	NOT NULL	补偿类型
biz_type	VARCHAR(32)	NOT NULL	业务实体类型
biz_id	VARCHAR(64)	NOT NULL	目标业务 ID
payload	JSON	NOT NULL	经过校验的补偿参数
status	VARCHAR(24)	NOT NULL，默认 PENDING	任务状态
risk_level	VARCHAR(16)	NOT NULL	补偿风险级别
approval_status	VARCHAR(24)	NOT NULL，默认 NOT_REQUIRED	审批状态
approved_by	BIGINT	NULL	审批人
approved_at	DATETIME(3)	NULL	审批时间
attempt_count	INT	NOT NULL，默认 0	已执行次数
max_attempts	INT	NOT NULL，默认 5	最大自动尝试次数
next_retry_at	DATETIME(3)	NULL	下一次允许执行时间
lease_owner	VARCHAR(64)	NULL	执行者令牌
lease_until	DATETIME(3)	NULL	租约截止时间
last_error_code	VARCHAR(64)	NULL	最近异常
result_snapshot	JSON	NULL	执行结果摘要
created_by	BIGINT	NULL	创建人员或系统
started_at	DATETIME(3)	NULL	最近执行开始时间
finished_at	DATETIME(3)	NULL	完成时间
created_at	DATETIME(3)	NOT NULL	创建时间
updated_at	DATETIME(3)	NOT NULL	更新时间
version	BIGINT	NOT NULL，默认 0	并发版本
7.2 补偿类型
类型	处理目标	自动化等级
OUTBOX_REPUBLISH	重新发布未确认的业务事件	L1
PURCHASE_RECOVER	恢复中断抢票请求	L2
RESERVATION_RELEASE	释放已确认可安全释放的预占	L2
ORDER_TIMEOUT_CLOSE	补关到期订单	L2
COUPON_HOLD_RELEASE	恢复异常锁定优惠券	L2
COUPON_CLAIM_RECOVER	恢复领券请求	L2
REFUND_RESULT_RECOVER	查询并恢复模拟退款结果	L2
INVENTORY_ADJUST	带依据的人工库存调整	L3
PAYMENT_DISCREPANCY	支付账务差异处理	L3
L1 可以确认幂等后直接重试；L2 必须重查业务状态；L3 必须经过人工复核和审批。
7.3 补偿状态机
PENDING
待处理


RUNNING
执行者取得租约


SUCCEEDED
安全完成


RETRY_WAIT
等待下一次重试



MANUAL_REVIEW
无法安全自动处理，等待人工复核

明确失败且不可重试可进入 FAILED。审批状态独立于执行状态，高风险操作不能仅凭 PENDING 就开始运行。
7.4 关键约束
- operation_id 全局唯一。
- attempt_count >= 0。
- max_attempts > 0。
- attempt_count <= max_attempts，人工授权的新执行周期需明确重新配置。
- SUCCEEDED 时必须保存完成时间。
- 高风险操作在审批通过前不得执行。
- 创建者和审批者不能是同一个人。
- 执行补偿时仍要重新验证当前业务状态，不只相信创建任务时的 JSON。
7.5 索引
索引	字段
PK	id
UNIQUE	task_no
UNIQUE	operation_id
INDEX	(status, next_retry_at, id)
INDEX	(status, lease_until, id)
INDEX	(exception_id, id)
INDEX	(biz_type, biz_id, id)
operation_id 的唯一性是补偿任务层面的第一道防重，真正更新订单、库存或优惠券时，还应使用各领域已有的幂等操作键和状态条件更新。
八、三张 P0 对账与补偿表的事务关系
#chatgpt-mermaid-_r_3dl_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-_r_3dl_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-_r_3dl_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-_r_3dl_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-_r_3dl_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-_r_3dl_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-_r_3dl_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-_r_3dl_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-_r_3dl_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-_r_3dl_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-_r_3dl_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-_r_3dl_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-_r_3dl_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-_r_3dl_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";font-size:16px;}#chatgpt-mermaid-_r_3dl_ p{margin:0;}#chatgpt-mermaid-_r_3dl_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";color:rgb(237, 237, 237);}#chatgpt-mermaid-_r_3dl_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-_r_3dl_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-_r_3dl_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-_r_3dl_ .label text,#chatgpt-mermaid-_r_3dl_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-_r_3dl_ .node rect,#chatgpt-mermaid-_r_3dl_ .node circle,#chatgpt-mermaid-_r_3dl_ .node ellipse,#chatgpt-mermaid-_r_3dl_ .node polygon,#chatgpt-mermaid-_r_3dl_ .node path{fill:rgb(19, 33, 54);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-_r_3dl_ .rough-node .label text,#chatgpt-mermaid-_r_3dl_ .node .label text,#chatgpt-mermaid-_r_3dl_ .image-shape .label,#chatgpt-mermaid-_r_3dl_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-_r_3dl_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-_r_3dl_ .rough-node .label,#chatgpt-mermaid-_r_3dl_ .node .label,#chatgpt-mermaid-_r_3dl_ .image-shape .label,#chatgpt-mermaid-_r_3dl_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-_r_3dl_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-_r_3dl_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-_r_3dl_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-_r_3dl_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-_r_3dl_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-_r_3dl_ .edgeLabel{background-color:rgb(14, 14, 14);text-align:center;}#chatgpt-mermaid-_r_3dl_ .edgeLabel p{background-color:rgb(14, 14, 14);}#chatgpt-mermaid-_r_3dl_ .edgeLabel rect{opacity:0.5;background-color:rgb(14, 14, 14);fill:rgb(14, 14, 14);}#chatgpt-mermaid-_r_3dl_ .labelBkg{background-color:rgba(14, 14, 14, 0.5);}#chatgpt-mermaid-_r_3dl_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-_r_3dl_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-_r_3dl_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-_r_3dl_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-_r_3dl_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-_r_3dl_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-_r_3dl_ .icon-shape,#chatgpt-mermaid-_r_3dl_ .image-shape{background-color:rgb(14, 14, 14);text-align:center;}#chatgpt-mermaid-_r_3dl_ .icon-shape p,#chatgpt-mermaid-_r_3dl_ .image-shape p{background-color:rgb(14, 14, 14);padding:2px;}#chatgpt-mermaid-_r_3dl_ .icon-shape .label rect,#chatgpt-mermaid-_r_3dl_ .image-shape .label rect{opacity:0.5;background-color:rgb(14, 14, 14);fill:rgb(14, 14, 14);}#chatgpt-mermaid-_r_3dl_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-_r_3dl_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-_r_3dl_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-_r_3dl_ [data-look="neo"].node rect,#chatgpt-mermaid-_r_3dl_ [data-look="neo"].cluster rect,#chatgpt-mermaid-_r_3dl_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-_r_3dl_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_3dl_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-_r_3dl_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-_r_3dl_-gradient);stroke-width:1px;}#chatgpt-mermaid-_r_3dl_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_3dl_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-_r_3dl_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-_r_3dl_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_3dl_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-_r_3dl_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-_r_3dl_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_3dl_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-_r_3dl_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_3dl_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(232, 232, 232);}#chatgpt-mermaid-_r_3dl_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(232, 232, 232);}#chatgpt-mermaid-_r_3dl_ .node tspan[font-weight="normal"],#chatgpt-mermaid-_r_3dl_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-_r_3dl_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(19, 19, 19);stroke:rgb(47, 47, 47);stroke-width:1px;}#chatgpt-mermaid-_r_3dl_ .node rect,#chatgpt-mermaid-_r_3dl_ .node circle,#chatgpt-mermaid-_r_3dl_ .node ellipse,#chatgpt-mermaid-_r_3dl_ .node polygon,#chatgpt-mermaid-_r_3dl_ .node path{fill:rgb(24, 24, 24);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-_r_3dl_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-_r_3dl_ .node.mermaid-decision .label-container{fill:rgb(19, 19, 19);stroke:rgb(47, 47, 47);stroke-dasharray:2,2;}#chatgpt-mermaid-_r_3dl_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-_r_3dl_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-_r_3dl_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";}对账任务启动按水位线分批检查发现差异?记录检查进度新增或更新异常工单可否安全自动恢复?创建幂等补偿任务重新查询权威业务状态人工复核与审批调用对应领域服务执行补偿重新对账验证异常解决或保留调查没有有L1 可重试L2 需验证L3 无法确定






事务 A：对账领取和进度更新
- 根据任务状态及租约条件领取对账任务。
- 每批检查结束后在事务内推进处理游标。
- 本批新发现的异常记录及任务进度应保证协调提交。
- 失败后允许从最后的已提交游标恢复。
事务 B：异常登记
- 根据活跃去重键判断是否已有相同未解决异常。
- 不存在则创建工单。
- 已存在则更新 last_detected_at 和 detected_count。
- 保留关联对账任务及检查水位线的证据。
事务 C：补偿执行
- 先领取补偿任务的执行权。
- 根据补偿类型调用对应业务模块。
- 重新校验订单、预占、支付或优惠券的当前状态。
- 在业务模块的同一 MySQL 事务中应用状态修改及对应流水。
- 可靠记录补偿结果。
- 若事务结果不确定，先查询已提交事实，不能直接重复执行不可逆操作。
对于 L3 补偿，人工审批本身不能代替业务事务的正确性检查。

九、P1 扩展：运营统计三张表
9.1 ft_stats_tier_hourly：票档小时统计
字段	类型	说明
tier_id	BIGINT	联合主键
stat_hour	DATETIME(3)	联合主键，小时起点
purchase_accepted_count	BIGINT	抢票受理数
purchase_succeeded_count	BIGINT	抢票成功数
ticket_paid_quantity	BIGINT	支付票数
payment_amount_cent	BIGINT	模拟实收金额
refund_amount_cent	BIGINT	成功退款金额
calculated_at	DATETIME(3)	完成计算时间
revision	BIGINT	修订版本
主键：
PRIMARY KEY(tier_id, stat_hour)
建议增加 (stat_hour, tier_id) 索引用于跨票档统计查询。
9.2 ft_stats_coupon_daily：优惠券每日统计
字段	类型	说明
template_id	BIGINT	联合主键
stat_date	DATE	联合主键
claim_accepted_count	BIGINT	领券受理数
issued_count	BIGINT	成功发券数
used_count	BIGINT	核销数
expired_count	BIGINT	过期数
discount_amount_cent	BIGINT	已支付订单优惠金额
calculated_at	DATETIME(3)	计算完成时间
revision	BIGINT	修订版本
主键：
PRIMARY KEY(template_id, stat_date)
统计发行量、核销量时要根据真实业务发生时间归属日期，不能根据用户券当前状态直接反推所有历史日期的流量。
9.3 ft_stats_aggregation_job：聚合任务表
字段	类型	说明
id	BIGINT	雪花主键
job_no	VARCHAR(64)	唯一聚合任务号
aggregation_type	VARCHAR(32)	PLATFORM_DAILY / EVENT_DAILY 等
window_start_at	DATETIME(3)	统计范围开始
window_end_at	DATETIME(3)	统计范围结束
cutoff_at	DATETIME(3)	数据水位线
status	VARCHAR(24)	任务状态
progress_cursor	VARCHAR(128)	分批处理游标
lease_owner	VARCHAR(64)	执行资格
lease_until	DATETIME(3)	租约截止
attempt_count	INT	尝试次数
revision	BIGINT	目标聚合版本
started_at	DATETIME(3)	开始时间
finished_at	DATETIME(3)	结束时间
created_at	DATETIME(3)	创建时间
updated_at	DATETIME(3)	更新时间
关键索引：
- UNIQUE(job_no)
- (status, lease_until, id)
- (aggregation_type, window_start_at, window_end_at)
如果某日期需要重算，应覆盖该日期对应的统计快照并更新 revision，而不是把同一批交易再次加到原统计值上。
十、P1 扩展：增量事件与分批对账
10.1 ft_stats_processed_event：增量统计事件去重
当以后通过 RocketMQ 实时累加统计时，需要这张表。
字段	类型	约束 / 说明
consumer_name	VARCHAR(64)	联合主键
event_id	VARCHAR(64)	联合主键
event_type	VARCHAR(64)	业务事件类型
aggregation_scope	VARCHAR(64)	影响的统计范围
processed_at	DATETIME(3)	已应用增量的时间
主键：
PRIMARY KEY(consumer_name, event_id)
业务聚合更新与事件处理记录需要在同一数据库事务内提交。
但要注意：增量统计和重算任务如果同时操作同一统计记录，只靠事件去重还不够。
推荐给统计窗口设置版本及截止水位线，在重算切换期间暂停相关窗口的增量应用，或设计明确的增量重放协议，避免覆盖和重复累计。
10.2 ft_reconcile_batch：对账分批任务表
当一次对账需要检查几十万条订单时，应将任务分批执行。
字段	类型	说明
id	BIGINT	主键
job_id	BIGINT	父对账任务
batch_no	INT	批次序号
range_start_key	VARCHAR(128)	本批起始对象
range_end_key	VARCHAR(128)	本批结束对象
status	VARCHAR(24)	批次状态
checked_count	BIGINT	已检查数量
difference_count	BIGINT	差异数量
lease_owner	VARCHAR(64)	执行者令牌
lease_until	DATETIME(3)	租约截止
attempt_count	INT	尝试次数
started_at	DATETIME(3)	开始时间
finished_at	DATETIME(3)	完成时间
created_at	DATETIME(3)	创建时间
updated_at	DATETIME(3)	更新时间
唯一约束：
UNIQUE(job_id, batch_no)
范围边界使用稳定业务主键或时间加主键组合，不能使用在并发更新下容易跳行的无限 OFFSET 分页。
10.3 ft_reconcile_action：对账处理审计表
字段	类型	说明
id	BIGINT	主键
operation_id	VARCHAR(64)	唯一处理操作编号
exception_id	BIGINT	关联异常
compensation_task_id	BIGINT	关联补偿，可空
action_type	VARCHAR(32)	ASSIGN / REVIEW / APPROVE / EXECUTE / CLOSE
from_status	VARCHAR(24)	操作前状态
to_status	VARCHAR(24)	操作后状态
operator_type	VARCHAR(16)	SYSTEM / ADMIN
operator_id	BIGINT	操作人，可空
reason	VARCHAR(1000)	处理说明
evidence_snapshot	JSON	必要证据
created_at	DATETIME(3)	操作时间
索引：
- UNIQUE(operation_id)
- (exception_id, created_at, id)
- (compensation_task_id, created_at, id)
处理审计是追加写入记录，不能直接修改既有记录来掩盖错误补偿。
十一、统计与对账的关键一致性设计
11.1 统计值允许重算
统计表应该满足：
\[
净模拟交易金额=成功支付金额-成功退款金额
\]

但这里应注意：
- 成功支付金额按支付确认时间分桶。
- 成功退款金额按退款完成时间分桶。
- 两者属于不同时间窗口的资金流事件。
- 当天净额可能为负数。
因此不建议把每日净收入字段设计为 UNSIGNED 或增加“净收入必须非负”的 CHECK。
V1 可以在查询层计算净模拟交易金额，避免再次存储一个重复字段。
11.2 对账不能破坏交易状态机
发现以下异常时：
异常	错误做法	正确处理
订单 PAID 但支付单未成功	直接改支付单为成功	查明支付结果，登记差异并恢复
库存 reserved 比明细多	直接增加 available	定位预占、流水和代次
优惠券仍 LOCKED 但订单已 CLOSED	直接把券改 AVAILABLE	验证订单、券有效期及使用流水后补偿
Outbox 一直未发送	删除事件	重试发布并保留状态
退款成功被重复处理	再执行退款	根据 refundNo 和操作记录幂等终结
11.3 对账时的数据水位线
一个需要特别强调的细节：
snapshot_cutoff_at 不能凭空保证跨表一致快照。
例如对账任务开始于 10:00，但扫描执行到 10:10，此时支付、订单和库存仍在并发写入。如果在不同事务中读取各表，就可能看到不同时间点的数据。
推荐两种策略：
- P0：稳定窗口对账。 优先扫描已经结束一段时间的交易窗口，按主键分批检查，遇到仍在变化的对象做二次确认。
- P1：更严格的稳定快照与变更水位线。 通过明确一致性快照、业务版本或变更事件进度确认各账本的可比较范围。
不要在一个长时间 RR 事务中扫描全部历史交易，导致数据库资源持续占用。
11.4 任务恢复和租约
ft_reconcile_job 与 ft_compensation_task 都需要考虑：
- 旧执行者持有过期租约后继续运行。
- 新执行者领取相同任务。
- 业务操作在旧执行者恢复后被执行第二次。
因此租约不能作为唯一正确性保护。
补偿最终写入时还要检查业务操作 ID、对象状态和版本。
对于重要任务，建议采用递增的执行代次或 fencing token，确保旧执行者不能提交已经失效的执行结果。

十二、索引与约束汇总
表	主键	唯一约束	核心查询索引
ft_stats_platform_daily	stat_date	主键	日期范围主键扫描
ft_stats_event_daily	(event_id, stat_date)	主键	(stat_date, event_id)（按需）
ft_reconcile_job	id	job_no	(status, next_retry_at, id)
ft_reconcile_exception	id	exception_no、有效异常去重键	(status, severity, created_at, id)
ft_compensation_task	id	task_no、operation_id	(status, next_retry_at, id)
ft_stats_tier_hourly	(tier_id, stat_hour)	主键	(stat_hour, tier_id)
ft_stats_coupon_daily	(template_id, stat_date)	主键	(stat_date, template_id)
ft_stats_aggregation_job	id	job_no	(status, lease_until, id)
ft_stats_processed_event	(consumer_name, event_id)	主键	按消费需要
ft_reconcile_batch	id	(job_id, batch_no)	(status, lease_until, id)
ft_reconcile_action	id	operation_id	(exception_id, created_at, id)
外键策略
建议：
- 对账任务和对账异常可以使用物理外键 RESTRICT，防止孤立异常。
- 补偿任务关联异常可以为空，避免强制所有补偿先产生对账工单。
- 对账异常的 biz_type + biz_id 是多态业务关联，不直接创建单一外键。
- 统计表可以通过稳定业务 ID 逻辑关联原始活动或票档，禁止删除被交易引用的业务基础数据。
- 账务流水、对账证据和补偿操作均不得级联物理删除。
MySQL 的 CHECK 适合验证本行计数、时间范围和版本值，但无法直接验证另一张支付表或库存表的数据是否一致。

MySQL



十三、业务验收与故障测试
第十批数据库验收清单
0/16 已检查



运营统计0/4 项[ ] 每日支付金额只统计真正成功的模拟支付
[ ] 同一交易事件重复消费不重复累计统计
[ ] 历史统计可以重算并替换旧版本
[ ] 退款日期和原支付日期不同也能够正确统计



对账正确性0/4 项

补偿任务0/4 项

并发与恢复0/4 项
这些是后续数据库集成测试、故障注入和并发验收要求，不代表当前功能已实现。
十四、开发落地顺序
阶段	主要实现	验收目标
P0-A	每日平台、演出统计	正确显示业务看板
P0-B	库存、支付、退款对账	能发现并保存真实差异
P0-C	对账任务状态和异常工单	支持调度、断点恢复
P0-D	安全补偿与幂等	不重复释放、不重复退款
P1-A	票档、优惠券细分统计	多维运营分析
P1-B	RocketMQ 增量统计与事件去重	准实时更新
P1-C	分批对账、聚合任务水位线	支持更大数据量
P1-D	处理审计、人工复核工作台	补偿全流程可追溯
十五、第十批数据库设计定稿
运营数据统计与系统对账域 V1.0


P0 五张表： ft_stats_platform_daily、ft_stats_event_daily、ft_reconcile_job、ft_reconcile_exception、ft_compensation_task。
P1 六张表： 票档小时统计、优惠券日统计、统计聚合任务、增量事件幂等、对账批次、处理审计。
数据原则： MySQL 交易事实为准；统计表可以重建，不能用来驱动支付和库存结果。
对账模型： 固定检查范围、水位线、异常去重、证据快照和重复验证。
补偿模型： 操作 ID 幂等、执行租约、风险分级、审批及重新查询业务状态。
调度： Spring Scheduling + MySQL 任务领取和恢复机制。
一致性： 统计允许延迟，订单、支付、库存和优惠券账目必须保持合法状态。


十六、全局数据库设计进度
目前十批业务域的详细数据库设计已经完成。按最新约定，核心表数量如下：
批次	业务域	P0 表数
01	用户与权限	6
02	演出与场次	7
03	库存与预占	3
04	抢票与排队	2
05	订单与交易	3
06	模拟支付与退款	5
07	优惠券与营销	8
08	风控与防刷	3
09	通知与消息	2
10	运营统计与对账	5
	十大业务域合计	44
另外还有共享可靠消息基础设施的两张 P0 表：
- ft_outbox_event
- ft_message_inbox
因此按当前详细模型，FlashTicket 一共规划 46 张 P0 表（44 张业务表 + 2 张基础设施表）。
这与最早全局 ER 草案的 45 张不同，主要来自优惠券域新增 P0 发行流水，以及后续表清单的去重和调整；最终仍应在生成 Flyway DDL 前进行一次全局字段、约束和数量审查。



FlashTicket 数据库详细设计 11：共享基础设施域
Transactional Outbox & Idempotent Inbox · MySQL 8.4 LTS · V1.0
第十一批是 FlashTicket 全局数据库设计的最后一个基础设施域。前十批已经覆盖用户、演出、库存、抢票、订单、支付、营销、风控、通知和对账。
本批重点解决 MySQL 事务提交与 RocketMQ 消息发送之间的不一致、重复消息消费、发布任务竞争，以及故障恢复。
本批设计范围
2 张 P0 核心表

ft_outbox_event
可靠消息发布表
解决业务事务提交成功，但 MQ 消息未可靠发布的问题。


ft_message_inbox
消息消费幂等表
解决重复投递导致业务操作重复执行的问题。




本轮仍然只确定表结构、字段、数据类型、约束、索引和事务规则，不编写 DDL 或 Java 代码。
一、共享基础设施与业务模块的关系
首先明确这两张表不归属于某个具体业务模块，而是全项目共用。
#chatgpt-mermaid-_r_3kg_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-_r_3kg_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-_r_3kg_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-_r_3kg_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-_r_3kg_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-_r_3kg_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-_r_3kg_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-_r_3kg_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-_r_3kg_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-_r_3kg_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-_r_3kg_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-_r_3kg_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-_r_3kg_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-_r_3kg_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";font-size:16px;}#chatgpt-mermaid-_r_3kg_ p{margin:0;}#chatgpt-mermaid-_r_3kg_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";color:rgb(237, 237, 237);}#chatgpt-mermaid-_r_3kg_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-_r_3kg_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-_r_3kg_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-_r_3kg_ .label text,#chatgpt-mermaid-_r_3kg_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-_r_3kg_ .node rect,#chatgpt-mermaid-_r_3kg_ .node circle,#chatgpt-mermaid-_r_3kg_ .node ellipse,#chatgpt-mermaid-_r_3kg_ .node polygon,#chatgpt-mermaid-_r_3kg_ .node path{fill:rgb(19, 33, 54);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-_r_3kg_ .rough-node .label text,#chatgpt-mermaid-_r_3kg_ .node .label text,#chatgpt-mermaid-_r_3kg_ .image-shape .label,#chatgpt-mermaid-_r_3kg_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-_r_3kg_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-_r_3kg_ .rough-node .label,#chatgpt-mermaid-_r_3kg_ .node .label,#chatgpt-mermaid-_r_3kg_ .image-shape .label,#chatgpt-mermaid-_r_3kg_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-_r_3kg_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-_r_3kg_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-_r_3kg_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-_r_3kg_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-_r_3kg_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-_r_3kg_ .edgeLabel{background-color:rgb(14, 14, 14);text-align:center;}#chatgpt-mermaid-_r_3kg_ .edgeLabel p{background-color:rgb(14, 14, 14);}#chatgpt-mermaid-_r_3kg_ .edgeLabel rect{opacity:0.5;background-color:rgb(14, 14, 14);fill:rgb(14, 14, 14);}#chatgpt-mermaid-_r_3kg_ .labelBkg{background-color:rgba(14, 14, 14, 0.5);}#chatgpt-mermaid-_r_3kg_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-_r_3kg_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-_r_3kg_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-_r_3kg_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-_r_3kg_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-_r_3kg_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-_r_3kg_ .icon-shape,#chatgpt-mermaid-_r_3kg_ .image-shape{background-color:rgb(14, 14, 14);text-align:center;}#chatgpt-mermaid-_r_3kg_ .icon-shape p,#chatgpt-mermaid-_r_3kg_ .image-shape p{background-color:rgb(14, 14, 14);padding:2px;}#chatgpt-mermaid-_r_3kg_ .icon-shape .label rect,#chatgpt-mermaid-_r_3kg_ .image-shape .label rect{opacity:0.5;background-color:rgb(14, 14, 14);fill:rgb(14, 14, 14);}#chatgpt-mermaid-_r_3kg_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-_r_3kg_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-_r_3kg_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-_r_3kg_ [data-look="neo"].node rect,#chatgpt-mermaid-_r_3kg_ [data-look="neo"].cluster rect,#chatgpt-mermaid-_r_3kg_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-_r_3kg_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_3kg_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-_r_3kg_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-_r_3kg_-gradient);stroke-width:1px;}#chatgpt-mermaid-_r_3kg_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_3kg_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-_r_3kg_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-_r_3kg_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_3kg_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-_r_3kg_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-_r_3kg_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_3kg_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-_r_3kg_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_3kg_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(232, 232, 232);}#chatgpt-mermaid-_r_3kg_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(232, 232, 232);}#chatgpt-mermaid-_r_3kg_ .node tspan[font-weight="normal"],#chatgpt-mermaid-_r_3kg_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-_r_3kg_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(19, 19, 19);stroke:rgb(47, 47, 47);stroke-width:1px;}#chatgpt-mermaid-_r_3kg_ .node rect,#chatgpt-mermaid-_r_3kg_ .node circle,#chatgpt-mermaid-_r_3kg_ .node ellipse,#chatgpt-mermaid-_r_3kg_ .node polygon,#chatgpt-mermaid-_r_3kg_ .node path{fill:rgb(24, 24, 24);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-_r_3kg_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-_r_3kg_ .node.mermaid-decision .label-container{fill:rgb(19, 19, 19);stroke:rgb(47, 47, 47);stroke-dasharray:2,2;}#chatgpt-mermaid-_r_3kg_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-_r_3kg_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-_r_3kg_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";}订单 / 支付 / 退款 /领券等业务模块MySQL 本地事务ft_outbox_eventOutbox PublisherRocketMQ 5.xPushConsumerft_message_inbox +业务数据库事务订单 / 通知 / 统计等处理结果






阶段	负责组件	核心保障
业务发生	业务 Service + MySQL	业务记录与 Outbox 同事务
发布消息	Outbox Publisher	失败重试、租约与发送确认
Broker 存储	RocketMQ	持久化与消息分发
消费消息	PushConsumer	重试及消费回调
执行业务	Inbox + 领域 Service	消费幂等与业务事务
异常恢复	Spring Scheduling	补发、重试、异常登记
本批最终采用“至少一次交付 + 业务幂等”，不宣称整个消息链路天然 Exactly Once。
Transactional Outbox 允许发布器在发送成功后、记录发送结果前崩溃，因此重复发布是需要正常处理的情况。

Microsoft Learn




二、ft_outbox_event：可靠消息发布表
这是共享基础设施域最重要的一张表。
2.1 事件身份与业务关联字段
字段	MySQL 类型	约束 / 默认值	说明
id	BIGINT	PK，NOT NULL	雪花主键
event_id	VARCHAR(64)	NOT NULL，UNIQUE	全局业务事件 ID
event_type	VARCHAR(64)	NOT NULL	事件类型
aggregate_type	VARCHAR(32)	NOT NULL	业务聚合类型
aggregate_id	VARCHAR(64)	NOT NULL	业务聚合 ID
aggregate_version	BIGINT	NULL	业务聚合版本或事件序号
topic	VARCHAR(128)	NOT NULL	RocketMQ Topic
message_type	VARCHAR(16)	NOT NULL，默认 NORMAL	消息类型
message_key	VARCHAR(128)	NOT NULL	RocketMQ 消息检索 Key
tag	VARCHAR(64)	NULL	消息过滤 Tag
schema_version	INT	NOT NULL，默认 1	消息协议版本
这些 ID 为什么不能混用？
字段	示例	用途
id	雪花数字 ID	MySQL 内部主键
event_id	evt_xxx	业务事件去重
aggregate_id	订单 ID	表示事件属于哪笔业务
message_key	evt_xxx	RocketMQ 消息检索
trace_id	Trace 标识	链路问题排查
建议默认让 message_key 使用 event_id，这样可以通过稳定业务 ID 检索重复投递的事件。
Broker 自身的消息 ID 不应代替 event_id，因为同一业务事件重新发布后可能产生不同的 Broker 消息标识。
2.2 事件内容字段
字段	MySQL 类型	约束 / 默认值	说明
payload	JSON	NOT NULL	消息业务数据
payload_hash	CHAR(64)	NOT NULL	规范化消息内容 SHA-256 摘要
headers	JSON	NULL	非敏感扩展消息头
trace_id	VARCHAR(64)	NULL	链路追踪 ID
request_id	VARCHAR(64)	NULL	原始业务请求 ID
occurred_at	DATETIME(3)	NOT NULL	业务事件发生时间
created_at	DATETIME(3)	NOT NULL	Outbox 入库时间
updated_at	DATETIME(3)	NOT NULL	最近状态更新时间
payload 需要包含足以让消费者处理业务的数据，但不能无边界地保存完整用户资料、JWT、密码或支付敏感信息。
事件创建之后，event_id、event_type、payload 和 schema_version 原则上不可修改。需要更正业务事实，应发布新的补偿事件，而不是覆盖旧消息。
2.3 消息调度与发送字段
字段	MySQL 类型	约束 / 默认值	说明
status	VARCHAR(24)	NOT NULL，默认 PENDING	发布状态
attempt_count	INT	NOT NULL，默认 0	已开始发送的次数
max_attempts	INT	NOT NULL，默认 12	自动发送次数上限
next_retry_at	DATETIME(3)	NOT NULL	下次可发送时间
lease_owner	VARCHAR(64)	NULL	当前发布者令牌
lease_until	DATETIME(3)	NULL	发布租约截止时间
lease_version	BIGINT	NOT NULL，默认 0	发布执行代次 / fencing token
sent_at	DATETIME(3)	NULL	Broker 确认接收时间
broker_message_id	VARCHAR(128)	NULL	Broker 返回的消息标识
last_error_code	VARCHAR(64)	NULL	最近发送错误码
last_error_message	VARCHAR(500)	NULL	脱敏错误摘要
2.4 延迟消息专用字段
我们此前确定使用 RocketMQ 5.x 的 Delay 消息实现订单到期检查，因此 Outbox 必须能保存目标投递时间。
字段	MySQL 类型	约束 / 默认值	说明
deliver_at	DATETIME(3)	NULL	计划向消费者投递的时间
规则：
- NORMAL 消息：deliver_at 必须为空。
- DELAY 消息：deliver_at 必须有合法值。
- 订单超时消息：通常使用订单 expire_at 作为目标时间。
- 发布器不能把 deliver_at 当作 Outbox 的下次重试时间。
next_retry_at 与 deliver_at 完全不同： 前者控制什么时候尝试向 Broker 发送；后者控制 Broker 什么时候向消费者提供消息。
对于已过期但尚未发布的关单事件，发布器需要有明确的迟到处理策略，例如直接发送即时关单检查消息，或由数据库到期扫描完成兜底。不能指望延迟时间戳一定仍处于 Broker 支持的可调度范围。

三、Outbox 发布状态机
建议采用五种状态。
PENDING
业务事务已经提交，等待发布


PUBLISHING
已取得发布租约，正在尝试发送


SENT
Broker 已确认接收


RETRY_WAIT
等待下一次发送



MANUAL_REVIEW
自动重试耗尽或存在不可自动处理的异常


状态转换规则
原状态	事件	新状态
PENDING	发布器领取成功	PUBLISHING
PUBLISHING	Broker 明确确认接收	SENT
PUBLISHING	发送失败或结果未知	RETRY_WAIT
PUBLISHING	租约超时	允许新发布者重新领取
RETRY_WAIT	达到重试时间	PUBLISHING
RETRY_WAIT	自动重试耗尽	MANUAL_REVIEW
MANUAL_REVIEW	经检查批准恢复	RETRY_WAIT
SENT	重复执行发布任务	不再主动发布
SENT 代表消息被 Broker 接收，不代表消费者业务已经处理成功。
Outbox 不应该根据订单消费者是否完成来更新自己的 SENT 状态。消费结果由业务状态和 Inbox 负责。
四、ft_outbox_event 的索引与约束
4.1 核心索引
索引名称	字段	用途
PRIMARY KEY	id	主键查询
uk_outbox_event_id	event_id	全局事件幂等
idx_outbox_pending	(status, next_retry_at, id)	扫描待发布事件
idx_outbox_lease	(status, lease_until, id)	扫描发送中但租约过期事件
idx_outbox_aggregate	(aggregate_type, aggregate_id, id)	按业务聚合追踪事件
idx_outbox_created	(created_at, id)	运营后台查询及归档
前期建议以 uk_outbox_event_id、idx_outbox_pending 和 idx_outbox_lease 为核心。其他索引根据真实查询场景逐步加入。
Outbox 表属于持续插入并频繁更新状态的表，不能无限增加状态字段索引。
4.2 CHECK 约束
建议验证：
约束	规则
自动重试次数	max_attempts > 0
已尝试次数	attempt_count >= 0
执行代次	lease_version >= 0
协议版本	schema_version >= 1
消息类型	只能是支持的 Normal / Delay
Delay 时间	Delay 消息必须有 deliver_at
发送成功状态	SENT 必须有 sent_at
事件时间	occurred_at 必须存在
不建议要求 attempt_count <= max_attempts 成为永久不变量，因为人工授权恢复后可能需要增加执行上限或重新开始一个受控恢复周期。
4.3 事件关联约束
aggregate_type 建议使用有限的业务类型：
- PURCHASE
- ORDER
- PAYMENT
- REFUND
- COUPON
- EVENT
- RISK
- NOTIFICATION
aggregate_id 采用字符串，便于容纳雪花 ID、业务编号或其他稳定标识。
这是多态逻辑关联，不建立到某张单一业务表的物理外键。

五、Outbox Publisher 的多实例安全设计
虽然 FlashTicket 是模块化单体，但未来可以部署多个相同的 Spring Boot 实例，因此发布任务需要支持多实例竞争。
5.1 任务领取
推荐采用：
MySQL FOR UPDATE SKIP LOCKED + 短事务领取 + 发布租约。
MySQL 官方明确说明 SKIP LOCKED 可用于多工作者竞争队列式表记录，但不适合一般交易的一致性查询。

MySQL



领取规则：
1. 只选取 PENDING、达到重试时间的 RETRY_WAIT，或租约已经过期的 PUBLISHING。
2. 使用数据库事务获得一批待发布记录的处理权。
3. 将其状态更新为 PUBLISHING。
4. 设置新的 lease_owner、lease_until。
5. 增加 lease_version 和 attempt_count。
6. 提交数据库事务。
7. 事务外发送 RocketMQ 消息。
8. 根据 Broker 发送结果，以 lease_owner + lease_version 条件更新发布状态。
为什么必须在事务外发 MQ？
因为发送消息是外部网络操作。
如果持有 MySQL 行锁等待 Broker 返回，会占用数据库连接和事务资源，降低吞吐并放大故障影响。
数据库短事务负责领取资格，MQ 网络操作在事务外执行，结果再通过一个新的短事务记录。
5.2 旧发布者与新发布者竞争
例如：
时间	发布者 A	发布者 B
T1	获得版本 5 的租约	—
T2	网络阻塞，租约过期	—
T3	—	获得版本 6 的租约
T4	A 的发送请求恢复并成功	B 可能也已经发送
T5	A 尝试更新 SENT，但版本已失效	B 合法更新状态
这里需要接受一个事实：
数据库 fencing token 能阻止旧发布者覆盖新的发布状态，但不能撤销旧发布者已经向 RocketMQ 成功发送的消息。
因此 Outbox 的正确性是允许消息重复，靠消费者业务幂等防止重复执行。
5.3 推荐初始运行参数
参数	建议起点
发布扫描间隔	1 秒
每批读取数量	100 条
同时发布任务	2–4
发布租约	30 秒
最大发送尝试	12 次
失败退避	指数退避 + 抖动
最大退避间隔	5 分钟
这些只是本地开发与压测的初始参数，必须根据 Broker 响应时间、队列积压和数据库资源调整。
不能认为扫描间隔越短、发布并发越高就一定越好。

六、ft_message_inbox：消息消费幂等表
第二张核心表用于记录某个消费者组的业务处理者已经成功处理过哪个事件。
我们此前确定使用 RocketMQ PushConsumer 集群消费。
RocketMQ 的 PushConsumer 可能因消费失败、超时或 ACK 未确认而重新处理相同消息，因此业务消费者必须具备幂等能力。

RocketMQ
+1



6.1 字段设计
字段	MySQL 类型	约束 / 默认值	说明
consumer_name	VARCHAR(64)	联合 PK，NOT NULL	稳定逻辑消费处理者标识
event_id	VARCHAR(64)	联合 PK，NOT NULL	业务事件唯一 ID
event_type	VARCHAR(64)	NOT NULL	事件类型
aggregate_type	VARCHAR(32)	NULL	归属业务类型
aggregate_id	VARCHAR(64)	NULL	归属业务 ID
payload_hash	CHAR(64)	NOT NULL	实际处理消息的规范化摘要
schema_version	INT	NOT NULL	消息结构版本
processed_at	DATETIME(3)	NOT NULL	业务处理成功时间
created_at	DATETIME(3)	NOT NULL	记录创建时间
6.2 为什么采用联合主键？
采用：
PRIMARY KEY(consumer_name, event_id)
例如同一个 ORDER_PAID 事件需要两种业务处理：
- 通知消费者创建支付成功通知。
- 统计消费者更新运营数据。
两种消费者都应有各自独立的消费幂等记录。
如果只以 event_id 为全局唯一主键，通知模块处理成功后，统计模块可能错误地把这条消息识别为已消费。
6.3 consumer_name 的含义
这里建议使用稳定的逻辑业务处理者名称，而不是随机进程 ID。
例如：
consumer_name	职责
purchase-order-handler	抢票异步建单
notification-handler	生成站内通知
payment-event-handler	支付事件非核心后续处理
coupon-claim-handler	异步发券
analytics-handler	增量运营统计
consumer_name 不必机械等同于 RocketMQ Consumer Group 名称，但应能稳定映射到明确的消费处理逻辑。
不能在每次应用启动时生成新的名字，否则旧消费幂等记录会失去作用。
6.4 为什么需要 payload_hash？
如果系统错误地复用了同一个 event_id，但消息正文发生变化，就不能直接把两条消息当成合法重试。
推荐规则：
- 相同 consumer_name + event_id，摘要一致：重复消息，幂等返回成功。
- 相同键但摘要不一致：消息身份冲突，登记异常，不执行新的业务操作。
- 不同 event_id：作为新事件处理，仍然受领域状态机和业务唯一约束保护。
这里需要明确：payload_hash 应使用固定的规范化消息序列化算法，不能让 JSON 字段顺序不同导致同一语义事件生成不同摘要。

七、Inbox 为什么不设计复杂状态机？
我建议 V1 的 Inbox 只记录已经成功提交的消费结果，不再增加 RECEIVED / PROCESSING / FAILED 等状态。
原因是我们已经有：
- RocketMQ 自带的消费重试机制。
- ft_purchase_request 的业务处理租约。
- ft_coupon_claim_request 的业务处理租约。
- 订单、支付和库存的持久化状态机。
- 系统对账与补偿任务。
如果再给每条 Inbox 增加自己的执行状态、租约与重试计数，会出现多套状态同时维护的问题。
推荐的消费事务
RocketMQ PushConsumer 收到消息


MySQL 本地事务
1. 验证事件 ID、事件类型、Schema 和 Payload 摘要
2. 根据联合唯一键取得消费幂等资格
3. 查询并校验业务当前状态
4. 执行合法领域操作
5. 写入已处理 Inbox 记录
6. 提交本地事务


提交成功后返回消费成功


Inbox 的去重资格可以通过事务内唯一键插入与冲突处理取得，或在已提交记录存在时识别为重复。无论具体采用哪种方式，必须保证 Inbox 与业务写入同事务提交。
三种异常情况
情况	应有行为
业务处理失败，MySQL 回滚	Inbox 不留下成功记录，后续允许重试
MySQL 已提交，消费者 ACK 前崩溃	MQ 重投，Inbox 识别重复并安全返回
Inbox 已存在，但 Payload 摘要不同	记录严重协议异常，不执行业务
需要强调一个容易出现的错误：不要先单独提交 Inbox，再处理订单。
否则订单事务失败之后，Inbox 已经写入“处理成功”，消息重试便会被错误跳过。
八、Inbox 的索引与 CHECK 约束
8.1 索引
索引名	字段	用途
PRIMARY KEY	(consumer_name, event_id)	消费业务幂等
idx_inbox_processed	(processed_at, consumer_name)	历史清理、统计
idx_inbox_aggregate	(aggregate_type, aggregate_id)	事件排查（按需）
V1 首先保留联合主键和处理时间索引。
Inbox 是高频写入表，不能为每个业务追踪字段都建立二级索引。
8.2 数据约束
- consumer_name 不能为空。
- event_id 不能为空。
- schema_version >= 1。
- processed_at 必须有值。
- 消费成功记录不能被普通业务随意修改。
- 一条事件对于同一逻辑消费者只存在一份有效完成记录。
8.3 物理外键选择
我建议 Inbox 不建立到 Outbox 的物理外键。
原因是：
- 消费的消息不一定都来自当前应用数据库。
- Outbox 可能按归档策略被清理。
- 消费处理逻辑独立于消息发布数据的生命周期。
- Inbox 的主要作用是幂等，而非强制保存事件来源完整关系。
两者通过 event_id 进行逻辑关联。

九、RocketMQ 重试、死信与数据库的关系
RocketMQ 5.x 支持消费失败重试；超过配置的重试次数后，消息可能进入死信队列。官方也说明消费重试属于故障恢复手段，不应用来处理大量预期的业务分支。

RocketMQ



9.1 哪些情况应该触发 MQ 重试？
消费结果	处理方式
MySQL 临时不可用	返回失败，等待重试
数据库死锁，当前事务回滚	有界重试
网络短时异常	返回失败，等待重试
抢票已成功且 Inbox 已记录	幂等返回成功
库存确定售罄	提交业务 FAILED 终态，消费返回成功
用户已达限购	提交业务 FAILED 终态，消费返回成功
事件结构不支持	隔离异常、告警，不执行业务
处理结果暂时不确定	根据业务请求状态恢复，不盲目重新扣减
9.2 DLQ 是否需要另建数据库表？
V1 不额外创建 ft_dead_letter_message。
我们已经有：
- RocketMQ 自身的死信队列。
- ft_outbox_event 的发送记录。
- ft_purchase_request、ft_coupon_claim_request 等业务请求事实。
- ft_compensation_task 的恢复记录。
- ft_reconcile_exception 的异常处理工单。
死信处理器可以在取得消息后，根据事件 ID 和业务状态创建异常或补偿任务。
如果未来需要长期保存死信原文、独立检索、人工修改重放内容和集中管理多种 MQ，再考虑独立建死信归档表。
9.3 DLQ 重放规则
不允许管理员点击“重新发送”就绕过全部业务校验。
正确的重放过程是：
1. 通过原始 event_id 找到相关业务事实。
2. 判断事件是否已经完成处理。
3. 如果已经完成，幂等结束。
4. 如果未完成，检查当前业务状态是否允许继续。
5. 通过受控恢复流程重新投递原事件或创建带有新操作 ID 的补偿任务。
6. 保留原始事件和人工操作审计。
特别是支付成功、退款成功、库存释放类事件，不能将其当作普通无副作用消息反复执行。
十、Outbox 与 Inbox 的保留和归档策略
这两张表会随着系统运行不断增长。
但清理策略必须考虑业务重试窗口、死信恢复时间和幂等保留要求。
10.1 Outbox 保留
状态	是否允许自动清理
PENDING	不允许
PUBLISHING	不允许
RETRY_WAIT	不允许
MANUAL_REVIEW	不允许
SENT	满足归档和恢复保留规则后允许
建议 V1 在开发环境保留全部记录，便于面试演示和故障追踪。
后续可以把较旧的 SENT 事件分批归档，避免影响待发送任务扫描。
10.2 Inbox 保留
Inbox 清理要比普通日志更谨慎。
假设 Inbox 记录已经删除，但一条旧消息从 DLQ 被重新投递，如果消费者只依赖 Inbox 防重，就可能再次执行旧业务。
因此必须同时满足：
- 已经过正常消费、重试和死信可能恢复的业务保留窗口。
- 领域业务仍然具有独立幂等约束。
- 清理后旧事件不会造成重复扣库存、退款或发券。
- 重要历史事件能通过交易记录或归档审计追踪。
V1 不自动删除 Inbox。
当表规模成为问题时再设计分区、归档和清理，不应为了优化表空间提前牺牲幂等能力。

十一、与十大业务域的消息关系
业务域	Outbox 事件举例	消费端处理
抢票	PURCHASE_REQUESTED	异步创建订单
订单	ORDER_CREATED	关单事件调度、通知
支付	PAYMENT_SUCCEEDED	通知与统计
退款	REFUND_SUCCEEDED	通知与统计
优惠券	COUPON_CLAIM_REQUESTED	异步发券
演出	EVENT_CANCELLED	通知受影响用户
风控	RISK_RESTRICTION_CREATED	缓存更新与安全通知
消息中心	NOTIFICATION_REQUESTED	创建站内消息
运营统计	原始交易事件	更新派生统计
对账补偿	COMPENSATION_COMPLETED	记录补偿后续事件
注意：不是每个业务事件都需要再复制生成一个新的通知事件。
例如 PAYMENT_SUCCEEDED 本身可以被通知消费者和统计消费者分别订阅，两个独立逻辑消费者使用各自的 Inbox 幂等键。
消费者组和事件契约需要按职责配置，不能依赖一个业务处理器消费完成后顺便完成所有后续功能。
十二、四个必须测试的故障窗口
这一部分很适合成为 FlashTicket 简历里的“消息可靠性设计”亮点。
故障 A：业务事务提交后，应用直接崩溃
业务已提交
尚未发 MQ

Outbox 已经持久化，应用重启后扫描发送，不丢失待发布业务事件。

故障 B：Broker 已收到消息，但发布者没有收到确认
Broker 已接收
发送结果未知

Outbox 可能重新发送，消费者必须按同一 event_id 幂等处理。

故障 C：消费者提交 MySQL 成功，ACK 前崩溃
业务已提交
MQ 未确认

RocketMQ 重新投递，Inbox 和业务状态共同防止重复建单或重复确认支付。

故障 D：消费者执行中发生数据库死锁
事务回滚
安全重试

Inbox 与业务变更一起回滚，后续允许在幂等条件下重新执行。


还有一个必须单独测试的边界：消息消费回调已经返回失败，但原执行线程仍有数据库操作在运行。
对于 PushConsumer，应保证业务处理在监听回调的受控执行期间结束，不要把事务偷偷交给另一个线程后立即返回消费成功或失败。RocketMQ 官方对 PushConsumer 的消费回调与同步完成语义有明确说明。

RocketMQ




十三、两张表的最终约束与索引汇总
表	主键	必要唯一约束	核心索引
ft_outbox_event	id	event_id	(status, next_retry_at, id)、(status, lease_until, id)
ft_message_inbox	(consumer_name, event_id)	联合主键	(processed_at, consumer_name)
数据库一致性规范
- 所有事件 ID 和业务请求 ID 统一采用大小写敏感的比较规则。
- Outbox 消息内容在首次创建后不可覆盖。
- Outbox 状态更新必须带合法执行租约与执行代次。
- Inbox 完成记录与本次消费的业务写入在同一个 MySQL 事务中提交。
- MQ ACK 只能在业务处理获得可靠结果后返回。
- 业务确定失败和技术异常必须分类处理。
- 清理历史 Inbox 之前必须证明领域幂等仍然成立。
- 不允许通过直接删除 Outbox 或 Inbox 来解决业务异常。
十四、本批数据库验收清单
共享基础设施域验收清单
0/16 项已检查



Outbox 可靠发送0/4[ ] 业务事务提交成功后即使进程崩溃事件也可以恢复发送
[ ] Broker 收到消息但发布者超时时允许安全重复投递
[ ] 旧租约发布者不能覆盖新发布者的状态
[ ] 消息体及协议版本在重试时保持不变



Inbox 消费幂等0/4

消息异常恢复0/4

性能和维护0/4
这些是后续数据库集成测试、RocketMQ 故障注入及并发测试的验收目标，不代表当前已经完成测试。
十五、第十一批数据库设计定稿
共享基础设施域 V1.0


P0 核心表： ft_outbox_event、ft_message_inbox。
Outbox 模型： 业务事件同事务入库，发布器短事务领取、事务外发送 MQ，再持久化发送结果。
Outbox 状态： PENDING / PUBLISHING / RETRY_WAIT / SENT / MANUAL_REVIEW。
Inbox 模型： 仅记录成功处理完成的事件，联合主键 (consumer_name, event_id)。
消息语义： 至少一次投递 + 业务幂等，不依赖 MQ 保证全链路恰好一次。
并发安全： MySQL 租约、执行代次、唯一索引、领域状态机。
异常恢复： Outbox 补发、RocketMQ 重试、DLQ、补偿任务和业务对账。
数据保留： 未发布消息不能清理，Inbox 清理必须符合业务幂等保留要求。

十六、FlashTicket 全局数据库设计进度
11 批详细数据库模型
设计阶段完成

46
P0 核心表


11
设计批次


1
MySQL 业务库



46 张 P0 表是当前逐批模型的规划数量。正式建库前还需全局去重、审查跨域约束以及退款票务权益等必要补表，最终表数量可能调整。