// 작성자 : 이원석
package sys.be4man.domains.approval.repository;

import static sys.be4man.domains.account.model.entity.QAccount.account;
import static sys.be4man.domains.approval.model.entity.QApproval.approval;
import static sys.be4man.domains.approval.model.entity.QApprovalLine.approvalLine;
import static sys.be4man.domains.deployment.model.entity.QDeployment.deployment;
import static sys.be4man.domains.project.model.entity.QProject.project;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import sys.be4man.domains.approval.model.entity.ApprovalLine;
import sys.be4man.domains.approval.model.entity.QApprovalLine;
import sys.be4man.domains.approval.model.type.ApprovalStatus;
import sys.be4man.domains.deployment.model.entity.Deployment;
import sys.be4man.domains.deployment.model.type.DeploymentStage;
import sys.be4man.domains.deployment.model.type.DeploymentStatus;

@Repository
@RequiredArgsConstructor
public class ApprovalLineRepositoryImpl implements ApprovalLineRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<ApprovalLine> findPendingApprovalsByAccountId(Long accountId) {
        return queryFactory
                .selectFrom(approvalLine)
                .innerJoin(approvalLine.approval, approval).fetchJoin()
                .innerJoin(approval.deployment, deployment).fetchJoin()
                .innerJoin(deployment.project, project).fetchJoin()
                .innerJoin(deployment.issuer, account).fetchJoin()
                .where(
                        approvalLine.account.id.eq(accountId)
                                .and(approvalLine.isApproved.isNull())
                                .and(deployment.status.in(DeploymentStatus.PENDING, DeploymentStatus.APPROVED))
                                .and(approval.isDeleted.eq(false))
                                .and(deployment.isDeleted.eq(false))
                )
                .orderBy(approval.createdAt.desc())
                .distinct()
                .fetch();
    }

    @Override
    public List<Deployment> findInProgressTasksByAccountId(Long accountId) {
        BooleanBuilder stageStatusCondition = new BooleanBuilder()
                .and(
                        deployment.stage.eq(DeploymentStage.PLAN)
                                .and(deployment.status.eq(DeploymentStatus.APPROVED))
                )
                .or(
                        deployment.stage.eq(DeploymentStage.DEPLOYMENT)
                                .and(deployment.status.eq(DeploymentStatus.PENDING))
                );

        return queryFactory
                .selectFrom(deployment)
                .innerJoin(deployment.project, project).fetchJoin()
                .innerJoin(deployment.issuer, account).fetchJoin()
                .innerJoin(approval).on(approval.deployment.id.eq(deployment.id))
                .innerJoin(approval.approvalLines, approvalLine)
                .where(
                        approvalLine.account.id.eq(accountId)
                                .and(approvalLine.isApproved.eq(true))
                                .and(stageStatusCondition)
                                .and(approval.isDeleted.eq(false))
                                .and(deployment.isDeleted.eq(false))
                )
                .orderBy(deployment.updatedAt.desc())
                .distinct()
                .fetch();
    }

    @Override
    public List<Deployment> findCanceledNotificationsByAccountId(Long accountId) {
        return queryFactory
                .selectFrom(deployment)
                .innerJoin(deployment.project, project).fetchJoin()
                .innerJoin(deployment.issuer, account).fetchJoin()
                .innerJoin(approval).on(approval.deployment.id.eq(deployment.id))
                .innerJoin(approval.approvalLines, approvalLine)
                .where(
                        approvalLine.account.id.eq(accountId)
                                .and(approvalLine.isApproved.eq(true))
                                .and(deployment.status.eq(DeploymentStatus.CANCELED))
                                .and(approval.isDeleted.eq(false))
                                .and(deployment.isDeleted.eq(false))
                )
                .orderBy(deployment.updatedAt.desc())
                .distinct()
                .fetch();
    }

    @Override
    public List<Deployment> findRejectedDeploymentsByIssuerId(Long accountId) {
        return queryFactory
                .selectFrom(deployment)
                .innerJoin(deployment.project, project).fetchJoin()
                .innerJoin(deployment.issuer, account).fetchJoin()
                .where(
                        deployment.issuer.id.eq(accountId)
                                .and(deployment.status.eq(DeploymentStatus.REJECTED))
                                .and(deployment.isDeleted.eq(false))
                )
                .orderBy(deployment.updatedAt.desc())
                .fetch();
    }

    @Override
    public List<Deployment> findRejectedApprovalsByApproverId(Long accountId) {
        // 현재 사용자가 승인한 approval_line
        QApprovalLine approvedLine = new QApprovalLine("approvedLine");
        // 반려된 approval_line (is_approved = false)
        QApprovalLine rejectedLine = new QApprovalLine("rejectedLine");

        return queryFactory
                .selectFrom(deployment)
                .innerJoin(deployment.project, project).fetchJoin()
                .innerJoin(deployment.issuer, account).fetchJoin()
                .innerJoin(approval).on(approval.deployment.id.eq(deployment.id))
                .innerJoin(approval.approvalLines, approvedLine)
                .where(
                        approvedLine.account.id.eq(accountId)
                                .and(approvedLine.isApproved.eq(true))
                                .and(
                                        deployment.status.eq(DeploymentStatus.REJECTED)
                                                .or(approval.status.eq(ApprovalStatus.REJECTED))
                                )
                                .and(approval.isDeleted.eq(false))
                                .and(deployment.isDeleted.eq(false))
                                .and(
                                        // 해당 approval의 approval_lines 중 is_approved = false인 항목이 존재
                                        // 그리고 반려 시각이 현재 사용자의 승인 시각보다 이후
                                        queryFactory
                                                .selectOne()
                                                .from(rejectedLine)
                                                .where(
                                                        rejectedLine.approval.id.eq(approval.id)
                                                                .and(rejectedLine.isApproved.eq(false))
                                                                .and(rejectedLine.approvedAt.isNotNull())
                                                                .and(approvedLine.approvedAt.isNotNull())
                                                                .and(rejectedLine.approvedAt.after(approvedLine.approvedAt))
                                                )
                                                .exists()
                                )
                )
                .orderBy(deployment.updatedAt.desc())
                .distinct()
                .fetch();
    }
}

