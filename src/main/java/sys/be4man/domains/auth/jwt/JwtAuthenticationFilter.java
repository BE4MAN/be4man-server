package sys.be4man.domains.auth.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import sys.be4man.domains.account.model.type.Role;
import sys.be4man.domains.auth.dto.AccountPrincipal;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;

    /**
     * 인증 없이 접근 가능한 경로 목록 이 경로들은 JWT 검증을 건너뜁니다.
     */
    private static final List<String> EXCLUDE_PATHS = Arrays.asList(
            "/oauth",
            "/public",
            "/swagger-ui",
            "/v3/api-docs",
            "/api-docs",
            "/swagger-resources",
            "/webjars",
            "/api/health",
            "/h2-console"
    );

    /**
     * 요청을 필터링하여 JWT 토큰을 검증합니다.
     *
     * @param request     HTTP 요청
     * @param response    HTTP 응답
     * @param filterChain 필터 체인
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            String token = extractTokenFromRequest(request);

            if (token != null && jwtProvider.validateToken(token)) {
                setAuthenticationToContext(request, token);
            }
        } catch (Exception e) {
            log.error("JWT 인증 처리 중 오류가 발생했습니다: {}", e.getMessage(), e);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 특정 경로는 필터를 건너뛰도록 설정
     *
     * @param request HTTP 요청
     * @return 필터를 건너뛸지 여부
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return EXCLUDE_PATHS.stream().anyMatch(path::startsWith);
    }

    /**
     * HTTP 요청의 Authorization 헤더에서 JWT 토큰을 추출합니다. "Bearer {token}" 형식에서 토큰 부분만 반환합니다.
     *
     * @param request HTTP 요청
     * @return 추출된 JWT 토큰, 없으면 null
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        return null;
    }

    /**
     * JWT 토큰에서 계정 정보를 추출하여 SecurityContext에 인증 정보를 설정합니다.
     *
     * @param request HTTP 요청
     * @param token   JWT 토큰
     */
    private void setAuthenticationToContext(HttpServletRequest request, String token) {
        Long accountId = jwtProvider.getAccountIdFromToken(token);
        Role role = jwtProvider.getRoleFromToken(token);

        AccountPrincipal principal = new AccountPrincipal(accountId, role);
        UsernamePasswordAuthenticationToken authentication = createAuthentication(principal);

        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        log.debug("JWT 인증 성공 - accountId: {}, 권한: {}", accountId, role);
    }

    /**
     * AccountPrincipal로부터 Spring Security Authentication 객체 생성
     *
     * @param principal AccountPrincipal
     * @return UsernamePasswordAuthenticationToken
     */
    private UsernamePasswordAuthenticationToken createAuthentication(AccountPrincipal principal) {
        SimpleGrantedAuthority authority =
                new SimpleGrantedAuthority("ROLE_" + principal.role().name());

        return new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(authority)
        );
    }
}



