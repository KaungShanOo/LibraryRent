package org.kaungshan.libraryrent.rental;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import org.kaungshan.libraryrent.book.Book;
import org.kaungshan.libraryrent.book.BookService;
import org.kaungshan.libraryrent.common.ConflictException;
import org.kaungshan.libraryrent.common.LibraryProperties;
import org.kaungshan.libraryrent.common.NotFoundException;
import org.kaungshan.libraryrent.member.Member;
import org.kaungshan.libraryrent.member.MemberService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RentalService {

    private final RentalRepository rentalRepository;
    private final BookService bookService;
    private final MemberService memberService;
    private final LibraryProperties properties;
    private final Clock clock;

    public List<RentalResponse> findAll(Long memberId) {
        List<Rental> rentals = memberId == null
                ? rentalRepository.findAllByOrderByRentedOnDesc()
                : rentalRepository.findByMemberIdOrderByRentedOnDesc(memberId);
        return toResponses(rentals);
    }

    public List<RentalResponse> findOverdue() {
        return toResponses(rentalRepository.findByReturnedOnIsNullAndDueOnBeforeOrderByDueOn(today()));
    }

    public RentalResponse findById(Long id) {
        return RentalResponse.from(getRental(id), today());
    }

    @Transactional
    public RentalResponse rent(RentRequest request) {
        Member member = memberService.getMember(request.memberId());
        Book book = bookService.getBook(request.bookId());

        if (book.getAvailableCopies() <= 0) {
            throw new ConflictException("No copies of '%s' are available".formatted(book.getTitle()));
        }
        int maxActive = properties.rental().maxActiveRentalsPerMember();
        if (rentalRepository.countByMemberIdAndReturnedOnIsNull(member.getId()) >= maxActive) {
            throw new ConflictException("Member already has the maximum of %d active rentals".formatted(maxActive));
        }
        if (rentalRepository.existsByMemberIdAndBookIdAndReturnedOnIsNull(member.getId(), book.getId())) {
            throw new ConflictException("Member already has an active rental of this book");
        }

        book.setAvailableCopies(book.getAvailableCopies() - 1);

        LocalDate today = today();
        int loanDays = request.loanDays() != null ? request.loanDays() : properties.rental().defaultLoanDays();
        Rental rental = new Rental();
        rental.setBook(book);
        rental.setMember(member);
        rental.setRentedOn(today);
        rental.setDueOn(today.plusDays(loanDays));
        return RentalResponse.from(rentalRepository.save(rental), today);
    }

    @Transactional
    public RentalResponse returnBook(Long rentalId) {
        Rental rental = getRental(rentalId);
        if (!rental.isActive()) {
            throw new ConflictException("Rental %d was already returned".formatted(rentalId));
        }
        Book book = rental.getBook();
        book.setAvailableCopies(book.getAvailableCopies() + 1);
        rental.setReturnedOn(today());
        return RentalResponse.from(rental, today());
    }

    private Rental getRental(Long id) {
        return rentalRepository.findById(id).orElseThrow(() -> new NotFoundException("Rental", id));
    }

    private List<RentalResponse> toResponses(List<Rental> rentals) {
        LocalDate today = today();
        return rentals.stream().map(r -> RentalResponse.from(r, today)).toList();
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }
}
