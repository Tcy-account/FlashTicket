FlashTicket 全局数据库模型与 ER 关系设计 V1.0
全局数据库设计
MySQL 8.4 LTS · InnoDB · 模块化单体

这一步我们要把前面设计的十大业务模块整合成一套完整、统一、可直接指导后续建表的数据库模型。
重点不是把之前提到的所有表简单拼在一起，而是统一实体之间的关系、业务状态、唯一约束、库存账本和事务边界，防止出现重复设计或数据相互矛盾。
一、数据库总体方案
FlashTicket Database V1.0
MySQL 8.4
数据库版本


InnoDB
存储引擎


RR
默认事务隔离


BIGINT
雪花主键




数据库名称： flash_ticket
命名规范： 所有业务表统一 ft_ 前缀、蛇形命名法。
时间字段： DATETIME(3)，应用统一以 UTC 写入。
金额字段： BIGINT，单位为人民币分。
主键： MyBatis-Plus 雪花算法生成 BIGINT。
数据访问： MyBatis-Plus 处理普通 CRUD，复杂查询、库存及交易状态操作采用手写 SQL。

对于事务隔离级别，保持 MySQL InnoDB 默认的 REPEATABLE READ。但这不意味着只靠 RR 就能防止所有业务并发问题，关键交易仍需唯一索引、行锁和条件更新。

MySQL
+1



二、全局实体与模块划分
我们把数据库划分成十大业务域，加一个共享基础设施域。
为了控制项目规模，不会把每个预留功能都放进第一批迁移。
业务域	P0 核心表	P1 扩展表
用户与权限	6	4
演出与场次	7	2
库存与预占	3	1
抢票排队	2	1
订单交易	3	0
支付与退款	5	1
优惠券营销	7	2
风控与防刷	3	3
通知中心	2	3
统计与对账	5	5
共享消息基础设施	2	0
合计	45	22
P0 是完整业务设计的核心表集合，不代表项目初始化就必须一次性建完 45 张表。后续会按交易主链路分批迁移。
我们当前规划的是 45 张 P0 核心表 + 22 张 P1 扩展表。相比只做一个秒杀 Demo，这套数据库已经有完整业务系统的规模，但仍然属于一个 MySQL 数据库、一个 Java 单体应用。
三、全局 ER 关系图
全系统如果把 45 张表全部放在一张图里，会很难阅读。因此采用一张全局核心关系图，再按照业务域展开。
核心交易实体关系
#chatgpt-mermaid-_r_2t2_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-_r_2t2_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-_r_2t2_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-_r_2t2_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-_r_2t2_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-_r_2t2_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-_r_2t2_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-_r_2t2_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-_r_2t2_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-_r_2t2_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-_r_2t2_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-_r_2t2_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-_r_2t2_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-_r_2t2_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";font-size:16px;}#chatgpt-mermaid-_r_2t2_ p{margin:0;}#chatgpt-mermaid-_r_2t2_ .entityBox{fill:rgb(19, 33, 54);stroke:rgb(31, 78, 148);}#chatgpt-mermaid-_r_2t2_ .relationshipLabelBox{fill:rgb(48, 48, 48);opacity:0.7;background-color:rgb(48, 48, 48);}#chatgpt-mermaid-_r_2t2_ .relationshipLabelBox rect{opacity:0.5;}#chatgpt-mermaid-_r_2t2_ .labelBkg{background-color:rgba(48, 48, 48, 0.5);}#chatgpt-mermaid-_r_2t2_ .edgeLabel{background-color:rgb(14, 14, 14);}#chatgpt-mermaid-_r_2t2_ .edgeLabel .label rect{fill:rgb(14, 14, 14);}#chatgpt-mermaid-_r_2t2_ .edgeLabel .label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-_r_2t2_ .edgeLabel .label{fill:rgb(31, 78, 148);font-size:14px;}#chatgpt-mermaid-_r_2t2_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";color:rgb(237, 237, 237);}#chatgpt-mermaid-_r_2t2_ .edge-pattern-dashed{stroke-dasharray:8,8;}#chatgpt-mermaid-_r_2t2_ .node rect,#chatgpt-mermaid-_r_2t2_ .node circle,#chatgpt-mermaid-_r_2t2_ .node ellipse,#chatgpt-mermaid-_r_2t2_ .node polygon{fill:rgb(19, 33, 54);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-_r_2t2_ .relationshipLine{stroke:rgb(175, 175, 175);stroke-width:1px;fill:none;}#chatgpt-mermaid-_r_2t2_ .marker{fill:none!important;stroke:rgb(175, 175, 175)!important;stroke-width:1;}#chatgpt-mermaid-_r_2t2_ [data-look=neo].labelBkg{background-color:rgba(48, 48, 48, 0.5);}#chatgpt-mermaid-_r_2t2_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-_r_2t2_ [data-look="neo"].node rect,#chatgpt-mermaid-_r_2t2_ [data-look="neo"].cluster rect,#chatgpt-mermaid-_r_2t2_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-_r_2t2_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_2t2_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-_r_2t2_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-_r_2t2_-gradient);stroke-width:1px;}#chatgpt-mermaid-_r_2t2_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_2t2_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-_r_2t2_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-_r_2t2_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_2t2_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-_r_2t2_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-_r_2t2_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_2t2_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-_r_2t2_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_2t2_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";}FT_USERFT_PURCHASE_REQUESTFT_ORDERFT_EVENTFT_SESSIONFT_VENUEFT_TICKET_TIERFT_INVENTORYFT_RESERVATIONFT_ORDER_ITEMFT_PAYMENTFT_REFUNDFT_USER_COUPONFT_COUPON_TEMPLATEFT_COUPON_USAGEsubmitsownscontainshostsprovideshasrequestedobtainscreatesreservescontainsreferencespayment_attemptsrefundsholdsissuesusage_historycoupon_usage





这是逻辑 ER 图。实际 MySQL 是否设置物理外键，会在下一步表结构设计时按数据一致性和高并发写入需求决定。
这里有几个需要统一的关键关系：
- 一个抢票请求最多创建一个订单。
- 一个成功抢票请求对应一条有效库存预占记录。
- 一个订单可以有多次支付尝试，但最多存在一个最终有效的成功支付结果。
- 一个用户优惠券可以有历史使用记录，但同一时刻只能被一个有效订单锁定。
- 一个订单第一版最多使用一张优惠券。
- 每个票档对应一条当前库存总账。

四、用户与权限域
P0 数据表
表名	核心字段	主要关系
ft_user	id、username、email、password_hash、status	统一身份主体
ft_user_profile	user_id、nickname、avatar_url	与用户一对一
ft_role	id、code、name、status	角色定义
ft_permission	id、code、name、type	权限定义
ft_user_role	user_id、role_id	多对多关联
ft_role_permission	role_id、permission_id	多对多关联
P1 扩展：
ft_login_audit、ft_operation_audit、ft_account_deletion、ft_user_session。
其中 Refresh Token 和短期会话状态继续使用 Redis；ft_user_session 主要用于需要长期审计和持久化恢复的场景，不重复存储明文 Refresh Token。
核心约束
- ft_user.username 唯一，注册规则不区分英文大小写。
- ft_user.email 唯一，邮箱规范化后比较。
- ft_user_role (user_id, role_id) 联合唯一。
- ft_role_permission (role_id, permission_id) 联合唯一。
- 角色码和权限码全局唯一。
- 禁止普通用户通过任何账号 API 自行提升权限。

五、演出、场馆、场次与票档域
P0 数据表
表名	作用
ft_event_category	演出分类
ft_event	演出活动
ft_venue	场馆
ft_session	演出场次
ft_ticket_tier	票档
ft_sale_plan	售票窗口及控制状态
ft_event_review	审核记录
P1：
ft_event_revision（发布快照）、ft_event_operation_log（活动操作历史）。
关系设计
#chatgpt-mermaid-_r_2tb_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-_r_2tb_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-_r_2tb_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-_r_2tb_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-_r_2tb_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-_r_2tb_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-_r_2tb_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-_r_2tb_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-_r_2tb_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-_r_2tb_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-_r_2tb_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-_r_2tb_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-_r_2tb_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-_r_2tb_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";font-size:16px;}#chatgpt-mermaid-_r_2tb_ p{margin:0;}#chatgpt-mermaid-_r_2tb_ .entityBox{fill:rgb(19, 33, 54);stroke:rgb(31, 78, 148);}#chatgpt-mermaid-_r_2tb_ .relationshipLabelBox{fill:rgb(48, 48, 48);opacity:0.7;background-color:rgb(48, 48, 48);}#chatgpt-mermaid-_r_2tb_ .relationshipLabelBox rect{opacity:0.5;}#chatgpt-mermaid-_r_2tb_ .labelBkg{background-color:rgba(48, 48, 48, 0.5);}#chatgpt-mermaid-_r_2tb_ .edgeLabel{background-color:rgb(14, 14, 14);}#chatgpt-mermaid-_r_2tb_ .edgeLabel .label rect{fill:rgb(14, 14, 14);}#chatgpt-mermaid-_r_2tb_ .edgeLabel .label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-_r_2tb_ .edgeLabel .label{fill:rgb(31, 78, 148);font-size:14px;}#chatgpt-mermaid-_r_2tb_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";color:rgb(237, 237, 237);}#chatgpt-mermaid-_r_2tb_ .edge-pattern-dashed{stroke-dasharray:8,8;}#chatgpt-mermaid-_r_2tb_ .node rect,#chatgpt-mermaid-_r_2tb_ .node circle,#chatgpt-mermaid-_r_2tb_ .node ellipse,#chatgpt-mermaid-_r_2tb_ .node polygon{fill:rgb(19, 33, 54);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-_r_2tb_ .relationshipLine{stroke:rgb(175, 175, 175);stroke-width:1px;fill:none;}#chatgpt-mermaid-_r_2tb_ .marker{fill:none!important;stroke:rgb(175, 175, 175)!important;stroke-width:1;}#chatgpt-mermaid-_r_2tb_ [data-look=neo].labelBkg{background-color:rgba(48, 48, 48, 0.5);}#chatgpt-mermaid-_r_2tb_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-_r_2tb_ [data-look="neo"].node rect,#chatgpt-mermaid-_r_2tb_ [data-look="neo"].cluster rect,#chatgpt-mermaid-_r_2tb_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-_r_2tb_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_2tb_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-_r_2tb_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-_r_2tb_-gradient);stroke-width:1px;}#chatgpt-mermaid-_r_2tb_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_2tb_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-_r_2tb_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-_r_2tb_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_2tb_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-_r_2tb_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-_r_2tb_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_2tb_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-_r_2tb_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_2tb_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";}FT_EVENT_CATEGORYFT_EVENTFT_SESSIONFT_VENUEFT_TICKET_TIERFT_SALE_PLANFT_EVENT_REVIEWFT_USERcategorizesscheduleshostscontainscontrolled_byreviewedperforms





第一版固定以下业务基数：
关系	约束
Event → Session	一对多
Venue → Session	一对多
Session → TicketTier	一对多
TicketTier → SalePlan	一对一
Event → EventReview	一对多
发布后的活动不允许随意修改已成交订单的历史信息，订单会保存交易快照。
ft_sale_plan 的当前版本号与销售控制状态需要能被库存和抢票模块校验，以防 Redis 使用旧的售票配置放行请求。
六、库存与预占域
这是整个数据库中一致性要求最高的部分之一。
P0 数据表
表名	主键	核心字段
ft_inventory	tier_id	total、available、reserved、sold、version、generation
ft_reservation	id	request_id、tier_id、quantity、status、expire_at
ft_inventory_ledger	id	operation_id、tier_id、reservation_id、operation、quantity
P1 增加 ft_inventory_adjustment，管理人工库存调整及审批。
库存关系
#chatgpt-mermaid-_r_2tk_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-_r_2tk_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-_r_2tk_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-_r_2tk_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-_r_2tk_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-_r_2tk_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-_r_2tk_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-_r_2tk_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-_r_2tk_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-_r_2tk_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-_r_2tk_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-_r_2tk_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-_r_2tk_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-_r_2tk_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";font-size:16px;}#chatgpt-mermaid-_r_2tk_ p{margin:0;}#chatgpt-mermaid-_r_2tk_ .entityBox{fill:rgb(19, 33, 54);stroke:rgb(31, 78, 148);}#chatgpt-mermaid-_r_2tk_ .relationshipLabelBox{fill:rgb(48, 48, 48);opacity:0.7;background-color:rgb(48, 48, 48);}#chatgpt-mermaid-_r_2tk_ .relationshipLabelBox rect{opacity:0.5;}#chatgpt-mermaid-_r_2tk_ .labelBkg{background-color:rgba(48, 48, 48, 0.5);}#chatgpt-mermaid-_r_2tk_ .edgeLabel{background-color:rgb(14, 14, 14);}#chatgpt-mermaid-_r_2tk_ .edgeLabel .label rect{fill:rgb(14, 14, 14);}#chatgpt-mermaid-_r_2tk_ .edgeLabel .label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-_r_2tk_ .edgeLabel .label{fill:rgb(31, 78, 148);font-size:14px;}#chatgpt-mermaid-_r_2tk_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";color:rgb(237, 237, 237);}#chatgpt-mermaid-_r_2tk_ .edge-pattern-dashed{stroke-dasharray:8,8;}#chatgpt-mermaid-_r_2tk_ .node rect,#chatgpt-mermaid-_r_2tk_ .node circle,#chatgpt-mermaid-_r_2tk_ .node ellipse,#chatgpt-mermaid-_r_2tk_ .node polygon{fill:rgb(19, 33, 54);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-_r_2tk_ .relationshipLine{stroke:rgb(175, 175, 175);stroke-width:1px;fill:none;}#chatgpt-mermaid-_r_2tk_ .marker{fill:none!important;stroke:rgb(175, 175, 175)!important;stroke-width:1;}#chatgpt-mermaid-_r_2tk_ [data-look=neo].labelBkg{background-color:rgba(48, 48, 48, 0.5);}#chatgpt-mermaid-_r_2tk_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-_r_2tk_ [data-look="neo"].node rect,#chatgpt-mermaid-_r_2tk_ [data-look="neo"].cluster rect,#chatgpt-mermaid-_r_2tk_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-_r_2tk_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_2tk_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-_r_2tk_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-_r_2tk_-gradient);stroke-width:1px;}#chatgpt-mermaid-_r_2tk_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_2tk_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-_r_2tk_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-_r_2tk_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_2tk_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-_r_2tk_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-_r_2tk_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_2tk_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-_r_2tk_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_2tk_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";}FT_TICKET_TIERFT_INVENTORYFT_RESERVATIONFT_INVENTORY_LEDGERFT_PURCHASE_REQUESTFT_ORDERhasallocatesrecordsgeneratesacquiresuses





统一库存语义
\[
total=available+reserved+sold
\]

操作	available	reserved	sold
初始化 100 张	100	0	0
预占 2 张	-2	+2	不变
支付确认 2 张	不变	-2	+2
未支付取消 2 张	+2	-2	不变
已支付全额退款	不变	不变	不变
经审批重新上架 2 张	+2	不变	-2
这里明确：sold 表示仍占用票务容量的已售数量，不直接代表当前未退款的有效票数。
退款成功但未重新上架的票，依然留在 sold 中。财务净销售额单独根据支付和退款记录统计。
必须具备的约束
- ft_inventory.tier_id 唯一，确保一个票档只有一份当前库存总账。
- ft_reservation.request_id 唯一。
- ft_inventory_ledger.operation_id 唯一。
- 库存字段不能小于零。
- 账本数量必须守恒。
- 重复确认和重复释放不能改变库存。
- 预占状态变更、库存更新、库存流水在同一事务中完成。
MySQL 8.4 支持 CHECK、唯一约束及外键等数据库约束，可以为重要库存字段增加数据库级防护。

MySQL
+1




七、抢票请求与异步排队域
P0 数据表
表名	核心字段	用途
ft_purchase_request	id、request_id、user_id、tier_id、quantity、status、lease_until、attempt_count	抢票请求和处理状态
ft_purchase_limit	user_id、tier_id、reserved_quantity、sold_quantity	用户限购占用计数
P1 增加 ft_purchase_attempt，记录详细处理尝试历史。
核心实体关系
#chatgpt-mermaid-_r_2u5_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-_r_2u5_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-_r_2u5_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-_r_2u5_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-_r_2u5_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-_r_2u5_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-_r_2u5_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-_r_2u5_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-_r_2u5_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-_r_2u5_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-_r_2u5_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-_r_2u5_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-_r_2u5_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-_r_2u5_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";font-size:16px;}#chatgpt-mermaid-_r_2u5_ p{margin:0;}#chatgpt-mermaid-_r_2u5_ .entityBox{fill:rgb(19, 33, 54);stroke:rgb(31, 78, 148);}#chatgpt-mermaid-_r_2u5_ .relationshipLabelBox{fill:rgb(48, 48, 48);opacity:0.7;background-color:rgb(48, 48, 48);}#chatgpt-mermaid-_r_2u5_ .relationshipLabelBox rect{opacity:0.5;}#chatgpt-mermaid-_r_2u5_ .labelBkg{background-color:rgba(48, 48, 48, 0.5);}#chatgpt-mermaid-_r_2u5_ .edgeLabel{background-color:rgb(14, 14, 14);}#chatgpt-mermaid-_r_2u5_ .edgeLabel .label rect{fill:rgb(14, 14, 14);}#chatgpt-mermaid-_r_2u5_ .edgeLabel .label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-_r_2u5_ .edgeLabel .label{fill:rgb(31, 78, 148);font-size:14px;}#chatgpt-mermaid-_r_2u5_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";color:rgb(237, 237, 237);}#chatgpt-mermaid-_r_2u5_ .edge-pattern-dashed{stroke-dasharray:8,8;}#chatgpt-mermaid-_r_2u5_ .node rect,#chatgpt-mermaid-_r_2u5_ .node circle,#chatgpt-mermaid-_r_2u5_ .node ellipse,#chatgpt-mermaid-_r_2u5_ .node polygon{fill:rgb(19, 33, 54);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-_r_2u5_ .relationshipLine{stroke:rgb(175, 175, 175);stroke-width:1px;fill:none;}#chatgpt-mermaid-_r_2u5_ .marker{fill:none!important;stroke:rgb(175, 175, 175)!important;stroke-width:1;}#chatgpt-mermaid-_r_2u5_ [data-look=neo].labelBkg{background-color:rgba(48, 48, 48, 0.5);}#chatgpt-mermaid-_r_2u5_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-_r_2u5_ [data-look="neo"].node rect,#chatgpt-mermaid-_r_2u5_ [data-look="neo"].cluster rect,#chatgpt-mermaid-_r_2u5_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-_r_2u5_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_2u5_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-_r_2u5_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-_r_2u5_-gradient);stroke-width:1px;}#chatgpt-mermaid-_r_2u5_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_2u5_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-_r_2u5_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-_r_2u5_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_2u5_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-_r_2u5_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-_r_2u5_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_2u5_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-_r_2u5_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_2u5_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";}FT_USERFT_PURCHASE_REQUESTFT_TICKET_TIERFT_RESERVATIONFT_ORDERFT_PURCHASE_LIMITsubmitstargetsholdscreateshaslimits





特别需要确认：
抢票请求、库存预占、订单并不是同一个业务实体。
例如：
- 请求被受理后可能失败，所以没有预占。
- Redis 可能临时预占成功，但 MySQL 订单尚未创建。
- 已完成的抢票请求会保留记录，订单后来可能取消。
- 订单关闭后，原抢票请求仍然应该保持抢票成功的历史终态。
因此，不能因为订单关闭就将原来的 ft_purchase_request.status 从 SUCCEEDED 改成 FAILED。
限购账本设计
建议 ft_purchase_limit 使用：
UNIQUE(user_id, tier_id)
限购额度包括当前未支付预占及仍计入限购的已支付票数。
对于退款是否恢复用户限购名额，V1 建议退款后不自动恢复，与门票不重新上架的政策一致。以后允许退票重售时，再独立定义限购额度恢复规则。

八、订单与交易域
P0 数据表
表名	核心字段	说明
ft_order	id、order_no、request_id、user_id、status、payable_amount_cent、expire_at	订单主体
ft_order_item	id、order_id、tier_id、quantity、unit_price_cent、snapshot	订单明细
ft_order_operation	id、operation_id、order_id、from_status、to_status	订单操作历史
关系
#chatgpt-mermaid-_r_2ue_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-_r_2ue_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-_r_2ue_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-_r_2ue_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-_r_2ue_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-_r_2ue_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-_r_2ue_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-_r_2ue_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-_r_2ue_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-_r_2ue_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-_r_2ue_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-_r_2ue_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-_r_2ue_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-_r_2ue_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";font-size:16px;}#chatgpt-mermaid-_r_2ue_ p{margin:0;}#chatgpt-mermaid-_r_2ue_ .entityBox{fill:rgb(19, 33, 54);stroke:rgb(31, 78, 148);}#chatgpt-mermaid-_r_2ue_ .relationshipLabelBox{fill:rgb(48, 48, 48);opacity:0.7;background-color:rgb(48, 48, 48);}#chatgpt-mermaid-_r_2ue_ .relationshipLabelBox rect{opacity:0.5;}#chatgpt-mermaid-_r_2ue_ .labelBkg{background-color:rgba(48, 48, 48, 0.5);}#chatgpt-mermaid-_r_2ue_ .edgeLabel{background-color:rgb(14, 14, 14);}#chatgpt-mermaid-_r_2ue_ .edgeLabel .label rect{fill:rgb(14, 14, 14);}#chatgpt-mermaid-_r_2ue_ .edgeLabel .label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-_r_2ue_ .edgeLabel .label{fill:rgb(31, 78, 148);font-size:14px;}#chatgpt-mermaid-_r_2ue_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";color:rgb(237, 237, 237);}#chatgpt-mermaid-_r_2ue_ .edge-pattern-dashed{stroke-dasharray:8,8;}#chatgpt-mermaid-_r_2ue_ .node rect,#chatgpt-mermaid-_r_2ue_ .node circle,#chatgpt-mermaid-_r_2ue_ .node ellipse,#chatgpt-mermaid-_r_2ue_ .node polygon{fill:rgb(19, 33, 54);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-_r_2ue_ .relationshipLine{stroke:rgb(175, 175, 175);stroke-width:1px;fill:none;}#chatgpt-mermaid-_r_2ue_ .marker{fill:none!important;stroke:rgb(175, 175, 175)!important;stroke-width:1;}#chatgpt-mermaid-_r_2ue_ [data-look=neo].labelBkg{background-color:rgba(48, 48, 48, 0.5);}#chatgpt-mermaid-_r_2ue_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-_r_2ue_ [data-look="neo"].node rect,#chatgpt-mermaid-_r_2ue_ [data-look="neo"].cluster rect,#chatgpt-mermaid-_r_2ue_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-_r_2ue_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_2ue_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-_r_2ue_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-_r_2ue_-gradient);stroke-width:1px;}#chatgpt-mermaid-_r_2ue_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_2ue_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-_r_2ue_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-_r_2ue_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_2ue_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-_r_2ue_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-_r_2ue_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_2ue_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-_r_2ue_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_2ue_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";}FT_USERFT_ORDERFT_PURCHASE_REQUESTFT_ORDER_ITEMFT_TICKET_TIERFT_ORDER_OPERATIONFT_RESERVATIONownsgeneratesincludesreferenceshistorybacks





订单唯一约束
约束	目的
UNIQUE(order_no)	业务订单号唯一
UNIQUE(request_id)	一个抢票请求最多创建一个订单
INDEX(user_id, created_at, id)	我的订单列表
INDEX(status, expire_at, id)	超时关单扫描
建议订单金额至少明确：
- original_amount_cent：优惠前金额。
- discount_amount_cent：优惠金额。
- payable_amount_cent：应付金额。
- paid_amount_cent：最终支付确认金额。
前三个字段在订单创建后作为交易快照，不能因优惠券或票价配置改变而自动更新。
订单状态采用：
PENDING_PAYMENT → PAID / CLOSED
退款进度另行管理，不将退款审核等流程混入订单主状态。

九、模拟支付与退款域
这里对之前的设计做一个重要统一：
Payment 是一笔支付业务单，PaymentAttempt 是这笔支付的某次执行尝试。
不建议用户每点击一次支付按钮就创建一条全新的、相互独立的有效支付业务单。
P0 数据表
表名	主要用途
ft_payment	支付业务单及最终状态
ft_payment_attempt	模拟渠道执行尝试
ft_payment_callback	回调记录与重复通知去重
ft_refund	退款业务单及审核状态
ft_refund_attempt	模拟退款执行尝试
P1 增加 ft_payment_operation，补充独立支付操作审计。
支付和退款关系
#chatgpt-mermaid-_r_2un_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-_r_2un_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-_r_2un_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-_r_2un_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-_r_2un_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-_r_2un_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-_r_2un_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-_r_2un_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-_r_2un_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-_r_2un_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-_r_2un_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-_r_2un_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-_r_2un_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-_r_2un_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";font-size:16px;}#chatgpt-mermaid-_r_2un_ p{margin:0;}#chatgpt-mermaid-_r_2un_ .entityBox{fill:rgb(19, 33, 54);stroke:rgb(31, 78, 148);}#chatgpt-mermaid-_r_2un_ .relationshipLabelBox{fill:rgb(48, 48, 48);opacity:0.7;background-color:rgb(48, 48, 48);}#chatgpt-mermaid-_r_2un_ .relationshipLabelBox rect{opacity:0.5;}#chatgpt-mermaid-_r_2un_ .labelBkg{background-color:rgba(48, 48, 48, 0.5);}#chatgpt-mermaid-_r_2un_ .edgeLabel{background-color:rgb(14, 14, 14);}#chatgpt-mermaid-_r_2un_ .edgeLabel .label rect{fill:rgb(14, 14, 14);}#chatgpt-mermaid-_r_2un_ .edgeLabel .label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-_r_2un_ .edgeLabel .label{fill:rgb(31, 78, 148);font-size:14px;}#chatgpt-mermaid-_r_2un_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";color:rgb(237, 237, 237);}#chatgpt-mermaid-_r_2un_ .edge-pattern-dashed{stroke-dasharray:8,8;}#chatgpt-mermaid-_r_2un_ .node rect,#chatgpt-mermaid-_r_2un_ .node circle,#chatgpt-mermaid-_r_2un_ .node ellipse,#chatgpt-mermaid-_r_2un_ .node polygon{fill:rgb(19, 33, 54);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-_r_2un_ .relationshipLine{stroke:rgb(175, 175, 175);stroke-width:1px;fill:none;}#chatgpt-mermaid-_r_2un_ .marker{fill:none!important;stroke:rgb(175, 175, 175)!important;stroke-width:1;}#chatgpt-mermaid-_r_2un_ [data-look=neo].labelBkg{background-color:rgba(48, 48, 48, 0.5);}#chatgpt-mermaid-_r_2un_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-_r_2un_ [data-look="neo"].node rect,#chatgpt-mermaid-_r_2un_ [data-look="neo"].cluster rect,#chatgpt-mermaid-_r_2un_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-_r_2un_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_2un_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-_r_2un_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-_r_2un_-gradient);stroke-width:1px;}#chatgpt-mermaid-_r_2un_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_2un_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-_r_2un_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-_r_2un_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_2un_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-_r_2un_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-_r_2un_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_2un_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-_r_2un_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_2un_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";}FT_ORDERFT_PAYMENTFT_PAYMENT_ATTEMPTFT_PAYMENT_CALLBACKFT_REFUNDFT_REFUND_ATTEMPTpaymentsattemptscallbacksrefundsattempts





数据库必须保障的规则
1. 同一订单最多存在一笔最终成功的有效支付业务。
2. 一笔支付可以存在多次执行尝试，但不能形成重复成功确认。
3. 回调以 (channel, channel_event_id) 去重。
4. 退款总成功金额不能超过原支付成功金额。
5. 重复退款执行不能造成两笔有效退款。
6. 支付、订单、库存最终确认在同一个 MySQL 事务中完成。
MySQL 并不支持直接创建传统意义上的部分唯一索引来表示“仅当 status='SUCCEEDED' 时唯一”。因此，最终成功支付的唯一性建议由每订单唯一的确认记录或唯一成功占用键配合事务状态条件更新实现，不能单纯依赖普通状态字段上的联合唯一索引。
对于 V1，可以进一步收紧为同一个订单只有一个支付业务单，失败重试创建新的 Attempt。这既降低复杂度，也方便证明支付幂等。

十、优惠券与营销域
P0 数据表
表名	作用
ft_campaign	营销活动
ft_coupon_template	优惠券规则模板
ft_coupon_quota	模板发行额度账本
ft_coupon_claim_request	异步领券请求
ft_coupon_user_quota	用户领取额度
ft_user_coupon	已发行的个人优惠券
ft_coupon_usage	优惠券订单锁定与核销记录
P1：
ft_coupon_ledger、ft_campaign_operation。
这里与先前模块草案稍作统一：优惠券发行和使用流水是重要的审计数据，应在 P0 核心操作中先确保记录可追溯。可以初期复用操作记录，P1 再独立优化查询与管理界面，不代表 P0 可以完全不记录额度变化。
ER 关系
#chatgpt-mermaid-_r_2v0_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-_r_2v0_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-_r_2v0_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-_r_2v0_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-_r_2v0_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-_r_2v0_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-_r_2v0_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-_r_2v0_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-_r_2v0_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-_r_2v0_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-_r_2v0_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-_r_2v0_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-_r_2v0_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-_r_2v0_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";font-size:16px;}#chatgpt-mermaid-_r_2v0_ p{margin:0;}#chatgpt-mermaid-_r_2v0_ .entityBox{fill:rgb(19, 33, 54);stroke:rgb(31, 78, 148);}#chatgpt-mermaid-_r_2v0_ .relationshipLabelBox{fill:rgb(48, 48, 48);opacity:0.7;background-color:rgb(48, 48, 48);}#chatgpt-mermaid-_r_2v0_ .relationshipLabelBox rect{opacity:0.5;}#chatgpt-mermaid-_r_2v0_ .labelBkg{background-color:rgba(48, 48, 48, 0.5);}#chatgpt-mermaid-_r_2v0_ .edgeLabel{background-color:rgb(14, 14, 14);}#chatgpt-mermaid-_r_2v0_ .edgeLabel .label rect{fill:rgb(14, 14, 14);}#chatgpt-mermaid-_r_2v0_ .edgeLabel .label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-_r_2v0_ .edgeLabel .label{fill:rgb(31, 78, 148);font-size:14px;}#chatgpt-mermaid-_r_2v0_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";color:rgb(237, 237, 237);}#chatgpt-mermaid-_r_2v0_ .edge-pattern-dashed{stroke-dasharray:8,8;}#chatgpt-mermaid-_r_2v0_ .node rect,#chatgpt-mermaid-_r_2v0_ .node circle,#chatgpt-mermaid-_r_2v0_ .node ellipse,#chatgpt-mermaid-_r_2v0_ .node polygon{fill:rgb(19, 33, 54);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-_r_2v0_ .relationshipLine{stroke:rgb(175, 175, 175);stroke-width:1px;fill:none;}#chatgpt-mermaid-_r_2v0_ .marker{fill:none!important;stroke:rgb(175, 175, 175)!important;stroke-width:1;}#chatgpt-mermaid-_r_2v0_ [data-look=neo].labelBkg{background-color:rgba(48, 48, 48, 0.5);}#chatgpt-mermaid-_r_2v0_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-_r_2v0_ [data-look="neo"].node rect,#chatgpt-mermaid-_r_2v0_ [data-look="neo"].cluster rect,#chatgpt-mermaid-_r_2v0_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-_r_2v0_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_2v0_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-_r_2v0_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-_r_2v0_-gradient);stroke-width:1px;}#chatgpt-mermaid-_r_2v0_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_2v0_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-_r_2v0_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-_r_2v0_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_2v0_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-_r_2v0_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-_r_2v0_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_2v0_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-_r_2v0_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_2v0_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";}FT_CAMPAIGNFT_COUPON_TEMPLATEFT_COUPON_QUOTAFT_COUPON_CLAIM_REQUESTFT_USERFT_USER_COUPONFT_COUPON_USAGEFT_ORDERoffersquotaclaimssubmitsissuesownshistoryuses





需要统一的业务约束
- 一笔订单最多使用一张优惠券。
- 一张用户优惠券同一时刻只能被一个有效订单锁定。
- 订单创建、锁券、金额快照同事务。
- 支付成功时将券从 LOCKED 变成 USED。
- 未支付关单时根据当前有效期返券。
- 已支付退款成功后，V1 默认不返券。
- 模板已经发放的券，不因后来修改模板而改变优惠金额和适用范围。
对于券重复锁定，可以使用用户优惠券行上的条件状态更新保证互斥，而不是依赖先查询状态再更新。
十一、风控与防刷域
风控模块采用规则持久化 + Redis 高频计数 + 风险事件审计的结构。
P0 数据表
表名	核心字段	作用
ft_risk_rule	id、scene、rule_code、config、status、version	风控规则
ft_risk_restriction	id、subject_type、subject_id、scope、expire_at	临时限制
ft_risk_event	id、request_id、rule_id、action、occurred_at	风险事件
P1：
ft_risk_rule_version、ft_risk_decision、ft_risk_operation。
关键设计
- 高并发限流计数保存在 Redis，不在每次请求时同步写 MySQL。
- MySQL 保存需要审计的限制、规则及重要风险事件。
- 临时限制必须保留生效与解除时间。
- 管理员撤销限制要保留操作依据。
- 用户业务限制与 ft_user.status 是两个不同概念。
- 风控事件不得直接替代订单和库存的业务状态。
十二、通知与消息中心域
P0 数据表
表名	关键字段	作用
ft_notification	id、event_id、type、title、content、biz_id	通知内容
ft_notification_recipient	notification_id、user_id、read_at	用户接收与已读状态
P1：
ft_notification_template、ft_announcement、ft_announcement_delivery。
ER 关系
#chatgpt-mermaid-_r_2v9_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-_r_2v9_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-_r_2v9_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-_r_2v9_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-_r_2v9_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-_r_2v9_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-_r_2v9_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-_r_2v9_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-_r_2v9_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-_r_2v9_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-_r_2v9_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-_r_2v9_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-_r_2v9_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-_r_2v9_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";font-size:16px;}#chatgpt-mermaid-_r_2v9_ p{margin:0;}#chatgpt-mermaid-_r_2v9_ .entityBox{fill:rgb(19, 33, 54);stroke:rgb(31, 78, 148);}#chatgpt-mermaid-_r_2v9_ .relationshipLabelBox{fill:rgb(48, 48, 48);opacity:0.7;background-color:rgb(48, 48, 48);}#chatgpt-mermaid-_r_2v9_ .relationshipLabelBox rect{opacity:0.5;}#chatgpt-mermaid-_r_2v9_ .labelBkg{background-color:rgba(48, 48, 48, 0.5);}#chatgpt-mermaid-_r_2v9_ .edgeLabel{background-color:rgb(14, 14, 14);}#chatgpt-mermaid-_r_2v9_ .edgeLabel .label rect{fill:rgb(14, 14, 14);}#chatgpt-mermaid-_r_2v9_ .edgeLabel .label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-_r_2v9_ .edgeLabel .label{fill:rgb(31, 78, 148);font-size:14px;}#chatgpt-mermaid-_r_2v9_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";color:rgb(237, 237, 237);}#chatgpt-mermaid-_r_2v9_ .edge-pattern-dashed{stroke-dasharray:8,8;}#chatgpt-mermaid-_r_2v9_ .node rect,#chatgpt-mermaid-_r_2v9_ .node circle,#chatgpt-mermaid-_r_2v9_ .node ellipse,#chatgpt-mermaid-_r_2v9_ .node polygon{fill:rgb(19, 33, 54);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-_r_2v9_ .relationshipLine{stroke:rgb(175, 175, 175);stroke-width:1px;fill:none;}#chatgpt-mermaid-_r_2v9_ .marker{fill:none!important;stroke:rgb(175, 175, 175)!important;stroke-width:1;}#chatgpt-mermaid-_r_2v9_ [data-look=neo].labelBkg{background-color:rgba(48, 48, 48, 0.5);}#chatgpt-mermaid-_r_2v9_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-_r_2v9_ [data-look="neo"].node rect,#chatgpt-mermaid-_r_2v9_ [data-look="neo"].cluster rect,#chatgpt-mermaid-_r_2v9_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-_r_2v9_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_2v9_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-_r_2v9_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-_r_2v9_-gradient);stroke-width:1px;}#chatgpt-mermaid-_r_2v9_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_2v9_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-_r_2v9_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-_r_2v9_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_2v9_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-_r_2v9_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-_r_2v9_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_2v9_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-_r_2v9_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_2v9_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";}FT_NOTIFICATIONFT_NOTIFICATION_RECIPIENTFT_USERdeliversreceives





个人通知与阅读状态分离，有利于复用通知正文。
建议采用：
UNIQUE(notification_id, user_id)
对于业务去重，则使用通知事件 ID、通知类型和接收人的组合保证幂等。
批量通知可以只生成一份通知内容，再为不同用户创建各自的接收记录。
十三、运营统计与对账域
P0 数据表
表名	作用
ft_stats_platform_daily	平台每日统计
ft_stats_event_daily	演出每日统计
ft_reconcile_job	对账任务
ft_reconcile_exception	差异工单
ft_compensation_task	自动或人工补偿任务
P1：
ft_stats_tier_hourly、ft_stats_coupon_daily、ft_stats_aggregation_job、ft_reconcile_batch、ft_reconcile_action。
特别重要的数据原则
统计表不是业务账本。
例如 ft_stats_platform_daily.payment_amount_cent 只能用于运营看板，不能决定某笔订单是否已支付。
对账模块发现：
- 订单已支付但库存未确认；
- 退款成功金额超过原支付金额；
- Redis 发放资格多于数据库可售额度；
应创建异常记录和补偿任务，而不是直接修改统计表来掩盖问题。
对账任务还要记录扫描范围、业务截止水位线和检查时间，避免把并发中的正常变化误判为账目异常。
十四、共享基础设施域
这是 RocketMQ 可靠消息的数据库支撑。
P0 数据表
表名	关键字段	作用
ft_outbox_event	id、event_id、topic、payload、status、next_retry_at	可靠待发布事件
ft_message_inbox	consumer_name、event_id、processed_at	消费幂等
关系示意
业务 MySQL 事务
订单、支付、库存等业务数据更新


ft_outbox_event
和业务状态同事务持久化


RocketMQ Broker


ft_message_inbox + 消费端业务事务
同事务去重、状态推进


建议 ft_outbox_event 具备：
- event_id：业务事件唯一标识。
- aggregate_type、aggregate_id：归属业务实体。
- topic、event_type：消息路由与类型。
- payload、schema_version：消息数据及版本。
- status：待发送、发送中、发送完成、异常。
- attempt_count、next_retry_at：重试管理。
- lease_owner、lease_until：发布器领取租约。
- created_at、sent_at：时间记录。
对于 ft_message_inbox，建议使用：
PRIMARY KEY(consumer_name, event_id)
但要注意：如果一个消费处理流程要写入其他业务数据，Inbox 插入与业务操作应同事务提交，不能先单独提交 Inbox，再执行业务，否则可能出现“消息已标记消费但业务未完成”的问题。
十五、全局主键、索引和关联规则
15.1 主键与业务编号
类型	建议方案
大部分实体主键	BIGINT 雪花 ID
用户 ID	BIGINT
订单号	VARCHAR(32) 唯一
抢票请求号	VARCHAR(64) 唯一
支付业务编号	VARCHAR(40) 唯一
退款业务编号	VARCHAR(40) 唯一
消息事件 ID	VARCHAR(64) 唯一
幂等操作 ID	VARCHAR(64) 唯一
雪花主键属于数据库内部实体标识，不需要让用户看到主键生成规则。
由于 JavaScript 无法安全表示全部 64 位整数，前端 API 中的雪花 ID 统一以字符串传输。
15.2 必须具备的唯一约束
表	唯一约束	保护目标
ft_user	username、email	防重复账号
ft_inventory	tier_id	一票档一账本
ft_reservation	request_id	一请求一预占
ft_purchase_request	request_id	抢票幂等
ft_purchase_limit	user_id + tier_id	原子限购
ft_order	order_no、request_id	防重复订单
ft_payment	order_id（V1）	一订单一支付业务
ft_payment_callback	channel + channel_event_id	回调幂等
ft_refund	refund_no	退款业务幂等
ft_coupon_quota	template_id	一模板一发行账本
ft_coupon_claim_request	request_id	领券幂等
ft_coupon_user_quota	user_id + template_id	用户限领
ft_notification_recipient	notification_id + user_id	防重复接收
ft_outbox_event	event_id	事件唯一
ft_message_inbox	consumer_name + event_id	消费幂等
15.3 索引设计原则
不建议每个字段都建索引，而是根据查询场景设计复合索引。
查询场景	推荐索引字段
我的订单	(user_id, created_at, id)
超时关单扫描	(status, expire_at, id)
Outbox 任务扫描	(status, next_retry_at, id)
抢票异常恢复	(status, lease_until, id)
用户优惠券	(user_id, status, valid_to, id)
票档预占扫描	(tier_id, status, expire_at)
对账异常列表	(status, severity, created_at)
演出场次列表	(event_id, start_at, id)
最终索引数量需要结合 EXPLAIN ANALYZE、真实 SQL 和数据分布决定。
MySQL InnoDB 的二级索引包含主键列，主键长度会影响索引空间，因此我们统一使用较紧凑的 BIGINT 主键。

MySQL



15.4 外键策略
我建议采用选择性数据库外键 + 完整业务一致性约束，而不是简单地“全部外键”或“全部不加外键”。
- 用户、角色、权限及相对静态的基础配置关系，可以使用物理外键。
- 订单、库存流水、预占、Outbox 等高频写入与历史流水表，优先使用显式关联 ID、唯一约束和事务维护业务一致性。
- 无论是否设置物理外键，都不允许随意物理删除已经被交易引用的记录。
- 必须明确哪些关系强制存在、哪些是可选关系，以及删除、禁用、归档时的行为。
MySQL 支持 InnoDB 外键并能维护引用完整性，但其锁与索引影响应在实际写入场景下评估。

MySQL




十六、最重要的五个本地事务边界
FlashTicket 是模块化单体，最大的优势之一是关键业务可以在同一个数据库事务中完成，不需要为了技术复杂度而引入分布式事务。
业务操作	必须协调的数据库操作
抢票创建订单	抢票请求、限购占用、库存预占、订单、Outbox
支付成功	支付单、订单状态、库存已售、优惠券核销、Outbox
未支付关单	订单关闭、库存释放、限购释放、优惠券解锁、Outbox
领券成功	发行额度、用户领取额度、用户券、请求终态、Outbox
模拟退款成功	退款单、退款执行记录、票务权益状态、Outbox
这些操作必须遵守：
- 核心数据库更新在同一个 Spring 本地事务中完成。
- 关键写入使用条件更新和唯一约束兜底。
- 多行更新采用稳定的加锁顺序。
- 不在持有数据库事务时执行缓慢的外部网络调用。
- 事务成功提交后再发布实时通知。
- Redis 操作不随 MySQL 回滚，必须有独立恢复机制。
其中模拟退款还需要明确票务权益的记录方式。建议在正式 DDL 设计时增加订单级的票务权益状态，或者 P1 增加独立的票券权益表，避免退款成功后仍存在可使用的有效购票凭证。
十七、全局设计中需要统一的六处问题
前面十个模块分别设计时，有些概念容易重复或产生不一致。现在统一如下：
问题	最终设计决定
库存里的 sold 是否等于未退款票数？	否。它是已售占用容量；净有效票数另行统计
抢票成功后订单取消，请求状态是否改为失败？	否。原抢票成功历史不回退
订单和库存是否通过 MQ 实现最终一致性？	核心数据库写入使用同一 MySQL 事务，MQ 主要负责异步任务及后续事件
一笔订单能否创建多个支付单？	V1 一订单一支付业务单，允许多次受控执行尝试
退款是否返还库存和优惠券？	V1 均不自动返还，特殊回补必须走独立业务
对账和统计能否直接修改交易事实？	不允许。统计可以重算；账务修复必须依照独立补偿流程
还需要将“已退款但未重新上架的票务权益”和“支付成功但出票尚未完成”的情况，在后续详细表设计中明确建模。这是整个交易闭环中必须落实的部分。
十八、分阶段数据库迁移规划
虽然最终 P0 模型规划了 45 张表，但开发不应该一次性创建所有表。
建议通过 Flyway 按以下阶段构建：
数据库落地路线图
1第一阶段：账户与活动完成登录、后台权限和演出展示2第二阶段：库存与订单完成 MySQL 同步抢票交易闭环3第三阶段：支付与可靠消息实现支付、关单和 RocketMQ 异步处理4第四阶段：营销与风控完成抢券、锁券和限流保护5第五阶段：通知与对账实现用户通知和异常恢复6第六阶段：增强扩展完善生产级审计及复杂票务能力
第一阶段：账户与活动
用户、角色权限、活动、场馆、场次、票档、售票计划
验收：完成登录、后台权限和演出展示




Flyway 迁移脚本后续按照业务演进命名和编号，每一阶段同时准备初始化数据、唯一约束及集成测试。
十九、数据库设计验收清单
全局 ER 设计验收
0/16 已检查



[ ] 所有实体拥有唯一主键，跨表 ID 类型一致
[ ] 抢票请求、库存预占和订单具有明确的一对零或一关系
[ ] 每个票档最多一份当前库存总账
[ ] 库存守恒、重复扣减、重复释放受到数据库约束保护
[ ] 一笔订单最多一个最终有效支付结果
[ ] 订单支付成功与超时关单不能同时提交
[ ] 优惠券不能被两个有效订单同时锁定
[ ] 订单金额及活动信息保存交易快照
[ ] 退款不会无意中恢复已售票务容量
[ ] Outbox 与业务数据同事务持久化
[ ] Inbox 与消费业务写入同事务提交
[ ] 所有关键流水都有稳定的幂等操作 ID
[ ] 对账可以定位到具体请求、订单、预占和事件
[ ] 业务时间、金额、雪花 ID 的类型规范统一
[ ] 每个高频查询都有明确的索引及压测验证计划
[ ] 高风险数据修改具备审计和安全补偿路径
本清单用于确认模型设计，是否真正符合要求还需通过 DDL 约束和数据库集成测试验证。
二十、FlashTicket 全局 ER 设计结论
全局逻辑数据库模型 V1.0


数据库： 一个 MySQL 8.4 实例，一个 flash_ticket 业务库。
业务划分： 十大业务域 + 共享可靠消息基础设施。
表结构规模： 当前规划 45 张 P0 核心表和 22 张 P1 扩展表，最终以详细 DDL 审核后的去重结果为准。
业务核心： ft_purchase_request → ft_reservation → ft_order → ft_payment → ft_refund。
库存一致性： 三态库存账本 + 预占状态机 + 库存流水。
交易一致性： MySQL 本地事务、唯一索引、条件状态更新。
异步一致性： Outbox + RocketMQ + Inbox + 恢复任务。
营销一致性： 独立发行额度、用户限领、订单锁券和核销。
数据治理： 交易快照、操作审计、统计汇总、对账差异与补偿。