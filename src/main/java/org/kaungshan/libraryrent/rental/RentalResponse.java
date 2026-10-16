package org.kaungshan.libraryrent.rental;

import java.time.LocalDate;

public record RentalResponse(
        Long id,
        Long bookId,
        String bookTitle,
        Long memberId,
        String memberName,
        LocalDate rentedOn,
        LocalDate dueOn,
        LocalDate returnedOn,
        boolean overdue) {

    static RentalResponse from(Rental rental, LocalDate today) {
        boolean overdue = rental.isActive() && rental.getDueOn().isBefore(today);
        return new RentalResponse(rental.getId(),
                rental.getBook().getId(), rental.getBook().getTitle(),
                rental.getMember().getId(), rental.getMember().getName(),
                rental.getRentedOn(), rental.getDueOn(), rental.getReturnedOn(), overdue);
    }
}
