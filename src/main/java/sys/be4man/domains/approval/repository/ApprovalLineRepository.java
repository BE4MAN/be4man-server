// 작성자 : 이원석
package sys.be4man.domains.approval.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sys.be4man.domains.approval.model.entity.ApprovalLine;

public interface ApprovalLineRepository extends JpaRepository<ApprovalLine, Long>, ApprovalLineRepositoryCustom {
    List<ApprovalLine> findByApprovalId(Long approvalId);

    /**
     * Approval ID 목록으로 ApprovalLine 조회
     */
    List<ApprovalLine> findByApprovalIdIn(List<Long> approvalIds);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from ApprovalLine l where l.approval.id = :approvalId")
    void deleteByApprovalId(@Param("approvalId") Long approvalId);
}
