package sys.be4man.domains.approval.service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
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

        Account next = findNextApproverAfter(approval, approver);
        if (next != null) {
            approval.updateNextApprover(next);
            approval.updateStatus(ApprovalStatus.PENDING);
        } else {
            approval.approve();
        }
    }

    @Override
    public void reject(Long approvalId, ApprovalDecisionRequest request) {
        Approval approval = getApprovalOrThrow(approvalId);
        ApprovalLine line = updateOrCreateLineComment(approval, request, false);
        line.reject();
        approval.reject();
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

    private ApprovalLine findLastDecisionLine(Approval approval) {
        return approval.getApprovalLines().stream()
                .filter(line -> line.getType() == ApprovalLineType.APPROVE || line.getType() == ApprovalLineType.CONSENT)
                .max(Comparator.comparing(line ->
                        line.getApprovedAt() != null ? line.getApprovedAt() : line.getApproval().getCreatedAt()))
                .orElse(null);
    }
}
