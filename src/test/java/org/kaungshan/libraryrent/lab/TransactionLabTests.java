package org.kaungshan.libraryrent.lab;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * One test per lesson. Every test starts with Alice = 500 and Bob = 0 (or Bob frozen).
 *
 * This class is deliberately NOT @Transactional. If it were, the whole test would run inside one
 * transaction and nothing would really commit or roll back, which would hide what we want to see.
 *
 * The logging property prints Spring's transaction decisions ("Creating new transaction",
 * "Committing", "Rolling back") in the test output. Read them while the tests run.
 */
@SpringBootTest(properties = "logging.level.org.springframework.orm.jpa.JpaTransactionManager=DEBUG")
@TestMethodOrder(MethodOrderer.MethodName.class)
@DisplayName("Transaction Lab (bank transfer)")
class TransactionLabTests {

    @Autowired
    TransferService transferService;
    @Autowired
    AccountRepository accountRepository;
    @Autowired
    AuditLogRepository auditLogRepository;

    @Test
    @DisplayName("Lesson 1: @Transactional - a successful transfer commits both steps")
    void lesson1_successfulTransferCommitsBothSteps() {
        Long alice = newAccount("Alice", 500, false);
        Long bob = newAccount("Bob", 0, false);

        transferService.transfer(alice, bob, 100);

        assertThat(balanceOf(alice)).isEqualTo(400);
        assertThat(balanceOf(bob)).isEqualTo(100);
    }

    @Test
    @DisplayName("Lesson 2: @Transactional - a RuntimeException rolls back everything")
    void lesson2_runtimeExceptionRollsBackEverything() {
        Long alice = newAccount("Alice", 500, false);
        Long bob = newAccount("Bob", 0, true);   // frozen: step 2 will fail

        assertThatThrownBy(() -> transferService.transfer(alice, bob, 100))
                .isInstanceOf(AccountFrozenException.class);

        // Step 1 (withdraw) was undone. Alice keeps her money.
        assertThat(balanceOf(alice)).isEqualTo(500);
        assertThat(balanceOf(bob)).isEqualTo(0);
    }

    @Test
    @DisplayName("Lesson 3: No transaction - the money disappears when step 2 fails")
    void lesson3_withoutTransactionTheMoneyDisappears() {
        Long alice = newAccount("Alice", 500, false);
        Long bob = newAccount("Bob", 0, true);

        assertThatThrownBy(() -> transferService.transferWithoutTransaction(alice, bob, 100))
                .isInstanceOf(AccountFrozenException.class);

        // Step 1 was already saved. 100 left Alice and never reached Bob.
        assertThat(balanceOf(alice)).isEqualTo(400);
        assertThat(balanceOf(bob)).isEqualTo(0);
    }

    @Test
    @DisplayName("Lesson 4a: Checked exception - NOT rolled back by default")
    void lesson4a_checkedExceptionDoesNotRollBackByDefault() {
        Long alice = newAccount("Alice", 5_000, false);
        Long bob = newAccount("Bob", 0, false);

        assertThatThrownBy(() -> transferService.transferWithLimit(alice, bob, 2_000))
                .isInstanceOf(TransferLimitException.class);

        // The method threw, but the transfer was COMMITTED anyway.
        assertThat(balanceOf(alice)).isEqualTo(3_000);
        assertThat(balanceOf(bob)).isEqualTo(2_000);
    }

    @Test
    @DisplayName("Lesson 4b: rollbackFor - makes a checked exception roll back")
    void lesson4b_rollbackForMakesCheckedExceptionRollBack() {
        Long alice = newAccount("Alice", 5_000, false);
        Long bob = newAccount("Bob", 0, false);

        assertThatThrownBy(() -> transferService.transferWithLimitAndRollbackFor(alice, bob, 2_000))
                .isInstanceOf(TransferLimitException.class);

        assertThat(balanceOf(alice)).isEqualTo(5_000);
        assertThat(balanceOf(bob)).isEqualTo(0);
    }

    @Test
    @DisplayName("Lesson 5: Self-call - this.transfer() skips @Transactional")
    void lesson5_selfCallSkipsTransactional() {
        Long alice = newAccount("Alice", 500, false);
        Long bob = newAccount("Bob", 0, true);

        assertThatThrownBy(() -> transferService.transferViaSelfCall(alice, bob, 100))
                .isInstanceOf(AccountFrozenException.class);

        // Same result as lesson 3: @Transactional on transfer() was ignored.
        assertThat(balanceOf(alice)).isEqualTo(400);
        assertThat(balanceOf(bob)).isEqualTo(0);
    }

    @Test
    @DisplayName("Lesson 6a: REQUIRED - the audit log is rolled back with the transfer")
    void lesson6a_requiredLogIsRolledBackWithTheTransfer() {
        Long alice = newAccount("Alice", 500, false);
        Long bob = newAccount("Bob", 0, true);

        assertThatThrownBy(() -> transferService.transferWithAuditInSameTransaction(alice, bob, 100))
                .isInstanceOf(AccountFrozenException.class);

        assertThat(balanceOf(alice)).isEqualTo(500);
        assertThat(auditLogRepository.findByMessageContaining(alice + "->" + bob)).isEmpty();
    }

    @Test
    @DisplayName("Lesson 6b: REQUIRES_NEW - the audit log survives the rollback")
    void lesson6b_requiresNewLogSurvivesTheRollback() {
        Long alice = newAccount("Alice", 500, false);
        Long bob = newAccount("Bob", 0, true);

        assertThatThrownBy(() -> transferService.transferWithAuditInNewTransaction(alice, bob, 100))
                .isInstanceOf(AccountFrozenException.class);

        // The transfer rolled back, but the log was committed in its own transaction.
        assertThat(balanceOf(alice)).isEqualTo(500);
        assertThat(auditLogRepository.findByMessageContaining(alice + "->" + bob)).hasSize(1);
    }

    @Test
    @DisplayName("Lesson 7: TransactionTemplate - rolls back just like @Transactional")
    void lesson7_programmaticTransactionBehavesLikeTheAnnotation() {
        Long alice = newAccount("Alice", 500, false);
        Long bob = newAccount("Bob", 0, true);

        assertThatThrownBy(() -> transferService.transferProgrammatically(alice, bob, 100))
                .isInstanceOf(AccountFrozenException.class);

        // Same as lesson 2: rolled back.
        assertThat(balanceOf(alice)).isEqualTo(500);
        assertThat(balanceOf(bob)).isEqualTo(0);
    }

    private Long newAccount(String owner, long balance, boolean frozen) {
        return accountRepository.save(new Account(owner, balance, frozen)).getId();
    }

    private long balanceOf(Long accountId) {
        return accountRepository.findById(accountId).orElseThrow().getBalance();
    }
}
