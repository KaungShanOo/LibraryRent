package org.kaungshan.libraryrent.lab;

/** Unchecked (extends RuntimeException): @Transactional rolls back on it by default. */
public class AccountFrozenException extends RuntimeException {

    public AccountFrozenException(String message) {
        super(message);
    }
}
