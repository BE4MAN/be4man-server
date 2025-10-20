package sys.be4man.domains.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sys.be4man.domains.auth.dto.AccountPrincipal;
import sys.be4man.domains.auth.dto.request.RefreshRequest;
import sys.be4man.domains.auth.dto.request.SigninRequest;
import sys.be4man.domains.auth.dto.request.SignupRequest;
import sys.be4man.domains.auth.dto.response.AuthResponse;
import sys.be4man.domains.auth.service.AuthService;
import sys.be4man.global.dto.response.ErrorResponse;

/**
 * 인증 관련 API 컨트롤러
 */
@Tag(name = "Auth", description = "인증 API")
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 회원가입 Authorization 헤더로 SignToken 받음
     *
     * @param authorization Authorization: Bearer {signToken}
     * @param request       회원가입 정보 (name, department, position)
     */
    @Operation(summary = "회원가입", description = "GitHub OAuth 인증 후 SignToken을 사용하여 계정을 생성합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "회원가입 성공",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "401", description = "유효하지 않은 SignToken",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "이미 가입된 계정",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(
            @Parameter(description = "SignToken (Bearer 토큰)", required = true)
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody SignupRequest request
    ) {
        log.info("회원가입 요청 - name: {}", request.name());

        // Bearer 토큰 추출
        String signToken = authorization.replace("Bearer ", "");

        AuthResponse authResponse = authService.signup(
                signToken,
                request.name(),
                request.department(),
                request.position()
        );

        return ResponseEntity.ok(authResponse);
    }

    /**
     * 로그인 (기존 사용자)
     * OAuth 인증 후 발급된 임시 코드로 토큰 발급
     *
     * @param request 임시 코드
     * @return Access Token + Refresh Token
     */
    @Operation(summary = "로그인", description = "GitHub OAuth 인증 후 발급된 임시 코드로 Access Token과 Refresh Token을 발급받습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 성공",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "401", description = "유효하지 않거나 만료된 임시 코드",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "계정을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/signin")
    public ResponseEntity<AuthResponse> signin(
            @Valid @RequestBody SigninRequest request
    ) {
        log.info("로그인 요청");

        AuthResponse authResponse = authService.signin(request.code());

        return ResponseEntity.ok(authResponse);
    }

    /**
     * 토큰 갱신
     * Refresh Token으로 새로운 Access Token 및 Refresh Token 발급
     *
     * @param request Refresh Token
     * @return 새로운 Access Token + Refresh Token
     */
    @Operation(summary = "토큰 갱신", description = "Refresh Token을 사용하여 새로운 Access Token과 Refresh Token을 발급받습니다. (Token Rotation)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "토큰 갱신 성공",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "401", description = "유효하지 않거나 만료된 Refresh Token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "계정을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @Valid @RequestBody RefreshRequest request
    ) {
        log.info("토큰 갱신 요청");

        AuthResponse authResponse = authService.refresh(request.refreshToken());

        return ResponseEntity.ok(authResponse);
    }

    /**
     * 로그아웃
     * Redis에서 Refresh Token을 삭제하여 로그아웃합니다.
     *
     * @param principal 현재 로그인한 사용자 정보
     * @return 성공 메시지
     */
    @Operation(summary = "로그아웃", description = "Redis에서 Refresh Token을 삭제하여 로그아웃합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그아웃 성공",
                    content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping("/logout")
    public ResponseEntity<String> logout(
            @Parameter(hidden = true) @AuthenticationPrincipal AccountPrincipal principal
    ) {
        log.info("로그아웃 요청 - accountId: {}", principal.accountId());

        authService.logout(principal.accountId());

        return ResponseEntity.ok("로그아웃이 완료되었습니다");
    }
}
