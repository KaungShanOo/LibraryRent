package org.kaungshan.libraryrent.common;

/** Thrown when a request is valid but breaks a business rule (no copies left, duplicate ISBN, ...). */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
