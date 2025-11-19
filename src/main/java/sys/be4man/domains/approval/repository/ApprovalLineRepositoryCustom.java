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
}

