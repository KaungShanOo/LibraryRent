package org.kaungshan.libraryrent.rental;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.kaungshan.libraryrent.book.BookRequest;
import org.kaungshan.libraryrent.book.BookService;
import org.kaungshan.libraryrent.common.ConflictException;
import org.kaungshan.libraryrent.member.MemberRequest;
import org.kaungshan.libraryrent.member.MemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Deliberately NOT @Transactional: a test-level transaction would wrap every call, so nothing
 * would really commit or roll back and the behavior under test would be hidden.
 */
@SpringBootTest
class BatchRentalTransactionTests {

    @Autowired
    BatchRentalService batchRentalService;
    @Autowired
    RentalService rentalService;
    @Autowired
    BookService bookService;
    @Autowired
    MemberService memberService;

    @ParameterizedTest(name = "programmatic={0}")
    @ValueSource(booleans = {false, true})
    void allBooksAreRentedWhenEveryRentalSucceeds(boolean programmatic) {
        long member = newMember();
        long bookA = newBook();
        long bookB = newBook();

        List<RentalResponse> rentals = rentAll(programmatic, member, bookA, bookB);

        assertThat(rentals).hasSize(2);
        assertThat(available(bookA)).isZero();
        assertThat(available(bookB)).isZero();
    }

    @ParameterizedTest(name = "programmatic={0}")
    @ValueSource(booleans = {false, true})
    void oneFailureRollsBackTheWholeBatch(boolean programmatic) {
        long member = newMember();
        long bookA = newBook();

        // The first rental of bookA succeeds, the second fails ("already has an active rental").
        assertThatThrownBy(() -> rentAll(programmatic, member, bookA, bookA))
                .isInstanceOf(ConflictException.class);

        // ...and the first one was rolled back with it.
        assertThat(available(bookA)).isEqualTo(1);
        assertThat(rentalService.findAll(member)).isEmpty();
    }

    @Test
    void withoutAnOuterTransactionEarlierRentalsStayCommitted() {
        long member = newMember();
        long bookA = newBook();

        // Each rent() call is its own transaction, so the first one commits before the second fails.
        assertThatThrownBy(() -> List.of(bookA, bookA)
                .forEach(bookId -> rentalService.rent(new RentRequest(bookId, member, null))))
                .isInstanceOf(ConflictException.class);

        assertThat(available(bookA)).isZero();
        assertThat(rentalService.findAll(member)).hasSize(1);
    }

    private List<RentalResponse> rentAll(boolean programmatic, long memberId, Long... bookIds) {
        BatchRentRequest request = new BatchRentRequest(memberId, List.of(bookIds), null);
        return programmatic
                ? batchRentalService.rentAllProgrammatically(request)
                : batchRentalService.rentAll(request);
    }

    private long newBook() {
        String isbn = "T-" + UUID.randomUUID().toString().substring(0, 12);
        return bookService.create(new BookRequest(isbn, "Title " + isbn, "Author", 2020, 1)).id();
    }

    private long newMember() {
        String email = UUID.randomUUID() + "@example.com";
        return memberService.create(new MemberRequest("Member", email, null)).id();
    }

    private int available(long bookId) {
        return bookService.findById(bookId).availableCopies();
    }
}
