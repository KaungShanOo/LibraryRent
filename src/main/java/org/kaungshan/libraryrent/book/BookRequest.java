package org.kaungshan.libraryrent.book;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BookRequest(
        @NotBlank @Size(max = 20) String isbn,
        @NotBlank @Size(max = 255) String title,
        @NotBlank @Size(max = 255) String author,
        @Min(0) @Max(9999) Integer publishedYear,
        @Min(1) int totalCopies) {
}
