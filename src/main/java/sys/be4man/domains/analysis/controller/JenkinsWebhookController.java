package sys.be4man.domains.analysis.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sys.be4man.domains.analysis.dto.response.JenkinsWebhooksResponseDto;
import sys.be4man.domains.analysis.service.LogService;
import sys.be4man.domains.analysis.service.WebhookService;
import sys.be4man.domains.deployment.model.type.DeploymentResult;

/**
 * Jenkins Webhook 요청을 처리하는 REST Controller입니다.
 * Jenkinsfile에서 설정된 "https://be4man.store/webhooks/jenkins" 경로에 해당합니다.
 */
@RequestMapping("/webhooks")
@RequiredArgsConstructor
@Tag(name = "젠킨스 웹훅 수신 API", description = "젠킨스 빌드 후 웹훅을 수신하여 배포 작업 상태 업데이트와 배포 작업 로그 저장")
@RestController
public class JenkinsWebhookController {

    // Jenkinsfile에서 설정된 경로: /webhooks/jenkins
    private static final String JENKINS_WEBHOOK_ENDPOINT = "/jenkins";
    private static final Logger log = LoggerFactory.getLogger(JenkinsWebhookController.class);
    private final WebhookService webhookService;
    private final LogService logService;

    @PostMapping(JENKINS_WEBHOOK_ENDPOINT)
    public ResponseEntity<String> handleJenkinsWebhook(@RequestBody JenkinsWebhooksResponseDto jenkinsData) {
        // 1. 수신된 데이터 로깅
        log.info("============== Jenkins Webhook Received ==============");
        // TODO 나중에 deploymentId도 젠킨스에서 실제로 받아오게 해야 함.
        long deploymentId = 1L;
        log.info("Job Name: {}", jenkinsData.jobName());
        log.info("Build Number: {}", jenkinsData.buildNumber());
        log.info("Result: {}", jenkinsData.result());
        log.info("Duration: {}", jenkinsData.duration());
        log.info("Start Time: {}", jenkinsData.startTime());
        log.info("End Time: {}", jenkinsData.endTime());
        log.info("======================================================");

        // 2. Jenkins 서버로부터 받은 status로부터 배포 여부 결정. SUCCESS, UNSTABLE, ABORTED, FAILURE, NOT_BUILT가 있으며 SUCCESS와 UNSTABLE은 배포 성공
        Boolean isDeployed = DeploymentResult.fromJenkinsStatus(jenkinsData.result()).getIsDeployed();
        webhookService.setDeployResult(deploymentId, isDeployed);

        // 빌드 로그 조회와 DeploymentLog 저장은 비동기적으로 실행
        logService.fetchAndSaveLogAsync(jenkinsData, deploymentId);

        // 5. Jenkins에게 성공 응답 반환
        return ResponseEntity.ok("Jenkins webhook data received and processed successfully.");
    }
}