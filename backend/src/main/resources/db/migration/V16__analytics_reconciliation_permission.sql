-- M16 运营统计与系统对账：权限点种子（7 个）
-- 对账/补偿业务表在 V17（M16-B 阶段）追加，本迁移只种权限点，避免阶段性空表。

-- 1) 运营统计 + 对账 + 补偿权限点（id 248 起，承接 M15 risk:audit:read 247）
INSERT INTO ft_permission (id, code, name, type, status) VALUES
    (248, 'analytics:read',        '运营统计查询', 'API', 'ACTIVE'),
    (249, 'analytics:export',      '统计数据导出', 'API', 'ACTIVE'),
    (250, 'reconciliation:read',   '对账查询',     'API', 'ACTIVE'),
    (251, 'reconciliation:execute','对账执行',     'API', 'ACTIVE'),
    (252, 'reconciliation:resolve','对账异常处理', 'API', 'ACTIVE'),
    (253, 'compensation:execute',  '补偿执行',     'API', 'ACTIVE'),
    (254, 'compensation:approve',  '补偿审批',     'API', 'ACTIVE');

-- 2) 角色授权（id 769 起，承接 M15 risk:audit:read 768）
--    角色：USER(1) OPERATOR(2) FINANCE(3) ADMIN(4) SUPER_ADMIN(5)
INSERT INTO ft_role_permission (id, role_id, permission_id) VALUES
    -- analytics:read (248) → OPERATOR / FINANCE / ADMIN / SUPER_ADMIN
    (769, 2, 248), (770, 3, 248), (771, 4, 248), (772, 5, 248),
    -- analytics:export (249) → FINANCE / SUPER_ADMIN
    (773, 3, 249), (774, 5, 249),
    -- reconciliation:read (250) → OPERATOR / FINANCE / SUPER_ADMIN
    (775, 2, 250), (776, 3, 250), (777, 5, 250),
    -- reconciliation:execute (251) → FINANCE / SUPER_ADMIN
    (778, 3, 251), (779, 5, 251),
    -- reconciliation:resolve (252) → SUPER_ADMIN
    (780, 5, 252),
    -- compensation:execute (253) → SUPER_ADMIN
    (781, 5, 253),
    -- compensation:approve (254) → SUPER_ADMIN
    (782, 5, 254);
