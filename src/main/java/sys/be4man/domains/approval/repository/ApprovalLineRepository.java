package sys.be4man.domains.approval.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sys.be4man.domains.approval.model.entity.ApprovalLine;

public interface ApprovalLineRepository extends JpaRepository<ApprovalLine, Long> {
    List<ApprovalLine> findByApprovalId(Long approvalId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from ApprovalLine l where l.approval.id = :approvalId")
    void deleteByApprovalId(@Param("approvalId") Long approvalId);
}
