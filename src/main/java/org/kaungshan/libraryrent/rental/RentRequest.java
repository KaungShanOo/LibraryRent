package org.kaungshan.libraryrent.rental;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** {@code loanDays} is optional; the configured default is used when omitted. */
public record RentRequest(
        @NotNull Long bookId,
        @NotNull Long memberId,
        @Min(1) @Max(90) Integer loanDays) {
}
