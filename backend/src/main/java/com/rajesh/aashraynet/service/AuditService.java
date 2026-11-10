package com.rajesh.aashraynet.service;

import com.rajesh.aashraynet.entity.AuditLog;
import com.rajesh.aashraynet.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

/** Writes audit rows inside the caller's transaction. Never pass personal data in "details". */
@Service
public class AuditService {
    private final AuditLogRepository repository;

    public AuditService(AuditLogRepository repository) {
        this.repository = repository;
    }

    public void log(Long actorId, String action, String entityType, Long entityId, String details) {
        repository.save(new AuditLog(actorId, action, entityType, entityId, details));
    }
}
