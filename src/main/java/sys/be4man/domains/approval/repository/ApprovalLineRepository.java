package sys.be4man.domains.approval.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import sys.be4man.domains.approval.model.entity.ApprovalLine;

public interface ApprovalLineRepository extends JpaRepository<ApprovalLine, Long> {
    List<ApprovalLine> findByApprovalId(Long approvalId);
}
