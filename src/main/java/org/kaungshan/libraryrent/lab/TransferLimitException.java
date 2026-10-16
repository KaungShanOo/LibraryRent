package org.kaungshan.libraryrent.lab;

/** Checked (extends Exception): @Transactional does NOT roll back on it unless told to (lesson 4). */
public class TransferLimitException extends Exception {

    public TransferLimitException(String message) {
        super(message);
    }
}
