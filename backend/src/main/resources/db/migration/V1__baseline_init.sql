-- FlashTicket M00 空基线迁移
-- 规则三「Flyway 跟随业务开发」：M00 不创建任何业务表。
-- 本文件仅用于验证 Flyway 管线可运行；flyway_schema_history 由 Flyway 自动创建。
-- 业务表（ft_user 等）自 M01 起以 V2、V3... 逐阶段追加。
SELECT 1;
