package org.kaungshan.libraryrent.book;

import java.util.List;

import org.kaungshan.libraryrent.common.ConflictException;
import org.kaungshan.libraryrent.common.NotFoundException;
import org.kaungshan.libraryrent.rental.RentalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookService {

    private final BookRepository bookRepository;
    private final RentalRepository rentalRepository;

    public List<BookResponse> findAll(String query) {
        List<Book> books = (query == null || query.isBlank())
                ? bookRepository.findAll()
                : bookRepository.findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCase(query, query);
        return books.stream().map(BookResponse::from).toList();
    }

    public BookResponse findById(Long id) {
        return BookResponse.from(getBook(id));
    }

    @Transactional
    public BookResponse create(BookRequest request) {
        if (bookRepository.existsByIsbn(request.isbn())) {
            throw new ConflictException("A book with ISBN " + request.isbn() + " already exists");
        }
        Book book = new Book();
        apply(book, request);
        book.setAvailableCopies(request.totalCopies());
        return BookResponse.from(bookRepository.save(book));
    }

    @Transactional
    public BookResponse update(Long id, BookRequest request) {
        Book book = getBook(id);
        if (bookRepository.existsByIsbnAndIdNot(request.isbn(), id)) {
            throw new ConflictException("A book with ISBN " + request.isbn() + " already exists");
        }
        int rentedOut = book.getTotalCopies() - book.getAvailableCopies();
        if (request.totalCopies() < rentedOut) {
            throw new ConflictException("Cannot reduce total copies below the %d currently rented out".formatted(rentedOut));
        }
        apply(book, request);
        book.setAvailableCopies(request.totalCopies() - rentedOut);
        return BookResponse.from(book);
    }

    @Transactional
    public void delete(Long id) {
        Book book = getBook(id);
        if (rentalRepository.existsByBookId(id)) {
            throw new ConflictException("Book has rental history and cannot be deleted");
        }
        bookRepository.delete(book);
    }

    public Book getBook(Long id) {
        return bookRepository.findById(id).orElseThrow(() -> new NotFoundException("Book", id));
    }

    private static void apply(Book book, BookRequest request) {
        book.setIsbn(request.isbn());
        book.setTitle(request.title());
        book.setAuthor(request.author());
        book.setPublishedYear(request.publishedYear());
        book.setTotalCopies(request.totalCopies());
    }
}
