package sys.be4man.domains.taskmanagement.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sys.be4man.domains.analysis.model.entity.BuildRun;
import sys.be4man.domains.analysis.repository.BuildRunRepository;
import sys.be4man.domains.approval.model.entity.Approval;
import sys.be4man.domains.approval.model.type.ApprovalLineType;
import sys.be4man.domains.approval.model.type.ApprovalType;
import sys.be4man.domains.approval.repository.ApprovalRepository;
import sys.be4man.domains.deployment.model.entity.Deployment;
import sys.be4man.domains.deployment.model.type.DeploymentStage;
import sys.be4man.domains.deployment.model.type.DeploymentStatus;
import sys.be4man.domains.taskmanagement.dto.*;
import sys.be4man.domains.taskmanagement.repository.TaskManagementRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskManagementService {

    private final TaskManagementRepository taskManagementRepository;
    private final ApprovalRepository approvalRepository;
    private final BuildRunRepository buildRunRepository;
    private final TaskDetailTimelineService taskDetailTimelineService;
    private final TaskDetailReportService taskDetailReportService;

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");

    /**
     * 작업 관리 목록 조회 - Deployment만 조회
     */
    public Page<TaskManagementResponseDto> getTaskList(
            TaskManagementSearchDto searchDto,
            int page,
            int size
    ) {
        log.debug("작업 관리 목록 조회 - searchDto: {}, page: {}, size: {}", searchDto, page, size);

        if (searchDto == null) {
            searchDto = TaskManagementSearchDto.builder()
                    .sortBy("최신순")
                    .build();
        }

        Pageable pageable = PageRequest.of(page, size);

        Page<Deployment> deployments = taskManagementRepository
                .findTasksBySearchConditions(searchDto, pageable);

        log.debug("조회된 Deployment 수: {}", deployments.getTotalElements());

        return deployments.map(TaskManagementResponseDto::new);
    }

    /**
     * 특정 작업 상세 조회 (기본)
     */
    public TaskManagementResponseDto getTaskDetail(Long taskId) {
        log.debug("작업 상세 조회 - taskId: {}", taskId);

        Deployment deployment = taskManagementRepository.findById(taskId)
                .orElseThrow(() -> {
                    log.warn("작업을 찾을 수 없음 - taskId: {}", taskId);
                    return new IllegalArgumentException("작업을 찾을 수 없습니다. ID: " + taskId);
                });

        if (deployment.getIsDeleted()) {
            log.warn("삭제된 작업 조회 시도 - taskId: {}", taskId);
            throw new IllegalArgumentException("삭제된 작업입니다. ID: " + taskId);
        }

        return new TaskManagementResponseDto(deployment);
    }

    /**
     * 작업 전체 상세 조회
     */
    public TaskDetailResponseDto getTaskDetailFull(Long deploymentId) {
        log.debug("작업 전체 상세 조회 - deploymentId: {}", deploymentId);

        // 1. Deployment 조회
        Deployment deployment = taskManagementRepository.findById(deploymentId)
                .orElseThrow(() -> new IllegalArgumentException("작업을 찾을 수 없습니다. ID: " + deploymentId));

        if (deployment.getIsDeleted()) {
            throw new IllegalArgumentException("삭제된 작업입니다.");
        }

        log.debug("Deployment 조회 완료 - deploymentId: {}, stage: {}, status: {}",
                deployment.getId(), deployment.getStage(), deployment.getStatus());

        // ✅ 2. 계획서와 배포 연결 (pull_request_id 기준)
        Deployment planDeployment = deployment;
        Deployment deploymentTask = null;

        Long pullRequestId = deployment.getPullRequest() != null ? deployment.getPullRequest().getId() : null;

        if (pullRequestId == null) {
            log.warn("PullRequest가 없는 Deployment - deploymentId: {}", deploymentId);
        } else {
            if (deployment.getStage() == DeploymentStage.PLAN) {
                // 계획서 조회 시 → 같은 PR의 배포 작업 찾기
                planDeployment = deployment;

                deploymentTask = taskManagementRepository.findAll().stream()
                        .filter(d -> !d.getIsDeleted())
                        .filter(d -> d.getStage() == DeploymentStage.DEPLOYMENT ||
                                   d.getStage() == DeploymentStage.RETRY ||
                                   d.getStage() == DeploymentStage.ROLLBACK)
                        .filter(d -> d.getPullRequest() != null && d.getPullRequest().getId().equals(pullRequestId))
                        .min((d1, d2) -> d1.getCreatedAt().compareTo(d2.getCreatedAt()))
                        .orElse(null);

                if (deploymentTask != null) {
                    log.debug("✅ 계획서 → 배포 작업 찾기 완료 - planId: {}, deploymentId: {}, pullRequestId: {}",
                            planDeployment.getId(), deploymentTask.getId(), pullRequestId);
                }

            } else if (deployment.getStage() == DeploymentStage.DEPLOYMENT ||
                       deployment.getStage() == DeploymentStage.RETRY ||
                       deployment.getStage() == DeploymentStage.ROLLBACK) {
                // 배포 조회 시 → 같은 PR의 계획서 찾기
                deploymentTask = deployment;

                planDeployment = taskManagementRepository.findAll().stream()
                        .filter(d -> !d.getIsDeleted())
                        .filter(d -> d.getStage() == DeploymentStage.PLAN)
                        .filter(d -> d.getPullRequest() != null && d.getPullRequest().getId().equals(pullRequestId))
                        .max((d1, d2) -> d1.getCreatedAt().compareTo(d2.getCreatedAt()))
                        .orElse(deployment);

                log.debug("✅ 배포 → 계획서 찾기 완료 - deploymentId: {}, planId: {}, pullRequestId: {}",
                        deploymentTask.getId(), planDeployment.getId(), pullRequestId);
            }
        }

        // 3. Approval 조회 (항상 계획서 기준)
        List<Approval> planApprovals = approvalRepository
                .findByDeploymentIdAndTypeOrderByIdAsc(planDeployment.getId(), ApprovalType.PLAN);

        // 결과보고는 원본 deployment 기준
        List<Approval> reportApprovals = approvalRepository
                .findByDeploymentIdAndTypeOrderByIdAsc(deployment.getId(), ApprovalType.REPORT);

        log.debug("Approval 조회 완료 - planDeploymentId: {}, planApprovals: {}, reportApprovals: {}",
                planDeployment.getId(), planApprovals.size(), reportApprovals.size());

        // 4. BuildRun 조회 (배포 작업이 있으면 그것 기준, 없으면 현재 deployment)
        Long buildRunDeploymentId = deploymentTask != null ? deploymentTask.getId() : deployment.getId();
        Optional<BuildRun> buildRunOpt = buildRunRepository
                .findByDeploymentIdAndIsDeletedFalse(buildRunDeploymentId);

        log.debug("BuildRun 조회 - deploymentId: {}, found: {}", buildRunDeploymentId, buildRunOpt.isPresent());

        // 5. 각 DTO 생성 (배포 작업 정보 우선 사용)
        Deployment displayDeployment = deploymentTask != null ? deploymentTask : deployment;

        List<TimelineStepDto> timeline = taskDetailTimelineService.buildTimeline(
                displayDeployment, planApprovals, reportApprovals, buildRunOpt.orElse(null));

        ApprovalInfoDto planApprovalInfo = buildApprovalInfo(planApprovals, DeploymentStage.PLAN);
        ApprovalInfoDto reportApprovalInfo = buildApprovalInfo(reportApprovals, DeploymentStage.REPORT);
        JenkinsLogDto jenkinsLog = buildRunOpt.map(this::buildJenkinsLog).orElse(null);
        PlanContentDto planContent = buildPlanContent(planDeployment, planApprovals, buildRunOpt.orElse(null));
        ReportContentDto reportContent = taskDetailReportService.buildReportContent(
                deployment, buildRunOpt.orElse(null), reportApprovals);

        // 6. currentStage 결정 (배포 작업 상태 우선)
        String currentStage;
        String currentStatus;
        String initialTab = "plan";

        if (!reportApprovals.isEmpty()) {
            currentStage = "결과보고";
            Approval reportApproval = reportApprovals.get(0);
            currentStatus = getApprovalStatusKorean(reportApproval.getStatus());
            initialTab = "report";
        } else if (deploymentTask != null) {
            // ✅ 배포 작업이 있으면 배포 상태 표시
            if (deploymentTask.getStatus() == DeploymentStatus.IN_PROGRESS) {
                currentStage = "배포";
                currentStatus = "진행중";
                initialTab = "jenkins";
            } else if (deploymentTask.getStatus() == DeploymentStatus.COMPLETED) {
                currentStage = "배포";
                currentStatus = "완료";
                initialTab = "jenkins";
            } else {
                // 배포 대기중
                currentStage = "계획서";
                currentStatus = "승인완료";
                initialTab = "plan";
            }
        } else if (deployment.getStage() == DeploymentStage.DEPLOYMENT ||
                deployment.getStage() == DeploymentStage.RETRY ||
                deployment.getStage() == DeploymentStage.ROLLBACK) {

            if (deployment.getStatus() == DeploymentStatus.APPROVED) {
                currentStage = "계획서";
                currentStatus = "승인완료";
                initialTab = "plan";
            } else {
                currentStage = deployment.getStage().getKoreanName();
                currentStatus = deployment.getStatus() != null ? deployment.getStatus().getKoreanName() : null;
                initialTab = "jenkins";
            }
        } else if (deployment.getStatus() == DeploymentStatus.COMPLETED && deployment.getIsDeployed() != null) {
            currentStage = "배포";
            currentStatus = "완료";
            initialTab = "jenkins";
        } else {
            currentStage = deployment.getStage() != null ? deployment.getStage().getKoreanName() : null;
            currentStatus = deployment.getStatus() != null ? deployment.getStatus().getKoreanName() : null;
            initialTab = "plan";
        }

        // 7. 전체 DTO 조합
        return TaskDetailResponseDto.builder()
                .taskId(deployment.getId())
                .serviceName(deployment.getProject() != null ? deployment.getProject().getName() : null)
                .taskTitle(deployment.getTitle())
                .currentStage(currentStage)
                .currentStatus(currentStatus)
                .initialTab(initialTab)
                .timeline(timeline)
                .planApproval(planApprovalInfo)
                .reportApproval(reportApprovalInfo)
                .jenkinsLog(jenkinsLog)
                .planContent(planContent)
                .reportContent(reportContent)
                .build();
    }

    /**
     * 계획서 내용 생성
     */
    private PlanContentDto buildPlanContent(Deployment deployment, List<Approval> planApprovals,
            BuildRun buildRun) {
        String content = deployment.getContent();
        if (!planApprovals.isEmpty()) {
            Approval planApproval = planApprovals.get(0);
            content = planApproval.getContent();
        }

        return PlanContentDto.builder()
                .drafter(deployment.getIssuer() != null ? deployment.getIssuer().getName() : null)
                .department(deployment.getIssuer() != null && deployment.getIssuer().getDepartment() != null ?
                        deployment.getIssuer().getDepartment().getKoreanName() : null)
                .createdAt(formatDateTime(deployment.getCreatedAt()))
                .serviceName(deployment.getProject() != null ? deployment.getProject().getName() : null)
                .taskTitle(deployment.getTitle())
                .scheduledAt(formatDateTime(deployment.getScheduledAt()))
                .scheduledToEndedAt(formatDateTime(deployment.getScheduledToEndedAt()))
                .riskDescription(null)
                .expectedDuration(deployment.getExpectedDuration())
                .version(deployment.getVersion())
                .strategy(null)
                .content(content)
                .build();
    }

    /**
     * 승인 정보 DTO 생성
     */
    private ApprovalInfoDto buildApprovalInfo(List<Approval> approvals, DeploymentStage stage) {
        if (approvals.isEmpty()) {
            return ApprovalInfoDto.builder()
                    .approvalId(null)
                    .approvalStage(stage.getKoreanName())
                    .totalApprovers(0)
                    .current_approver_account_id(null)
                    .approvers(new ArrayList<>())
                    .build();
        }

        Approval approval = approvals.get(0);

        Long currentApproverAccountId = approval.getNextApprover() != null
                ? approval.getNextApprover().getId()
                : null;

        List<ApproverDto> approvers = approval.getApprovalLines().stream()
                .filter(line -> line.getType() != ApprovalLineType.CC)
                .sorted((a, b) -> a.getId().compareTo(b.getId()))
                .map(line -> {
                    String approvalStatus;
                    LocalDateTime processedAt = null;

                    if (line.getIsApproved() == null) {
                        approvalStatus = "대기중";
                    } else if (line.getIsApproved()) {
                        approvalStatus = "승인";
                        processedAt = line.getApprovedAt();
                    } else {
                        approvalStatus = "반려";
                        processedAt = line.getApprovedAt();
                    }

                    return ApproverDto.builder()
                            .approverId(line.getAccount().getId())
                            .approverName(line.getAccount().getName())
                            .approverDepartment(line.getAccount().getDepartment() != null ?
                                    line.getAccount().getDepartment().getKoreanName() : null)
                            .current_approver_account_id(line.getAccount().getId())
                            .approvalStatus(approvalStatus)
                            .processedAt(formatDateTime(processedAt))
                            .comment(line.getComment())
                            .isCurrentTurn(currentApproverAccountId != null &&
                                    currentApproverAccountId.equals(line.getAccount().getId()))
                            .build();
                })
                .toList();

        return ApprovalInfoDto.builder()
                .approvalId(approval.getId())  // ✅ approvalId 추가!
                .approvalStage(stage.getKoreanName())
                .totalApprovers(approvers.size())
                .current_approver_account_id(currentApproverAccountId)
                .approvers(approvers)
                .build();
    }

    /**
     * Jenkins 로그 DTO 생성
     */
    private JenkinsLogDto buildJenkinsLog(BuildRun buildRun) {
        String buildStatus;
        if (buildRun.getEndedAt() == null) {
            buildStatus = "IN_PROGRESS";
        } else {
            buildStatus = "SUCCESS";
        }

        return JenkinsLogDto.builder()
                .jenkinsJobName(buildRun.getJenkinsJobName())
                .buildNumber(buildRun.getBuildNumber())
                .buildStatus(buildStatus)
                .startedAt(formatDateTime(buildRun.getStartedAt()))
                .endedAt(formatDateTime(buildRun.getEndedAt()))
                .duration(buildRun.getDuration())
                .durationFormatted(formatDuration(buildRun.getDuration()))
                .log(buildRun.getLog())
                .build();
    }

    private String getApprovalStatusKorean(sys.be4man.domains.approval.model.type.ApprovalStatus status) {
        if (status == null) {
            return null;
        }
        switch (status) {
            case PENDING:
                return "승인대기";
            case APPROVED:
                return "승인완료";
            case REJECTED:
                return "반려";
            case CANCELED:
                return "취소";
            case DRAFT:
                return "임시저장";
            default:
                return status.name();
        }
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATETIME_FORMATTER) : null;
    }

    private String formatDuration(Long durationMs) {
        if (durationMs == null) {
            return null;
        }

        long seconds = durationMs / 1000;
        long minutes = seconds / 60;
        long remainingSeconds = seconds % 60;

        if (minutes > 0) {
            return String.format("%d분 %d초", minutes, remainingSeconds);
        } else {
            return String.format("%d초", remainingSeconds);
        }
    }
}