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
    public String fetchConsoleLog(String jobName, String buildNumber) {

        // 1. Jenkins Basic Authentication 헤더 생성
        // 인증 문자열을 "username:password"로 구성하고 Base64로 인코딩합니다.
        String auth = jenkinsUsername + ":" + jenkinsPassword;
        String encodedAuth = Base64.getEncoder()
                .encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        String authHeader = "Basic " + encodedAuth;

        // 2. HTTP Headers 및 Entity 설정 (인증 헤더 포함)
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, authHeader);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        // 3. Jenkins Console Log API 엔드포인트 전체 URL 구성
        // 예: http://<jenkins-url>/job/jobName/buildNumber/logText/progressiveText?start=0
        String logApiUrl = String.format("%s/job/%s/%s/logText/progressiveText?start=0", jenkinsUrl,
                jobName, buildNumber);

        try {
            // 4. RestTemplate을 사용하여 API 호출 (GET 요청)
            // exchange 메서드를 사용하여 헤더(인증)를 포함한 요청을 보냅니다.
            ResponseEntity<String> response = restTemplate.exchange(
                    logApiUrl,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            // 5. 응답 확인 및 본문 반환
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Jenkins API: 콘솔 로그 (Status: {})", response.getStatusCode());
                return response.getBody();
            } else {
                // RestTemplate은 4xx/5xx 시 예외를 던지므로 이 경로는 주로 200번대가 아닌 300번대 리다이렉션 등의 경우에만 도달합니다.
                log.error("Jenkins API 호출은 성공했으나 예상치 못한 상태 코드: {}", response.getStatusCode());
                return "ERROR: Unexpected status code (" + response.getStatusCode() + ")";
            }

        } catch (HttpClientErrorException e) {
            // 4xx (클라이언트 오류: 401 Unauthorized, 404 Not Found 등) 또는 5xx 오류 처리
            log.error("Jenkins API 응답 오류 (HTTP Status: {}): {}", e.getStatusCode(),
                    e.getResponseBodyAsString());
            throw new RuntimeException(e.getResponseBodyAsString());
        } catch (Exception e) {
            // 기타 IO 오류, 연결 문제 등 처리
            log.error("콘솔 로그 가져오기 실패: {}", e.getMessage(), e);
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * 3 & 4번 로직: Jenkins 콘솔 로그를 가져오고 DeploymentLog를 DB에 저장합니다. 이 메소드는 `@Async`로 인해 별도의 스레드에서 실행되어
     * 메인 웹훅 응답을 지연시키지 않습니다.
     */
    @Async("webhookTaskExecutor")
    @Transactional
    public void fetchAndSaveLogAsync(JenkinsWebhooksResponseDto jenkinsData, long deploymentId) {

        try {
            // 1) Jenkins 콘솔 로그 조회
            String consoleLog = this.fetchConsoleLog(
                    jenkinsData.jobName(),
                    jenkinsData.buildNumber()
            );

            // 2) 참조 Deployment 조회
            Deployment deployment = deploymentRepository.findByIdAndIsDeletedFalse(deploymentId)
                    .orElseThrow(() -> new NotFoundException(
                            DeploymentExceptionType.DEPLOYMENT_NOT_FOUND)
                    );

            // 3) duration(문자열) → 초(Long) 변환
            long durationSeconds = DurationParser.toSeconds(jenkinsData.duration());

            // 4) 시간 파싱
            //  - 주어진 문자열은 실제로 KST이지만 "Z"가 붙어 있음
            //  - "Z"를 오프셋으로 해석하지 않고 문자 그대로 취급해 LocalDateTime으로 파싱
            LocalDateTime startedAt = IsoLocalDateTimeParser.parseKstLikeUtc(
                    jenkinsData.startTime());
            LocalDateTime endedAt = IsoLocalDateTimeParser.parseKstLikeUtc(jenkinsData.endTime());

            // 빌드 실행 객체 생성
            BuildRun buildRun = BuildRun.builder()
                    .deployment(deployment)
                    .jenkinsJobName(jenkinsData.jobName())
                    .buildNumber(Long.parseLong(jenkinsData.buildNumber()))
                    .log(consoleLog)
                    .duration(durationSeconds)
                    .startedAt(startedAt)
                    .endedAt(endedAt)
                    .build();

            BuildRun savedBuildRun = buildRunRepository.save(buildRun);

            // 콘솔 로그 파싱 → StageRun 생성
            List<StageBlock> blocks = JenkinsConsoleLogParser.parse(consoleLog);

            List<StageRun> stages = blocks.stream()
                    .map(b -> StageRun.builder()
                            .buildRun(savedBuildRun)
                            .stageName(b.name())
                            .isSuccess(b.success())
                            .orderIndex((long) b.orderIndex())
                            .log(b.log())
                            .problemSummary(null)   // 실패시 LLM분석으로 채움
                            .problemSolution(null)  // 실패시 LLM분석으로 채움
                            .build())
                    .toList();

            log.info("stage 파싱 완료 {}", blocks);
            // 같은 BuildRun에 대한 StageRun이 이미 있다면 정리
            stageRunRepository.deleteAllByBuildRunId(savedBuildRun);
            stageRunRepository.saveAll(stages);
            log.info("[StageRun] {}#{} => {} stages savedBuildRun.", jenkinsData.jobName(),
                    jenkinsData.buildNumber(), stages.size());

            // 빌드에서 실패한 단계 조회
            List<StageRun> failedStageList = stages.stream()
                    .filter((stageRun -> !stageRun.getIsSuccess()))
                    .toList();

            // 실패한 단계 Gemini api로 문제점 요약 및 해결책 제시
            stageAnalysisService.analyzeFailedStages(failedStageList);
            

        } catch (RuntimeException e) {
            log.error("[Async Failure] Deployment ID {} 로그 처리 중 오류 발생: {}", deploymentId,
                    e.getMessage(), e);
            throw new RuntimeException(e.getMessage());
        }
    }

}