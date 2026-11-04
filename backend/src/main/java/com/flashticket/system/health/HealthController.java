package com.flashticket.system.health;

import com.flashticket.common.api.ApiResponse;
import com.flashticket.common.api.ErrorCode;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 健康检查（契约见 doc/M00-工程初始化.md 3.3）。
 *
 * <p>/api/v1/health          存活探针（liveness）：进程存活即 UP。
 * <p>/api/v1/health/readiness 就绪探针：DB SELECT 1 + Redis PING 双依赖连通才 UP，
 * 任一失败返回 503 SERVICE_UNAVAILABLE，供 API Lab 展示「服务不可用」三态。
 */
@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    private static final Logger log = LoggerFactory.getLogger(HealthController.class);

    private final JdbcTemplate jdbcTemplate;
    private final RedisConnectionFactory redisConnectionFactory;

    public HealthController(JdbcTemplate jdbcTemplate, RedisConnectionFactory redisConnectionFactory) {
        this.jdbcTemplate = jdbcTemplate;
        this.redisConnectionFactory = redisConnectionFactory;
    }

    @GetMapping
    public ApiResponse<Map<String, String>> liveness() {
        return ApiResponse.ok(Map.of("status", "UP"));
    }

    @GetMapping("/readiness")
    public ResponseEntity<ApiResponse<Map<String, Object>>> readiness() {
        Map<String, Object> detail = new LinkedHashMap<>();
        boolean dbUp = checkDatabase(detail);
        boolean redisUp = checkRedis(detail);
        detail.put("status", dbUp && redisUp ? "UP" : "DOWN");

        if (dbUp && redisUp) {
            return ResponseEntity.ok(ApiResponse.ok(detail));
        }
        return ResponseEntity.status(ErrorCode.SERVICE_UNAVAILABLE.getHttpStatus())
                .body(ApiResponse.fail(ErrorCode.SERVICE_UNAVAILABLE, "依赖服务不可用"));
    }

    private boolean checkDatabase(Map<String, Object> detail) {
        try {
            Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            detail.put("db", result != null && result == 1 ? "UP" : "DOWN");
            return result != null && result == 1;
        } catch (Exception ex) {
            log.warn("数据库就绪检查失败: {}", ex.getMessage());
            detail.put("db", "DOWN: " + ex.getClass().getSimpleName());
            return false;
        }
    }

    private boolean checkRedis(Map<String, Object> detail) {
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            String pong = connection.ping();
            detail.put("redis", pong != null ? "UP" : "DOWN");
            return pong != null;
        } catch (RedisConnectionFailureException | IllegalStateException ex) {
            log.warn("Redis 就绪检查失败: {}", ex.getMessage());
            detail.put("redis", "DOWN: " + ex.getClass().getSimpleName());
            return false;
        } catch (Exception ex) {
            log.warn("Redis 就绪检查失败: {}", ex.getMessage());
            detail.put("redis", "DOWN: " + ex.getClass().getSimpleName());
            return false;
        }
    }
}
