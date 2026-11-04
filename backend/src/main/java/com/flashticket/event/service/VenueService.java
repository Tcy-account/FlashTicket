package com.flashticket.event.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.flashticket.common.api.ErrorCode;
import com.flashticket.common.dto.PageResult;
import com.flashticket.common.exception.BizException;
import com.flashticket.common.security.AuthUser;
import com.flashticket.event.dto.VenueRequest;
import com.flashticket.event.dto.VenueResponse;
import com.flashticket.event.entity.Venue;
import com.flashticket.event.enums.VenueStatus;
import com.flashticket.event.mapper.VenueMapper;
import com.flashticket.identity.domain.OperationAction;
import com.flashticket.identity.service.OperationAuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 演出场馆编排服务（M04）：创建、编辑、启停、分页查询（城市筛选、容量校验）。
 *
 * <p>规则（见 doc/演出活动与场次.md 4.1 / doc/全局逻辑ER模型.md 四）：
 * max_capacity &gt; 0；场馆停用不自动取消已有演出；被场次引用后不得物理删除。
 * 场馆 version 字段预留用于 M05 场次排期的并发版本控制，M04 暂不启用乐观锁。
 */
@Service
public class VenueService {

    private static final String DEFAULT_COUNTRY_CODE = "CN";
    private static final String DEFAULT_TIMEZONE = "Asia/Shanghai";

    private final VenueMapper venueMapper;
    private final OperationAuditService auditService;

    public VenueService(VenueMapper venueMapper, OperationAuditService auditService) {
        this.venueMapper = venueMapper;
        this.auditService = auditService;
    }

    /** 后台场馆分页列表（城市/状态/名称筛选）。 */
    public PageResult<VenueResponse> list(long page, long pageSize,
                                          String city, String status, String name) {
        Page<Venue> p = new Page<>(page, pageSize);
        IPage<Venue> result = venueMapper.selectVenuePage(p,
                trimToNull(city), trimToNull(status), trimToNull(name));
        List<VenueResponse> items = result.getRecords().stream()
                .map(VenueService::toResponse).toList();
        return new PageResult<>(items, result.getTotal(), result.getCurrent(), result.getSize());
    }

    /** 公开场馆列表（仅启用，城市筛选），供用户端浏览。 */
    public PageResult<VenueResponse> listEnabled(long page, long pageSize, String city) {
        Page<Venue> p = new Page<>(page, pageSize);
        IPage<Venue> result = venueMapper.selectVenuePage(p,
                trimToNull(city), VenueStatus.ENABLED.name(), null);
        List<VenueResponse> items = result.getRecords().stream()
                .map(VenueService::toResponse).toList();
        return new PageResult<>(items, result.getTotal(), result.getCurrent(), result.getSize());
    }

    /** 创建场馆。 */
    @Transactional
    public VenueResponse create(VenueRequest req, AuthUser operator, String ip) {
        Venue venue = new Venue();
        venue.setName(req.name().trim());
        venue.setCountryCode(valueOrDefault(req.countryCode(), DEFAULT_COUNTRY_CODE));
        venue.setProvince(trimToNull(req.province()));
        venue.setCity(req.city().trim());
        venue.setDistrict(trimToNull(req.district()));
        venue.setAddress(req.address().trim());
        venue.setTimezone(valueOrDefault(req.timezone(), DEFAULT_TIMEZONE));
        venue.setMaxCapacity(req.maxCapacity());
        venue.setDescription(trimToNull(req.description()));
        venue.setStatus(VenueStatus.ENABLED.name());
        venue.setVersion(0L);
        venueMapper.insert(venue);

        auditService.record(operator.getUserId(), operator.getUsername(), OperationAction.VENUE_CREATE,
                OperationAction.TARGET_VENUE, venue.getId(),
                Map.of("name", venue.getName(), "city", venue.getCity(),
                        "maxCapacity", venue.getMaxCapacity()), ip);

        return toResponse(venue);
    }

    /** 编辑场馆。 */
    @Transactional
    public VenueResponse update(Long id, VenueRequest req, AuthUser operator, String ip) {
        Venue venue = requireVenue(id);
        venue.setName(req.name().trim());
        venue.setCountryCode(valueOrDefault(req.countryCode(), venue.getCountryCode()));
        venue.setProvince(trimToNull(req.province()));
        venue.setCity(req.city().trim());
        venue.setDistrict(trimToNull(req.district()));
        venue.setAddress(req.address().trim());
        venue.setTimezone(valueOrDefault(req.timezone(), venue.getTimezone()));
        venue.setMaxCapacity(req.maxCapacity());
        venue.setDescription(trimToNull(req.description()));
        venueMapper.updateById(venue);

        auditService.record(operator.getUserId(), operator.getUsername(), OperationAction.VENUE_UPDATE,
                OperationAction.TARGET_VENUE, id,
                Map.of("name", venue.getName(), "city", venue.getCity(),
                        "maxCapacity", venue.getMaxCapacity()), ip);

        return toResponse(venue);
    }

    /** 启用/停用场馆。 */
    @Transactional
    public void toggle(Long id, String status, AuthUser operator, String ip) {
        Venue venue = requireVenue(id);
        venue.setStatus(status);
        venueMapper.updateById(venue);

        auditService.record(operator.getUserId(), operator.getUsername(), OperationAction.VENUE_TOGGLE,
                OperationAction.TARGET_VENUE, id, Map.of("status", status), ip);
    }

    /* ---------- 内部 ---------- */

    private Venue requireVenue(Long id) {
        Venue venue = venueMapper.selectById(id);
        if (venue == null) {
            throw new BizException(ErrorCode.EVENT_VENUE_NOT_FOUND);
        }
        return venue;
    }

    private String valueOrDefault(String value, String defaultValue) {
        String v = trimToNull(value);
        return v == null ? defaultValue : v;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static VenueResponse toResponse(Venue v) {
        return new VenueResponse(
                String.valueOf(v.getId()), v.getName(), v.getCountryCode(), v.getProvince(),
                v.getCity(), v.getDistrict(), v.getAddress(), v.getTimezone(),
                v.getMaxCapacity(), v.getDescription(), v.getStatus(),
                v.getCreatedAt(), v.getUpdatedAt());
    }
}
