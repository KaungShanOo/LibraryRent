package org.kaungshan.libraryrent.lab;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Transaction lab. Every method moves money from one account to another, but each one is
 * set up differently. Read it alongside TransactionLabTests and TRANSACTION_LAB.md.
 *
 * A transfer is two steps: take money out of "from", then put it into "to".
 * A transaction makes the two steps all-or-nothing.
 */
@Service
public class TransferService {

    static final long TRANSFER_LIMIT = 1_000;

    private final AccountRepository accountRepository;
    private final AuditService auditService;
    private final TransactionTemplate transactionTemplate;

    public TransferService(AccountRepository accountRepository, AuditService auditService,
            PlatformTransactionManager transactionManager) {
        this.accountRepository = accountRepository;
        this.auditService = auditService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    // ---------------------------------------------------------------------------------------
    // Lessons 1 & 2: @Transactional commits on success and rolls back on a RuntimeException
    // ---------------------------------------------------------------------------------------

    @Transactional
    public void transfer(Long fromId, Long toId, long amount) {
        moveMoney(fromId, toId, amount);
    }

    // ---------------------------------------------------------------------------------------
    // Lesson 3: no transaction. Each save() commits on its own, so a failure halfway
    // leaves the database half-updated.
    // ---------------------------------------------------------------------------------------

    public void transferWithoutTransaction(Long fromId, Long toId, long amount) {
        moveMoney(fromId, toId, amount);
    }

    // ---------------------------------------------------------------------------------------
    // Lesson 4: checked exceptions do NOT cause a rollback by default.
    // The limit check is placed AFTER moving the money on purpose, to show the effect.
    // ---------------------------------------------------------------------------------------

    @Transactional
    public void transferWithLimit(Long fromId, Long toId, long amount) throws TransferLimitException {
        moveMoney(fromId, toId, amount);
        checkLimit(amount);
    }

    @Transactional(rollbackFor = TransferLimitException.class)
    public void transferWithLimitAndRollbackFor(Long fromId, Long toId, long amount) throws TransferLimitException {
        moveMoney(fromId, toId, amount);
        checkLimit(amount);
    }

    // ---------------------------------------------------------------------------------------
    // Lesson 5: self-invocation. @Transactional works through a Spring "proxy" that wraps
    // this bean. Calling transfer() from inside this class skips the proxy, so there is
    // NO transaction, even though transfer() is annotated.
    // ---------------------------------------------------------------------------------------

    public void transferViaSelfCall(Long fromId, Long toId, long amount) {
        this.transfer(fromId, toId, amount);
    }

    // ---------------------------------------------------------------------------------------
    // Lesson 6: propagation. Write an audit log, then attempt the transfer.
    // ---------------------------------------------------------------------------------------

    @Transactional
    public void transferWithAuditInSameTransaction(Long fromId, Long toId, long amount) {
        auditService.logInSameTransaction("attempt " + fromId + "->" + toId);
        moveMoney(fromId, toId, amount);
    }

    @Transactional
    public void transferWithAuditInNewTransaction(Long fromId, Long toId, long amount) {
        auditService.logInNewTransaction("attempt " + fromId + "->" + toId);
        moveMoney(fromId, toId, amount);
    }

    // ---------------------------------------------------------------------------------------
    // Lesson 7: programmatic transaction (no annotation). The lambda is the transaction:
    // commit when it returns, roll back when it throws.
    // ---------------------------------------------------------------------------------------

    public void transferProgrammatically(Long fromId, Long toId, long amount) {
        transactionTemplate.executeWithoutResult(status -> moveMoney(fromId, toId, amount));
    }

    // ---------------------------------------------------------------------------------------

    private void moveMoney(Long fromId, Long toId, long amount) {
        Account from = accountRepository.findById(fromId).orElseThrow();
        Account to = accountRepository.findById(toId).orElseThrow();

        from.withdraw(amount);
        accountRepository.save(from);   // step 1: money leaves "from"

        to.deposit(amount);             // step 2: throws AccountFrozenException if "to" is frozen
        accountRepository.save(to);
    }

    private static void checkLimit(long amount) throws TransferLimitException {
        if (amount > TRANSFER_LIMIT) {
            throw new TransferLimitException("Transfers above " + TRANSFER_LIMIT + " are not allowed");
        }
    }
}
