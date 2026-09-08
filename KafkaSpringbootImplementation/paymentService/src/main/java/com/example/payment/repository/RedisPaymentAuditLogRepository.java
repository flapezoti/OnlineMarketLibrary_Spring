package com.example.payment.repository;

import com.example.common.audit.AuditRecord;
import com.example.common.audit.IAuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
public class RedisPaymentAuditLogRepository implements IAuditLogRepository {

    private static final String PREFIX = "paymentAuditLog:";

    @Autowired
    private RedisTemplate<String, AuditRecord> auditLogRedisTemplate;

    private String key(int customerId, int orderId, String trigger) {
        return PREFIX + customerId + "-" + orderId + ":" + trigger;
    }

    @Override
    public void append(AuditRecord record) {
        auditLogRedisTemplate.opsForValue()
                .set(key(record.getCustomerId(), record.getOrderId(), record.getTrigger()), record);
    }

    @Override
    public List<AuditRecord> findByOrder(int customerId, int orderId) {
        Set<String> keys = auditLogRedisTemplate.keys(PREFIX + customerId + "-" + orderId + ":*");
        if (keys == null || keys.isEmpty()) {
            return List.of();
        }
        List<AuditRecord> values = auditLogRedisTemplate.opsForValue().multiGet(keys);
        return values == null ? List.of()
                : values.stream().filter(Objects::nonNull).collect(Collectors.toList());
    }

    @Override
    public void deleteAll() {
        Set<String> keys = auditLogRedisTemplate.keys(PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            auditLogRedisTemplate.delete(keys);
        }
    }
}
