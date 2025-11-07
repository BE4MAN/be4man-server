package sys.be4man.domains.approval.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import sys.be4man.domains.approval.model.entity.ApprovalLine;

import java.util.List;

@Repository
public interface ApprovalLineRepository extends JpaRepository<ApprovalLine, Long> {

    /**
     * 특정 승인 문서의 결재선 목록 조회 (ID 순서대로)
     */
    List<ApprovalLine> findByApprovalIdOrderByIdAsc(Long approvalId);

    /**
     * 특정 승인 문서의 결재선 목록 조회
     */
    List<ApprovalLine> findByApprovalId(Long approvalId);

    /**
     * 특정 승인 문서의 결재선 목록 조회 (Account Join Fetch)
     */
    @Query("SELECT al FROM ApprovalLine al " +
           "JOIN FETCH al.account " +
           "WHERE al.approval.id = :approvalId " +
           "ORDER BY al.id ASC")
    List<ApprovalLine> findByApprovalIdWithAccount(@Param("approvalId") Long approvalId);
}