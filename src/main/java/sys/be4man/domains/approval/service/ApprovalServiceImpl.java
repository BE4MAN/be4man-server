package sys.be4man.domains.approval.service;

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
        List<Approval> approvals;

        if (status == null) {
            approvals = approvalRepository.findByAccountId(accountId);
        } else {
            approvals = approvalRepository.findByAccountIdAndStatus(accountId, status);
        }

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

        Account nextApprover = resolveInitialNextApprover(approval);
        approval.updateNextApprover(nextApprover);

        return approval.getId();
    }

    @Override
    public void submit(Long approvalId) {
        Approval approval = getApprovalOrThrow(approvalId);

        if (approval.getNextApprover() == null) {
            Account nextApprover = resolveInitialNextApprover(approval);
            approval.updateNextApprover(nextApprover);
        }

        approval.updateStatus(ApprovalStatus.PENDING);
    }

    @Override
    public Long createAndSubmit(ApprovalCreateRequest request) {
        Approval approval = createApprovalEntity(request, ApprovalStatus.PENDING, false);
        approvalRepository.save(approval);
        createLines(request, approval);

        Account nextApprover = resolveInitialNextApprover(approval);
        approval.updateNextApprover(nextApprover);

        return approval.getId();
    }

    @Override
    public void cancel(Long approvalId) {
        Approval approval = getApprovalOrThrow(approvalId);
        approval.updateStatus(ApprovalStatus.CANCELED);
    }

    @Override
    public void approve(Long approvalId, ApprovalDecisionRequest request) {
        Approval approval = getApprovalOrThrow(approvalId);

        Account approver = accountRepository.findById(request.getApproverAccountId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Account not found. id=" + request.getApproverAccountId()));

        updateOrCreateLineComment(approval, request, true);

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
        updateOrCreateLineComment(approval, request, false);
        approval.reject();
    }

    private Approval getApprovalOrThrow(Long id) {
        return approvalRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Approval not found. id=" + id));
    }

    private Approval createApprovalEntity(
            ApprovalCreateRequest request,
            ApprovalStatus status,
            boolean isApproved
    ) {
        Account drafter = accountRepository.findById(request.getDrafterAccountId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Account not found. id=" + request.getDrafterAccountId()));

        Deployment deployment;
        Long deploymentId = request.getDeploymentId();

        if (deploymentId != null && deploymentId > 0) {
            deployment = deploymentRepository.findById(deploymentId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Deployment not found. id=" + deploymentId));

            if (request.getRelatedProjectIds() != null && !request.getRelatedProjectIds().isEmpty()) {
                saveRelatedProjects(deployment, request.getRelatedProjectIds());
            }
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

            if (request.getRelatedProjectIds() != null && !request.getRelatedProjectIds().isEmpty()) {
                saveRelatedProjects(deployment, request.getRelatedProjectIds());
            }
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
        if (request.getLines() == null) {
            return;
        }

        request.getLines().forEach(lineReq -> {
            Account account = accountRepository.findById(lineReq.getAccountId())
                    .orElseThrow(() -> new IllegalArgumentException("Account not found. id=" + lineReq.getAccountId()));

            ApprovalLine line = ApprovalLine.builder()
                    .approval(approval)
                    .account(account)
                    .type(lineReq.getType())
                    .comment(lineReq.getComment() == null ? "" : lineReq.getComment())
                    .build();

            approval.addApprovalLine(line);
        });
    }

    private void updateOrCreateLineComment(Approval approval,
            ApprovalDecisionRequest request,
            boolean approve) {

        Account approver = accountRepository.findById(request.getApproverAccountId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Account not found. id=" + request.getApproverAccountId()));

        ApprovalLine target = approval.getApprovalLines().stream()
                .filter(line -> line.getAccount().getId().equals(approver.getId()))
                .findFirst()
                .orElse(null);

        if (target == null) {
            ApprovalLineType lineType = ApprovalLineType.APPROVE;

            ApprovalLine line = ApprovalLine.builder()
                    .approval(approval)
                    .account(approver)
                    .type(lineType)
                    .comment(request.getComment() == null ? "" : request.getComment())
                    .build();

            approval.addApprovalLine(line);
        } else {
            target.updateComment(request.getComment());
        }
    }

    private Account resolveInitialNextApprover(Approval approval) {
        return approval.getApprovalLines().stream()
                .filter(line -> line.getType() == ApprovalLineType.APPROVE
                        || line.getType() == ApprovalLineType.CONSENT)
                .map(ApprovalLine::getAccount)
                .findFirst()
                .orElse(null);
    }

    private Account findNextApproverAfter(Approval approval, Account currentApprover) {
        boolean foundCurrent = false;

        for (ApprovalLine line : approval.getApprovalLines()) {
            if (line.getType() != ApprovalLineType.APPROVE
                    && line.getType() != ApprovalLineType.CONSENT) {
                continue;
            }

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

    private ApprovalSummaryResponse toSummaryDto(Approval approval) {
        return ApprovalSummaryResponse.builder()
                .id(approval.getId())
                .title(approval.getTitle())
                .service(approval.getService())
                .type(approval.getType())
                .status(approval.getStatus())
                .isApproved(approval.getIsApproved())
                .drafterAccountId(approval.getAccount().getId())
                .createdAt(approval.getCreatedAt())
                .build();
    }

    private ApprovalDetailResponse toDetailDto(Approval approval, List<ApprovalLine> lines) {
        return ApprovalDetailResponse.builder()
                .id(approval.getId())
                .deploymentId(approval.getDeployment() != null ? approval.getDeployment().getId() : null)
                .drafterAccountId(approval.getAccount().getId())
                .title(approval.getTitle())
                .content(approval.getContent())
                .service(approval.getService())
                .type(approval.getType())
                .status(approval.getStatus())
                .isApproved(approval.getIsApproved())
                .createdAt(approval.getCreatedAt())
                .updatedAt(approval.getUpdatedAt())
                .lines(lines.stream()
                        .map(line -> ApprovalDetailResponse.ApprovalLineDto.builder()
                                .id(line.getId())
                                .accountId(line.getAccount().getId())
                                .type(line.getType())
                                .comment(line.getComment())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }

    private void saveRelatedProjects(Deployment deployment, List<Long> relatedProjectIds) {
        relatedProjectIds.forEach(projectId -> {
            var project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Related project not found. id=" + projectId));

            var related = RelatedProject.builder()
                    .deployment(deployment)
                    .project(project)
                    .build();

            relatedProjectRepository.save(related);
        });
    }
}
