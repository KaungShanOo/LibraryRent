package org.kaungshan.libraryrent.rental;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RentalRepository extends JpaRepository<Rental, Long> {

    boolean existsByBookId(Long bookId);

    boolean existsByMemberId(Long memberId);

    long countByMemberIdAndReturnedOnIsNull(Long memberId);

    boolean existsByMemberIdAndBookIdAndReturnedOnIsNull(Long memberId, Long bookId);

    @EntityGraph(attributePaths = {"book", "member"})
    List<Rental> findAllByOrderByRentedOnDesc();

    @EntityGraph(attributePaths = {"book", "member"})
    List<Rental> findByMemberIdOrderByRentedOnDesc(Long memberId);

    @EntityGraph(attributePaths = {"book", "member"})
    List<Rental> findByReturnedOnIsNullAndDueOnBeforeOrderByDueOn(LocalDate date);
}
