package org.kaungshan.libraryrent.rental;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Rents several books to one member as a single all-or-nothing unit: if any book can't be rented
 * (no copies left, member limit reached, duplicate...), none of the rentals are kept and every
 * book's availableCopies is restored.
 *
 * The same operation is implemented twice to show both ways Spring manages transactions.
 * Both rely on {@link RentalService#rent} using the default propagation (REQUIRED), so each
 * single rental joins the outer transaction instead of committing on its own.
 */
@Service
public class BatchRentalService {

    private final RentalService rentalService;
    private final TransactionTemplate transactionTemplate;

    public BatchRentalService(RentalService rentalService, PlatformTransactionManager transactionManager) {
        this.rentalService = rentalService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    /**
     * Declarative: Spring wraps this method in a proxy that begins a transaction before the call,
     * commits after it returns, and rolls back if it throws a RuntimeException.
     *
     * Only works when the call comes through the Spring proxy, i.e. from another bean. Calling
     * this method from inside BatchRentalService (this.rentAll(...)) would skip the transaction.
     */
    @Transactional
    public List<RentalResponse> rentAll(BatchRentRequest request) {
        return rentEach(request);
    }

    /**
     * Programmatic: no annotation; the transaction boundary is the lambda passed to
     * TransactionTemplate. It commits when the lambda returns and rolls back when it throws
     * (or when status.setRollbackOnly() is called). Works for self-calls too.
     */
    public List<RentalResponse> rentAllProgrammatically(BatchRentRequest request) {
        return transactionTemplate.execute(status -> rentEach(request));
    }

    private List<RentalResponse> rentEach(BatchRentRequest request) {
        return request.bookIds().stream()
                .map(bookId -> rentalService.rent(new RentRequest(bookId, request.memberId(), request.loanDays())))
                .toList();
    }
}
