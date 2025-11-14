package sys.be4man.domains.deployment.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sys.be4man.domains.account.model.entity.Account;
import sys.be4man.domains.account.repository.AccountRepository;
import sys.be4man.domains.deployment.dto.request.DeploymentCreateRequest;
import sys.be4man.domains.deployment.dto.response.DeploymentResponse;
import sys.be4man.domains.deployment.model.entity.Deployment;
import sys.be4man.domains.deployment.model.type.DeploymentStage;
import sys.be4man.domains.deployment.model.type.DeploymentStatus;
import sys.be4man.domains.deployment.repository.DeploymentRepository;
import sys.be4man.domains.project.model.entity.Project;
import sys.be4man.domains.project.repository.ProjectRepository;
import sys.be4man.domains.pullrequest.model.entity.PullRequest;
import sys.be4man.domains.pullrequest.repository.PullRequestRepository;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class DeploymentServiceImpl implements DeploymentService {

    private final DeploymentRepository deploymentRepository;
    private final ProjectRepository projectRepository;
    private final AccountRepository accountRepository;
    private final PullRequestRepository pullRequestRepository;
    private final DeploymentScheduler deploymentScheduler;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(
            "yyyy-MM-dd HH:mm");

    @Override
    public DeploymentResponse createDeployment(DeploymentCreateRequest request) {
        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new IllegalArgumentException("프로젝트를 찾을 수 없습니다."));
        Account issuer = accountRepository.findById(request.getIssuerId())
                .orElseThrow(() -> new IllegalArgumentException("배포 신청자 계정을 찾을 수 없습니다."));
        PullRequest pr = pullRequestRepository.findById(request.getPullRequestId())
                .orElseThrow(() -> new IllegalArgumentException("PR 정보를 찾을 수 없습니다."));

        LocalDateTime startTime = extractStartTime(request.getContent());
        LocalDateTime now = LocalDateTime.now();

        Deployment deployment = Deployment.builder()
                .project(project)
                .issuer(issuer)
                .pullRequest(pr)
                .title(request.getTitle())
                .stage(DeploymentStage.valueOf(request.getStage()))
                .status(DeploymentStatus.valueOf(
                        request.getStatus() != null ? request.getStatus() : "PENDING"))
                .isDeployed(false)
                .expectedDuration(
                        request.getExpectedDuration() != null ? request.getExpectedDuration() : "0")
                .version(request.getVersion())
                .content(request.getContent())
                .scheduledAt(startTime != null ? startTime : now)
                .build();

        deploymentRepository.save(deployment);

        try {
            String webhookUrl = project.getJenkinsIp() + "/job/deploy/build?token=DEPLOY_TOKEN";
            LocalDateTime scheduleTime = startTime != null ? startTime : now.plusMinutes(1);
            deploymentScheduler.scheduleDeployment(webhookUrl, scheduleTime);
            log.info("✅ Jenkins webhook scheduled for project {} at {}", project.getName(),
                     scheduleTime);
        } catch (Exception e) {
            log.error("❌ Jenkins webhook scheduling failed: {}", e.getMessage());
        }

        return toDto(deployment);
    }

    private LocalDateTime extractStartTime(String content) {
        try {
            Pattern pattern = Pattern.compile("(\\d{4}-\\d{2}-\\d{2}\\s\\d{2}:\\d{2})");
            Matcher matcher = pattern.matcher(content);
            if (matcher.find()) {
                return LocalDateTime.parse(matcher.group(1), FORMATTER);
            }
        } catch (Exception e) {
            log.warn("⚠️ 배포 시작 시간 파싱 실패: {}", e.getMessage());
        }
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public DeploymentResponse getDeployment(Long id) {
        Deployment deployment = deploymentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("배포 작업을 찾을 수 없습니다."));
        return toDto(deployment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeploymentResponse> getAllDeployments() {
        return deploymentRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private DeploymentResponse toDto(Deployment deployment) {
        return DeploymentResponse.builder()
                .id(deployment.getId())
                .projectId(deployment.getProject().getId())
                .issuerId(deployment.getIssuer().getId())
                .pullRequestId(deployment.getPullRequest().getId())
                .title(deployment.getTitle())
                .stage(deployment.getStage().getKoreanName())
                .status(deployment.getStatus().getKoreanName())
                .isDeployed(deployment.getIsDeployed())
                .scheduledAt(deployment.getScheduledAt())
                .scheduledToEndedAt(deployment.getScheduledToEndedAt())
                .expectedDuration(deployment.getExpectedDuration())
                .version(deployment.getVersion())
                .content(deployment.getContent())
                .createdAt(deployment.getCreatedAt())
                .updatedAt(deployment.getUpdatedAt())
                .build();
    }
}
