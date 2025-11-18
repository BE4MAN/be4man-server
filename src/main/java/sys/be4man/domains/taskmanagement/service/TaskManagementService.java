package sys.be4man.domains.taskmanagement.service;

import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sys.be4man.domains.analysis.model.entity.BuildRun;
import sys.be4man.domains.analysis.repository.BuildRunRepository;
import sys.be4man.domains.approval.model.entity.Approval;
import sys.be4man.domains.approval.model.type.ApprovalType;
import sys.be4man.domains.approval.repository.ApprovalRepository;
import sys.be4man.domains.deployment.model.entity.Deployment;
import sys.be4man.domains.deployment.model.type.DeploymentStage;
import sys.be4man.domains.deployment.model.type.DeploymentStatus;
import sys.be4man.domains.taskmanagement.dto.*;
import sys.be4man.domains.taskmanagement.repository.TaskManagementRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    private final TaskDetailInfoService taskDetailInfoService;
    private final TaskDetailApprovalService taskDetailApprovalService;

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");

    public Page<TaskManagementResponseDto> getTaskList(TaskManagementSearchDto searchDto, int page, int size) {
        log.debug("작업 관리 목록 조회 - searchDto: {}, page: {}, size: {}", searchDto, page, size);

        if (searchDto == null) {
            searchDto = TaskManagementSearchDto.builder().sortBy("최신순").build();
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<Deployment> deployments = taskManagementRepository.findTasksBySearchConditions(searchDto, pageable);

        log.debug("조회된 Deployment 수: {}", deployments.getTotalElements());

        // ✅ Deployment를 DTO로 변환하면서 필터링
        List<TaskManagementResponseDto> filteredList = deployments.getContent().stream()
                .filter(deployment -> {
                    // ✅ 배포/롤백/재배포가 PENDING 상태면 제외
                    if ((deployment.getStage() == DeploymentStage.DEPLOYMENT ||
                            deployment.getStage() == DeploymentStage.ROLLBACK ||
                            deployment.getStage() == DeploymentStage.RETRY) &&
                            deployment.getStatus() == DeploymentStatus.PENDING) {
                        log.debug("⏭️ PENDING 상태의 배포/롤백/재배포 제외 - id: {}, stage: {}, status: {}",
                                deployment.getId(), deployment.getStage(), deployment.getStatus());
                        return false;  // ✅ 제외
                    }
                    return true;  // ✅ 포함
                })
                .map(deployment -> {
                    // ✅ PLAN Approval 조회
                    List<Approval> planApprovals = approvalRepository.findByDeploymentIdAndTypeAndIsDeletedFalse(
                            deployment.getId(), ApprovalType.PLAN);

                    // ✅ REPORT 단계면 REPORT Approval도 조회
                    List<Approval> reportApprovals = null;
                    if (deployment.getStage() == DeploymentStage.REPORT) {
                        reportApprovals = approvalRepository.findByDeploymentIdAndTypeAndIsDeletedFalse(
                                deployment.getId(), ApprovalType.REPORT);
                    }

                    return new TaskManagementResponseDto(deployment, planApprovals, reportApprovals);
                })
                .collect(Collectors.toList());

        // ✅ PageImpl로 Page 객체 생성
        return new PageImpl<>(filteredList, pageable, deployments.getTotalElements());
    }

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

    public TaskDetailResponseDto getTaskDetailFull(Long deploymentId) {
        log.debug("작업 전체 상세 조회 - deploymentId: {}", deploymentId);

        Deployment deployment = taskManagementRepository.findById(deploymentId)
                .orElseThrow(() -> new IllegalArgumentException("작업을 찾을 수 없습니다. ID: " + deploymentId));

        if (deployment.getIsDeleted()) {
            throw new IllegalArgumentException("삭제된 작업입니다.");
        }

        log.debug("Deployment 조회 완료 - deploymentId: {}, stage: {}, status: {}",
                deployment.getId(), deployment.getStage(), deployment.getStatus());

        Deployment planDeployment = deployment;
        Deployment deploymentTask = null;
        Deployment reportDeployment = null;

        Long pullRequestId = deployment.getPullRequest() != null ? deployment.getPullRequest().getId() : null;

        // ✅ 같은 PR의 모든 deployment 조회
        DeploymentStage maxStage = deployment.getStage();  // 가장 진행된 단계

        if (pullRequestId != null) {
            // ✅ 같은 PR의 모든 deployment 조회하여 가장 진행된 단계 확인
            List<Deployment> relatedDeployments = taskManagementRepository.findAll().stream()
                    .filter(d -> !d.getIsDeleted())
                    .filter(d -> d.getPullRequest() != null && d.getPullRequest().getId().equals(pullRequestId))
                    .collect(Collectors.toList());

            log.debug("같은 PR의 deployment 개수: {}", relatedDeployments.size());

            // 가장 진행된 단계 찾기
            for (Deployment d : relatedDeployments) {
                if (getStageOrder(d.getStage()) > getStageOrder(maxStage)) {
                    maxStage = d.getStage();
                }
            }

            log.debug("가장 진행된 단계: {}", maxStage);

            // 계획서 찾기
            planDeployment = relatedDeployments.stream()
                    .filter(d -> d.getStage() == DeploymentStage.PLAN)
                    .max((d1, d2) -> d1.getCreatedAt().compareTo(d2.getCreatedAt()))
                    .orElse(deployment);

            // 배포 작업 찾기
            deploymentTask = relatedDeployments.stream()
                    .filter(d -> d.getStage() == DeploymentStage.DEPLOYMENT ||
                            d.getStage() == DeploymentStage.RETRY ||
                            d.getStage() == DeploymentStage.ROLLBACK)
                    .min((d1, d2) -> d1.getCreatedAt().compareTo(d2.getCreatedAt()))
                    .orElse(null);

            // 결과보고 찾기
            reportDeployment = relatedDeployments.stream()
                    .filter(d -> d.getStage() == DeploymentStage.REPORT)
                    .max((d1, d2) -> d1.getCreatedAt().compareTo(d2.getCreatedAt()))
                    .orElse(null);

            log.debug("✅ PR 기반 조회 완료 - planId: {}, deploymentId: {}, reportId: {}",
                    planDeployment.getId(),
                    deploymentTask != null ? deploymentTask.getId() : "없음",
                    reportDeployment != null ? reportDeployment.getId() : "없음");
        }

        // ✅ Approval 조회
        List<Approval> planApprovals = approvalRepository.findByDeploymentIdAndTypeAndIsDeletedFalse(
                planDeployment.getId(), ApprovalType.PLAN);

        List<Approval> reportApprovals = reportDeployment != null ?
                approvalRepository.findByDeploymentIdAndTypeAndIsDeletedFalse(
                        reportDeployment.getId(), ApprovalType.REPORT) :
                List.of();

        log.debug("Approval 조회 - planApprovals: {}, reportApprovals: {}",
                planApprovals.size(), reportApprovals.size());

        Long buildRunDeploymentId = deploymentTask != null ? deploymentTask.getId() : deployment.getId();
        Optional<BuildRun> buildRunOpt = buildRunRepository.findByDeploymentIdAndIsDeletedFalse(buildRunDeploymentId);

        log.debug("BuildRun 조회 - deploymentId: {}, found: {}", buildRunDeploymentId, buildRunOpt.isPresent());

        // ✅ 타임라인용 Deployment는 가장 진행된 것 사용
        Deployment timelineDeployment = reportDeployment != null ? reportDeployment :
                (deploymentTask != null ? deploymentTask : deployment);

        log.debug("타임라인용 Deployment - id: {}, stage: {}",
                timelineDeployment.getId(), timelineDeployment.getStage());

        List<TimelineStepDto> timeline = taskDetailTimelineService.buildTimeline(
                timelineDeployment, planApprovals, reportApprovals, buildRunOpt.orElse(null));

        JenkinsLogDto jenkinsLog = buildRunOpt.map(this::buildJenkinsLog).orElse(null);

        PlanContentDto planContent = taskDetailInfoService.buildPlanContent(
                planDeployment, planApprovals, buildRunOpt.orElse(null));

        ApprovalInfoDto planApprovalInfo = taskDetailApprovalService.buildApprovalInfo(
                planApprovals, DeploymentStage.PLAN);

        ApprovalInfoDto reportApprovalInfo = reportDeployment != null ?
                taskDetailApprovalService.buildApprovalInfo(reportApprovals, DeploymentStage.REPORT) :
                null;

        Deployment reportTargetDeployment = reportDeployment != null ? reportDeployment : deployment;
        ReportContentDto reportContent = reportDeployment != null ?
                taskDetailReportService.buildReportContent(reportTargetDeployment, buildRunOpt.orElse(null), reportApprovals) :
                null;

        // ✅ initialTab 결정도 timelineDeployment 기준으로
        String currentStage;
        String currentStatus;
        String initialTab = "plan";

        if (timelineDeployment.getStage() == DeploymentStage.REPORT) {
            currentStage = "결과보고";
            if (timelineDeployment.getStatus() == DeploymentStatus.PENDING) {
                currentStatus = "승인대기";
                initialTab = "report";
            } else if (timelineDeployment.getStatus() == DeploymentStatus.COMPLETED) {
                currentStatus = "승인완료";
                initialTab = "report";
            } else if (timelineDeployment.getStatus() == DeploymentStatus.REJECTED) {
                currentStatus = "반려";
                initialTab = "report";
            } else {
                currentStatus = timelineDeployment.getStatus().getKoreanName();
                initialTab = "report";
            }
        } else if (timelineDeployment.getStage() == DeploymentStage.DEPLOYMENT ||
                timelineDeployment.getStage() == DeploymentStage.RETRY ||
                timelineDeployment.getStage() == DeploymentStage.ROLLBACK) {

            if (timelineDeployment.getStage() == DeploymentStage.ROLLBACK) {
                currentStage = "복구";
            } else if (timelineDeployment.getStage() == DeploymentStage.RETRY) {
                currentStage = "재배포";
            } else {
                currentStage = "배포";
            }

            if (timelineDeployment.getStatus() == DeploymentStatus.IN_PROGRESS) {
                currentStatus = "진행중";
                initialTab = "jenkins";
            } else if (timelineDeployment.getStatus() == DeploymentStatus.COMPLETED) {
                currentStatus = timelineDeployment.getIsDeployed() ? "성공" : "실패";
                initialTab = "jenkins";
            } else if (timelineDeployment.getStatus() == DeploymentStatus.REJECTED) {
                currentStatus = "반려";
                initialTab = "plan";
            } else if (timelineDeployment.getStatus() == DeploymentStatus.PENDING) {
                currentStatus = "대기";
                initialTab = "plan";
            } else {
                currentStatus = timelineDeployment.getStatus().getKoreanName();
                initialTab = "plan";
            }
        } else if (timelineDeployment.getStatus() == DeploymentStatus.APPROVED) {
            currentStage = "계획서";
            currentStatus = "승인완료";
            initialTab = "plan";
        } else if (timelineDeployment.getStatus() == DeploymentStatus.REJECTED) {
            currentStage = "계획서";
            currentStatus = "반려";
            initialTab = "plan";
        } else if (timelineDeployment.getStatus() == DeploymentStatus.PENDING) {
            currentStage = "계획서";
            currentStatus = "승인대기";
            initialTab = "plan";
        } else {
            currentStage = timelineDeployment.getStage().getKoreanName();
            currentStatus = timelineDeployment.getStatus().getKoreanName();
            initialTab = "plan";
        }

        // 가장 진행된 단계를 전달 (프론트엔드에서 탭 활성화 판단용)
        String maxStageKorean = getStageKorean(maxStage);

        return TaskDetailResponseDto.builder()
                .taskId(deployment.getId())
                .serviceName(deployment.getProject() != null ? deployment.getProject().getName() : null)
                .taskTitle(deployment.getTitle())
                .currentStage(currentStage)
                .currentStatus(currentStatus)
                .maxStage(maxStageKorean)  // ✅ 가장 진행된 단계 추가
                .initialTab(initialTab)
                .timeline(timeline)
                .jenkinsLog(jenkinsLog)
                .planContent(planContent)
                .planApproval(planApprovalInfo)
                .reportApproval(reportApprovalInfo)
                .reportContent(reportContent)
                .build();
    }

    // ✅ 단계 순서 반환 (비교용)
    private int getStageOrder(DeploymentStage stage) {
        switch (stage) {
            case PLAN: return 1;
            case DEPLOYMENT: return 2;
            case RETRY: return 2;
            case ROLLBACK: return 2;
            case REPORT: return 3;
            default: return 0;
        }
    }

    // ✅ 단계 한글명 반환
    private String getStageKorean(DeploymentStage stage) {
        switch (stage) {
            case PLAN: return "계획서";
            case DEPLOYMENT: return "배포";
            case RETRY: return "재배포";
            case ROLLBACK: return "복구";
            case REPORT: return "결과보고";
            default: return stage.getKoreanName();
        }
    }

    private JenkinsLogDto buildJenkinsLog(BuildRun buildRun) {
        String buildStatus = buildRun.getEndedAt() == null ? "IN_PROGRESS" : "SUCCESS";

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

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATETIME_FORMATTER) : null;
    }

    private String formatDuration(Long durationMs) {
        if (durationMs == null) return null;

        long seconds = durationMs / 1000;
        long minutes = seconds / 60;
        long remainingSeconds = seconds % 60;

        return minutes > 0 ? String.format("%d분 %d초", minutes, remainingSeconds) : String.format("%d초", remainingSeconds);
    }
}