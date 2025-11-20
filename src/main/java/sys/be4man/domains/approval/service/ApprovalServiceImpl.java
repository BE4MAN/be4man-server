package sys.be4man.domains.approval.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sys.be4man.domains.account.model.entity.Account;
import sys.be4man.domains.account.repository.AccountRepository;
import sys.be4man.domains.approval.dto.request.ApprovalCreateRequest;
import sys.be4man.domains.approval.dto.request.ApprovalDecisionRequest;
import sys.be4man.domains.approval.dto.request.ApprovalUpdateRequest;
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
import sys.be4man.domains.deployment.service.DeploymentScheduler;
import sys.be4man.domains.deployment.service.DeploymentService;
import sys.be4man.domains.project.model.entity.Project;
import sys.be4man.domains.project.model.entity.RelatedProject;
import sys.be4man.domains.project.repository.ProjectRepository;
import sys.be4man.domains.project.repository.RelatedProjectRepository;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ApprovalServiceImpl implements ApprovalService {

    private final ApprovalRepository approvalRepository;
    private final ApprovalLineRepository approvalLineRepository;
    private final AccountRepository accountRepository;
    private final DeploymentRepository deploymentRepository;
    private final DeploymentService deploymentService;
    private final ProjectRepository projectRepository;
    private final RelatedProjectRepository relatedProjectRepository;
    private final DeploymentScheduler deploymentScheduler;

    @Override
    @Transactional(readOnly = true)
    public List<ApprovalSummaryResponse> getApprovals(Long accountId, ApprovalStatus status) {
        List<Approval> approvals = (status == null)
                ? approvalRepository.findMyApprovals(accountId)
                : approvalRepository.findMyApprovalsByStatus(accountId, status);
        return approvals.stream().map(this::toSummaryDto).collect(Collectors.toList());
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
        if (approval.getDeployment() != null) {
            parseScheduleFromContent(approval.getContent()).ifPresent(s -> approval.getDeployment().updateSchedule(s.start, s.end));
        }
        return approval.getId();
    }

    @Override
    public void submit(Long approvalId) {
        Approval approval = getApprovalOrThrow(approvalId);
        if (approval.getDeployment() != null) {
            parseScheduleFromContent(approval.getContent()).ifPresent(s -> approval.getDeployment().updateSchedule(s.start, s.end));
        }
        if (ApprovalStatus.DRAFT.equals(approval.getStatus())) {
            Account drafter = approval.getAccount();
            LocalDateTime when = (approval.getCreatedAt() != null) ? approval.getCreatedAt() : LocalDateTime.now();
            ApprovalLine drafterLine = approval.getApprovalLines().stream()
                    .filter(line -> line.getAccount().getId().equals(drafter.getId()))
                    .findFirst()
                    .orElseGet(() -> {
                        ApprovalLine created = ApprovalLine.builder()
                                .approval(approval)
                                .account(drafter)
                                .type(ApprovalLineType.APPROVE)
                                .comment(null)
                                .approvedAt(null)
                                .isApproved(null)
                                .build();
                        approval.addApprovalLine(created);
                        return created;
                    });
            drafterLine.updateApproved(true);
            drafterLine.updateApprovedAt(when);
            approval.updateNextApprover(resolveInitialNextApprover(approval));
            approval.updateStatus(ApprovalStatus.PENDING);
            syncReportToLatestCompletedByPullRequest(approval);
            return;
        }
        if (approval.getNextApprover() == null) {
            approval.updateNextApprover(resolveInitialNextApprover(approval));
        }
        syncReportToLatestCompletedByPullRequest(approval);
        approval.updateStatus(ApprovalStatus.PENDING);
    }

    @Override
    public Long createAndSubmit(ApprovalCreateRequest request) {
        Approval approval = createApprovalEntity(request, ApprovalStatus.PENDING, false);
        approvalRepository.save(approval);
        createLines(request, approval);
        Account drafter = approval.getAccount();
        LocalDateTime createdAt = approval.getCreatedAt();
        approval.getApprovalLines().stream()
                .filter(line -> line.getAccount().getId().equals(drafter.getId()))
                .findFirst()
                .ifPresent(line -> {
                    line.updateApproved(true);
                    line.updateApprovedAt(createdAt);
                });
        approval.updateNextApprover(resolveInitialNextApprover(approval));
        if (approval.getDeployment() != null) {
            parseScheduleFromContent(approval.getContent()).ifPresent(s -> approval.getDeployment().updateSchedule(s.start, s.end));
        }
        syncReportToLatestCompletedByPullRequest(approval);
        return approval.getId();
    }

    @Override
    public void approve(Long approvalId, ApprovalDecisionRequest request) {
        Approval approval = getApprovalOrThrow(approvalId);
        ApprovalLine line = updateOrCreateLineComment(approval, request);
        line.approve();
        approvalLineRepository.save(line);
        approval.updateApprovedAt(line.getApprovedAt());
        Account next = findNextApproverAfter(approval, line.getAccount());

        if (next != null) {
            approval.updateNextApprover(next);
            approval.updateStatus(ApprovalStatus.PENDING);
        } else {
            approval.approve();
            approval.updateNextApprover(null);

            Deployment d = approval.getDeployment();
            if (d != null) {
                String typeName = (approval.getType() != null)
                        ? approval.getType().name()
                        : null;
                if (typeName != null && !"PLAN".equals(typeName)) {
                    d.updateStatus(DeploymentStatus.APPROVED);
                }

                parseScheduleFromContent(approval.getContent())
                        .ifPresent(s -> d.updateSchedule(s.start, s.end));
                LocalDateTime when = d.getScheduledAt();
                deploymentScheduler.scheduleStageFlipToDeployment(d.getId(), when);

                var pr = d.getPullRequest();
                if (pr == null) {
                    log.warn("PR_NULL depId={}", d.getId());
                    return;
                }
                String repoUrl = pr.getRepositoryUrl();
                String head = pr.getBranch();
                String[] ow = parseOwnerRepo(repoUrl);
                String owner = ow[0];
                String repo = ow[1];
                log.info("FINAL_APPROVE depId={}, when={}, owner={}, repo={}, head={}, repoUrl={}",
                        d.getId(), when, owner, repo, head, repoUrl);
                if (owner == null || repo == null || head == null) {
                    log.warn("MERGE_PARTS_MISSING depId={}", d.getId());
                    return;
                }
                deploymentScheduler.scheduleMergeIntoMain(
                        d.getId(),
                        owner,
                        repo,
                        head,
                        "[BE4MAN] Scheduled merge " + head + " -> main",
                        when
                );
            }
        }
    }


    @Override
    public void reject(Long approvalId, ApprovalDecisionRequest request) {
        Approval approval = getApprovalOrThrow(approvalId);
        ApprovalLine line = updateOrCreateLineComment(approval, request);
        line.reject();
        approvalLineRepository.save(line);
        approval.updateApprovedAt(line.getApprovedAt());
        approval.updateNextApprover(null);
        approval.reject();

        Deployment d = approval.getDeployment();
        if (d != null) {
            d.updateStatus(DeploymentStatus.REJECTED);
            deploymentScheduler.cancelAll(d.getId());
        }
    }

    @Override
    public void cancel(Long approvalId, ApprovalDecisionRequest request) {
        Approval approval = getApprovalOrThrow(approvalId);
        LocalDateTime now = LocalDateTime.now();
        approval.updateApprovedAt(now);
        approval.updateNextApprover(null);
        approval.updateStatus(ApprovalStatus.CANCELED);

        Deployment d = approval.getDeployment();
        if (d != null) {
            d.updateStatus(DeploymentStatus.CANCELED);
            deploymentScheduler.cancelAll(d.getId());
        }
    }

    @Override
    public void delete(Long approvalId) {
        Approval approval = getApprovalOrThrow(approvalId);
        if (approval.getStatus() != ApprovalStatus.DRAFT) throw new IllegalStateException("임시저장 문서만 삭제할 수 있습니다.");
        approvalLineRepository.deleteByApprovalId(approvalId);
        approvalRepository.delete(approval);
    }

    @Override
    public void update(Long approvalId, ApprovalUpdateRequest request) {
        Approval approval = getApprovalOrThrow(approvalId);
        if (approval.getStatus() == ApprovalStatus.APPROVED
                || approval.getStatus() == ApprovalStatus.REJECTED
                || approval.getStatus() == ApprovalStatus.CANCELED) {
            throw new IllegalStateException("해당 상태에서는 수정할 수 없습니다. (APPROVED/REJECTED/CANCELED)");
        }
        if (request.getTitle() != null) approval.updateTitle(request.getTitle());
        if (request.getContent() != null) approval.updateContent(request.getContent());
        if (request.getService() != null) approval.updateService(request.getService());
        if (request.getType() != null) approval.updateType(request.getType());
        if (request.getRelatedProjectIds() != null) {
            Deployment deployment = approval.getDeployment();
            if (deployment != null) {
                saveRelatedProjects(deployment, request.getRelatedProjectIds());
            }
        }
        boolean linesChanged = (request.getLines() != null) && isLinesChanged(approval, request);
        if (linesChanged) {
            approvalLineRepository.deleteByApprovalId(approvalId);
            approval.getApprovalLines().clear();
            replaceLinesFromUpdate(approval, request.getLines());
            approval.updateApprovedAt(null);
            approval.updateNextApprover(resolveInitialNextApprover(approval));
        }
        if (request.getContent() != null && approval.getDeployment() != null) {
            parseScheduleFromContent(request.getContent()).ifPresent(s -> {
                var dep = approval.getDeployment();
                dep.updateSchedule(s.start, s.end);
                if (approval.getStatus() == ApprovalStatus.APPROVED) {
                    deploymentScheduler.cancelStageFlip(dep.getId());
                    deploymentScheduler.scheduleStageFlipToDeployment(dep.getId(), dep.getScheduledAt());
                }
            });
        }
    }

    private Approval getApprovalOrThrow(Long id) {
        return approvalRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Approval not found. id=" + id));
    }

    private Approval createApprovalEntity(ApprovalCreateRequest request, ApprovalStatus status, boolean isApproved) {
        Account drafter = accountRepository.findById(request.getDrafterAccountId())
                .orElseThrow(() -> new IllegalArgumentException("Account not found."));

        String typeName = (request.getType() != null) ? request.getType().name() : null;
        DeploymentStage stageForDeployment = DeploymentStage.PLAN;
        if (typeName != null && !"REPORT".equals(typeName)) {
            try {
                stageForDeployment = DeploymentStage.valueOf(typeName);
            } catch (IllegalArgumentException e) {
                log.warn("Unknown type for stage mapping: {} -> fallback PLAN", typeName);
            }
        }

        Deployment deployment;
        Long deploymentId = request.getDeploymentId();
        if (deploymentId != null && deploymentId > 0) {
            deployment = deploymentRepository.findById(deploymentId)
                    .orElseThrow(() -> new IllegalArgumentException("Deployment not found id=" + deploymentId));
            if (request.getRelatedProjectIds() != null) saveRelatedProjects(deployment, request.getRelatedProjectIds());

            if (typeName != null && !"REPORT".equals(typeName)) {
                deployment.updateStage(stageForDeployment);
            }
        } else {
            DeploymentCreateRequest req = DeploymentCreateRequest.builder()
                    .projectId(request.getProjectId())
                    .issuerId(request.getDrafterAccountId())
                    .pullRequestId(request.getPullRequestId())
                    .title(request.getTitle())
                    .stage(stageForDeployment.name())
                    .status(DeploymentStatus.PENDING.name())
                    .expectedDuration("0")
                    .version(null)
                    .content(request.getContent())
                    .build();
            DeploymentResponse created = deploymentService.createDeployment(req);
            deployment = deploymentRepository.getReferenceById(created.getId());
            if (request.getRelatedProjectIds() != null) saveRelatedProjects(deployment, request.getRelatedProjectIds());
        }

        parseScheduleFromContent(request.getContent()).ifPresent(s -> deployment.updateSchedule(s.start, s.end));

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
        if (request.getLines() == null || request.getLines().isEmpty()) return;
        for (ApprovalCreateRequest.ApprovalLineRequest lineReq : request.getLines()) {
            if (lineReq.getAccountId() == null) continue;
            Account account = accountRepository.findById(lineReq.getAccountId()).orElseThrow(() -> new IllegalArgumentException("Account not found."));
            ApprovalLine line = ApprovalLine.builder()
                    .approval(approval)
                    .account(account)
                    .type(lineReq.getType())
                    .comment(lineReq.getComment())
                    .approvedAt(null)
                    .isApproved(null)
                    .build();
            approval.addApprovalLine(line);
        }
    }

    private ApprovalLine updateOrCreateLineComment(Approval approval, ApprovalDecisionRequest request) {
        Account approver = accountRepository.findById(request.getApproverAccountId()).orElseThrow(() -> new IllegalArgumentException("Account not found"));
        return approval.getApprovalLines().stream()
                .filter(l -> l.getAccount().getId().equals(approver.getId()))
                .findFirst()
                .map(line -> {
                    if (request.getComment() != null) line.updateComment(request.getComment());
                    return line;
                })
                .orElseGet(() -> {
                    ApprovalLine line = ApprovalLine.builder()
                            .approval(approval)
                            .account(approver)
                            .type(ApprovalLineType.APPROVE)
                            .comment(request.getComment())
                            .build();
                    approval.addApprovalLine(line);
                    return line;
                });
    }

    private Account resolveInitialNextApprover(Approval approval) {
        Long drafterId = approval.getAccount().getId();
        return approval.getApprovalLines().stream()
                .filter(line -> (line.getType() == ApprovalLineType.APPROVE || line.getType() == ApprovalLineType.CONSENT) && !line.getAccount().getId().equals(drafterId))
                .sorted(Comparator.comparing(ApprovalLine::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(ApprovalLine::getAccount)
                .findFirst()
                .orElse(null);
    }

    private Account findNextApproverAfter(Approval approval, Account currentApprover) {
        boolean found = false;
        for (ApprovalLine line : approval.getApprovalLines()) {
            if (line.getType() != ApprovalLineType.APPROVE && line.getType() != ApprovalLineType.CONSENT) continue;
            if (!found) {
                if (line.getAccount().getId().equals(currentApprover.getId())) found = true;
                continue;
            }
            return line.getAccount();
        }
        return null;
    }

    private ApprovalSummaryResponse toSummaryDto(Approval approval) {
        ApprovalStatus status = approval.getStatus();
        String approvedBy = null, approvedReason = null;
        String rejectedBy = null, rejectedReason = null;
        String canceledBy = null, canceledReason = null;
        LocalDateTime approvedAt = null;
        LocalDateTime rejectedAt = null;
        LocalDateTime canceledAt = null;
        if (status == ApprovalStatus.APPROVED) {
            ApprovalLine line = approval.getApprovalLines().stream()
                    .filter(l -> Boolean.TRUE.equals(l.getIsApproved()))
                    .max(Comparator.comparing(ApprovalLine::getApprovedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                    .orElse(null);
            approvedBy = (line != null) ? line.getAccount().getName() : approval.getAccount().getName();
            approvedReason = (line != null) ? line.getComment() : null;
            approvedAt = (line != null && line.getApprovedAt() != null) ? line.getApprovedAt() : approval.getApprovedAt();
        } else if (status == ApprovalStatus.REJECTED) {
            ApprovalLine line = approval.getApprovalLines().stream()
                    .filter(l -> Boolean.FALSE.equals(l.getIsApproved()))
                    .max(Comparator.comparing(ApprovalLine::getApprovedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                    .orElse(null);
            rejectedBy = (line != null) ? line.getAccount().getName() : null;
            rejectedReason = (line != null) ? line.getComment() : null;
            rejectedAt = (line != null && line.getApprovedAt() != null) ? line.getApprovedAt() : approval.getApprovedAt();
        } else if (status == ApprovalStatus.CANCELED) {
            canceledBy = approval.getAccount().getName();
            canceledReason = null;
            canceledAt = approval.getApprovedAt();
        }
        return ApprovalSummaryResponse.builder()
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
                .approvedAt(approvedAt)
                .rejectedAt(rejectedAt)
                .canceledAt(canceledAt)
                .updatedAt(approval.getUpdatedAt())
                .approvedBy(approvedBy)
                .approvedReason(approvedReason)
                .rejectedBy(rejectedBy)
                .rejectedReason(rejectedReason)
                .canceledBy(canceledBy)
                .canceledReason(canceledReason)
                .build();
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
                .lines(
                        lines.stream()
                                .sorted(Comparator.comparing(ApprovalLine::getId))
                                .map(line -> {
                                    String statusLabel;
                                    if (line.getIsApproved() == null) statusLabel = "대기";
                                    else if (line.getIsApproved()) statusLabel = "승인";
                                    else statusLabel = "반려";
                                    return ApprovalDetailResponse.ApprovalLineDto.builder()
                                            .id(line.getId())
                                            .accountId(line.getAccount().getId())
                                            .accountName(line.getAccount().getName())
                                            .deptName(line.getAccount().getDepartment().getKoreanName())
                                            .rank(line.getAccount().getPosition().getKoreanName())
                                            .type(line.getType())
                                            .comment(line.getComment())
                                            .approvedAt(line.getApprovedAt())
                                            .statusLabel(statusLabel)
                                            .build();
                                })
                                .collect(Collectors.toList())
                )
                .build();
    }

    private void saveRelatedProjects(Deployment deployment, List<Long> relatedProjectIds) {
        Project base = deployment.getProject();
        if (base == null) throw new IllegalStateException("Deployment has no base project.");
        relatedProjectRepository.deleteByProject(base);
        if (relatedProjectIds == null || relatedProjectIds.isEmpty()) return;
        List<Long> clean = relatedProjectIds.stream().filter(id -> id != null && !id.equals(base.getId())).distinct().collect(Collectors.toList());
        for (Long rpId : clean) {
            Project related = projectRepository.findById(rpId).orElseThrow(() -> new IllegalArgumentException("Related project not found"));
            if (!relatedProjectRepository.existsByProjectAndRelatedProject(base, related)) {
                RelatedProject link = RelatedProject.builder().project(base).relatedProject(related).build();
                relatedProjectRepository.save(link);
            }
        }
    }

    private boolean isLinesChanged(Approval approval, ApprovalUpdateRequest req) {
        List<ApprovalLine> oldLines = approval.getApprovalLines().stream().sorted(Comparator.comparing(ApprovalLine::getId)).collect(Collectors.toList());
        List<ApprovalUpdateRequest.ApprovalLineRequest> newLines = req.getLines();
        if (newLines == null) return false;
        if (oldLines.size() != newLines.size()) return true;
        for (int i = 0; i < oldLines.size(); i++) {
            ApprovalLine o = oldLines.get(i);
            ApprovalUpdateRequest.ApprovalLineRequest n = newLines.get(i);
            Long oAcc = (o.getAccount() != null) ? o.getAccount().getId() : null;
            Long nAcc = n.getAccountId();
            if (!safeEq(oAcc, nAcc)) return true;
            if (!safeEq(o.getType(), n.getType())) return true;
            String oc = (o.getComment() == null) ? "" : o.getComment();
            String nc = (n.getComment() == null) ? "" : n.getComment();
            if (!oc.equals(nc)) return true;
        }
        return false;
    }

    private static boolean safeEq(Object a, Object b) {
        return (a == null) ? (b == null) : a.equals(b);
    }

    private void replaceLinesFromUpdate(Approval approval, List<ApprovalUpdateRequest.ApprovalLineRequest> lineReqs) {
        if (lineReqs == null || lineReqs.isEmpty()) return;
        for (ApprovalUpdateRequest.ApprovalLineRequest lr : lineReqs) {
            if (lr.getAccountId() == null) continue;
            Account account = accountRepository.findById(lr.getAccountId()).orElseThrow(() -> new IllegalArgumentException("Account not found."));
            ApprovalLine line = ApprovalLine.builder()
                    .approval(approval)
                    .account(account)
                    .type(lr.getType())
                    .comment(lr.getComment())
                    .approvedAt(null)
                    .isApproved(null)
                    .build();
            approval.addApprovalLine(line);
        }
    }

    private static class ScheduleRange {
        final LocalDateTime start;
        final LocalDateTime end;
        final long minutes;
        ScheduleRange(LocalDateTime s, LocalDateTime e) { this.start = s; this.end = e; this.minutes = Duration.between(s, e).toMinutes(); }
    }

    private Optional<ScheduleRange> parseScheduleFromContent(String content) {
        if (content == null || content.isBlank()) return Optional.empty();
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
                .trim();
        text = text.replaceAll("\\s+", " ");
        Pattern p = Pattern.compile("(20\\d{2}-\\d{2}-\\d{2})\\s+(\\d{2}:\\d{2})\\s*[~\\-–—]\\s*(20\\d{2}-\\d{2}-\\d{2})\\s+(\\d{2}:\\d{2})");
        Matcher m = p.matcher(text);
        if (!m.find()) return Optional.empty();
        String d1 = m.group(1) + " " + m.group(2);
        String d2 = m.group(3) + " " + m.group(4);
        DateTimeFormatter F = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm").withResolverStyle(ResolverStyle.STRICT);
        try {
            LocalDateTime start = LocalDateTime.parse(d1, F);
            LocalDateTime end = LocalDateTime.parse(d2, F);
            if (end.isBefore(start)) return Optional.empty();
            return Optional.of(new ScheduleRange(start, end));
        } catch (Exception e) {
            log.debug("SCHEDULE_PARSE_FAILED {}", e.getMessage());
            return Optional.empty();
        }
    }

    private static boolean isNotBlank(String s) { return s != null && !s.isBlank(); }

    @SafeVarargs
    private static <T> T firstNonNull(T... vals) {
        for (T v : vals) if (v != null) return v;
        return null;
    }
    private static String firstNonBlank(String... vals) {
        for (String v : vals) if (isNotBlank(v)) return v;
        return null;
    }
    private static <T> T safe(java.util.concurrent.Callable<T> c) {
        try { return c.call(); } catch (Exception e) { return null; }
    }

    private String[] parseOwnerRepo(String repoUrl) {
        if (!isNotBlank(repoUrl)) return new String[]{null, null};
        String s = repoUrl.replace(".git", "");
        int i = s.indexOf("github.com/");
        if (i >= 0) {
            String[] parts = s.substring(i + "github.com/".length()).split("/");
            if (parts.length >= 2) return new String[]{parts[0], parts[1]};
        }
        i = s.indexOf("github.com:");
        if (i >= 0) {
            String[] parts = s.substring(i + "github.com:".length()).split("/");
            if (parts.length >= 2) return new String[]{parts[0], parts[1]};
        }
        return new String[]{null, null};
    }

    private void syncReportToLatestCompletedByPullRequest(Approval approval) {
        if (approval == null || approval.getType() == null) return;
        if (!"REPORT".equals(approval.getType().name())) return;

        Deployment dep = approval.getDeployment();
        if (dep == null || dep.getPullRequest() == null) {
            log.warn("REPORT_SYNC_SKIP: deployment or pullRequest is null (approvalId={})", approval.getId());
            return;
        }

        Long prId = dep.getPullRequest().getId();

        deploymentRepository
                .findTopByPullRequest_IdAndStatusOrderByCreatedAtDesc(prId, DeploymentStatus.COMPLETED)
                .ifPresentOrElse(latest -> {
                    latest.updateStage(DeploymentStage.REPORT);
                    latest.updateStatus(DeploymentStatus.PENDING);
                    deploymentRepository.save(latest);
                    log.info("REPORT_SYNC done: prId={}, targetDepId={}", prId, latest.getId());
                }, () -> {
                    log.info("REPORT_SYNC skipped: no COMPLETED deployment for prId={}", prId);
                });
    }
}
