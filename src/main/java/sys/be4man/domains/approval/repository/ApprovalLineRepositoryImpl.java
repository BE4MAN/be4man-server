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
}

