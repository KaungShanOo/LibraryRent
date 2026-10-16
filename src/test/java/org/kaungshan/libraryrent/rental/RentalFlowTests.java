package org.kaungshan.libraryrent.rental;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RentalFlowTests {

    @Autowired
    MockMvc mvc;

    @Test
    void rentingTakesACopyAndReturningGivesItBack() throws Exception {
        long bookId = createBook("978-0000000001", 1);
        long alice = createMember("alice@example.com");
        long bob = createMember("bob@example.com");

        long rentalId = rent(bookId, alice);
        mvc.perform(get("/api/books/{id}", bookId)).andExpect(jsonPath("$.availableCopies").value(0));

        // last copy is out
        mvc.perform(post("/api/rentals").contentType(MediaType.APPLICATION_JSON)
                        .content(rentJson(bookId, bob)))
                .andExpect(status().isConflict());

        mvc.perform(post("/api/rentals/{id}/return", rentalId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.returnedOn").isNotEmpty());
        mvc.perform(get("/api/books/{id}", bookId)).andExpect(jsonPath("$.availableCopies").value(1));

        mvc.perform(post("/api/rentals/{id}/return", rentalId)).andExpect(status().isConflict());
    }

    @Test
    void memberCannotExceedActiveRentalLimit() throws Exception {
        long member = createMember("carol@example.com");
        rent(createBook("978-0000000002", 3), member);
        rent(createBook("978-0000000003", 3), member);

        // limit is 2 in src/test/resources/application.properties
        mvc.perform(post("/api/rentals").contentType(MediaType.APPLICATION_JSON)
                        .content(rentJson(createBook("978-0000000004", 3), member)))
                .andExpect(status().isConflict());
    }

    @Test
    void invalidBookIsRejected() throws Exception {
        mvc.perform(post("/api/books").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"isbn": "", "title": "", "author": "Someone", "totalCopies": 0}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownBookReturns404() throws Exception {
        mvc.perform(get("/api/books/{id}", 999_999)).andExpect(status().isNotFound());
    }

    private long createBook(String isbn, int copies) throws Exception {
        String body = """
                {"isbn": "%s", "title": "Book %s", "author": "Author", "publishedYear": 2020, "totalCopies": %d}
                """.formatted(isbn, isbn, copies);
        return idOf(mvc.perform(post("/api/books").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
    }

    private long createMember(String email) throws Exception {
        String body = """
                {"name": "%s", "email": "%s"}
                """.formatted(email, email);
        return idOf(mvc.perform(post("/api/members").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
    }

    private long rent(long bookId, long memberId) throws Exception {
        return idOf(mvc.perform(post("/api/rentals").contentType(MediaType.APPLICATION_JSON)
                        .content(rentJson(bookId, memberId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.overdue").value(false))
                .andReturn().getResponse().getContentAsString());
    }

    private static String rentJson(long bookId, long memberId) {
        return """
                {"bookId": %d, "memberId": %d}
                """.formatted(bookId, memberId);
    }

    private static long idOf(String json) {
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }
}
