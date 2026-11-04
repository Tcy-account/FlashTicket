package com.flashticket.event.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.flashticket.common.api.ErrorCode;
import com.flashticket.common.exception.BizException;
import com.flashticket.common.security.AuthUser;
import com.flashticket.event.dto.CategoryRequest;
import com.flashticket.event.dto.CategoryResponse;
import com.flashticket.event.entity.EventCategory;
import com.flashticket.event.enums.CategoryStatus;
import com.flashticket.event.mapper.EventCategoryMapper;
import com.flashticket.identity.domain.OperationAction;
import com.flashticket.identity.service.OperationAuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 演出分类编排服务（M04）：创建、编辑、启停、列表（后台/公开）。
 *
 * <p>规则（见 doc/全局逻辑ER模型.md 二）：code 为稳定业务标识、全局唯一；
 * 分类不物理删除，停用后前台不可见，已有活动引用关系保留。
 */
@Service
public class EventCategoryService {

    private final EventCategoryMapper categoryMapper;
    private final OperationAuditService auditService;

    public EventCategoryService(EventCategoryMapper categoryMapper,
                                OperationAuditService auditService) {
        this.categoryMapper = categoryMapper;
        this.auditService = auditService;
    }

    /** 后台全量分类列表（含停用），按 sort_order 升序。 */
    public List<CategoryResponse> listAll() {
        return categoryMapper.selectList(new LambdaQueryWrapper<EventCategory>()
                        .orderByAsc(EventCategory::getSortOrder)
                        .orderByAsc(EventCategory::getId))
                .stream().map(EventCategoryService::toResponse).toList();
    }

    /** 公开分类列表（仅启用），供用户端筛选。 */
    public List<CategoryResponse> listEnabled() {
        return categoryMapper.selectList(new LambdaQueryWrapper<EventCategory>()
                        .eq(EventCategory::getStatus, CategoryStatus.ENABLED.name())
                        .orderByAsc(EventCategory::getSortOrder)
                        .orderByAsc(EventCategory::getId))
                .stream().map(EventCategoryService::toResponse).toList();
    }

    /** 创建分类。 */
    @Transactional
    public CategoryResponse create(CategoryRequest req, AuthUser operator, String ip) {
        String code = normalizeCode(req.code());
        assertCodeAvailable(code, null);

        EventCategory category = new EventCategory();
        category.setCode(code);
        category.setName(req.name().trim());
        category.setDescription(trimToNull(req.description()));
        category.setSortOrder(req.sortOrder() == null ? 0 : req.sortOrder());
        category.setStatus(CategoryStatus.ENABLED.name());
        categoryMapper.insert(category);

        auditService.record(operator.getUserId(), operator.getUsername(), OperationAction.CATEGORY_CREATE,
                OperationAction.TARGET_CATEGORY, category.getId(),
                Map.of("code", code, "name", category.getName()), ip);

        return toResponse(category);
    }

    /** 编辑分类（code 唯一性排除自身）。 */
    @Transactional
    public CategoryResponse update(Long id, CategoryRequest req, AuthUser operator, String ip) {
        EventCategory category = requireCategory(id);
        String code = normalizeCode(req.code());
        assertCodeAvailable(code, id);

        category.setCode(code);
        category.setName(req.name().trim());
        category.setDescription(trimToNull(req.description()));
        category.setSortOrder(req.sortOrder() == null ? category.getSortOrder() : req.sortOrder());
        categoryMapper.updateById(category);

        auditService.record(operator.getUserId(), operator.getUsername(), OperationAction.CATEGORY_UPDATE,
                OperationAction.TARGET_CATEGORY, id,
                Map.of("code", code, "name", category.getName()), ip);

        return toResponse(category);
    }

    /** 启用/停用分类。 */
    @Transactional
    public void toggle(Long id, String status, AuthUser operator, String ip) {
        EventCategory category = requireCategory(id);
        category.setStatus(status);
        categoryMapper.updateById(category);

        auditService.record(operator.getUserId(), operator.getUsername(), OperationAction.CATEGORY_TOGGLE,
                OperationAction.TARGET_CATEGORY, id, Map.of("status", status), ip);
    }

    /* ---------- 内部 ---------- */

    private EventCategory requireCategory(Long id) {
        EventCategory category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BizException(ErrorCode.EVENT_CATEGORY_NOT_FOUND);
        }
        return category;
    }

    private void assertCodeAvailable(String code, Long excludeId) {
        boolean taken = excludeId == null
                ? categoryMapper.findByCode(code) != null
                : categoryMapper.countByCodeExcluding(code, excludeId) > 0;
        if (taken) {
            throw new BizException(ErrorCode.EVENT_CATEGORY_CODE_TAKEN);
        }
    }

    private String normalizeCode(String code) {
        return code == null ? null : code.trim().toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static CategoryResponse toResponse(EventCategory c) {
        return new CategoryResponse(
                String.valueOf(c.getId()), c.getCode(), c.getName(), c.getDescription(),
                c.getSortOrder(), c.getStatus(), c.getCreatedAt(), c.getUpdatedAt());
    }
}
