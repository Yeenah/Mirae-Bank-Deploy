package com.bank.manager;

import com.bank.dao.AccountDAO;
import com.bank.exception.AccountNotFoundException;
import com.bank.model.Account;
import com.bank.util.ReferenceNumberGenerator;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

public class AccountManager {

    private static final Logger LOGGER =
            Logger.getLogger(AccountManager.class.getName());

    private final AccountDAO accountDAO;


    public AccountManager(AccountDAO accountDAO) {
        this.accountDAO = accountDAO;
    }

    public Account createAccount(
            String name,
            BigDecimal initialBalance
    )
            throws SQLException {


        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Account holder name cannot be empty"
            );
        }


        if (initialBalance == null ||
                initialBalance.compareTo(BigDecimal.ZERO) < 0) {

            throw new IllegalArgumentException(
                    "Balance cannot be negative"
            );
        }


        String accountNumber =
                ReferenceNumberGenerator.generate();


        Account account = new Account(
                accountNumber,
                name.trim(),
                initialBalance
        );


        accountDAO.createAccount(account);


        LOGGER.info(
                "New account created: "
                        + accountNumber
        );


        return account;
    }


    public Account balanceInquiry(
            String accountNumber
    )
            throws AccountNotFoundException, SQLException {


        return findAccountOrThrow(accountNumber);
    }


    public Account findAccountOrThrow(
            String accountNumber
    )
            throws AccountNotFoundException, SQLException {


        Optional<Account> account =
                accountDAO.findByAccountNumber(accountNumber);


        return account.orElseThrow(
                () -> new AccountNotFoundException(accountNumber)
        );
    }


    public List<Account> listAccounts()
            throws SQLException {

        return accountDAO.findAllAccounts();
    }
}