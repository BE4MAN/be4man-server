package sys.be4man.domains.analysis.service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import sys.be4man.domains.analysis.dto.response.JenkinsWebhooksResponseDto;
import sys.be4man.domains.analysis.model.entity.BuildRun;
import sys.be4man.domains.analysis.model.entity.StageRun;
import sys.be4man.domains.analysis.repository.BuildRunRepository;
import sys.be4man.domains.analysis.repository.StageRunRepository;
import sys.be4man.domains.analysis.util.DurationParser;
import sys.be4man.domains.analysis.util.IsoLocalDateTimeParser;
import sys.be4man.domains.analysis.util.JenkinsConsoleLogParser;
import sys.be4man.domains.analysis.util.JenkinsConsoleLogParser.StageBlock;
import sys.be4man.domains.deployment.repository.DeploymentRepository;
import sys.be4man.domains.deployment.exception.type.DeploymentExceptionType;
import sys.be4man.domains.deployment.model.entity.Deployment;
import sys.be4man.global.exception.NotFoundException;

@RequiredArgsConstructor
@Service
public class JenkinsLogServiceImpl implements LogService {

    private static final Logger log = LoggerFactory.getLogger(JenkinsLogServiceImpl.class);
    private final RestTemplate restTemplate;
    private final DeploymentRepository deploymentRepository;
    private final BuildRunRepository buildRunRepository;
    private final StageRunRepository stageRunRepository;
    private final StageAnalysisService stageAnalysisService;

    @Value("${jenkins.url}")
    private String jenkinsUrl;

    @Value("${jenkins.username}")
    private String jenkinsUsername;

    @Value("${jenkins.password}")
    private String jenkinsPassword;

    /**
     * Jenkins 서버에서 특정 빌드의 콘솔 로그(TEXT)를 가져온다. * @param jobName Jenkins Job 이름
     *
     * @param buildNumber 빌드 번호
     * @return 콘솔 로그 전체 내용 (String)
     */
    @Override
    public String fetchConsoleLog(String jobName, String buildNumber) {
        try {
            // Basic Auth 헤더
            HttpHeaders headers = new HttpHeaders();
            String auth = jenkinsUsername + ":" + jenkinsPassword;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
            headers.set("Authorization", "Basic " + encodedAuth);

            // progressiveText 엔드포인트
            // 예: http(s)://JENKINS/job/{jobName}/{buildNumber}/logText/progressiveText?start=0
            String baseUrl = jenkinsUrl
                    + "/job/" + jobName
                    + "/" + buildNumber
                    + "/logText/progressiveText";

            int start = 0;
            StringBuilder buf = new StringBuilder(64 * 1024);

            while (true) {
                String url = baseUrl + "?start=" + start;
                ResponseEntity<String> resp = restTemplate.exchange(
                        url, HttpMethod.GET, new HttpEntity<>(headers), String.class);

                String chunk = resp.getBody() != null ? resp.getBody() : "";
                buf.append(chunk);

                // 헤더에서 다음 포인터와 더 받을 데이터가 있는지 확인
                String textSize = resp.getHeaders().getFirst("X-Text-Size");
                String moreData = resp.getHeaders().getFirst("X-More-Data");

                boolean hasMore = "true".equalsIgnoreCase(moreData);
                int nextStart;
                if (textSize != null) {
                    try {
                        nextStart = Integer.parseInt(textSize);
                    } catch (NumberFormatException nfe) {
                        // 안전장치: 못 읽으면 현재 포인터를 chunk 길이만큼 증가
                        nextStart = start + chunk.getBytes(StandardCharsets.UTF_8).length;
                    }
                } else {
                    nextStart = start + chunk.getBytes(StandardCharsets.UTF_8).length;
                }

                if (!hasMore) {
                    break;
                }

                // 다음 루프를 위해 포인터 이동, 너무 빠른 폴링 방지 딜레이
                start = nextStart;
                try { Thread.sleep(250L); } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            log.info("Jenkins API: progressive console log fully fetched ({} bytes)", buf.length());
            return buf.toString();
        } catch (Exception e) {
            log.error("콘솔 로그(progressive) 가져오기 실패: {}", e.getMessage(), e);
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * 3 & 4번 로직: Jenkins 콘솔 로그를 가져오고 DeploymentLog를 DB에 저장합니다. 이 메소드는 `@Async`로 인해 별도의 스레드에서 실행되어
     * 메인 웹훅 응답을 지연시키지 않습니다.
     */
    @Async("webhookTaskExecutor")
    @Transactional
    @Override
    public void fetchAndSaveLogAsync(JenkinsWebhooksResponseDto jenkinsData) {
        final Long deploymentId = jenkinsData.deploymentId();
        final String jobName = jenkinsData.jobName();
        final String buildNumber = jenkinsData.buildNumber();

        try {
            // 1) 우선 웹훅에 실린 값으로 시각 확정 시도
            LocalDateTime startedAt = parseIsoOrNull(jenkinsData.startTime());
            LocalDateTime endedAt   = parseIsoOrNull(jenkinsData.endTime());
            Long durSecFromWebhook  = parseDurationSeconds(jenkinsData.duration()); // "and counting"이면 null

            if (startedAt == null && endedAt != null && durSecFromWebhook != null) {
                startedAt = endedAt.minusSeconds(durSecFromWebhook);
            } else if (endedAt == null && startedAt != null && durSecFromWebhook != null) {
                endedAt = startedAt.plusSeconds(durSecFromWebhook);
            }

            // 2) 여전히 started/ended 확정이 안 되면: Jenkins Build JSON API를 5초마다 폴링
            if (startedAt == null || endedAt == null) {
                final int MAX_MINUTES = 10;       // 최대 대기 10분 (필요시 조정/설정값화)
                final int SLEEP_MS = 5000;        // 5초 간격
                final long deadline = System.currentTimeMillis() + MAX_MINUTES * 60_000L;

                while (System.currentTimeMillis() < deadline) {
                    BuildMeta meta = fetchBuildMeta(jobName, buildNumber);
                    if (meta != null && !meta.building) {
                        // Jenkins API: timestamp(ms) = 시작시각, duration(ms) = 총 소요
                        LocalDateTime start = LocalDateTime.ofInstant(
                                java.time.Instant.ofEpochMilli(meta.timestamp),
                                java.time.ZoneOffset.UTC);
                        LocalDateTime end = start.plusNanos(java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(meta.durationMs));
                        startedAt = (startedAt == null) ? start : startedAt;
                        endedAt   = (endedAt   == null) ? end   : endedAt;
                        log.info("Jenkins build finished via polling. job={}, build={}, startedAt={}, endedAt={}, result={}",
                                jobName, buildNumber, startedAt, endedAt, meta.result);
                        break;
                    }
                    try { Thread.sleep(SLEEP_MS); } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }

            // 3) 그래도 확정 못했으면 (예: 타임아웃) 저장 스킵
            if (startedAt == null || endedAt == null) {
                log.warn("[Skip Persist] started_at/ended_at 미확정 (타임아웃/진행중). depId={}, job={}, build={}, duration='{}'",
                        deploymentId, jobName, buildNumber, jenkinsData.duration());
                return;
            }

            // 4) 이제 최종 로그(완료본) 수집 → 스테이지 파싱
            String fullLog = fetchConsoleLog(jobName, buildNumber); // progressiveText로 완료본 취득
            List<JenkinsConsoleLogParser.StageBlock> stages = JenkinsConsoleLogParser.parse(fullLog);

            // 5) 엔티티 조회
            Deployment deployment = deploymentRepository.findByIdAndIsDeletedFalse(deploymentId)
                    .orElseThrow(() -> new NotFoundException(DeploymentExceptionType.DEPLOYMENT_NOT_FOUND));

            long durationSeconds = java.time.Duration.between(startedAt, endedAt).getSeconds();

            // 6) BuildRun 저장
            BuildRun buildRun = BuildRun.builder()
                    .deployment(deployment)
                    .buildNumber(Long.parseLong(buildNumber))
                    .jenkinsJobName(jobName)
                    .duration((Long)Math.max(0, durationSeconds))
                    .startedAt(startedAt)
                    .endedAt(endedAt)
                    .log(fullLog)
                    .build();
            buildRunRepository.save(buildRun);

            // 7) StageRun 저장
            var stageEntities = stages.stream()
                    .map(s -> StageRun.builder()
                            .buildRun(buildRun)
                            .orderIndex((long) s.orderIndex())
                            .stageName(s.name())
                            .isSuccess(s.success())
                            .log(s.log())
                            .build())
                    .toList();
            stageRunRepository.saveAll(stageEntities);

            // 8) 실패 스테이지 분석
            var failedStages = stageEntities.stream().filter(sr -> !sr.getIsSuccess()).toList();
            stageAnalysisService.analyzeFailedStages(failedStages);

            log.info("[Persist OK] BuildRun/StageRun 저장 완료. depId={}, job={}, build={}, startedAt={}, endedAt={}",
                    deploymentId, jobName, buildNumber, startedAt, endedAt);

        } catch (RuntimeException e) {
            log.error("[Async Failure] depId={}, job={}, build={} 처리 중 오류: {}", deploymentId, jobName, buildNumber, e.getMessage(), e);
            throw e;
        }
    }

    /** Jenkins Build JSON API 조회 (building, timestamp, duration, result) */
    private BuildMeta fetchBuildMeta(String jobName, String buildNumber) {
        try {
            HttpHeaders headers = new HttpHeaders();
            String auth = jenkinsUsername + ":" + jenkinsPassword;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
            headers.set("Authorization", "Basic " + encodedAuth);

            // 예: https://JENKINS/job/{job}/{build}/api/json?tree=building,timestamp,duration,result
            String url = jenkinsUrl + "/job/" + jobName + "/" + buildNumber
                    + "/api/json?tree=building,timestamp,duration,result";

            ResponseEntity<java.util.Map> resp = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), java.util.Map.class);

            java.util.Map<?,?> map = resp.getBody();
            if (map == null) return null;

            boolean building = Boolean.TRUE.equals(map.get("building"));
            Number ts = (Number) map.get("timestamp"); // epoch millis (start)
            Number dur = (Number) map.get("duration"); // millis
            String result = (String) map.get("result"); // SUCCESS/FAILURE/...

            return new BuildMeta(building,
                    ts != null ? ts.longValue() : 0L,
                    dur != null ? dur.longValue() : 0L,
                    result);
        } catch (HttpClientErrorException.NotFound nf) {
            // 빌드 직후 곧바로 조회하면 404일 수 있음 → 폴링 계속
            return null;
        } catch (Exception e) {
            log.warn("fetchBuildMeta 실패: job={}, build={}, err={}", jobName, buildNumber, e.getMessage());
            return null;
        }
    }

    /** 빌드 메타 정보 */
    private static final class BuildMeta {
        final boolean building;
        final long timestamp;   // ms (시작시각)
        final long durationMs;  // ms (총 소요)
        final String result;
        BuildMeta(boolean building, long timestamp, long durationMs, String result) {
            this.building = building;
            this.timestamp = timestamp;
            this.durationMs = durationMs;
            this.result = result;
        }
    }

    /** ====== 유틸 메서드 (클래스 내부 private static) ====== */

    private static LocalDateTime parseIsoOrNull(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            // Jenkinsfile에서 UTC로 'Z' 붙여 보내는 경우를 가정
            // 예: 2025-11-03T06:44:33.123Z
            return java.time.OffsetDateTime.parse(s).toLocalDateTime();
        } catch (Exception e) {
            return null;
        }
    }

    private static Long parseDurationSeconds(String s) {
        if (s == null) return null;
        // "1 min 58 sec" 또는 "22 sec" 등 대응, "and counting"이면 null 리턴
        String lower = s.toLowerCase();
        if (lower.contains("and counting")) return null;
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(?:(\\d+)\\s*min\\s*)?(\\d+)\\s*sec\\b")
                .matcher(lower);
        if (m.find()) {
            long min = m.group(1) != null ? Long.parseLong(m.group(1)) : 0L;
            long sec = Long.parseLong(m.group(2));
            return min * 60 + sec;
        }
        return null;
    }

}