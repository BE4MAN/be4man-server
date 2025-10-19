package sys.be4man.domains.account.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sys.be4man.domains.account.dto.response.AccountInfoResponse;
import sys.be4man.domains.account.service.AccountService;
import sys.be4man.domains.auth.dto.AccountPrincipal;

/**
 * 계정 관련 API 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    /**
     * 내 계정 정보 조회
     */
    @GetMapping("/me")
    public ResponseEntity<AccountInfoResponse> getMyAccount(
            @AuthenticationPrincipal AccountPrincipal principal
    ) {
        return ResponseEntity.ok(accountService.getMyAccount(principal.accountId()));
    }
}
