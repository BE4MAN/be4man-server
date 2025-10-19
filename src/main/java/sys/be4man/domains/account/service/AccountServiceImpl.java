package sys.be4man.domains.account.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sys.be4man.domains.account.dto.response.AccountInfoResponse;
import sys.be4man.domains.account.model.entity.Account;

/**
 * 계정 관련 비즈니스 로직 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountChecker accountChecker;

    @Override
    @Transactional(readOnly = true)
    public AccountInfoResponse getMyAccount(Long accountId) {
        log.info("계정 정보 조회 - accountId: {}", accountId);

        Account account = accountChecker.checkAccountExists(accountId);

        return AccountInfoResponse.from(account);
    }
}
