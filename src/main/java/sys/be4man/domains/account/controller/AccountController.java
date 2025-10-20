package sys.be4man.domains.account.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import sys.be4man.global.dto.response.ErrorResponse;

/**
 * 계정 관련 API 컨트롤러
 */
@Tag(name = "Account", description = "계정 API")
@Slf4j
@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    /**
     * 내 계정 정보 조회
     */
    @Operation(summary = "내 계정 정보 조회", description = "현재 로그인한 사용자의 계정 정보를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = AccountInfoResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "계정을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/me")
    public ResponseEntity<AccountInfoResponse> getMyAccount(
            @AuthenticationPrincipal AccountPrincipal principal
    ) {
        return ResponseEntity.ok(accountService.getMyAccount(principal.accountId()));
    }
}
