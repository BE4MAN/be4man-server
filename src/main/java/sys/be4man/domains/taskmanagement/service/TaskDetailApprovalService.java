package sys.be4man.domains.taskmanagement.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import sys.be4man.domains.approval.model.entity.Approval;
import sys.be4man.domains.approval.model.entity.ApprovalLine;
import sys.be4man.domains.approval.model.type.ApprovalLineType;
import sys.be4man.domains.deployment.model.type.DeploymentStage;
import sys.be4man.domains.taskmanagement.dto.ApprovalInfoDto;
import sys.be4man.domains.taskmanagement.dto.ApproverDto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskDetailApprovalService {

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");

    public ApprovalInfoDto buildApprovalInfo(List<Approval> approvals, DeploymentStage stage) {
        log.debug("승인 정보 생성 - approvals.size: {}, stage: {}", approvals.size(), stage);

        if (approvals.isEmpty()) {
            return ApprovalInfoDto.builder()
                    .approvalId(null)
                    .approvalStage(stage == DeploymentStage.PLAN ? "계획서" : "결과보고")
                    .totalApprovers(0)
                    .current_approver_account_id(null)
                    .approvers(new ArrayList<>())
                    .build();
        }

        Approval approval = approvals.get(0);
        List<ApproverDto> approvers = new ArrayList<>();

        // ✅ 기안자 ID 가져오기
        Long drafterAccountId = approval.getAccount() != null ? approval.getAccount().getId() : null;
        log.debug("기안자 account_id: {}", drafterAccountId);

        // ✅ ApprovalLine을 id 기준으로 정렬 (생성 순서)
        List<ApprovalLine> sortedLines = approval.getApprovalLines().stream()
                .filter(line -> line.getType() != ApprovalLineType.CC)  // 참조자 제외
                .filter(line -> {
                    // ✅ 기안자 제외 (approval_line의 account_id와 approval의 drafter account_id가 같으면 제외)
                    Long lineAccountId = line.getAccount() != null ? line.getAccount().getId() : null;
                    boolean isDrafter = lineAccountId != null && lineAccountId.equals(drafterAccountId);
                    if (isDrafter) {
                        log.debug("기안자를 승인자 목록에서 제외 - account_id: {}, name: {}",
                                lineAccountId, line.getAccount().getName());
                    }
                    return !isDrafter;
                })
                .sorted(Comparator.comparing(ApprovalLine::getId))      // ✅ id 기준 정렬
                .toList();

        log.debug("정렬된 ApprovalLine 개수 (기안자 제외): {}", sortedLines.size());

        // ✅ 현재 차례 승인자의 account_id 찾기
        Long currentApproverAccountId = null;
        for (ApprovalLine line : sortedLines) {
            if (line.getIsApproved() == null) {
                currentApproverAccountId = line.getAccount() != null ? line.getAccount().getId() : null;
                log.debug("현재 차례 승인자 account_id: {}", currentApproverAccountId);
                break;
            }
        }

        // ✅ 승인자 정보 생성
        for (ApprovalLine line : sortedLines) {
            Long approverAccountId = line.getAccount() != null ? line.getAccount().getId() : null;

            // 승인 상태 결정
            String approvalStatus;
            if (line.getIsApproved() == null) {
                approvalStatus = "대기중";
            } else if (line.getIsApproved()) {
                approvalStatus = "승인";
            } else {
                approvalStatus = "반려";
            }

            log.debug("ApprovalLine - id: {}, account_id: {}, name: {}, isApproved: {}, status: {}",
                    line.getId(),
                    approverAccountId,
                    line.getAccount() != null ? line.getAccount().getName() : "null",
                    line.getIsApproved(),
                    approvalStatus);

            // ✅ 현재 차례인지 확인
            boolean isCurrentTurn = approverAccountId != null &&
                    approverAccountId.equals(currentApproverAccountId);

            ApproverDto approver = ApproverDto.builder()
                    .approverId(approverAccountId)
                    .approverName(line.getAccount() != null ? line.getAccount().getName() : "알 수 없음")
                    .approverDepartment(line.getAccount() != null && line.getAccount().getDepartment() != null
                            ? line.getAccount().getDepartment().getKoreanName()
                            : "부서 없음")
                    .current_approver_account_id(currentApproverAccountId)
                    .approvalStatus(approvalStatus)
                    .processedAt(line.getApprovedAt() != null ? formatDateTime(line.getApprovedAt()) : null)
                    .comment(line.getComment())
                    .isCurrentTurn(isCurrentTurn)
                    .build();

            approvers.add(approver);
        }

        log.debug("생성된 승인자 정보: {}", approvers.size());

        return ApprovalInfoDto.builder()
                .approvalId(approval.getId())
                .approvalStage(stage == DeploymentStage.PLAN ? "계획서" : "결과보고")
                .totalApprovers(approvers.size())
                .current_approver_account_id(currentApproverAccountId)
                .approvers(approvers)
                .build();
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATETIME_FORMATTER) : null;
    }
}