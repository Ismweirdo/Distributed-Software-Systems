package com.seckill.controller;

import com.seckill.vo.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthController {

    private final JdbcTemplate jdbcTemplate;
    private final LocalDateTime appStartupTime = LocalDateTime.now();

    @GetMapping("/init-status")
    public Result<Map<String, Object>> getInitStatus() {
        Integer productCount = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM seckill_product", Integer.class);
        Map<String, Object> latestAudit = jdbcTemplate.query(
                "SELECT seeded, create_time FROM startup_init_audit WHERE event_type = 'seckill_product_seed' ORDER BY id DESC LIMIT 1",
                rs -> {
                    if (!rs.next()) {
                        return null;
                    }
                    Map<String, Object> row = new HashMap<>();
                    row.put("seeded", rs.getBoolean("seeded"));
                    row.put("createTime", rs.getTimestamp("create_time").toLocalDateTime());
                    return row;
                }
        );

        boolean seededThisStartup = false;
        if (latestAudit != null) {
            LocalDateTime auditTime = (LocalDateTime) latestAudit.get("createTime");
            boolean seeded = (boolean) latestAudit.get("seeded");
            seededThisStartup = seeded && !auditTime.isBefore(appStartupTime.minusMinutes(10));
        }

        Map<String, Object> data = new HashMap<>();
        data.put("appStartupTime", appStartupTime);
        data.put("productCount", productCount == null ? 0 : productCount);
        data.put("seededThisStartup", seededThisStartup);
        data.put("latestSeedAudit", latestAudit);
        return Result.success(data);
    }
}
