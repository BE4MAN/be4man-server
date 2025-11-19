package sys.be4man.domains.dashboard.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sys.be4man.domains.approval.model.entity.Approval;
import sys.be4man.domains.approval.model.entity.ApprovalLine;
import sys.be4man.domains.approval.model.type.ApprovalType;
import sys.be4man.domains.approval.repository.ApprovalLineRepository;
import sys.be4man.domains.dashboard.dto.response.DeploymentInfoResponse;
import sys.be4man.domains.dashboard.dto.response.PendingApprovalResponse;
import sys.be4man.domains.dashboard.dto.response.RelatedServiceResponse;
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

    // TODO: Step 3에서 진행중인 업무 목록 조회 메서드 구현 예정
    // @Override
    // @Transactional(readOnly = true)
    // public List<InProgressTaskResponse> getInProgressTasks(Long accountId) {
    //     log.info("진행중인 업무 목록 조회 - accountId: {}", accountId);
    //     // 구현 예정
    //     return List.of();
    // }

    // TODO: Step 4에서 알림 목록 조회 메서드 구현 예정
    // @Override
    // @Transactional(readOnly = true)
    // public List<NotificationResponse> getNotifications(Long accountId) {
    //     log.info("알림 목록 조회 - accountId: {}", accountId);
    //     // 구현 예정
    //     return List.of();
    // }

    // TODO: Step 5에서 복구현황 목록 조회 메서드 구현 예정
    // @Override
    // @Transactional(readOnly = true)
    // public PageResponse<RecoveryResponse> getRecovery(int page, int pageSize) {
    //     log.info("복구현황 목록 조회 - page: {}, pageSize: {}", page, pageSize);
    //     // 구현 예정
    //     return new PageResponse<>(List.of(), new PaginationResponse(0, page, pageSize, 0));
    // }
}

