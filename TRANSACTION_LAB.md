# Transaction Lab (Bank Transfer)

A small lab for learning Spring transactions, one idea at a time.

| File | What it is |
|---|---|
| `src/main/java/.../lab/TransferService.java` | One method per lesson |
| `src/main/java/.../lab/AuditService.java` | Used in lesson 6 |
| `src/test/java/.../lab/TransactionLabTests.java` | One test per lesson, proving what happens |

## The idea

A transfer is **two steps**:

1. Take money out of Alice's account
2. Put money into Bob's account

If step 2 fails after step 1 has been saved, the money disappears. A **transaction** groups both steps so they either **all happen (commit)** or **none happen (rollback)**.

To make step 2 fail on purpose, Bob's account can be **frozen**. Depositing into a frozen account throws `AccountFrozenException`.

## How to run

```bash
./mvnw test -Dtest=TransactionLabTests                  # all lessons
./mvnw test -Dtest='TransactionLabTests#lesson2*'       # one lesson
```

The tests print Spring's transaction log. These lines are the most useful thing to read:

| Log line | Meaning |
|---|---|
| `Creating new transaction with name [...]` | A transaction starts here |
| `Participating in existing transaction` | This call joined the transaction already running |
| `Committing JPA transaction` | Changes are saved for real |
| `Rolling back JPA transaction` | Changes are undone |
| `Suspending current transaction` | REQUIRES_NEW paused the outer one (lesson 6) |

Notice that `SimpleJpaRepository.save` and `findById` create their **own small transaction** when nothing is running. That's why, without `@Transactional`, every `save()` commits immediately.

## Lessons

### 1. Commit: `transfer()`
`@Transactional` starts a transaction before the method and commits after it returns. Alice 500 → 400, Bob 0 → 100.

### 2. Rollback: `transfer()` with Bob frozen
Step 2 throws a `RuntimeException`, so Spring rolls back and step 1 is undone. Alice still has 500.

### 3. No transaction: `transferWithoutTransaction()`
Same code, no annotation. Step 1's `save()` already committed, so Alice has 400 and Bob has 0. **100 disappeared.** This is the bug transactions prevent.

### 4. Checked exceptions: `transferWithLimit()` / `transferWithLimitAndRollbackFor()`
By default Spring only rolls back on **unchecked** exceptions (`RuntimeException`, `Error`).
`TransferLimitException extends Exception` is **checked**, so:
- 4a: the method throws, but the transfer is **committed anyway**.
- 4b: `@Transactional(rollbackFor = TransferLimitException.class)` makes it roll back.

### 5. Self-call trap: `transferViaSelfCall()`
Spring adds transactions with a **proxy**, an object wrapped around your bean. Calls from *other* beans go through the proxy. A call to `this.transfer(...)` inside the same class does **not**, so `@Transactional` is silently ignored and the result matches lesson 3.
Fix: call the method from a different bean, or use lesson 7.

### 6. Propagation: `AuditService`
What happens when a `@Transactional` method calls another `@Transactional` method?
- 6a, `REQUIRED` (default): **join** the caller's transaction. When the transfer rolls back, the audit log is rolled back too.
- 6b, `REQUIRES_NEW`: **pause** the caller and start a separate transaction, which commits on its own. The transfer rolls back, but the log **survives**.
  Use this for things you want to keep even on failure, like audit or error logs.

### 7. Without the annotation: `transferProgrammatically()`
`TransactionTemplate` does the same job as `@Transactional`, but in code: the lambda is the transaction. It commits when the lambda returns and rolls back when it throws. It has no proxy, so the lesson 5 trap doesn't apply.

## Try it yourself

Change one thing, **predict** which test will fail, run the tests, then undo the change.

1. Delete `@Transactional` from `transfer()`. Which lessons change?
2. Change `TransferLimitException` to `extends RuntimeException`. What happens to lesson 4a?
3. Add `@Transactional` to `transferViaSelfCall()` itself. Does lesson 5 still lose money? Why?
4. In `AuditService`, change `REQUIRES_NEW` to `REQUIRED`. Which test fails?
5. Write a new test: a *successful* transfer with `TransactionTemplate` where the lambda calls `status.setRollbackOnly()`. What are the balances afterwards?

<details>
<summary>Answers</summary>

1. Only lesson 2 fails (Alice ends with 400). Lesson 1 still passes because nothing goes wrong there, and lesson 5 was already running without a transaction.
2. Lesson 4a fails: unchecked exceptions roll back by default, so Alice keeps 5,000.
3. Lesson 5 now rolls back (Alice keeps 500), so its test fails. The transaction comes from the proxy around `transferViaSelfCall()`, and the inner `this.transfer()` simply runs inside it.
4. Lesson 6b fails: the log joins the transfer's transaction and is rolled back with it.
5. Nothing is saved. Alice keeps 500 and Bob has 0, even though no exception was thrown.

</details>

## Why the test class is not `@Transactional`

A `@Transactional` test runs everything inside **one** transaction that is rolled back at the end. Every call would just join it ("Participating in existing transaction"), so nothing would really commit or roll back, and the lessons would show nothing. Tests that check transaction behavior must leave the annotation off.
