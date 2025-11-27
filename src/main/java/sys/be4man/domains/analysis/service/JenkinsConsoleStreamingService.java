// 작성자 : 조윤상
package sys.be4man.domains.analysis.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import sys.be4man.domains.analysis.repository.BuildSessionRegistry;
import sys.be4man.domains.analysis.repository.BuildSessionRegistry.BuildSession;
import sys.be4man.domains.analysis.repository.JenkinsLogCache;
import sys.be4man.domains.analysis.repository.JenkinsLogCache.LogBuffer;
import sys.be4man.domains.analysis.repository.JenkinsLogEmitterRegistry;
import sys.be4man.domains.analysis.util.AnsiAndHiddenCleaner;

@Slf4j
@Service
@RequiredArgsConstructor
public class JenkinsConsoleStreamingService {

    private final BuildSessionRegistry buildSessionRegistry;
    private final JenkinsLogCache logCache;
    private final JenkinsLogEmitterRegistry emitterRegistry;
    private final JenkinsProgressiveLogClient progressiveLogClient;

    // 🔹 webhookTaskExecutor 재사용 (이미 Async 설정에 있을 거라 가정)
    @Qualifier("webhookTaskExecutor")
    private final Executor webhookTaskExecutor;

    // 내부적으로는 "deploymentId#buildNumber" 기준으로 스트리밍 상태 관리
    private final Map<String, Boolean> streamingInProgress = new ConcurrentHashMap<>();

    private String key(Long deploymentId, int buildNumber) {
        return deploymentId + "#" + buildNumber;
    }

    /** 클라이언트는 deploymentId만 넘김 */
    public SseEmitter subscribe(Long deploymentId) {
        // 1) 세션에서 jobName + buildNumber 조회
        BuildSession session = buildSessionRegistry.getByDeploymentId(deploymentId);
        if (session == null) {
            throw new IllegalStateException(
                    "활성화된 빌드 세션이 없습니다. deploymentId=" + deploymentId
            );
        }

        int buildNumber = session.getBuildNumber();
        String cacheKey = key(deploymentId, buildNumber);

        SseEmitter emitter = emitterRegistry.addEmitter(cacheKey);

        // 2) 기존 로그 있으면 먼저 전송
        LogBuffer buffer = logCache.get(cacheKey);
        if (buffer != null) {
            String history = buffer.getContent();
            if (!history.isEmpty()) {
                try {
                    emitter.send(SseEmitter.event()
                            .name("log")
                            .data(history));
                } catch (Exception e) {
                    emitter.complete();
                    emitterRegistry.removeEmitter(cacheKey, emitter);
                    return emitter;
                }
            }
        }

        // 3) connected 이벤트 (선택)
        try {
            emitter.send(SseEmitter.event().name("connected").data("ok"));
        } catch (Exception ignored) {}

        // 4) 스트리밍 루프 시작 (이미 돌고 있으면 skip)
        startStreamingIfNeeded(session);   // ⚠️ 여기서 이제 비동기 실행만 트리거

        return emitter;  // 🔹 컨트롤러는 여기까지 실행되고 바로 리턴해야 함
    }

    private void startStreamingIfNeeded(BuildSession session) {
        String cacheKey = key(session.getDeploymentId(), session.getBuildNumber());
        // 이미 돌고 있으면 패스
        if (Boolean.TRUE.equals(streamingInProgress.putIfAbsent(cacheKey, true))) {
            return;
        }

        // 🔹 여기서부터는 별도 쓰레드에서 while 루프를 돌린다
        webhookTaskExecutor.execute(() -> {
            try {
                doStreaming(session);
            } finally {
                streamingInProgress.remove(cacheKey);
            }
        });
    }

    // ⛔ @Async 제거!!
    private void doStreaming(BuildSession session) {
        Long deploymentId = session.getDeploymentId();
        String jobName    = session.getJobName();
        int buildNumber   = session.getBuildNumber();

        String cacheKey = key(deploymentId, buildNumber);
        LogBuffer buffer = logCache.getOrCreate(cacheKey);

        int start = buffer.getLastOffset();
        log.info("[JenkinsStreaming] start depId={}, job={}, build={}, offset={}",
                deploymentId, jobName, buildNumber, start);

        try {
            while (true) {
                // 1) 구독자가 한 명도 없으면 중단
                if (!emitterRegistry.hasEmitters(cacheKey)) {
                    log.info("[JenkinsStreaming] no subscribers, stop depId={}, build={}",
                            deploymentId, buildNumber);
                    break;
                }

                // 2) progressiveText에서 한 번씩 chunk 가져오기
                var chunk = progressiveLogClient.fetchChunk(jobName, buildNumber, start);

                String cleaned = AnsiAndHiddenCleaner.clean(chunk.text());
                if (!cleaned.isEmpty()) {
                    buffer.append(cleaned, chunk.nextStart());
                    emitterRegistry.sendLog(cacheKey, cleaned);
                    log.info("[JenkinsStreaming] chunk depId={}, build={}, len={}, nextStart={}, hasMore={}",
                            deploymentId, buildNumber, cleaned.length(), chunk.nextStart(), chunk.hasMore());
                }

                start = chunk.nextStart();

                // 3) 더 이상 로그가 없으면 (hasMore=false)
                if (!chunk.hasMore()) {
                    buffer.markCompleted();
                    buildSessionRegistry.markCompleted(deploymentId);

                    emitterRegistry.sendComplete(cacheKey, "UNKNOWN");
                    log.info("[JenkinsStreaming] finished depId={}, build={}",
                            deploymentId, buildNumber);
                    break;
                }

                Thread.sleep(1000L);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("[JenkinsStreaming] interrupted depId={}, build={}",
                    deploymentId, buildNumber);
        } catch (Exception e) {
            log.error("[JenkinsStreaming] error depId={}, build={}, ex={}",
                    deploymentId, buildNumber, e.getMessage(), e);
        }
    }
}
