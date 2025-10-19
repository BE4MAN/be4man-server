package sys.be4man.domains.auth.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import sys.be4man.domains.account.model.type.Role;

/**
 * JWT 토큰 생성 및 검증을 담당하는 Provider 클래스
 */
@Slf4j
@Component
public class JwtProvider {

    private final SecretKey secretKey;
    private final long accessTokenExpiration;
    private final long signTokenExpiration;

    public JwtProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-expiration}") long accessExpiration,
            @Value("${jwt.sign-expiration}") long signExpiration
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiration = accessExpiration;
        this.signTokenExpiration = signExpiration;
    }

    /**
     * AccessToken 페이로드: accountId, role
     *
     * @param accountId 계정 ID
     * @param role      계정 권한
     * @return 생성된 AccessToken
     */
    public String generateAccessToken(Long accountId, Role role) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + accessTokenExpiration);

        return Jwts.builder()
                .subject(String.valueOf(accountId))
                .claim("role", role.name())
                .issuedAt(now)
                .expiration(expiration)
                .signWith(secretKey)
                .compact();
    }

    /**
     * SignToken 페이로드: GitHub ID
     *
     * @param githubId GitHub User ID
     * @return 생성된 SignToken
     */
    public String generateSignToken(Long githubId) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + signTokenExpiration);

        return Jwts.builder()
                .subject(String.valueOf(githubId))
                .claim("type", "sign")
                .issuedAt(now)
                .expiration(expiration)
                .signWith(secretKey)
                .compact();
    }

    /**
     * JWT 토큰 검증
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("만료된 JWT 토큰입니다: {}", e.getMessage());
        } catch (JwtException e) {
            log.warn("유효하지 않은 JWT 토큰입니다: {}", e.getMessage());
        } catch (Exception e) {
            log.error("JWT 토큰 검증 중 오류가 발생했습니다: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Access Token에서 계정 ID 추출
     */
    public Long getAccountIdFromToken(String token) {
        return getIdFromToken(token);
    }

    /**
     * Access Token에서 권한(Role) 추출
     */
    public Role getRoleFromToken(String token) {
        String roleName = getClaims(token).get("role", String.class);
        return Role.valueOf(roleName);
    }

    /**
     * SignToken에서 GitHub ID 추출
     */
    public Long getGithubIdFromSignToken(String signToken) {
        return getIdFromToken(signToken);
    }

    /**
     * JWT 토큰에서 ID(subject) 추출
     */
    private Long getIdFromToken(String token) {
        return Long.valueOf(getClaims(token).getSubject());
    }

    /**
     * SignToken 검증
     */
    public boolean validateSignToken(String signToken) {
        try {
            Claims claims = getClaims(signToken);
            String tokenType = claims.get("type", String.class);

            if (!"sign".equals(tokenType)) {
                log.warn("SignToken이 아닌 토큰입니다: {}", tokenType);
                return false;
            }

            return true;
        } catch (ExpiredJwtException e) {
            log.warn("만료된 SignToken입니다: {}", e.getMessage());
        } catch (JwtException e) {
            log.warn("유효하지 않은 SignToken입니다: {}", e.getMessage());
        } catch (Exception e) {
            log.error("SignToken 검증 중 오류가 발생했습니다: {}", e.getMessage());
        }
        return false;
    }

    /**
     * JWT 토큰에서 Claims 추출
     */
    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
