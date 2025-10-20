package sys.be4man.domains.account.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import sys.be4man.domains.account.exception.type.AccountExceptionType;
import sys.be4man.domains.account.model.entity.Account;
import sys.be4man.domains.account.repository.AccountRepository;
import sys.be4man.global.exception.ConflictException;
import sys.be4man.global.exception.NotFoundException;

/**
 * 계정 존재 여부 검증 유틸리티
 */
@Component
@RequiredArgsConstructor
public class AccountChecker {

    private final AccountRepository accountRepository;

    /**
     * 계정 ID로 계정 존재 여부 확인
     *
     * @param accountId 계정 ID
     * @return 존재하는 계정
     * @throws NotFoundException 계정을 찾을 수 없는 경우
     */
    public Account checkAccountExists(Long accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException(AccountExceptionType.ACCOUNT_NOT_FOUND));
    }

    /**
     * GitHub ID로 계정 중복 확인
     *
     * @param githubId GitHub ID
     * @throws ConflictException 이미 존재하는 계정인 경우
     */
    public void checkConflictAccountExistsByGithubId(Long githubId) {
        if (accountRepository.findByGithubId(githubId).isPresent()) {
            throw new ConflictException();
        }
    }
}
