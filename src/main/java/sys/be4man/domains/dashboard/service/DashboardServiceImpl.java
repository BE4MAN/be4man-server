package sys.be4man.domains.dashboard.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;
import sys.be4man.domains.analysis.model.entity.BuildRun;
import sys.be4man.domains.analysis.repository.BuildRunRepository;
import sys.be4man.domains.approval.model.entity.Approval;
import sys.be4man.domains.approval.model.entity.ApprovalLine;
import sys.be4man.domains.approval.model.type.ApprovalType;
import sys.be4man.domains.approval.repository.ApprovalLineRepository;
import sys.be4man.domains.approval.repository.ApprovalRepository;
import sys.be4man.domains.dashboard.dto.response.DeploymentInfoResponse;
import sys.be4man.domains.dashboard.dto.response.InProgressTaskResponse;
import sys.be4man.domains.dashboard.dto.response.NotificationResponse;
import sys.be4man.domains.dashboard.dto.response.PaginationResponse;
import sys.be4man.domains.dashboard.dto.response.PendingApprovalResponse;
import sys.be4man.domains.dashboard.dto.response.RecoveryResponse;
import sys.be4man.domains.dashboard.dto.response.RelatedServiceResponse;
import sys.be4man.domains.deployment.model.type.DeploymentStatus;
import sys.be4man.domains.deployment.model.entity.Deployment;
import sys.be4man.domains.project.model.entity.Project;
import sys.be4man.domains.project.model.entity.RelatedProject;
import sys.be4man.domains.project.repository.RelatedProjectRepository;

/**
 * 홈(Dashboard) 페이지 비즈니스 로직 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final ApprovalLineRepository approvalLineRepository;
    private final ApprovalRepository approvalRepository;
    private final BuildRunRepository buildRunRepository;
    private final RelatedProjectRepository relatedProjectRepository;

    @Override
    @Transactional(readOnly = true)
    public List<PendingApprovalResponse> getPendingApprovals(Long accountId) {
        log.info("승인 대기 목록 조회 - accountId: {}", accountId);

        List<ApprovalLine> approvalLines = approvalLineRepository.findPendingApprovalsByAccountId(accountId);

        if (approvalLines.isEmpty()) {
            return List.of();
        }

        // N+1 쿼리 방지를 위한 배치 조회: 각 Deployment의 project_id로 관련 프로젝트 조회
        List<Long> projectIds = approvalLines.stream()
                .map(line -> line.getApproval().getDeployment().getProject().getId())
                .distinct()
                .toList();

        Map<Long, List<RelatedProject>> projectRelatedServicesMap = buildProjectRelatedServicesMap(projectIds);

        // 각 ApprovalLine에 대해 현재 승인 예정자 목록 조회 (is_approved = NULL인 approval_line의 account 이름들)
        Map<Long, List<String>> approvalCurrentApproversMap = buildApprovalCurrentApproversMap(
                approvalLines.stream()
                        .map(ApprovalLine::getApproval)
                        .map(Approval::getId)
                        .distinct()
                        .toList()
        );

        return approvalLines.stream()
                .map(line -> {
                    Approval approval = line.getApproval();
                    Deployment deployment = approval.getDeployment();
                    Project project = deployment.getProject();

                    // 관련 서비스 목록 조회
                    List<RelatedProject> relatedProjects = projectRelatedServicesMap.getOrDefault(
                            project.getId(), List.of());
                    List<RelatedServiceResponse> relatedServiceResponses = relatedProjects.stream()
                            .map(rp -> new RelatedServiceResponse(
                                    rp.getRelatedProject().getId(),
                                    rp.getRelatedProject().getName(),
                                    rp.getRelatedProject().getId()
                            ))
                            .toList();

                    // 서비스 이름 배열 (메인 프로젝트 + 관련 프로젝트)
                    List<String> serviceNames = new ArrayList<>();
                    serviceNames.add(project.getName());
                    serviceNames.addAll(relatedProjects.stream()
                            .map(rp -> rp.getRelatedProject().getName())
                            .toList());

                    // 현재 승인 예정자 목록
                    List<String> currentApprovers = approvalCurrentApproversMap.getOrDefault(
                            approval.getId(), List.of());

                    // docType 결정 (ApprovalType에 따라)
                    String docType = mapApprovalTypeToDocType(approval.getType());

                    // Deployment 정보
                    DeploymentInfoResponse deploymentInfo = new DeploymentInfoResponse(
                            deployment.getId(),
                            deployment.getTitle(),
                            deployment.getStatus().name(),
                            deployment.getStage().name(),
                            project.getName(),
                            deployment.getScheduledAt() != null
                                    ? deployment.getScheduledAt().toLocalDate()
                                    : null,
                            deployment.getScheduledAt() != null
                                    ? deployment.getScheduledAt().toLocalTime()
                                    : null,
                            deployment.getIssuer().getName(),
                            deployment.getIssuer().getDepartment() != null
                                    ? deployment.getIssuer().getDepartment().name()
                                    : null,
                            relatedServiceResponses
                    );

                    return new PendingApprovalResponse(
                            approval.getId(),
                            deployment.getTitle(),
                            docType,
                            serviceNames,
                            approval.getCreatedAt(),
                            currentApprovers,
                            deployment.getIssuer().getName(),
                            deployment.getIssuer().getDepartment() != null
                                    ? deployment.getIssuer().getDepartment().name()
                                    : null,
                            deployment.getContent(),
                            serviceNames,
                            "승인 대기",
                            deploymentInfo
                    );
                })
                .toList();
    }

    /**
     * 프로젝트 ID 목록에 대한 관련 프로젝트 Map 생성
     */
    private Map<Long, List<RelatedProject>> buildProjectRelatedServicesMap(List<Long> projectIds) {
        if (projectIds.isEmpty()) {
            return Map.of();
        }

        List<RelatedProject> relatedProjects = relatedProjectRepository.findByProjectIdIn(projectIds);

        return relatedProjects.stream()
                .collect(Collectors.groupingBy(rp -> rp.getProject().getId()));
    }

    /**
     * Approval ID 목록에 대한 현재 승인 예정자 Map 생성
     * (is_approved = NULL인 approval_line의 account 이름들)
     */
    private Map<Long, List<String>> buildApprovalCurrentApproversMap(List<Long> approvalIds) {
        if (approvalIds.isEmpty()) {
            return Map.of();
        }

        // 각 Approval의 approvalLines에서 is_approved = NULL인 항목들의 account 이름 조회
        List<ApprovalLine> pendingLines = approvalLineRepository.findByApprovalIdIn(approvalIds).stream()
                .filter(line -> line.getIsApproved() == null)
                .toList();

        return pendingLines.stream()
                .collect(Collectors.groupingBy(
                        line -> line.getApproval().getId(),
                        Collectors.mapping(
                                line -> line.getAccount().getName(),
                                Collectors.toList()
                        )
                ));
    }

    /**
     * ApprovalType을 docType 문자열로 변환
     */
    private String mapApprovalTypeToDocType(ApprovalType approvalType) {
        return switch (approvalType) {
            case PLAN -> "작업계획서";
            case REPORT -> "결과보고";
            case DEPLOYMENT -> "배포";
            case RETRY -> "재배포";
            case ROLLBACK -> "복구";
            case DRAFT -> "임시저장";
        };
    }

    @Override
    @Transactional(readOnly = true)
    public List<InProgressTaskResponse> getInProgressTasks(Long accountId) {
        log.info("진행중인 업무 목록 조회 - accountId: {}", accountId);

        List<Deployment> deployments = approvalLineRepository.findInProgressTasksByAccountId(accountId);

        if (deployments.isEmpty()) {
            return List.of();
        }

        // N+1 쿼리 방지를 위한 배치 조회: 각 Deployment의 project_id로 관련 프로젝트 조회
        List<Long> projectIds = deployments.stream()
                .map(deployment -> deployment.getProject().getId())
                .distinct()
                .toList();

        Map<Long, List<RelatedProject>> projectRelatedServicesMap = buildProjectRelatedServicesMap(projectIds);

        return deployments.stream()
                .map(deployment -> {
                    Project project = deployment.getProject();

                    // 관련 서비스 목록 조회
                    List<RelatedProject> relatedProjects = projectRelatedServicesMap.getOrDefault(
                            project.getId(), List.of());

                    // 서비스 이름 배열 (메인 프로젝트 + 관련 프로젝트)
                    List<String> serviceNames = new ArrayList<>();
                    serviceNames.add(project.getName());
                    serviceNames.addAll(relatedProjects.stream()
                            .map(rp -> rp.getRelatedProject().getName())
                            .toList());

                    return new InProgressTaskResponse(
                            deployment.getId(),
                            deployment.getTitle(),
                            deployment.getScheduledAt() != null
                                    ? deployment.getScheduledAt().toLocalDate()
                                    : null,
                            deployment.getScheduledAt() != null
                                    ? deployment.getScheduledAt().toLocalTime()
                                    : null,
                            deployment.getStatus().name(),
                            deployment.getStage().name(),
                            deployment.getIsDeployed(),
                            project.getName(),
                            deployment.getIssuer().getName(),
                            deployment.getIssuer().getDepartment() != null
                                    ? deployment.getIssuer().getDepartment().name()
                                    : null,
                            deployment.getContent(),
                            serviceNames
                    );
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotifications(Long accountId) {
        log.info("알림 목록 조회 - accountId: {}", accountId);

        // 취소 알림 조회
        List<Deployment> canceledDeployments = approvalLineRepository.findCanceledNotificationsByAccountId(accountId);

        // 반려 알림 조회 (케이스 1: 현재 사용자가 요청한 deployment가 반려된 경우)
        List<Deployment> rejectedByIssuer = approvalLineRepository.findRejectedDeploymentsByIssuerId(accountId);

        // 반려 알림 조회 (케이스 3: 현재 사용자가 승인한 approval이 반려된 경우)
        List<Deployment> rejectedByApprover = approvalLineRepository.findRejectedApprovalsByApproverId(accountId);

        // 취소 알림 DTO 변환
        List<NotificationResponse> canceledNotifications = canceledDeployments.stream()
                .map(deployment -> new NotificationResponse(
                        deployment.getId(),
                        "취소",
                        "작업 금지 기간에 해당되어 자동 취소되었습니다.", // TODO: 실제 취소 사유가 있다면 사용
                        deployment.getProject().getName(),
                        deployment.getId(),
                        deployment.getTitle(),
                        deployment.getUpdatedAt(), // canceledAt
                        null // rejectedAt
                ))
                .toList();

        // N+1 쿼리 방지를 위한 배치 조회: 반려 알림의 반려 사유 조회
        List<Deployment> allRejectedDeployments = new ArrayList<>();
        allRejectedDeployments.addAll(rejectedByIssuer);
        allRejectedDeployments.addAll(rejectedByApprover);
        
        Map<Long, String> rejectionReasonMap = buildRejectionReasonMap(
                allRejectedDeployments.stream()
                        .map(Deployment::getId)
                        .toList()
        );

        // 반려 알림 DTO 변환 (케이스 1)
        List<NotificationResponse> rejectedByIssuerNotifications = rejectedByIssuer.stream()
                .map(deployment -> {
                    String reason = rejectionReasonMap.getOrDefault(
                            deployment.getId(), "반려되었습니다.");
                    return new NotificationResponse(
                            deployment.getId(),
                            "반려",
                            reason,
                            deployment.getProject().getName(),
                            deployment.getId(),
                            deployment.getTitle(),
                            null, // canceledAt
                            deployment.getUpdatedAt() // rejectedAt
                    );
                })
                .toList();

        // 반려 알림 DTO 변환 (케이스 3)
        List<NotificationResponse> rejectedByApproverNotifications = rejectedByApprover.stream()
                .map(deployment -> {
                    String reason = rejectionReasonMap.getOrDefault(
                            deployment.getId(), "반려되었습니다.");
                    return new NotificationResponse(
                            deployment.getId(),
                            "반려",
                            reason,
                            deployment.getProject().getName(),
                            deployment.getId(),
                            deployment.getTitle(),
                            null, // canceledAt
                            deployment.getUpdatedAt() // rejectedAt (또는 approval.updatedAt)
                    );
                })
                .toList();

        // 모든 알림 통합 및 정렬 (updatedAt 기준 최신순)
        List<NotificationResponse> allNotifications = new ArrayList<>();
        allNotifications.addAll(canceledNotifications);
        allNotifications.addAll(rejectedByIssuerNotifications);
        allNotifications.addAll(rejectedByApproverNotifications);

        // updatedAt 기준 최신순 정렬
        return allNotifications.stream()
                .sorted((a, b) -> {
                    LocalDateTime aTime = a.canceledAt() != null ? a.canceledAt() : a.rejectedAt();
                    LocalDateTime bTime = b.canceledAt() != null ? b.canceledAt() : b.rejectedAt();
                    if (aTime == null && bTime == null) return 0;
                    if (aTime == null) return 1;
                    if (bTime == null) return -1;
                    return bTime.compareTo(aTime); // DESC
                })
                .toList();
    }

    /**
     * Deployment ID 목록에 대한 반려 사유 Map 생성
     * (ApprovalLine에서 isApproved = false인 항목의 comment)
     */
    private Map<Long, String> buildRejectionReasonMap(List<Long> deploymentIds) {
        if (deploymentIds.isEmpty()) {
            return Map.of();
        }

        // Deployment ID로 Approval 조회
        List<Approval> approvals = new ArrayList<>();
        for (Long deploymentId : deploymentIds) {
            approvals.addAll(approvalRepository.findByDeploymentId(deploymentId));
        }

        if (approvals.isEmpty()) {
            return Map.of();
        }

        // Approval ID 목록으로 ApprovalLine 조회 (fetch join)
        List<Long> approvalIds = approvals.stream()
                .map(Approval::getId)
                .toList();

        Map<Long, String> rejectionReasonMap = new java.util.HashMap<>();
        for (Long approvalId : approvalIds) {
            Optional<Approval> approvalWithLines = approvalRepository.findByIdWithLines(approvalId);
            if (approvalWithLines.isPresent()) {
                Approval approval = approvalWithLines.get();
                // isApproved = false인 ApprovalLine의 comment 찾기
                String reason = approval.getApprovalLines().stream()
                        .filter(line -> Boolean.FALSE.equals(line.getIsApproved()))
                        .map(ApprovalLine::getComment)
                        .findFirst()
                        .orElse("반려되었습니다.");
                
                // 해당 Approval의 Deployment ID에 매핑
                if (approval.getDeployment() != null) {
                    rejectionReasonMap.put(approval.getDeployment().getId(), reason);
                }
            }
        }

        return rejectionReasonMap;
    }

    @Override
    @Transactional(readOnly = true)
    public PaginationResponse<RecoveryResponse> getRecovery(int page, int pageSize) {
        log.info("복구현황 목록 조회 - page: {}, pageSize: {}", page, pageSize);

        // 페이지네이션 계산
        int offset = (page - 1) * pageSize;
        long total = approvalRepository.countRollbackDeployments();
        int totalPages = (int) Math.ceil((double) total / pageSize);

        // 복구현황 Deployment 목록 조회
        List<Deployment> deployments = approvalRepository.findRollbackDeployments(offset, pageSize);

        // N+1 쿼리 방지를 위한 배치 조회: BuildRun 조회
        List<Long> deploymentIds = deployments.stream()
                .map(Deployment::getId)
                .toList();

        Map<Long, List<BuildRun>> buildRunsMap = buildRunRepository.findByDeploymentIdIn(deploymentIds)
                .stream()
                .collect(Collectors.groupingBy(br -> br.getDeployment().getId()));

        // DTO 변환
        List<RecoveryResponse> recoveryList = deployments.stream()
                .map(deployment -> {
                    String status = determineRecoveryStatus(deployment);
                    List<BuildRun> buildRuns = buildRunsMap.getOrDefault(deployment.getId(), List.of());

                    String duration = null;
                    Integer buildRunDuration = null;
                    LocalDateTime recoveredAt = null;
                    LocalDateTime updatedAt = deployment.getUpdatedAt();

                    if ("COMPLETED".equals(status) && !buildRuns.isEmpty()) {
                        // duration 계산: 첫 번째 buildRun.startedAt ~ 마지막 buildRun.endedAt
                        BuildRun firstBuildRun = buildRuns.stream()
                                .min((a, b) -> a.getStartedAt().compareTo(b.getStartedAt()))
                                .orElse(null);

                        BuildRun lastBuildRun = buildRuns.stream()
                                .max((a, b) -> a.getEndedAt().compareTo(b.getEndedAt()))
                                .orElse(null);

                        if (firstBuildRun != null && lastBuildRun != null) {
                            Duration durationBetween = Duration.between(
                                    firstBuildRun.getStartedAt(),
                                    lastBuildRun.getEndedAt()
                            );
                            long minutes = durationBetween.toMinutes();
                            duration = minutes + "분";

                            // buildRunDuration: 마지막 BuildRun의 duration (밀리초 -> 초 단위 변환)
                            if (lastBuildRun.getDuration() != null) {
                                buildRunDuration = (int) (lastBuildRun.getDuration() / 1000);
                            }

                            // recoveredAt: status가 COMPLETED일 때만 deployment.updatedAt 사용
                            recoveredAt = deployment.getUpdatedAt();
                        }
                    }

                    return new RecoveryResponse(
                            deployment.getId(),
                            deployment.getTitle(),
                            deployment.getProject().getName(),
                            status,
                            duration,
                            buildRunDuration,
                            recoveredAt,
                            updatedAt,
                            deployment.getIssuer().getName(),
                            deployment.getIssuer().getDepartment().getKoreanName(),
                            deployment.getId()
                    );
                })
                .toList();

        return new PaginationResponse<>(
                recoveryList,
                new PaginationResponse.PaginationInfo(total, page, pageSize, totalPages)
        );
    }

    /**
     * 복구 상태 결정
     * - COMPLETED: deployment.status = 'COMPLETED' 또는 deployment.isDeployed = true
     * - IN_PROGRESS: deployment.status = 'IN_PROGRESS'
     * - PENDING: 그 외 (기본값)
     */
    private String determineRecoveryStatus(Deployment deployment) {
        if (deployment.getStatus() == DeploymentStatus.COMPLETED
                || Boolean.TRUE.equals(deployment.getIsDeployed())) {
            return "COMPLETED";
        } else if (deployment.getStatus() == DeploymentStatus.IN_PROGRESS) {
            return "IN_PROGRESS";
        } else {
            return "PENDING";
        }
    }
}

