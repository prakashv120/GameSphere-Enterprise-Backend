package com.gamesphere.controller;

import com.gamesphere.dto.response.ApiResponse;
import com.gamesphere.dto.response.HealthResponse;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    private final JdbcTemplate jdbcTemplate;
    private final StringRedisTemplate redisTemplate;

    public HealthController(JdbcTemplate jdbcTemplate, StringRedisTemplate redisTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.redisTemplate = redisTemplate;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<HealthResponse>> checkHealth() {
        String dbStatus = "DOWN";
        try {
            Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            if (result != null && result == 1) {
                dbStatus = "UP";
            }
        } catch (Exception e) {
            // Database is unreachable or query failed
        }

        String redisStatus = "DOWN";
        try {
            RedisConnection connection = redisTemplate.getConnectionFactory().getConnection();
            String pingResult = connection.ping();
            if ("PONG".equalsIgnoreCase(pingResult)) {
                redisStatus = "UP";
            }
        } catch (Exception e) {
            // Redis is unreachable or connection failed
        }

        boolean overallHealthy = "UP".equals(dbStatus) && "UP".equals(redisStatus);
        String appStatus = "UP";

        HealthResponse healthResponse = HealthResponse.builder()
                .status(appStatus)
                .database(dbStatus)
                .redis(redisStatus)
                .serverTime(LocalDateTime.now().toString())
                .build();

        if (overallHealthy) {
            return ResponseEntity.ok(ApiResponse.success("System is fully healthy", healthResponse));
        } else {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ApiResponse.error("System is degraded", healthResponse));
        }
    }
}
