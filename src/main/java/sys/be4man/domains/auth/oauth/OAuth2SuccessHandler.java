package sys.be4man.domains.auth.oauth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
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
import sys.be4man.domains.auth.service.RefreshTokenRedisService;
import sys.be4man.domains.auth.service.SignTokenRedisService;

/**
 * OAuth2 로그인 성공 후 처리하는 핸들러
 * <p>
 * Flow: 1. GitHub에서 사용자 정보 추출 2. DB에서 계정 조회 (githubId 기준) 3-a. 계정 존재: JWT 발급, Refresh Token Redis
 * 저장 3-b. 계정 없음: SignToken 발급 (githubId만 포함), GitHub 정보는 Fragment로 전달
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final AccountRepository accountRepository;
    private final JwtProvider jwtProvider;
    private final RefreshTokenRedisService refreshTokenRedisService;
    private final SignTokenRedisService signTokenRedisService;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Override
    @Transactional
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {
        try {
            // GitHub 사용자 정보 추출
            OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
            Map<String, Object> attributes = oAuth2User.getAttributes();

            Long githubId = ((Integer) attributes.get("id")).longValue();
            String email = (String) attributes.get("email");
            String githubAccessToken = (String) attributes.get("githubAccessToken");
            String profileImageUrl = (String) attributes.get("avatar_url");

            log.info("OAuth2 success - githubId: {}, email: {}", githubId, email);

            Account account = accountRepository.findByGithubId(githubId)
                    .or(() -> accountRepository.findByEmail(email))
                    .orElse(null);

            String redirectUrl;

            if (account != null) {
                log.info("Existing account found - ID: {}, githubId: {}", account.getId(),
                         githubId);

                // GitHub Access Token 업데이트 (DB에만 저장)
                account.updateGitHubAccessToken(githubAccessToken);
                accountRepository.save(account);

                // Refresh Token 생성 및 Redis 저장 (UUID)
                refreshTokenRedisService.createAndSave(account.getId());

                redirectUrl = String.format(
                        "%s/auth/callback#requires_signup=false&access_token=%s&token_type=Bearer",
                        frontendUrl,
                        URLEncoder.encode(jwtProvider.generateAccessToken(
                                                  account.getId(),
                                                  account.getRole()
                                          ),
                                          StandardCharsets.UTF_8
                        )
                );

            } else {
                log.info("New user - signup required - githubId: {}", githubId);

                // GitHub 정보를 Redis에 임시 저장 (5분 TTL)
                GitHubTempInfo tempInfo = new GitHubTempInfo(
                        email,
                        githubAccessToken,
                        profileImageUrl
                );
                signTokenRedisService.save(githubId, tempInfo);

                // SignToken 생성 (githubId만 포함, 5분 유효)
                String signToken = jwtProvider.generateSignToken(githubId);

                // Fragment에 SignToken만 전달
                redirectUrl = String.format(
                        "%s/auth/callback#requires_signup=true&sign_token=%s",
                        frontendUrl,
                        URLEncoder.encode(signToken, StandardCharsets.UTF_8)
                );

            }
            response.sendRedirect(redirectUrl);

        } catch (Exception e) {
            log.error("OAuth2 success handler error: {}", e.getMessage(), e);
            String errorUrl = String.format(
                    "%s/auth/callback#error=%s",
                    frontendUrl,
                    URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8)
            );
            response.sendRedirect(errorUrl);
        }
    }
}
