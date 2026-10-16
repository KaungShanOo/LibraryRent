package org.kaungshan.libraryrent.lab;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lesson 6: propagation. This is a separate bean on purpose: TransferService calls it through
 * the Spring proxy, so the annotations below really take effect (see lesson 5 for why that matters).
 */
@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * REQUIRED (the default): if a transaction is already running, join it.
     * The log is saved or rolled back together with the caller's work.
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void logInSameTransaction(String message) {
        auditLogRepository.save(new AuditLog(message));
    }

    /**
     * REQUIRES_NEW: pause the caller's transaction and start a brand-new one.
     * The log commits as soon as this method returns, even if the caller later rolls back.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logInNewTransaction(String message) {
        auditLogRepository.save(new AuditLog(message));
    }
}
