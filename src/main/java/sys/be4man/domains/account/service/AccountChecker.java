package sys.be4man.domains.account.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import sys.be4man.domains.account.model.entity.Account;
import sys.be4man.domains.account.repository.AccountRepository;
import sys.be4man.global.exception.NotFoundException;
import sys.be4man.global.exception.type.CommonExceptionType;

@Component
@RequiredArgsConstructor
public class AccountChecker {
    private final AccountRepository accountRepository;

    public Account checkAccountExists(Long accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException(CommonExceptionType.NOT_FOUND));
    }

}
