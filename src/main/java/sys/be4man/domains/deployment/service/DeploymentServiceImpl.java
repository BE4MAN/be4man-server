// 작성자 : 이원석
package sys.be4man.domains.deployment.service;

import java.time.Duration;
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
import sys.be4man.domains.analysis.dto.response.DeploymentStageAndStatusResponseDto;
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
import sys.be4man.global.exception.NotFoundException;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class DeploymentServiceImpl implements DeploymentService {

    private final DeploymentRepository deploymentRepository;
    private final ProjectRepository projectRepository;
    private final AccountRepository accountRepository;
    private final PullRequestRepository pullRequestRepository;

    private static final DateTimeFormatter YMDHM = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final Pattern RANGE = Pattern.compile(
            "(20\\d{2}-\\d{2}-\\d{2})\\s+(\\d{2}:\\d{2})\\s*[~\\-–—]\\s*(20\\d{2}-\\d{2}-\\d{2})\\s+(\\d{2}:\\d{2})"
    );

    @Override
    public DeploymentResponse createDeployment(DeploymentCreateRequest request) {
        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new IllegalArgumentException("프로젝트를 찾을 수 없습니다."));
        Account issuer = accountRepository.findById(request.getIssuerId())
                .orElseThrow(() -> new IllegalArgumentException("배포 신청자 계정을 찾을 수 없습니다."));
        PullRequest pr = pullRequestRepository.findById(request.getPullRequestId())
                .orElseThrow(() -> new IllegalArgumentException("PR 정보를 찾을 수 없습니다."));

        Schedule s = parseSchedule(request.getContent());

        Deployment deployment = Deployment.builder()
                .project(project)
                .issuer(issuer)
                .pullRequest(pr)
                .title(request.getTitle())
                .stage(DeploymentStage.valueOf(request.getStage()))
                .status(DeploymentStatus.valueOf(
                        request.getStatus() != null ? request.getStatus() : "PENDING"))
                .isDeployed(null)
                .expectedDuration(String.valueOf(s.expectedMinutes))
                .version(request.getVersion())
                .content(request.getContent())
                .scheduledAt(s.start != null ? s.start : LocalDateTime.now())
                .scheduledToEndedAt(s.end)
                .build();

        deploymentRepository.save(deployment);

        log.info(
                "📝 Deployment created id={}, scheduledAt={}, scheduledToEndedAt={}, expectedMinutes={}",
                deployment.getId(), deployment.getScheduledAt(), deployment.getScheduledToEndedAt(),
                s.expectedMinutes);

        return toDto(deployment);
    }

    public String buildWebhookUrl(Project project) {
        if (project == null || project.getJenkinsIp() == null) {
            return null;
        }
        return project.getJenkinsIp() + "/job/deploy/build?token=DEPLOY_TOKEN";
    }

    private static class Schedule {

        final LocalDateTime start;
        final LocalDateTime end;
        final long expectedMinutes;

        Schedule(LocalDateTime s, LocalDateTime e) {
            this.start = s;
            this.end = e;
            this.expectedMinutes = (s != null && e != null && !e.isBefore(s))
                    ? Duration.between(s, e).toMinutes()
                    : 0L;
        }
    }

    private Schedule parseSchedule(String content) {
        if (content == null || content.isBlank()) {
            return new Schedule(null, null);
        }

        String text = content
                .replaceAll("(?is)<style[^>]*>.*?</style>", " ")
                .replaceAll("(?is)<script[^>]*>.*?</script>", " ")
                .replaceAll("(?is)<[^>]+>", " ")
                .replace("&nbsp;", " ")
                .replace("&ensp;", " ")
                .replace("&emsp;", " ")
                .replace("&ndash;", "-")
                .replace("&minus;", "-")
                .replace("&mdash;", "-")
                .trim()
                .replaceAll("\\s+", " ");

        Matcher rm = RANGE.matcher(text);
        if (rm.find()) {
            try {
                LocalDateTime s = LocalDateTime.parse(rm.group(1) + " " + rm.group(2), YMDHM);
                LocalDateTime e = LocalDateTime.parse(rm.group(3) + " " + rm.group(4), YMDHM);
                if (!e.isBefore(s)) {
                    return new Schedule(s, e);
                }
            } catch (Exception ignore) {
            }
        }

        try {
            Matcher sm = Pattern.compile("(20\\d{2}-\\d{2}-\\d{2}\\s\\d{2}:\\d{2})").matcher(text);
            if (sm.find()) {
                LocalDateTime s = LocalDateTime.parse(sm.group(1), YMDHM);
                return new Schedule(s, null);
            }
        } catch (Exception ignore) {
        }

        return new Schedule(null, null);
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

    @Transactional
    public void flipStageToDeploymentIfDue(Long deploymentId) {
        Deployment d = deploymentRepository.findByIdForUpdate(deploymentId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Deployment not found. id=" + deploymentId));

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime when = d.getScheduledAt();

        if (when == null || when.isAfter(now)) {
            return;
        }

        if (d.getStage() != DeploymentStage.DEPLOYMENT) {
            d.updateStage(DeploymentStage.DEPLOYMENT);
            d.updateStatus(DeploymentStatus.IN_PROGRESS);
        }
    }

    @Override
    public DeploymentStageAndStatusResponseDto getBuildStageAndStatus(Long deploymentId) {
        Deployment deployment = deploymentRepository.findByIdAndIsDeletedFalse(deploymentId)
                .orElseThrow(
                        NotFoundException::new);

        return DeploymentStageAndStatusResponseDto.builder().stage(deployment.getStage())
                .status(deployment.getStatus())
                .deploymentId(deployment.getId())
                .build();
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
