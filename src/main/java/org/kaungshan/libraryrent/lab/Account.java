package org.kaungshan.libraryrent.lab;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A bank account. Balance is a whole number to keep the lab simple (real money code uses BigDecimal). */
@Entity
@Table(name = "lab_accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String owner;

    private long balance;

    /** A frozen account can't receive money. Used to make a transfer fail halfway. */
    private boolean frozen;

    protected Account() {
        // required by JPA
    }

    public Account(String owner, long balance, boolean frozen) {
        this.owner = owner;
        this.balance = balance;
        this.frozen = frozen;
    }

    public void withdraw(long amount) {
        if (amount > balance) {
            throw new IllegalStateException(owner + " doesn't have enough money");
        }
        balance -= amount;
    }

    public void deposit(long amount) {
        if (frozen) {
            throw new AccountFrozenException(owner + "'s account is frozen");
        }
        balance += amount;
    }

    public Long getId() {
        return id;
    }

    public String getOwner() {
        return owner;
    }

    public long getBalance() {
        return balance;
    }
}
