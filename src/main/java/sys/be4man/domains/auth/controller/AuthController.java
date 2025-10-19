package sys.be4man.domains.auth.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sys.be4man.domains.auth.dto.request.RefreshRequest;
import sys.be4man.domains.auth.dto.request.SigninRequest;
import sys.be4man.domains.auth.dto.request.SignupRequest;
import sys.be4man.domains.auth.dto.response.AuthResponse;
import sys.be4man.domains.auth.service.AuthService;

/**
 * 인증 관련 API 컨트롤러
 */
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
    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(
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
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @Valid @RequestBody RefreshRequest request
    ) {
        log.info("토큰 갱신 요청");

        AuthResponse authResponse = authService.refresh(request.refreshToken());

        return ResponseEntity.ok(authResponse);
    }

    /**
     * 로그아웃 Authorization 헤더로 Access Token 받음
     *
     * @param authorization Authorization: Bearer {accessToken}
     * @return 성공 메시지
     */
    @PostMapping("/logout")
    public ResponseEntity<String> logout(
            @RequestHeader("Authorization") String authorization
    ) {
        log.info("로그아웃 요청");

        authService.logout(authorization);

        return ResponseEntity.ok("로그아웃이 완료되었습니다");
    }
}
