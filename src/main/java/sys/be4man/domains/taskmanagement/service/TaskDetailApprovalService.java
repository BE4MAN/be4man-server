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

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskDetailApprovalService {

    public ApprovalInfoDto buildApprovalInfo(List<Approval> approvals, DeploymentStage stage) {
        log.debug("승인 정보 생성 - stage: {}, approvals: {}", stage, approvals.size());

        if (approvals.isEmpty()) {
            return ApprovalInfoDto.builder()
                    .approvalStage(stage.getKoreanName())
                    .totalApprovers(0)
                    .current_approver_account_id(null)
                    .approvers(new ArrayList<>())
                    .build();
        }

        Approval approval = approvals.get(0);

        // ✅ 반려 여부 확인
        boolean isRejected = approval.getApprovalLines().stream()
                .filter(line -> line.getType() != ApprovalLineType.CC)
                .anyMatch(line -> {
                    Boolean isApproved = line.getIsApproved();
                    return isApproved != null && !isApproved;
                });

        // ✅ 반려 상태일 경우 current_approver_account_id는 null
        Long currentApproverAccountId;
        if (!isRejected && approval.getNextApprover() != null) {
            currentApproverAccountId = approval.getNextApprover().getId();
        } else {
            currentApproverAccountId = null;
        }

        List<ApproverDto> approvers = approval.getApprovalLines().stream()
                .filter(line -> line.getType() != ApprovalLineType.CC)
                .sorted((a, b) -> a.getId().compareTo(b.getId()))
                .map(line -> ApproverDto.builder()
                        .approverId(line.getAccount().getId())
                        .approverName(line.getAccount().getName())
                        .approverDepartment(line.getAccount().getDepartment() != null ?
                                line.getAccount().getDepartment().getKoreanName() : null)
                        .current_approver_account_id(line.getAccount().getId())
                        .approvalStatus(getApprovalLineStatus(line))
                        .processedAt(null)
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

    private String getApprovalLineStatus(ApprovalLine line) {
        Boolean isApproved = line.getIsApproved();
        if (isApproved == null) {
            return "대기중";
        }
        return isApproved ? "승인" : "반려";
    }
}