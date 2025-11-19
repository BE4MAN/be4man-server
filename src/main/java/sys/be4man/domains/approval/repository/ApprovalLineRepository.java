package sys.be4man.domains.approval.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sys.be4man.domains.approval.model.entity.ApprovalLine;

public interface ApprovalLineRepository extends JpaRepository<ApprovalLine, Long> {
    List<ApprovalLine> findByApprovalId(Long approvalId);

    /**
     * Approval ID 목록으로 ApprovalLine 조회
     */
    List<ApprovalLine> findByApprovalIdIn(List<Long> approvalIds);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from ApprovalLine l where l.approval.id = :approvalId")
    void deleteByApprovalId(@Param("approvalId") Long approvalId);

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
    @Query("""
        SELECT DISTINCT al
        FROM ApprovalLine al
        JOIN FETCH al.approval a
        JOIN FETCH a.deployment d
        JOIN FETCH d.project p
        JOIN FETCH d.issuer issuer
        WHERE al.account.id = :accountId
          AND al.isApproved IS NULL
          AND d.status IN ('PENDING', 'APPROVED')
          AND a.isDeleted = false
          AND d.isDeleted = false
        ORDER BY a.createdAt DESC
        """)
    List<ApprovalLine> findPendingApprovalsByAccountId(@Param("accountId") Long accountId);
}
