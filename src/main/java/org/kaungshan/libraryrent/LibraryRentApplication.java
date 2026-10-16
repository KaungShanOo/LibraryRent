package org.kaungshan.libraryrent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class LibraryRentApplication {

    public static void main(String[] args) {
        SpringApplication.run(LibraryRentApplication.class, args);
    }

}
