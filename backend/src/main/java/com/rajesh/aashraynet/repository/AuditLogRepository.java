package com.rajesh.aashraynet.repository;

import com.rajesh.aashraynet.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}
