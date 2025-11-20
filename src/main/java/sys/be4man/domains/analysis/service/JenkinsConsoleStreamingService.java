package sys.be4man.domains.analysis.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
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
        startStreamingIfNeeded(session);

        return emitter;
    }

    private synchronized void startStreamingIfNeeded(BuildSession session) {
        String cacheKey = key(session.getDeploymentId(), session.getBuildNumber());
        if (Boolean.TRUE.equals(streamingInProgress.get(cacheKey))) {
            return; // 이미 스트리밍 중
        }
        streamingInProgress.put(cacheKey, true);
        startStreaming(session);
    }

    @Async("webhookTaskExecutor")
    public void startStreaming(BuildSession session) {
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
                if (!emitterRegistry.hasEmitters(cacheKey)) {
                    log.info("[JenkinsStreaming] no subscribers, stop depId={}, build={}",
                            deploymentId, buildNumber);
                    break;
                }

                var chunk = progressiveLogClient.fetchChunk(jobName, buildNumber, start);

                String cleaned = AnsiAndHiddenCleaner.clean(chunk.text());
                if (!cleaned.isEmpty()) {
                    buffer.append(cleaned, chunk.nextStart());
                    emitterRegistry.sendLog(cacheKey, cleaned);
                }

                start = chunk.nextStart();

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
        } finally {
            streamingInProgress.remove(cacheKey);
        }
    }
}
