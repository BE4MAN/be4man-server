package sys.be4man.domains.auth.service;

import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import sys.be4man.domains.auth.jwt.JwtProvider;

/**
 * Refresh Token을 Redis에 저장/조회/삭제하는 서비스
 * <p>
 * - Refresh Token: JWT 형식 (accountId, jti, type) - Redis 저장 형태: Key = "refresh:{accountId}", Value
 * = JTI (JWT ID) - TTL: 2주 (Redis key expiration으로 자동 관리) - 클라이언트에 전달: JWT 형식의 Refresh Token
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenRedisService {

    private final RedisTemplate<String, String> refreshTokenRedisTemplate;
    private final JwtProvider jwtProvider;

    private static final String KEY_PREFIX = "refresh:";
    private static final Duration TTL = Duration.ofDays(14); // 2주

    /**
     * Refresh Token 생성 및 Redis 저장
     *
     * @param accountId 계정 ID
     * @return JWT 형식의 Refresh Token (클라이언트에 전달)
     */
    public String createAndSave(Long accountId) {
        String refreshToken = jwtProvider.generateRefreshToken(accountId);
        String jti = jwtProvider.getJtiFromToken(refreshToken);

        String key = KEY_PREFIX + accountId;
        refreshTokenRedisTemplate.opsForValue().set(key, jti, TTL);

        log.info("Refresh Token created and saved - accountId: {}, jti: {}", accountId, jti);

        return refreshToken;
    }

    /**
     * Redis에서 JTI 조회
     *
     * @param accountId 계정 ID
     * @return JTI (JWT ID)
     */
    public Optional<String> get(Long accountId) {
        String key = KEY_PREFIX + accountId;
        String jti = refreshTokenRedisTemplate.opsForValue().get(key);
        return Optional.ofNullable(jti);
    }

    /**
     * Refresh Token 삭제 (로그아웃)
     *
     * @param accountId 계정 ID
     */
    public void delete(Long accountId) {
        String key = KEY_PREFIX + accountId;
        refreshTokenRedisTemplate.delete(key);
        log.info("Refresh Token deleted - accountId: {}", accountId);
    }

    /**
     * Refresh Token 존재 여부 확인
     *
     * @param accountId 계정 ID
     * @return 존재하면 true
     */
    public boolean exists(Long accountId) {
        String key = KEY_PREFIX + accountId;
        return refreshTokenRedisTemplate.hasKey(key);
    }
}

