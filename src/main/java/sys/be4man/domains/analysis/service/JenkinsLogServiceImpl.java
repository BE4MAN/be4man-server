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
        Long deploymentId = jenkinsData.deploymentId();
        try {
            // (A) progressive 방식으로 콘솔 로그 전체 수집
            String fullLog = fetchConsoleLog(jenkinsData.jobName(), jenkinsData.buildNumber());

            LocalDateTime startedAt = parseIsoOrNull(jenkinsData.startTime()); // 'yyyy-MM-ddTHH:mm:ss.SSSZ' 형태 가정(UTC Z)
            LocalDateTime endedAt   = parseIsoOrNull(jenkinsData.endTime());
            Long durSec             = parseDurationSeconds(jenkinsData.duration()); // "1 min 58 sec" → 118

            if (startedAt == null && endedAt != null && durSec != null) {
                startedAt = endedAt.minusSeconds(durSec);
            } else if (endedAt == null && startedAt != null && durSec != null) {
                endedAt = startedAt.plusSeconds(durSec);
            }

            if (startedAt == null || endedAt == null) {
                log.warn("[Skip Persist] started_at/ended_at 확정 불가 → 저장하지 않음. " +
                                "(deploymentId={}, startTime={}, endTime={}, duration={})",
                        deploymentId, jenkinsData.startTime(), jenkinsData.endTime(), jenkinsData.duration());
                return;
            }


            var deployment = deploymentRepository.findByIdAndIsDeletedFalse(deploymentId)
                    .orElseThrow(() -> new NotFoundException(DeploymentExceptionType.DEPLOYMENT_NOT_FOUND));

            long durationSeconds = durSec != null ? durSec : java.time.Duration.between(startedAt, endedAt).getSeconds();

            // 콘솔 로그를 스테이지 단위로 파싱 (기존 JenkinsConsoleParser 등 사용)
            List<StageBlock> stages = JenkinsConsoleLogParser.parse(fullLog);

            // BuildRun 저장
            BuildRun buildRun = BuildRun.builder()
                    .deployment(deployment)
                    .buildNumber(Long.parseLong(jenkinsData.buildNumber()))
                    .jenkinsJobName(jenkinsData.jobName())
                    .duration(DurationParser.toSeconds(jenkinsData.duration()))
                    .startedAt(startedAt)
                    .endedAt(endedAt)
                    .log(fullLog)
                    .build();
            buildRunRepository.save(buildRun);

            // StageRun 저장
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

            // 실패 스테이지만 따로 분석 (기존 로직 유지)
            var failedStages = stageEntities.stream().filter(sr -> !sr.getIsSuccess()).toList();
            stageAnalysisService.analyzeFailedStages(failedStages);

            log.info("[Persist OK] BuildRun/StageRun saved. deploymentId={}, build={}, startedAt={}, endedAt={}",
                    deploymentId, jenkinsData.buildNumber(), startedAt, endedAt);

        } catch (RuntimeException e) {
            log.error("[Async Failure] Deployment ID {} 로그 처리 중 오류 발생: {}", deploymentId, e.getMessage(), e);
            throw e;
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