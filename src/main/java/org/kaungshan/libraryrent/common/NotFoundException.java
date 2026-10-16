package org.kaungshan.libraryrent.common;

public class NotFoundException extends RuntimeException {

    public NotFoundException(String resource, Long id) {
        super("%s with id %d not found".formatted(resource, id));
    }
}
