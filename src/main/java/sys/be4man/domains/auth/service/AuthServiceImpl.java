package sys.be4man.domains.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sys.be4man.domains.account.model.entity.Account;
import sys.be4man.domains.account.model.type.AccountPosition;
import sys.be4man.domains.account.model.type.Role;
import sys.be4man.domains.account.repository.AccountRepository;
import sys.be4man.domains.auth.dto.GitHubTempInfo;
import sys.be4man.domains.auth.dto.response.AuthResponse;
import sys.be4man.domains.auth.exception.AuthException;
import sys.be4man.domains.auth.exception.type.AuthExceptionType;
import sys.be4man.domains.auth.jwt.JwtProvider;
import sys.be4man.global.exception.ConflictException;
import sys.be4man.global.exception.NotFoundException;

/**
 * 인증 관련 비즈니스 로직 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AccountRepository accountRepository;
    private final JwtProvider jwtProvider;
    private final RefreshTokenRedisService refreshTokenRedisService;
    private final SignTokenRedisService signTokenRedisService;

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

        // Redis에서 가져온 이메일 사용
        String email = tempInfo.email();

        log.info("회원가입 진행 - githubId: {}, email: {}", githubId, email);

        // 중복 계정 확인
        if (accountRepository.findByGithubId(githubId).isPresent()) {
            log.warn("중복 계정 시도 - githubId: {}", githubId);
            throw new ConflictException();
        }

        // 신규 계정 생성
        Account newAccount = Account.builder()
                .githubId(githubId)
                .email(email)
                .name(name)
                .department(department)
                .profileImageUrl(tempInfo.profileImageUrl())
                .role(Role.DEVELOPER) // 기본 역할
                .position(position)
                .githubAccessToken(tempInfo.githubAccessToken())
                .build();

        accountRepository.save(newAccount);

        // JWT AccessToken 생성 (userId, role만)
        String accessToken = jwtProvider.generateAccessToken(
                newAccount.getId(),
                newAccount.getRole()
        );

        // Refresh Token 생성 및 Redis 저장 (UUID)
        refreshTokenRedisService.createAndSave(newAccount.getId());

        log.info("회원가입 완료 - ID: {}, githubId: {}", newAccount.getId(), githubId);

        return new AuthResponse(accessToken, "Bearer");
    }

    /**
     * Access Token으로 새로운 Access Token 발급
     */
    @Override
    @Transactional(readOnly = true)
    public AuthResponse refresh(String accessToken) {
        Long userId;
        try {
            userId = jwtProvider.getAccountIdFromToken(accessToken);
        } catch (Exception e) {
            log.warn("Access Token 파싱 실패 - error: {}", e.getMessage());
            throw new AuthException(AuthExceptionType.ACCESS_TOKEN_PARSE_FAILED);
        }

        if (!refreshTokenRedisService.exists(userId)) {
            log.warn("Refresh Token 없음 - userId: {}", userId);
            throw new AuthException(AuthExceptionType.REFRESH_TOKEN_NOT_FOUND);
        }

        Account account = accountRepository.findById(userId)
                .orElseThrow(NotFoundException::new);

        String newAccessToken = jwtProvider.generateAccessToken(
                account.getId(),
                account.getRole()
        );

        log.info("토큰 갱신 완료 - userId: {}", userId);

        return new AuthResponse(newAccessToken, "Bearer");
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
