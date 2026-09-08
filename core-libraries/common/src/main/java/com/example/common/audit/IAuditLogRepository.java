package com.example.common.audit;

import java.util.List;

/**
 * Append-only store for {@link AuditRecord}s. Implementations must persist entries
 * durably; existing entries are only removed by an explicit reset.
 */
public interface IAuditLogRepository {

    /** Writes an audit entry. Re-writing the same (order, trigger) overwrites it. */
    void append(AuditRecord record);

    /** All audit entries for one order. */
    List<AuditRecord> findByOrder(int customerId, int orderId);

    /** Removes every audit entry (cleanup / reset). */
    void deleteAll();
}
