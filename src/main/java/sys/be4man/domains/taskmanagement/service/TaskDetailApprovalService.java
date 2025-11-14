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

/**
 * 작업 상세 - 승인 정보 생성 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskDetailApprovalService {

    /**
     * 승인 정보 DTO 생성
     * - Approval 문서와 ApprovalLine (결재선)을 조합하여 생성
     */
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

        // 첫 번째 Approval 사용 (일반적으로 Deployment당 Stage당 하나의 Approval만 존재)
        Approval approval = approvals.get(0);

        // 현재 차례 승인자 ID 찾기 (nextApprover = 다음 결재자)
        Long currentApproverAccountId = approval.getNextApprover() != null
                ? approval.getNextApprover().getId()
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
}