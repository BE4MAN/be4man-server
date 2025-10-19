package sys.be4man.domains.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sys.be4man.domains.account.model.entity.Account;
import sys.be4man.domains.account.model.type.AccountPosition;
import sys.be4man.domains.account.model.type.Role;
import sys.be4man.domains.account.repository.AccountRepository;
import sys.be4man.domains.account.service.AccountChecker;
import sys.be4man.domains.auth.dto.GitHubTempInfo;
import sys.be4man.domains.auth.dto.response.AuthResponse;
import sys.be4man.domains.auth.exception.AuthException;
import sys.be4man.domains.auth.exception.type.AuthExceptionType;
import sys.be4man.domains.auth.jwt.JwtProvider;

/**
 * 인증 관련 비즈니스 로직 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AccountRepository accountRepository;
    private final AccountChecker accountChecker;
    private final JwtProvider jwtProvider;
    private final RefreshTokenRedisService refreshTokenRedisService;
    private final SignTokenRedisService signTokenRedisService;
    private final OAuthCodeRedisService oauthCodeRedisService;

    /**
     * SignToken을 사용하여 신규 계정 생성
     */
    @Override
    @Transactional
    public AuthResponse signup(String signToken, String name,
            String department, AccountPosition position) {
        log.info("회원가입 시도 - name: {}", name);

        if (!jwtProvider.validateSignToken(signToken)) {
            throw new AuthException(AuthExceptionType.INVALID_SIGN_TOKEN);
        }

        // SignToken에서 githubId 추출
        Long githubId = jwtProvider.getGithubIdFromSignToken(signToken);

        // Redis에서 GitHub 정보 조회 및 삭제
        GitHubTempInfo tempInfo = signTokenRedisService.getAndDelete(githubId)
                .orElseThrow(() -> new AuthException(AuthExceptionType.SIGN_TOKEN_INFO_NOT_FOUND));
        log.info("회원가입 진행 - githubId: {}", githubId);

        // 중복 계정 확인
        accountChecker.checkConflictAccountExistsByGithubId(githubId);

        // 신규 계정 생성
        Account newAccount = Account.builder()
                .githubId(githubId)
                .email(tempInfo.email())
                .name(name)
                .department(department)
                .profileImageUrl(tempInfo.profileImageUrl())
                .role(Role.DEVELOPER) // 기본 역할
                .position(position)
                .githubAccessToken(tempInfo.githubAccessToken())
                .build();

        accountRepository.save(newAccount);

        String accessToken = jwtProvider.generateAccessToken(
                newAccount.getId(),
                newAccount.getRole()
        );

        String refreshToken = refreshTokenRedisService.createAndSave(newAccount.getId());

        log.info("회원가입 완료 - ID: {}, githubId: {}", newAccount.getId(), githubId);

        return new AuthResponse(accessToken, refreshToken);
    }

    /**
     * OAuth 임시 코드로 로그인 (기존 사용자)
     */
    @Override
    @Transactional(readOnly = true)
    public AuthResponse signin(String tempCode) {
        log.info("로그인 시도 - tempCode: {}", tempCode);

        // Temporary Code에서 계정 ID 조회 (1회용)
        Long accountId = oauthCodeRedisService.getAndDelete(tempCode)
                .orElseThrow(() -> new AuthException(AuthExceptionType.INVALID_TEMP_CODE));

        Account account = accountChecker.checkAccountExists(accountId);

        // Access Token 발급
        String accessToken = jwtProvider.generateAccessToken(
                account.getId(),
                account.getRole()
        );

        // Refresh Token 발급 (기존 토큰 교체)
        String refreshToken = refreshTokenRedisService.createAndSave(account.getId());

        log.info("로그인 완료 - accountId: {}", accountId);

        return new AuthResponse(accessToken, refreshToken);
    }

    /**
     * Refresh Token으로 새로운 Access Token 및 Refresh Token 발급 (Rotation)
     */
    @Override
    @Transactional(readOnly = true)
    public AuthResponse refresh(String refreshToken) {
        log.info("토큰 갱신 시도");

        // 1단계: JWT 검증 (만료, 서명, 형식)
        if (!jwtProvider.validateRefreshToken(refreshToken)) {
            log.warn("유효하지 않은 Refresh Token");
            throw new AuthException(AuthExceptionType.INVALID_REFRESH_TOKEN);
        }

        Long accountId = jwtProvider.getAccountIdFromToken(refreshToken);
        String jti = jwtProvider.getJtiFromToken(refreshToken);

        // 2단계: Redis 검증 (로그아웃 여부, JTI 일치)
        String storedJti = refreshTokenRedisService.get(accountId)
                .orElseThrow(() -> new AuthException(AuthExceptionType.REFRESH_TOKEN_NOT_FOUND));

        if (!storedJti.equals(jti)) {
            log.warn("Refresh Token JTI 불일치 - accountId: {}", accountId);
            throw new AuthException(AuthExceptionType.REFRESH_TOKEN_MISMATCH);
        }

        // 계정 확인
        Account account = accountChecker.checkAccountExists(accountId);

        // 새 토큰 발급 (Rotation)
        String newAccessToken = jwtProvider.generateAccessToken(
                account.getId(),
                account.getRole()
        );

        String newRefreshToken = refreshTokenRedisService.createAndSave(account.getId());

        log.info("토큰 갱신 완료 - accountId: {}", accountId);

        return new AuthResponse(newAccessToken, newRefreshToken);
    }

    /**
     * 로그아웃 - Redis에서 Refresh Token 삭제
     */
    @Override
    @Transactional
    public void logout(String authorization) {
        String accessToken = authorization.replace("Bearer ", "");

        // Access Token 검증
        if (!jwtProvider.validateToken(accessToken)) {
            log.warn("유효하지 않은 Access Token으로 로그아웃 시도");
            throw new AuthException(AuthExceptionType.INVALID_ACCESS_TOKEN);
        }

        Long accountId = jwtProvider.getAccountIdFromToken(accessToken);
        refreshTokenRedisService.delete(accountId);

        log.info("로그아웃 완료 - accountId: {}", accountId);
    }
}
