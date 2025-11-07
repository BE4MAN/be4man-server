package sys.be4man.domains.approval.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sys.be4man.domains.approval.model.entity.Approval;
import sys.be4man.domains.deployment.model.type.DeploymentStage;

import java.util.List;

public interface ApprovalRepository extends JpaRepository<Approval, Long> {

    /**
     * 특정 deployment의 특정 단계 승인 목록 조회 (승인 순서대로 정렬)
     */
    @Query("SELECT a FROM Approval a " +
           "JOIN FETCH a.reviewer " +
           "WHERE a.deployment.id = :deploymentId " +
           "AND a.approvalStage = :approvalStage " +
           "AND a.isDeleted = false " +
           "ORDER BY a.current_approver_account_id ASC")
    List<Approval> findByDeploymentIdAndApprovalStageOrderByApprovalOrderAsc(
        @Param("deploymentId") Long deploymentId,
        @Param("approvalStage") DeploymentStage approvalStage
    );

    /**
     * 특정 deployment의 모든 승인 목록 조회 (단계별, 순서별 정렬)
     */
    @Query("SELECT a FROM Approval a " +
           "JOIN FETCH a.reviewer " +
           "WHERE a.deployment.id = :deploymentId " +
           "AND a.isDeleted = false " +
           "ORDER BY a.approvalStage ASC, a.current_approver_account_id ASC")
    List<Approval> findByDeploymentIdOrderByApprovalStageAndOrder(
        @Param("deploymentId") Long deploymentId
    );

    /**
     * 특정 deployment의 특정 단계에서 다음 차례 승인자 조회
     * (아직 처리되지 않은 첫 번째 승인자)
     */
    @Query("SELECT a FROM Approval a " +
           "JOIN FETCH a.reviewer " +
           "WHERE a.deployment.id = :deploymentId " +
           "AND a.approvalStage = :approvalStage " +
           "AND a.processedAt IS NULL " +
           "AND a.isDeleted = false " +
           "ORDER BY a.current_approver_account_id ASC")
    List<Approval> findPendingApprovalsByDeploymentIdAndStage(
        @Param("deploymentId") Long deploymentId,
        @Param("approvalStage") DeploymentStage approvalStage
    );
}