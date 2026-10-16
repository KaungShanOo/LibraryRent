package org.kaungshan.libraryrent.book;

public record BookResponse(
        Long id,
        String isbn,
        String title,
        String author,
        Integer publishedYear,
        int totalCopies,
        int availableCopies) {

    static BookResponse from(Book book) {
        return new BookResponse(book.getId(), book.getIsbn(), book.getTitle(), book.getAuthor(),
                book.getPublishedYear(), book.getTotalCopies(), book.getAvailableCopies());
    }
}
