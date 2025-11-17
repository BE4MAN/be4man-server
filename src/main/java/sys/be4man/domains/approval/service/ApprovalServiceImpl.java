package sys.be4man.domains.approval.service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sys.be4man.domains.account.model.entity.Account;
import sys.be4man.domains.account.repository.AccountRepository;
import sys.be4man.domains.approval.dto.request.ApprovalCreateRequest;
import sys.be4man.domains.approval.dto.request.ApprovalDecisionRequest;
import sys.be4man.domains.approval.dto.response.ApprovalDetailResponse;
import sys.be4man.domains.approval.dto.response.ApprovalSummaryResponse;
import sys.be4man.domains.approval.model.entity.Approval;
import sys.be4man.domains.approval.model.entity.ApprovalLine;
import sys.be4man.domains.approval.model.type.ApprovalLineType;
import sys.be4man.domains.approval.model.type.ApprovalStatus;
import sys.be4man.domains.approval.model.type.ApprovalType;
import sys.be4man.domains.approval.repository.ApprovalLineRepository;
import sys.be4man.domains.approval.repository.ApprovalRepository;
import sys.be4man.domains.deployment.dto.request.DeploymentCreateRequest;
import sys.be4man.domains.deployment.dto.response.DeploymentResponse;
import sys.be4man.domains.deployment.model.entity.Deployment;
import sys.be4man.domains.deployment.model.type.DeploymentStage;
import sys.be4man.domains.deployment.model.type.DeploymentStatus;
import sys.be4man.domains.deployment.repository.DeploymentRepository;
import sys.be4man.domains.deployment.service.DeploymentService;
import sys.be4man.domains.project.model.entity.Project;
import sys.be4man.domains.project.model.entity.RelatedProject;
import sys.be4man.domains.project.repository.ProjectRepository;
import sys.be4man.domains.project.repository.RelatedProjectRepository;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ApprovalServiceImpl implements ApprovalService {

    private final ApprovalRepository approvalRepository;
    private final ApprovalLineRepository approvalLineRepository;
    private final AccountRepository accountRepository;

    private final DeploymentRepository deploymentRepository;
    private final DeploymentService deploymentService;
    private final ProjectRepository projectRepository;
    private final RelatedProjectRepository relatedProjectRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ApprovalSummaryResponse> getApprovals(Long accountId, ApprovalStatus status) {
        List<Approval> approvals = (status == null)
                ? approvalRepository.findMyApprovals(accountId)
                : approvalRepository.findMyApprovalsByStatus(accountId, status);

        return approvals.stream()
                .map(this::toSummaryDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ApprovalDetailResponse getApprovalDetail(Long approvalId) {
        Approval approval = getApprovalOrThrow(approvalId);
        List<ApprovalLine> lines = approvalLineRepository.findByApprovalId(approvalId);
        return toDetailDto(approval, lines);
    }

    @Override
    public Long saveDraft(ApprovalCreateRequest request) {
        Approval approval = createApprovalEntity(request, ApprovalStatus.DRAFT, false);
        approvalRepository.save(approval);
        createLines(request, approval);
        approval.updateNextApprover(resolveInitialNextApprover(approval));
        return approval.getId();
    }

    @Override
    public void submit(Long approvalId) {
        Approval approval = getApprovalOrThrow(approvalId);
        if (approval.getNextApprover() == null)
            approval.updateNextApprover(resolveInitialNextApprover(approval));
        approval.updateStatus(ApprovalStatus.PENDING);
    }

    @Override
    public Long createAndSubmit(ApprovalCreateRequest request) {
        Approval approval = createApprovalEntity(request, ApprovalStatus.PENDING, false);
        approvalRepository.save(approval);
        createLines(request, approval);
        approval.updateNextApprover(resolveInitialNextApprover(approval));
        return approval.getId();
    }

    @Override
    public void approve(Long approvalId, ApprovalDecisionRequest request) {
        Approval approval = getApprovalOrThrow(approvalId);
        Account approver = accountRepository.findById(request.getApproverAccountId())
                .orElseThrow(() -> new IllegalArgumentException("Account not found. id=" + request.getApproverAccountId()));

        ApprovalLine line = updateOrCreateLineComment(approval, request, true);
        line.updateComment(request.getComment());
        line.approve();
        approvalLineRepository.save(line);

        // ✅ 수정: 정렬된 리스트로 다음 승인자 찾기
        List<ApprovalLine> approverLines = approval.getApprovalLines().stream()
                .filter(l -> l.getType() == ApprovalLineType.APPROVE || l.getType() == ApprovalLineType.CONSENT)
                .sorted(Comparator.comparing(ApprovalLine::getId))
                .toList();

        Account next = findNextApproverAfter(approverLines, approver);

        log.debug("승인 처리 - approvalId: {}, currentApprover: {}, nextApprover: {}",
                approvalId, approver.getName(), next != null ? next.getName() : "없음");

        if (next != null) {
            approval.updateNextApprover(next);
            approval.updateStatus(ApprovalStatus.PENDING);
            log.info("다음 승인자로 이동 - approvalId: {}, nextApprover: {}", approvalId, next.getName());
        } else {
            // ✅ 마지막 승인자 → 승인 완료 처리
            java.time.LocalDateTime lastApprovedAt = approval.getApprovalLines().stream()
                    .filter(l -> l.getType() != ApprovalLineType.CC)
                    .filter(l -> Boolean.TRUE.equals(l.getIsApproved()))
                    .map(ApprovalLine::getApprovedAt)
                    .max(java.time.LocalDateTime::compareTo)
                    .orElse(java.time.LocalDateTime.now());

            approval.approve(lastApprovedAt);
            log.info("승인 완료 - approvalId: {}, type: {}", approvalId, approval.getType());

            Deployment deployment = approval.getDeployment();
            if (deployment != null) {
                if (approval.getType() == ApprovalType.PLAN) {
                    deployment.updateStatus(DeploymentStatus.APPROVED);
                    deployment.updateStage(DeploymentStage.DEPLOYMENT);
                    deploymentRepository.save(deployment);

                    log.info("✅ 계획서 승인 완료 - Deployment 업데이트: deploymentId={}, status=APPROVED, stage=DEPLOYMENT",
                            deployment.getId());

                } else if (approval.getType() == ApprovalType.REPORT) {
                    deployment.updateStatus(DeploymentStatus.COMPLETED);
                    deploymentRepository.save(deployment);

                    log.info("✅ 결과보고 승인 완료 - Deployment 업데이트: deploymentId={}, status=COMPLETED",
                            deployment.getId());
                }
            }
        }
    }

    // ✅ 수정: List를 파라미터로 받도록 변경
    private Account findNextApproverAfter(List<ApprovalLine> approverLines, Account currentApprover) {
        boolean foundCurrent = false;
        for (ApprovalLine line : approverLines) {
            if (!foundCurrent) {
                if (line.getAccount().getId().equals(currentApprover.getId())) {
                    foundCurrent = true;
                }
                continue;
            }
            return line.getAccount();
        }
        return null;
    }

    @Override
    public void reject(Long approvalId, ApprovalDecisionRequest request) {
        Approval approval = getApprovalOrThrow(approvalId);

        // ✅ 승인 권한 확인
        Account rejector = accountRepository.findById(request.getApproverAccountId())
                .orElseThrow(() -> new IllegalArgumentException("Account not found. id=" + request.getApproverAccountId()));

        ApprovalLine line = approval.getApprovalLines().stream()
                .filter(l -> l.getAccount().getId().equals(rejector.getId()))
                .filter(l -> l.getType() == ApprovalLineType.APPROVE || l.getType() == ApprovalLineType.CONSENT)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "승인 권한이 없습니다. approvalId=" + approvalId + ", accountId=" + rejector.getId()));

        // ✅ 이미 승인했던 경우 승인 취소 로그
        if (Boolean.TRUE.equals(line.getIsApproved())) {
            log.info("승인 취소 후 반려 - approvalId: {}, accountId: {}, accountName: {}",
                    approvalId, rejector.getId(), rejector.getName());
        }

        // ✅ 반려 처리 (승인 여부와 관계없이)
        line.reject();
        line.updateComment(request.getComment() != null ? request.getComment() : "");
        approvalLineRepository.save(line);

        // ✅ Approval 전체를 REJECTED로 변경
        approval.reject();

        log.info("반려 처리 완료 - approvalId: {}, type: {}, rejector: {}",
                approvalId, approval.getType(), rejector.getName());

        // ✅ Deployment 상태 업데이트
        Deployment deployment = approval.getDeployment();
        if (deployment != null) {
            deployment.updateStatus(DeploymentStatus.REJECTED);
            deploymentRepository.save(deployment);

            log.info("✅ 반려 처리 - Deployment 업데이트: deploymentId={}, status=REJECTED",
                    deployment.getId());
        }
    }

    @Override
    public void cancel(Long approvalId, ApprovalDecisionRequest request) {
        Approval approval = getApprovalOrThrow(approvalId);
        Account drafter = approval.getAccount();

        ApprovalLine line = ApprovalLine.builder()
                .approval(approval)
                .account(drafter)
                .type(ApprovalLineType.APPROVE)
                .comment(request.getComment() == null ? "" : request.getComment())
                .isApproved(false)
                .build();

        approval.addApprovalLine(line);
        approval.updateStatus(ApprovalStatus.CANCELED);

        log.info("취소 처리 - approvalId: {}, type: {}", approvalId, approval.getType());

        // ✅ Deployment 상태 업데이트
        Deployment deployment = approval.getDeployment();
        if (deployment != null) {
            deployment.updateStatus(DeploymentStatus.CANCELED);
            deploymentRepository.save(deployment);

            log.info("✅ 취소 처리 - Deployment 업데이트: deploymentId={}, status=CANCELED",
                    deployment.getId());
        }
    }

    private Approval getApprovalOrThrow(Long id) {
        return approvalRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Approval not found. id=" + id));
    }

    private Approval createApprovalEntity(
            ApprovalCreateRequest request, ApprovalStatus status, boolean isApproved
    ) {
        if (request.getDrafterAccountId() == null) {
            throw new IllegalArgumentException("drafterAccountId must not be null");
        }

        Account drafter = accountRepository.findById(request.getDrafterAccountId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Account not found. id=" + request.getDrafterAccountId()
                ));

        Deployment deployment;
        Long deploymentId = request.getDeploymentId();

        if (deploymentId != null && deploymentId > 0) {
            deployment = deploymentRepository.findById(deploymentId)
                    .orElseThrow(() -> new IllegalArgumentException("Deployment not found. id=" + deploymentId));

            if (request.getRelatedProjectIds() != null && !request.getRelatedProjectIds().isEmpty())
                saveRelatedProjects(deployment, request.getRelatedProjectIds());
        } else {
            DeploymentCreateRequest deploymentReq = DeploymentCreateRequest.builder()
                    .projectId(request.getProjectId())
                    .issuerId(request.getDrafterAccountId())
                    .pullRequestId(request.getPullRequestId())
                    .title(request.getTitle())
                    .stage(DeploymentStage.PLAN.name())
                    .status(DeploymentStatus.PENDING.name())
                    .expectedDuration("0")
                    .version(null)
                    .content(request.getContent())
                    .build();

            DeploymentResponse created = deploymentService.createDeployment(deploymentReq);
            deployment = deploymentRepository.getReferenceById(created.getId());

            if (request.getRelatedProjectIds() != null && !request.getRelatedProjectIds().isEmpty())
                saveRelatedProjects(deployment, request.getRelatedProjectIds());
        }

        return Approval.builder()
                .deployment(deployment)
                .account(drafter)
                .isApproved(isApproved)
                .type(request.getType())
                .title(request.getTitle())
                .content(request.getContent())
                .status(status)
                .service(request.getService())
                .build();
    }

    private void createLines(ApprovalCreateRequest request, Approval approval) {
        if (request.getLines() == null) return;

        request.getLines().stream()
                .filter(lineReq -> lineReq.getAccountId() != null)
                .forEach(lineReq -> {
                    Account account = accountRepository.findById(lineReq.getAccountId())
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "Account not found. id=" + lineReq.getAccountId()
                            ));

                    ApprovalLine line = ApprovalLine.builder()
                            .approval(approval)
                            .account(account)
                            .type(lineReq.getType())
                            .comment(lineReq.getComment() == null ? "" : lineReq.getComment())
                            .build();

                    approval.addApprovalLine(line);
                });
    }

    private ApprovalLine updateOrCreateLineComment(Approval approval, ApprovalDecisionRequest request, boolean approve) {
        Account approver = accountRepository.findById(request.getApproverAccountId())
                .orElseThrow(() -> new IllegalArgumentException("Account not found. id=" + request.getApproverAccountId()));

        ApprovalLine target = approval.getApprovalLines().stream()
                .filter(line -> line.getAccount().getId().equals(approver.getId()))
                .findFirst()
                .orElse(null);

        if (target == null) {
            ApprovalLine line = ApprovalLine.builder()
                    .approval(approval)
                    .account(approver)
                    .type(ApprovalLineType.APPROVE)
                    .comment(request.getComment() == null ? "" : request.getComment())
                    .build();
            approval.addApprovalLine(line);
            return line;
        } else {
            if (request.getComment() != null && !request.getComment().isBlank()) {
                target.updateComment(request.getComment());
            }
            return target;
        }
    }

    private Account resolveInitialNextApprover(Approval approval) {
        return approval.getApprovalLines().stream()
                .filter(line -> line.getType() == ApprovalLineType.APPROVE || line.getType() == ApprovalLineType.CONSENT)
                .map(ApprovalLine::getAccount)
                .findFirst()
                .orElse(null);
    }

    private Account findNextApproverAfter(Approval approval, Account currentApprover) {
        boolean foundCurrent = false;
        for (ApprovalLine line : approval.getApprovalLines()) {
            if (line.getType() != ApprovalLineType.APPROVE && line.getType() != ApprovalLineType.CONSENT)
                continue;
            if (!foundCurrent) {
                if (line.getAccount().getId().equals(currentApprover.getId()))
                    foundCurrent = true;
                continue;
            }
            return line.getAccount();
        }
        return null;
    }

    private ApprovalSummaryResponse toSummaryDto(Approval approval) {
        var builder = ApprovalSummaryResponse.builder()
                .id(approval.getId())
                .title(approval.getTitle())
                .service(approval.getService())
                .type(approval.getType())
                .status(approval.getStatus())
                .isApproved(approval.getIsApproved())
                .drafterAccountId(approval.getAccount().getId())
                .drafterName(approval.getAccount().getName())
                .nextApproverAccountId(approval.getNextApprover() != null ? approval.getNextApprover().getId() : null)
                .nextApproverName(approval.getNextApprover() != null ? approval.getNextApprover().getName() : null)
                .createdAt(approval.getCreatedAt())
                .approvedAt(approval.getApprovedAt())
                .updatedAt(approval.getUpdatedAt());

        var status = approval.getStatus();

        if (status == ApprovalStatus.APPROVED) {
            ApprovalLine approvedLine = approval.getApprovalLines().stream()
                    .filter(line -> Boolean.TRUE.equals(line.getIsApproved()))
                    .max(Comparator.comparing(ApprovalLine::getApprovedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                    .orElse(null);

            builder.approvedBy(approvedLine != null ? approvedLine.getAccount().getName() : approval.getAccount().getName())
                    .approvedReason(approvedLine != null ? approvedLine.getComment() : null)
                    .approvedAt(approvedLine != null ? approvedLine.getApprovedAt() : approval.getApprovedAt());
        } else if (status == ApprovalStatus.REJECTED) {
            ApprovalLine rejectedLine = approval.getApprovalLines().stream()
                    .filter(line -> !Boolean.TRUE.equals(line.getIsApproved()))
                    .max(Comparator.comparing(ApprovalLine::getCreatedAt))
                    .orElse(null);

            builder.rejectedBy(rejectedLine != null ? rejectedLine.getAccount().getName() : null)
                    .rejectedReason(rejectedLine != null ? rejectedLine.getComment() : null)
                    .rejectedAt(rejectedLine != null ? rejectedLine.getApprovedAt() : approval.getUpdatedAt());
        } else if (status == ApprovalStatus.CANCELED) {
            ApprovalLine canceledLine = approval.getApprovalLines().stream()
                    .max(Comparator.comparing(ApprovalLine::getCreatedAt))
                    .orElse(null);

            builder.canceledBy(approval.getAccount().getName())
                    .canceledAt(approval.getUpdatedAt())
                    .canceledReason(canceledLine != null ? canceledLine.getComment() : null);
        }

        return builder.build();
    }

    private ApprovalDetailResponse toDetailDto(Approval approval, List<ApprovalLine> lines) {
        Account drafter = approval.getAccount();
        return ApprovalDetailResponse.builder()
                .id(approval.getId())
                .deploymentId(approval.getDeployment() != null ? approval.getDeployment().getId() : null)
                .drafterAccountId(drafter.getId())
                .drafterName(drafter.getName())
                .drafterDept(drafter.getDepartment().getKoreanName())
                .drafterRank(drafter.getPosition().getKoreanName())
                .title(approval.getTitle())
                .content(approval.getContent())
                .service(approval.getService())
                .type(approval.getType())
                .status(approval.getStatus())
                .isApproved(approval.getIsApproved())
                .createdAt(approval.getCreatedAt())
                .updatedAt(approval.getUpdatedAt())
                .lines(lines.stream().map(line -> {
                    Account acc = line.getAccount();
                    return ApprovalDetailResponse.ApprovalLineDto.builder()
                            .id(line.getId())
                            .accountId(acc.getId())
                            .accountName(acc.getName())
                            .deptName(acc.getDepartment().getKoreanName())
                            .rank(acc.getPosition().getKoreanName())
                            .type(line.getType())
                            .comment(line.getComment())
                            .build();
                }).collect(Collectors.toList()))
                .build();
    }

    private void saveRelatedProjects(Deployment deployment, List<Long> relatedProjectIds) {
        Project base = deployment.getProject();
        if (base == null) {
            throw new IllegalStateException("Deployment has no base project.");
        }

        relatedProjectRepository.deleteByProject(base);

        if (relatedProjectIds == null || relatedProjectIds.isEmpty()) return;

        List<Long> toLink = relatedProjectIds.stream()
                .filter(id -> id != null && !id.equals(base.getId()))
                .distinct()
                .collect(Collectors.toList());

        for (Long rpId : toLink) {
            Project related = projectRepository.findById(rpId)
                    .orElseThrow(() -> new IllegalArgumentException("Related project not found. id=" + rpId));

            if (relatedProjectRepository.existsByProjectAndRelatedProject(base, related)) {
                continue;
            }

            RelatedProject link = RelatedProject.builder()
                    .project(base)
                    .relatedProject(related)
                    .build();

            relatedProjectRepository.save(link);
        }
    }
}