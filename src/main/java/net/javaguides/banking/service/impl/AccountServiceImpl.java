package net.javaguides.banking.service.impl;

import net.javaguides.banking.dto.AccountDto;
import net.javaguides.banking.dto.TransactionDTO;
import net.javaguides.banking.dto.TransferFundDTO;
import net.javaguides.banking.entity.Account;
import net.javaguides.banking.entity.Transaction;
import net.javaguides.banking.exception.AccountException;
import net.javaguides.banking.mapper.AccountMapper;
import net.javaguides.banking.repository.AccountRepository;
import net.javaguides.banking.repository.TransactionRepository;
import net.javaguides.banking.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AccountServiceImpl implements AccountService {
    private AccountRepository accountRepository;

    private TransactionRepository transactionRepository;

    private static final String TRANSACTION_TYPE_DEPOSIT = "DEPOSIT";

    private static  final String TRANSACTION_TYPE_WITHDRAW="WITHDRAW";

    private static  final String TRANSACTION_TYPE_TRANSFER="TRANSFER";
    @Autowired 
    public  AccountServiceImpl(AccountRepository accountRepository, TransactionRepository transactionRepository){
        this.accountRepository=accountRepository;
        this.transactionRepository=transactionRepository;
    }
    @Override
    public AccountDto createAccount(AccountDto accountDto) {
        Account account= AccountMapper.mapToAccount(accountDto);
        Account savedAccount=accountRepository.save(account);

        return AccountMapper.mapToAccountDto(savedAccount);
    }

    @Override
    public AccountDto getAccountById(Long id) {
        Account account= accountRepository
                .findById(id)
                .orElseThrow(()->new AccountException("Account does not exist"));
        return AccountMapper.mapToAccountDto(account);
    }

    @Override
    public AccountDto deposit(Long id, double amount) {
        Account account= accountRepository
                .findById(id)
                .orElseThrow(()->new AccountException("Account does not exist"));
        double total=account.getBalance()+amount;
        account.setBalance(total);
        Account savedAccount= accountRepository.save(account);

        Transaction transaction=new Transaction();
        transaction.setAccountId(id);
        transaction.setAmount(amount);
        transaction.setTransactionType(TRANSACTION_TYPE_DEPOSIT);
        transaction.setTimestamp(LocalDateTime.now());

        transactionRepository.save(transaction);

        return AccountMapper.mapToAccountDto(savedAccount);
    }

    @Override
    public AccountDto withdraw(Long id, double amount) {
        Account account= accountRepository
                .findById(id)
                .orElseThrow(()->new AccountException("Account does not exist"));
        if(account.getBalance()<amount){
            throw new RuntimeException("Insufficient amount");
        }
        double total=account.getBalance()-amount;
        account.setBalance(total);
        Account savedAccount= accountRepository.save(account);

        Transaction transaction = new Transaction();
        transaction.setAccountId(id);
        transaction.setAmount(amount);
        transaction.setTransactionType(TRANSACTION_TYPE_WITHDRAW);
        transaction.setTimestamp(LocalDateTime.now());

        transactionRepository.save(transaction);

        return AccountMapper.mapToAccountDto(savedAccount);
    }

    @Override
    public List<AccountDto> getAllAccounts() {
        List<Account> accounts= accountRepository.findAll();
        return accounts.stream().map((account)->AccountMapper.mapToAccountDto(account))
                .collect(Collectors.toList());
    }

    @Override
    public void deleteAccount(Long id) {
        Account account= accountRepository
                .findById(id)
                .orElseThrow(()->new AccountException("Account does not exist"));
        accountRepository.deleteById(id);
    }

    @Override
    public void transferFunds(TransferFundDTO transferFundDTO) {
        //Retrieve the account from which we send the amount
        Account fromAccount = accountRepository
                .findById(transferFundDTO.fromAccountId())
                .orElseThrow(()->new AccountException("Account does not exist"));

        //Retrieve the account to which we send the amount
        Account toAccount = accountRepository.findById(transferFundDTO.toAccountId())
                .orElseThrow(()->new AccountException("Account does not exist"));

        if (fromAccount.getBalance()<transferFundDTO.amount()){
            throw  new RuntimeException("Insufficient Funds");
        }

        //debit the amount from fromAccount object
        fromAccount.setBalance(fromAccount.getBalance() - transferFundDTO.amount());

        //credit the amount to toAccount object
        toAccount.setBalance(toAccount.getBalance() + transferFundDTO.amount());

        accountRepository.save(fromAccount);

        accountRepository.save(toAccount);

        Transaction transaction = new Transaction();
        transaction.setAccountId(transferFundDTO.fromAccountId());
        transaction.setAmount(transferFundDTO.amount());
        transaction.setTransactionType(TRANSACTION_TYPE_TRANSFER);
        transaction.setTimestamp(LocalDateTime.now());

        transactionRepository.save(transaction);
    }

    @Override
    public List<TransactionDTO> getAccountTransactions(Long accountId) {

        List<Transaction> transactions = transactionRepository
                .findByAccountIdOrderByTimestampDesc(accountId);

        return  transactions.stream()
                .map(transaction -> convertEntityToDTO(transaction))
                .collect(Collectors.toList());

    }

    private TransactionDTO convertEntityToDTO(Transaction transaction){
        return new TransactionDTO(
                transaction.getId(),
                transaction.getAccountId(),
                transaction.getAmount(),
                transaction.getTransactionType(),
                transaction.getTimestamp()
        );
    }


}
