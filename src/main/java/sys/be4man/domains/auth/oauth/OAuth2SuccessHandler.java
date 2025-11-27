// 작성자 : 이원석
package sys.be4man.domains.auth.oauth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import sys.be4man.domains.account.model.entity.Account;
import sys.be4man.domains.account.repository.AccountRepository;
import sys.be4man.domains.auth.dto.GitHubTempInfo;
import sys.be4man.domains.auth.jwt.JwtProvider;
import sys.be4man.domains.auth.service.OAuthCodeRedisService;
import sys.be4man.domains.auth.service.SignTokenRedisService;

/**
 * OAuth2 로그인 성공 후 처리하는 핸들러
 * <p>
 * Flow: 1. GitHub에서 사용자 정보 추출 2. DB에서 계정 조회 (githubId 기준) 3-a. 계정 존재: Temporary Code 발급 → /signin
 * API 3-b. 계정 없음: SignToken 발급 → /signup API
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final AccountRepository accountRepository;
    private final JwtProvider jwtProvider;
    private final SignTokenRedisService signTokenRedisService;
    private final OAuthCodeRedisService oauthCodeRedisService;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    /**
     * GitHub 사용자 정보 DTO (내부 사용)
     */
    private record GitHubUserInfo(
            Long githubId,
            String email,
            String githubAccessToken,
            String profileImageUrl
    ) {

    }

    @Override
    @Transactional
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {
        try {
            GitHubUserInfo userInfo = extractGitHubUserInfo(authentication);
            Account account = accountRepository.findByGithubId(userInfo.githubId()).orElse(null);

            String redirectUrl = (account != null)
                    ? handleExistingAccount(account, userInfo)
                    : handleNewUser(userInfo);

            response.sendRedirect(redirectUrl);

        } catch (Exception e) {
            handleError(response, e);
        }
    }

    /**
     * GitHub 사용자 정보 추출 및 검증
     */
    private GitHubUserInfo extractGitHubUserInfo(Authentication authentication) {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        Map<String, Object> attributes = oAuth2User.getAttributes();

        Long githubId = Optional.ofNullable(attributes.get("id"))
                .map(id -> ((Number) id).longValue())
                .orElseThrow(() -> new IllegalStateException("GitHub ID를 가져올 수 없습니다"));

        String email = (String) attributes.get("email");

        String githubAccessToken = Optional.ofNullable((String) attributes.get("githubAccessToken"))
                .orElseThrow(() -> new IllegalStateException("GitHub Access Token을 가져올 수 없습니다"));

        String profileImageUrl = (String) attributes.get("avatar_url");

        log.info("GitHub 정보 추출 완료 - githubId: {}", githubId);
        return new GitHubUserInfo(githubId, email, githubAccessToken, profileImageUrl);
    }

    /**
     * 기존 사용자 처리: Temporary Code 발급
     */
    private String handleExistingAccount(Account account, GitHubUserInfo userInfo) {
        log.info("기존 계정 발견 - accountId: {}, githubId: {}",
                 account.getId(), userInfo.githubId());

        // GitHub Access Token 업데이트
        account.updateGitHubAccessToken(userInfo.githubAccessToken());
        accountRepository.save(account);

        // Temporary Code 생성 및 Redis 저장 (5분 유효)
        String tempCode = UUID.randomUUID().toString();
        oauthCodeRedisService.save(tempCode, account.getId());

        return String.format(
                "%s/auth/callback#requires_signup=false&code=%s",
                frontendUrl,
                tempCode
        );
    }

    /**
     * 신규 사용자 처리: SignToken 발급
     */
    private String handleNewUser(GitHubUserInfo userInfo) {
        log.info("신규 사용자 - githubId: {}", userInfo.githubId());

        // GitHub 정보를 Redis에 임시 저장 (5분 TTL)
        GitHubTempInfo tempInfo = new GitHubTempInfo(
                userInfo.email(),
                userInfo.githubAccessToken(),
                userInfo.profileImageUrl()
        );
        signTokenRedisService.save(userInfo.githubId(), tempInfo);

        // SignToken 생성 (githubId만 포함, 5분 유효)
        String signToken = jwtProvider.generateSignToken(userInfo.githubId());

        return String.format(
                "%s/auth/callback#requires_signup=true&sign_token=%s",
                frontendUrl,
                URLEncoder.encode(signToken, StandardCharsets.UTF_8)
        );
    }

    /**
     * 에러 처리: 에러 메시지와 함께 프론트엔드로 리다이렉트
     */
    private void handleError(HttpServletResponse response, Exception e) throws IOException {
        log.error("OAuth2 처리 중 오류 발생: {}", e.getMessage(), e);

        String errorUrl = String.format(
                "%s/auth/callback#error=%s",
                frontendUrl,
                URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8)
        );
        response.sendRedirect(errorUrl);
    }
}
