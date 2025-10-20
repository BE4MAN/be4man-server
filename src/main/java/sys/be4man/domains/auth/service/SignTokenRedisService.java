package sys.be4man.domains.auth.service;

import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import sys.be4man.domains.auth.dto.GitHubTempInfo;

/**
 * GitHub OAuth 성공 시 임시 정보를 Redis에 저장/조회/삭제하는 서비스 Key: "github:temp:{githubId}" Value:
 * GitHubTempInfo (Serializable) TTL: 5분 (SignToken과 동일)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SignTokenRedisService {

    private final RedisTemplate<String, GitHubTempInfo> signTokenRedisTemplate;

    private static final String KEY_PREFIX = "sign:";
    private static final Duration TTL = Duration.ofMinutes(5); // 5분

    /**
     * GitHub 임시 정보 저장
     */
    public void save(Long githubId, GitHubTempInfo info) {
        String key = KEY_PREFIX + githubId;
        signTokenRedisTemplate.opsForValue().set(key, info, TTL);
        log.info("Sign Token saved - githubId: {}", githubId);
    }

    /**
     * GitHub 임시 정보 조회 및 삭제 (1회용)
     */
    public Optional<GitHubTempInfo> getAndDelete(Long githubId) {
        String key = KEY_PREFIX + githubId;
        GitHubTempInfo info = signTokenRedisTemplate.opsForValue().get(key);
        if (info != null) {
            signTokenRedisTemplate.delete(key);
            log.info("Sign Token retrieved and deleted - githubId: {}", githubId);
        }
        return Optional.ofNullable(info);
    }
}

