package com.bank.manager;

import com.bank.config.DBConnection;
import com.bank.dao.AccountDAO;
import com.bank.dao.TransactionDAO;
import com.bank.exception.AccountNotFoundException;
import com.bank.exception.InsufficientBalanceException;
import com.bank.exception.InvalidTransactionException;
import com.bank.model.Account;
import com.bank.model.Transaction;
import com.bank.model.TransactionType;
import com.bank.util.ReferenceNumberGenerator;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Logger;


public class TransactionManager {

    private static final Logger LOGGER =
            Logger.getLogger(TransactionManager.class.getName());

    private static final int MINI_STATEMENT_LIMIT = 10;


    private final AccountDAO accountDAO;
    private final TransactionDAO transactionDAO;
    private final AccountManager accountService;


    public TransactionManager(
            AccountDAO accountDAO,
            TransactionDAO transactionDAO,
            AccountManager accountService
    ) {
        this.accountDAO = accountDAO;
        this.transactionDAO = transactionDAO;
        this.accountService = accountService;
    }


    public Transaction deposit(
            String accountNumber,
            BigDecimal amount
    )
            throws SQLException,
            AccountNotFoundException,
            InvalidTransactionException {


        validateAmount(amount);


        Account account =
                accountService.findAccountOrThrow(accountNumber);


        BigDecimal newBalance =
                account.getBalance().add(amount);


        accountDAO.updateBalance(
                accountNumber,
                newBalance
        );


        Transaction txn = new Transaction(
                accountNumber,
                TransactionType.DEPOSIT,
                amount,
                newBalance,
                ReferenceNumberGenerator.generate(),
                "Cash deposit"
        );


        transactionDAO.save(txn);


        LOGGER.info(
                "Deposit completed: "
                        + txn.getReferenceNumber()
        );


        return txn;
    }



    public Transaction withdraw(
            String accountNumber,
            BigDecimal amount
    )
            throws SQLException,
            AccountNotFoundException,
            InvalidTransactionException,
            InsufficientBalanceException {


        validateAmount(amount);


        Account account =
                accountService.findAccountOrThrow(accountNumber);


        validateSufficientBalance(
                account,
                amount
        );


        BigDecimal newBalance =
                account.getBalance().subtract(amount);


        accountDAO.updateBalance(
                accountNumber,
                newBalance
        );


        Transaction txn = new Transaction(
                accountNumber,
                TransactionType.WITHDRAW,
                amount,
                newBalance,
                ReferenceNumberGenerator.generate(),
                "Cash withdrawal"
        );


        transactionDAO.save(txn);


        LOGGER.info(
                "Withdrawal completed: "
                        + txn.getReferenceNumber()
        );


        return txn;
    }



    public Transaction transfer(
            String senderNumber,
            String receiverNumber,
            BigDecimal amount
    )
            throws SQLException,
            AccountNotFoundException,
            InvalidTransactionException,
            InsufficientBalanceException {


        validateAmount(amount);


        if (senderNumber.equalsIgnoreCase(receiverNumber)) {
            throw new InvalidTransactionException(
                    "Sender and receiver cannot be the same account."
            );
        }


        Account sender =
                accountService.findAccountOrThrow(senderNumber);


        Account receiver =
                accountService.findAccountOrThrow(receiverNumber);



        validateSufficientBalance(
                sender,
                amount
        );


        BigDecimal senderNewBalance =
                sender.getBalance()
                        .subtract(amount);


        BigDecimal receiverNewBalance =
                receiver.getBalance()
                        .add(amount);



        Transaction transferOut =
                new Transaction(
                        senderNumber,
                        TransactionType.TRANSFER_OUT,
                        amount,
                        senderNewBalance,
                        ReferenceNumberGenerator.generate(),
                        "Transfer to " + receiverNumber
                );


        Transaction transferIn =
                new Transaction(
                        receiverNumber,
                        TransactionType.TRANSFER_IN,
                        amount,
                        receiverNewBalance,
                        ReferenceNumberGenerator.generate(),
                        "Transfer from " + senderNumber
                );



        try (Connection conn =
                     DBConnection.getConnection()) {


            conn.setAutoCommit(false);


            try {

                accountDAO.updateBalance(
                        conn,
                        senderNumber,
                        senderNewBalance
                );


                accountDAO.updateBalance(
                        conn,
                        receiverNumber,
                        receiverNewBalance
                );


                transactionDAO.save(
                        conn,
                        transferOut
                );


                transactionDAO.save(
                        conn,
                        transferIn
                );


                conn.commit();


            } catch (SQLException e) {

                conn.rollback();

                throw e;

            } finally {

                conn.setAutoCommit(true);
            }
        }



        LOGGER.info(
                "Transfer completed: "
                        + transferOut.getReferenceNumber()
        );


        return transferOut;
    }


    public List<Transaction> viewTransactionHistory(
            String accountNumber
    )
            throws SQLException,
            AccountNotFoundException {


        accountService.findAccountOrThrow(
                accountNumber
        );


        return transactionDAO.findByAccountNumber(
                accountNumber
        );
    }


    public List<Transaction> miniStatement(
            String accountNumber
    )
            throws SQLException,
            AccountNotFoundException {


        accountService.findAccountOrThrow(
                accountNumber
        );


        return transactionDAO.findRecentTransactions(
                accountNumber,
                MINI_STATEMENT_LIMIT
        );
    }




    private void validateAmount(
            BigDecimal amount
    )
            throws InvalidTransactionException {


        if (amount == null ||
                amount.compareTo(BigDecimal.ZERO) <= 0) {


            throw new InvalidTransactionException(
                    "Amount must be greater than zero."
            );
        }
    }



    private void validateSufficientBalance(
            Account account,
            BigDecimal amount
    )
            throws InsufficientBalanceException {


        if (account.getBalance()
                .compareTo(amount) < 0) {


            throw new InsufficientBalanceException(
                    account.getBalance(),
                    amount
            );
        }
    }

}