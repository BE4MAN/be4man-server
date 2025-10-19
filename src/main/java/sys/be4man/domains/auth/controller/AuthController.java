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
     * 토큰 갱신 Authorization 헤더로 Access Token 받음
     *
     * @param authorization Authorization: Bearer {accessToken}
     * @return 새로운 Access Token + 계정 정보
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @RequestHeader("Authorization") String authorization
    ) {
        log.info("토큰 갱신 요청");

        // Bearer 토큰 추출
        String accessToken = authorization.replace("Bearer ", "");

        AuthResponse authResponse = authService.refresh(accessToken);

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
