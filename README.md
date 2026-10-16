# LibraryRent

A REST API for a lending library, built with Spring Boot and MySQL. Members can rent and return books, and the API tracks how many copies are available and which rentals are overdue.

The project also includes a **transaction lab**: a small bank-transfer example for learning how Spring transactions work. See [TRANSACTION_LAB.md](TRANSACTION_LAB.md).

## Tech stack

- Java 17
- Spring Boot 4.1 (Web MVC, Data JPA, Validation)
- MySQL (H2 in-memory database for tests)
- Maven (wrapper included, no install needed)
- Lombok

## Getting started

### 1. Prerequisites

- JDK 17 or newer
- A running MySQL server

### 2. Set your database password

Create `src/main/resources/application-local.properties`:

```properties
spring.datasource.password=your_mysql_password
```

This file is git-ignored, so your password never gets committed. The default username is `root`. To change it or other connection details, add them to the same file, or use the environment variables `DB_USERNAME`, `DB_PASSWORD`, `DB_HOST`, `DB_PORT` and `DB_NAME`.

### 3. Run

```bash
./mvnw spring-boot:run
```

The API starts on `http://localhost:8080`. On first start, the `library_rent` database and its tables are created automatically.

## Running tests

Tests use an in-memory H2 database, so they don't need MySQL.

```bash
./mvnw test                                   # all tests
./mvnw test -Dtest=RentalFlowTests            # one test class
./mvnw test -Dtest=TransactionLabTests        # the transaction lab
```

## API

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/books?q=` | List books (optional search by title or author) |
| `GET` | `/api/books/{id}` | Get one book |
| `POST` | `/api/books` | Add a book |
| `PUT` | `/api/books/{id}` | Update a book |
| `DELETE` | `/api/books/{id}` | Delete a book (only if it was never rented) |
| `GET` | `/api/members` | List members |
| `GET` | `/api/members/{id}` | Get one member |
| `POST` | `/api/members` | Add a member |
| `PUT` | `/api/members/{id}` | Update a member |
| `DELETE` | `/api/members/{id}` | Delete a member (only if they never rented) |
| `GET` | `/api/rentals?memberId=` | List rentals (optionally for one member) |
| `GET` | `/api/rentals/overdue` | List overdue rentals |
| `GET` | `/api/rentals/{id}` | Get one rental |
| `POST` | `/api/rentals` | Rent a book |
| `POST` | `/api/rentals/batch` | Rent several books at once, all or nothing |
| `POST` | `/api/rentals/{id}/return` | Return a book |

### Example

```bash
# Add a book with 2 copies
curl -X POST localhost:8080/api/books -H 'Content-Type: application/json' \
  -d '{"isbn": "978-0134685991", "title": "Effective Java", "author": "Joshua Bloch", "publishedYear": 2018, "totalCopies": 2}'

# Add a member
curl -X POST localhost:8080/api/members -H 'Content-Type: application/json' \
  -d '{"name": "Alice", "email": "alice@example.com"}'

# Rent the book (loanDays is optional, default 14)
curl -X POST localhost:8080/api/rentals -H 'Content-Type: application/json' \
  -d '{"bookId": 1, "memberId": 1}'

# Return it
curl -X POST localhost:8080/api/rentals/1/return
```

### Rental rules

- A book can only be rented while it has copies available.
- A member can have at most 5 active rentals (set by `library.rental.max-active-rentals-per-member`).
- A member can't rent a second copy of a book they already have.
- The default loan is 14 days (set by `library.rental.default-loan-days`).

Breaking a rule returns `409 Conflict`. Invalid input returns `400 Bad Request` and unknown IDs return `404 Not Found`. Error bodies use the standard [Problem Details](https://www.rfc-editor.org/rfc/rfc9457) format.

## Project structure

```
src/main/java/org/kaungshan/libraryrent/
├── book/      Books: entity, repository, service, controller
├── member/    Members
├── rental/    Rentals, including the batch rental
├── common/    Error handling and configuration
└── lab/       Transaction lab (bank transfer)
```
