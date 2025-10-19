package sys.be4man.domains.account.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sys.be4man.domains.account.dto.response.AccountInfoResponse;
import sys.be4man.domains.account.model.entity.Account;
import sys.be4man.domains.account.repository.AccountRepository;
import sys.be4man.global.exception.NotFoundException;

/**
 * 계정 관련 API 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountRepository accountRepository;

    /**
     * 내 계정 정보 조회 JWT AccessToken에서 userId 추출하여 계정 조회
     */
    @GetMapping("/me")
    public ResponseEntity<AccountInfoResponse> getMyAccount(Authentication authentication) {
        Long userId = Long.valueOf(authentication.getName());

        log.info("계정 정보 조회 요청 - userId: {}", userId);

        Account account = accountRepository.findById(userId)
                .orElseThrow(NotFoundException::new);

        AccountInfoResponse accountInfo = new AccountInfoResponse(
                account.getId(),
                account.getEmail(),
                account.getName(),
                account.getProfileImageUrl(),
                account.getRole(),
                account.getPosition(),
                account.getDepartment()
        );

        return ResponseEntity.ok(accountInfo);
    }
}
