package org.kaungshan.libraryrent.common;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "library")
public record LibraryProperties(Rental rental) {

    public record Rental(int defaultLoanDays, int maxActiveRentalsPerMember) {
    }
}
