package org.kaungshan.libraryrent.lab;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A record that someone tried a transfer. Used in lesson 6 (propagation). */
@Entity
@Table(name = "lab_audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String message;

    protected AuditLog() {
        // required by JPA
    }

    public AuditLog(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
