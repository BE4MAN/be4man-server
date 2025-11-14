package sys.be4man.domains.account.service;

import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sys.be4man.domains.account.dto.response.AccountInfoResponse;
import sys.be4man.domains.account.dto.response.ApprovalLineAccountResponse;
import sys.be4man.domains.account.model.entity.Account;
import sys.be4man.domains.account.model.type.JobDepartment;
import sys.be4man.domains.account.repository.AccountRepository;

/**
 * 계정 관련 비즈니스 로직 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountChecker accountChecker;
    private final AccountRepository accountRepository;

    @Override
    @Transactional(readOnly = true)
    public AccountInfoResponse getMyAccount(Long accountId) {
        log.info("계정 정보 조회 - accountId: {}", accountId);

        Account account = accountChecker.checkAccountExists(accountId);

        return AccountInfoResponse.from(account);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApprovalLineAccountResponse> searchApprovalLineAccounts(String department, String keyword) {
        log.info("결재라인 사원 목록 조회 - department: {}, keyword: {}", department, keyword);

        List<Account> accounts = accountRepository.findAll();

        JobDepartment deptEnum = null;
        if (department != null && !department.isBlank()) {
            try {
                deptEnum = JobDepartment.valueOf(department.trim());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid department enum value: {}", department);
            }
        }

        String trimmedKeyword = keyword != null ? keyword.trim().toLowerCase() : null;

        JobDepartment finalDeptEnum = deptEnum;

        return accounts.stream()
                .filter(acc -> {
                    if (finalDeptEnum != null && acc.getDepartment() != finalDeptEnum) {
                        return false;
                    }

                    if (trimmedKeyword != null && !trimmedKeyword.isEmpty()) {
                        String haystack = String.join(" ",
                                        safe(acc.getName()),
                                        safe(acc.getEmail()),
                                        acc.getDepartment() != null ? acc.getDepartment().name() : "",
                                        acc.getPosition() != null ? acc.getPosition().name() : ""
                                )
                                .toLowerCase();

                        return haystack.contains(trimmedKeyword);
                    }

                    return true;
                })
                .map(ApprovalLineAccountResponse::from)
                .collect(Collectors.toList());
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }
}
