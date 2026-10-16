package org.kaungshan.libraryrent.rental;

import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record BatchRentRequest(
        @NotNull Long memberId,
        @NotEmpty List<@NotNull Long> bookIds,
        @Min(1) @Max(90) Integer loanDays) {
}
