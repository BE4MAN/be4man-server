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
import sys.be4man.domains.approval.model.entity.ApprovalLine;
import sys.be4man.domains.approval.model.type.ApprovalLineType;
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

/**
 * 작업 관리 페이지 서비스
 * - 작업 목록 조회 및 검색/필터링 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskManagementService {

    private final TaskManagementRepository taskManagementRepository;
    private final ApprovalRepository approvalRepository;
    private final BuildRunRepository buildRunRepository;

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");

    /**
     * 작업 관리 목록 조회 (검색 및 필터링 포함)
     *
     * @param searchDto 검색/필터 조건
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @return 페이징된 작업 목록 DTO
     */
    public Page<TaskManagementResponseDto> getTaskList(
            TaskManagementSearchDto searchDto,
            int page,
            int size
    ) {
        log.debug("작업 관리 목록 조회 - searchDto: {}, page: {}, size: {}", searchDto, page, size);

        // 기본값 설정
        if (searchDto == null) {
            searchDto = TaskManagementSearchDto.builder()
                    .sortBy("최신순")
                    .build();
        }

        // 페이징 객체 생성
        Pageable pageable = PageRequest.of(page, size);

        // Repository에서 조회
        Page<Deployment> deployments = taskManagementRepository
                .findTasksBySearchConditions(searchDto, pageable);

        log.debug("조회된 작업 수: {}", deployments.getTotalElements());

        // Entity → DTO 변환
        return deployments.map(TaskManagementResponseDto::new);
    }

    /**
     * 특정 작업 상세 조회
     *
     * @param taskId 작업 ID
     * @return 작업 상세 DTO
     */
    public TaskManagementResponseDto getTaskDetail(Long taskId) {
        log.debug("작업 상세 조회 - taskId: {}", taskId);

        Deployment deployment = taskManagementRepository.findById(taskId)
                .orElseThrow(() -> {
                    log.warn("작업을 찾을 수 없음 - taskId: {}", taskId);
                    return new IllegalArgumentException("작업을 찾을 수 없습니다. ID: " + taskId);
                });

        // 삭제된 작업인지 확인
        if (deployment.getIsDeleted()) {
            log.warn("삭제된 작업 조회 시도 - taskId: {}", taskId);
            throw new IllegalArgumentException("삭제된 작업입니다. ID: " + taskId);
        }

        return new TaskManagementResponseDto(deployment);
    }

    /**
     * 작업 상세 정보 전체 조회 (타임라인, 승인 정보, Jenkins 로그 포함)
     *
     * @param taskId 작업 ID
     * @return 작업 상세 정보 DTO
     */
    public TaskDetailResponseDto getTaskDetailFull(Long taskId) {
        log.debug("작업 전체 상세 조회 - taskId: {}", taskId);

        // 1. Deployment 조회
        Deployment deployment = taskManagementRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("작업을 찾을 수 없습니다. ID: " + taskId));

        if (deployment.getIsDeleted()) {
            throw new IllegalArgumentException("삭제된 작업입니다. ID: " + taskId);
        }

        // 2. 승인 정보 조회
        List<Approval> planApprovals = approvalRepository
                .findByDeploymentIdAndApprovalStageOrderByApprovalOrderAsc(taskId, DeploymentStage.PLAN);
        List<Approval> reportApprovals = approvalRepository
                .findByDeploymentIdAndApprovalStageOrderByApprovalOrderAsc(taskId, DeploymentStage.REPORT);

        // 3. BuildRun 조회
        Optional<BuildRun> buildRunOpt = buildRunRepository.findByDeploymentIdAndIsDeletedFalse(taskId);

        // 4. 각 DTO 생성
        List<TimelineStepDto> timeline = buildTimeline(deployment, planApprovals, reportApprovals, buildRunOpt.orElse(null));
        ApprovalInfoDto planApprovalInfo = buildApprovalInfo(planApprovals, DeploymentStage.PLAN);
        ApprovalInfoDto reportApprovalInfo = buildApprovalInfo(reportApprovals, DeploymentStage.REPORT);
        JenkinsLogDto jenkinsLog = buildRunOpt.map(this::buildJenkinsLog).orElse(null);
        PlanContentDto planContent = buildPlanContent(deployment);
        ReportContentDto reportContent = buildReportContent(deployment, buildRunOpt.orElse(null));

        // 5. 전체 DTO 조합
        return TaskDetailResponseDto.builder()
                .taskId(deployment.getId())
                .serviceName(deployment.getProject() != null ? deployment.getProject().getName() : null)
                .taskTitle(deployment.getTitle())
                .currentStage(deployment.getStage() != null ? deployment.getStage().getKoreanName() : null)
                .currentStatus(deployment.getStatus() != null ? deployment.getStatus().getKoreanName() : null)
                .timeline(timeline)
                .planApproval(planApprovalInfo)
                .reportApproval(reportApprovalInfo)
                .jenkinsLog(jenkinsLog)
                .planContent(planContent)
                .reportContent(reportContent)
                .build();
    }

    /**
     * 타임라인 생성 (6단계)
     */
    private List<TimelineStepDto> buildTimeline(Deployment deployment, List<Approval> planApprovals,
                                                 List<Approval> reportApprovals, BuildRun buildRun) {
        List<TimelineStepDto> timeline = new ArrayList<>();

        // 1. 계획서 작성 (deployment 생성 시각)
        timeline.add(TimelineStepDto.builder()
                .stepNumber(1)
                .stepName("작업 신청")
                .status("completed")
                .timestamp(formatDateTime(deployment.getCreatedAt()))
                .description("완료")
                .build());

        // 2. 계획서 승인 (모든 plan approver가 승인했을 때)
        LocalDateTime planApprovalCompletedAt = getPlanApprovalCompletedTime(planApprovals);
        String planApprovalStatus = planApprovalCompletedAt != null ? "completed" :
                (deployment.getStage() == DeploymentStage.PLAN ? "active" : "pending");
        timeline.add(TimelineStepDto.builder()
                .stepNumber(2)
                .stepName("작업 승인")
                .status(planApprovalStatus)
                        .timestamp(formatDateTime(planApprovalCompletedAt))
                .description(planApprovalCompletedAt != null ? "완료" : null)
                .build());

        // 3. 배포 시작
        LocalDateTime deploymentStartedAt = deployment.getScheduledAt();
        boolean deploymentStarted = deploymentStartedAt != null &&
                (deployment.getStage() == DeploymentStage.RETRY ||
                 deployment.getStage() == DeploymentStage.ROLLBACK ||
                 deployment.getStage() == DeploymentStage.REPORT);
        timeline.add(TimelineStepDto.builder()
                .stepNumber(3)
                .stepName("배포시작")
                .status(deploymentStarted ? "completed" :
                       (planApprovalStatus.equals("completed") ? "active" : "pending"))
                .timestamp(formatDateTime(deploymentStartedAt))
                .description(deploymentStarted ? "배포가 시작되었습니다." : null)
                .build());

        // 4. 배포 종료
        LocalDateTime deploymentEndedAt = buildRun != null ? buildRun.getEndedAt() : null;
        boolean deploymentCompleted = deployment.getStatus() == DeploymentStatus.COMPLETED ||
                                      deployment.getStage() == DeploymentStage.REPORT;
        timeline.add(TimelineStepDto.builder()
                .stepNumber(4)
                .stepName("배포종료")
                .status(deploymentCompleted ? "completed" :
                       (deployment.getStatus() == DeploymentStatus.IN_PROGRESS ? "active" : "pending"))
                .timestamp(formatDateTime(deploymentEndedAt))
                .description(deploymentCompleted ? "배포가 종료되었습니다." : null)
                .build());

        // 5. 결과보고 작성 (stage가 REPORT로 변경된 시점)
        LocalDateTime reportCreatedAt = deployment.getStage() == DeploymentStage.REPORT ?
                deployment.getUpdatedAt() : null;
        timeline.add(TimelineStepDto.builder()
                .stepNumber(5)
                .stepName("결과보고작성")
                .status(reportCreatedAt != null ? "completed" :
                       (deploymentCompleted ? "active" : "pending"))
                .timestamp(formatDateTime(reportCreatedAt))
                .description(reportCreatedAt != null ? "결과보고가 작성되었습니다." : null)
                .build());

        // 6. 결과보고 승인 (모든 report approver가 승인했을 때)
        LocalDateTime reportApprovalCompletedAt = getReportApprovalCompletedTime(reportApprovals);
        timeline.add(TimelineStepDto.builder()
                .stepNumber(6)
                .stepName("결과보고승인")
                .status(reportApprovalCompletedAt != null ? "completed" :
                       (deployment.getStage() == DeploymentStage.REPORT ? "active" : "pending"))
                .timestamp(formatDateTime(reportApprovalCompletedAt))
                .description(reportApprovalCompletedAt != null ? "결과보고 승인이 완료되었습니다." : null)
                .build());

        return timeline;
    }

    /**
     * 계획서 승인 완료 시각 조회
     */
    private LocalDateTime getPlanApprovalCompletedTime(List<Approval> planApprovals) {
        if (planApprovals.isEmpty()) {
            return null;
        }

        // 첫 번째 Approval의 ApprovalLine 확인
        Approval approval = planApprovals.get(0);

        // 모든 승인자가 승인했는지 확인
        boolean allApproved = approval.getApprovalLines().stream()
                .filter(line -> line.getType() != ApprovalLineType.CC)  // 참조 제외
                .allMatch(line -> {
                    Boolean isApproved = line.getIsApproved();
                    return isApproved != null && isApproved;
                });

        if (!allApproved) {
            return null;
        }

        // 마지막 승인자의 처리 시각 반환 (approval.processedAt 사용)
        return approval.getProcessedAt();
    }

    /**
     * 결과보고 승인 완료 시각 조회
     */
    private LocalDateTime getReportApprovalCompletedTime(List<Approval> reportApprovals) {
        if (reportApprovals.isEmpty()) {
            return null;
        }

        Approval approval = reportApprovals.get(0);

        boolean allApproved = approval.getApprovalLines().stream()
                .filter(line -> line.getType() != ApprovalLineType.CC)
                .allMatch(line -> {
                    Boolean isApproved = line.getIsApproved();
                    return isApproved != null && isApproved;
                });

        if (!allApproved) {
            return null;
        }

        return approval.getProcessedAt();
    }

    /**
     * 승인 정보 DTO 생성
     * - Approval 문서와 ApprovalLine (결재선)을 조합하여 생성
     */
    private ApprovalInfoDto buildApprovalInfo(List<Approval> approvals, DeploymentStage stage) {
        if (approvals.isEmpty()) {
            return ApprovalInfoDto.builder()
                    .approvalStage(stage.getKoreanName())
                    .totalApprovers(0)
                    .current_approver_account_id(null)
                    .approvers(new ArrayList<>())
                    .build();
        }

        // 첫 번째 Approval 사용 (일반적으로 Deployment당 Stage당 하나의 Approval만 존재)
        Approval approval = approvals.get(0);

        // 현재 차례 승인자 ID 찾기
        Long currentApproverAccountId = approval.getCurrentApprover() != null
                ? approval.getCurrentApprover().getId()
                : null;

        // ApprovalLine에서 승인자 목록 생성
        List<ApproverDto> approvers = approval.getApprovalLines().stream()
                .filter(line -> line.getType() != ApprovalLineType.CC)  // 참조 제외
                .sorted((a, b) -> a.getId().compareTo(b.getId()))  // ID 순서대로 정렬
                .map(line -> ApproverDto.builder()
                        .approverId(line.getAccount().getId())
                        .approverName(line.getAccount().getName())
                        .approverDepartment(line.getAccount().getDepartment() != null ?
                                line.getAccount().getDepartment().getKoreanName() : null)
                        .current_approver_account_id(line.getAccount().getId())  // 실제 account_id
                        .approvalStatus(getApprovalLineStatus(line))
                        .processedAt(null)  // ApprovalLine에는 처리 시각 없음
                        .comment(line.getComment())
                        .isCurrentTurn(currentApproverAccountId != null &&
                                currentApproverAccountId.equals(line.getAccount().getId()))
                        .build())
                .toList();

        return ApprovalInfoDto.builder()
                .approvalStage(stage.getKoreanName())
                .totalApprovers(approvers.size())
                .current_approver_account_id(currentApproverAccountId)
                .approvers(approvers)
                .build();
    }

    /**
     * ApprovalLine 상태 문자열 반환
     */
    private String getApprovalLineStatus(ApprovalLine line) {
        Boolean isApproved = line.getIsApproved();
        if (isApproved == null) {
            return "대기중";
        }
        return isApproved ? "승인" : "반려";
    }

    /**
     * Jenkins 로그 DTO 생성
     */
    private JenkinsLogDto buildJenkinsLog(BuildRun buildRun) {
        String buildStatus;
        if (buildRun.getEndedAt() == null) {
            buildStatus = "IN_PROGRESS";
        } else {
            // deployment의 isDeployed로 판단할 수도 있지만, buildRun 자체로는 알 수 없음
            // 여기서는 간단히 endedAt이 있으면 완료로 처리
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

    /**
     * 계획서 내용 DTO 생성
     */
    private PlanContentDto buildPlanContent(Deployment deployment) {
        return PlanContentDto.builder()
                .drafter(deployment.getIssuer() != null ? deployment.getIssuer().getName() : null)
                .department(deployment.getIssuer() != null && deployment.getIssuer().getDepartment() != null ?
                        deployment.getIssuer().getDepartment().getKoreanName() : null)
                .createdAt(formatDateTime(deployment.getCreatedAt()))
                .serviceName(deployment.getProject() != null ? deployment.getProject().getName() : null)
                .taskTitle(deployment.getTitle())
                .deploymentType(deployment.getType() != null ? deployment.getType().name() : null)
                .scheduledAt(formatDateTime(deployment.getScheduledAt()))
                .scheduledToEndedAt(formatDateTime(deployment.getScheduledToEndedAt()))
                .riskDescription(null)  // Deployment에 없는 필드
                .expectedDuration(deployment.getExpectedDuration())
                .version(deployment.getVersion())
                .strategy(null)  // Deployment에 없는 필드
                .content(deployment.getContent())
//                .pullRequestUrl(deployment.getPullRequest() != null ?
//                        deployment.getPullRequest().getUrl() : null)
                .build();
    }

    /**
     * 결과보고 내용 DTO 생성
     */
    private ReportContentDto buildReportContent(Deployment deployment, BuildRun buildRun) {
        if (deployment.getStage() != DeploymentStage.REPORT) {
            return null;
        }

        String deploymentResult = null;
        if (deployment.getIsDeployed() != null) {
            deploymentResult = deployment.getIsDeployed() ? "성공" : "실패";
        }

        return ReportContentDto.builder()
                .deploymentResult(deploymentResult)
                .actualStartedAt(buildRun != null ? formatDateTime(buildRun.getStartedAt()) : null)
                .actualEndedAt(buildRun != null ? formatDateTime(buildRun.getEndedAt()) : null)
                .actualDuration(buildRun != null ? formatDuration(buildRun.getDuration()) : null)
                .reportContent(deployment.getContent())
                .reportCreatedAt(formatDateTime(deployment.getUpdatedAt()))
                .build();
    }

    /**
     * LocalDateTime을 yyyy.MM.dd HH:mm 형식으로 포맷
     */
    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATETIME_FORMATTER) : null;
    }

    /**
     * Duration (밀리초)를 "X분 Y초" 형식으로 포맷
     */
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