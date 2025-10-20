package sys.be4man.domains.auth.service;

import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * OAuth Temporary Code Redis 관리 서비스 GitHub OAuth 성공 후 임시 코드를 통해 토큰을 안전하게 전달하기 위한 서비스
 */
@Service
@RequiredArgsConstructor
public class OAuthCodeRedisService {

    private final RedisTemplate<String, String> refreshTokenRedisTemplate;

    private static final String KEY_PREFIX = "oauth:code:";
    private static final Duration TTL = Duration.ofMinutes(5);

    /**
     * Temporary Code 저장
     *
     * @param code      임시 코드 (UUID)
     * @param accountId 계정 ID
     */
    public void save(String code, Long accountId) {
        String key = KEY_PREFIX + code;
        refreshTokenRedisTemplate.opsForValue().set(
                key,
                String.valueOf(accountId),
                TTL
        );
    }

    /**
     * Temporary Code로 계정 ID 조회 및 삭제 (1회용)
     *
     * @param code 임시 코드
     * @return 계정 ID (존재하지 않으면 empty)
     */
    public Optional<Long> getAndDelete(String code) {
        String key = KEY_PREFIX + code;
        String accountIdStr = refreshTokenRedisTemplate.opsForValue().get(key);

        if (accountIdStr != null) {
            refreshTokenRedisTemplate.delete(key);
            return Optional.of(Long.valueOf(accountIdStr));
        }

        return Optional.empty();
    }
}

