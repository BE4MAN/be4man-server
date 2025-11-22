package sys.be4man.domains.approval.repository;

import java.util.List;
import sys.be4man.domains.approval.model.entity.ApprovalLine;
import sys.be4man.domains.deployment.model.entity.Deployment;

public interface ApprovalLineRepositoryCustom {

    /**
     * 승인 대기 목록 조회
     * - 현재 사용자가 승인 라인에 포함되어 있고
     * - is_approved가 NULL인 항목
     * - deployment.status가 PENDING 또는 APPROVED인 항목
     * - 최신순 정렬 (approval.createdAt DESC)
     *
     * @param accountId 현재 사용자 ID
     * @return 승인 대기 ApprovalLine 목록
     */
    List<ApprovalLine> findPendingApprovalsByAccountId(Long accountId);

    /**
     * 진행중인 업무 목록 조회
     * - 현재 사용자가 승인한 항목 (is_approved = true)
     * - 다음 stage-status 조합 중 하나:
     *   - PLAN + APPROVED
     *   - DEPLOYMENT + PENDING
     * - 최신순 정렬 (deployment.updatedAt DESC)
     *
     * @param accountId 현재 사용자 ID
     * @return 진행중인 업무 Deployment 목록
     */
    List<Deployment> findInProgressTasksByAccountId(Long accountId);

    /**
     * 취소 알림 목록 조회
     * - deployment.status = 'CANCELED'
     * - 현재 사용자가 approval_line에서 승인한 deployment (is_approved = true)
     * - 최신순 정렬 (deployment.updatedAt DESC)
     *
     * @param accountId 현재 사용자 ID
     * @return 취소된 Deployment 목록
     */
    List<Deployment> findCanceledNotificationsByAccountId(Long accountId);

    /**
     * 반려 알림 목록 조회 (케이스 1: 현재 사용자가 요청한 deployment가 반려된 경우)
     * - deployment.status = 'REJECTED'
     * - deployment.issuer.id = current_user_id
     * - 최신순 정렬 (deployment.updatedAt DESC)
     *
     * @param accountId 현재 사용자 ID
     * @return 반려된 Deployment 목록
     */
    List<Deployment> findRejectedDeploymentsByIssuerId(Long accountId);

    /**
     * 반려 알림 목록 조회 (케이스 3: 현재 사용자가 승인한 approval이 반려된 경우)
     * - deployment.status = 'REJECTED' 또는 approval.status = 'REJECTED'
     * - approval_line에서 현재 사용자가 승인한 항목 (is_approved = true)
     * - 해당 approval의 approval_lines 중 is_approved = false인 항목이 존재
     * - 최신순 정렬 (deployment.updatedAt DESC 또는 approval.updatedAt DESC)
     *
     * @param accountId 현재 사용자 ID
     * @return 반려된 Deployment 목록
     */
    List<Deployment> findRejectedApprovalsByApproverId(Long accountId);
}

