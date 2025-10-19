package sys.be4man.domains.auth.service;

import java.time.Duration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Refresh Token을 Redis에 저장/조회/삭제하는 서비스
 * <p>
 * - Refresh Token: UUID로 발급 (JWT 아님) - Redis 저장 형태: Key = "refresh:{userId}", Value = UUID - TTL:
 * 2주 (Redis key expiration으로 자동 관리) - 클라이언트에 전달하지 않음 (서버 내부에서만 관리)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenRedisService {

    private final RedisTemplate<String, String> refreshTokenRedisTemplate;

    private static final String KEY_PREFIX = "refresh:";
    private static final Duration TTL = Duration.ofDays(14); // 2주

    /**
     * Refresh Token 생성 및 저장
     *
     * @param userId 사용자 ID
     */
    public void createAndSave(Long userId) {
        String refreshToken = UUID.randomUUID().toString();
        String key = KEY_PREFIX + userId;
        refreshTokenRedisTemplate.opsForValue().set(key, refreshToken, TTL);
        log.info("Refresh Token created and saved - userId: {}", userId);
    }

    /**
     * Refresh Token 조회
     *
     * @param userId 사용자 ID
     * @return Refresh Token (UUID)
     */
    public String get(Long userId) {
        String key = KEY_PREFIX + userId;
        return refreshTokenRedisTemplate.opsForValue().get(key);
    }

    /**
     * Refresh Token 검증
     *
     * @param userId       사용자 ID
     * @param refreshToken 검증할 Refresh Token
     * @return 유효하면 true
     */
    public boolean validate(Long userId, String refreshToken) {
        String savedToken = get(userId);
        return savedToken != null && savedToken.equals(refreshToken);
    }

    /**
     * Refresh Token 삭제 (로그아웃)
     *
     * @param userId 사용자 ID
     */
    public void delete(Long userId) {
        String key = KEY_PREFIX + userId;
        refreshTokenRedisTemplate.delete(key);
        log.info("Refresh Token deleted - userId: {}", userId);
    }

    /**
     * Refresh Token 존재 여부 확인
     *
     * @param userId 사용자 ID
     * @return 존재하면 true
     */
    public boolean exists(Long userId) {
        String key = KEY_PREFIX + userId;
        return refreshTokenRedisTemplate.hasKey(key);
    }
}

