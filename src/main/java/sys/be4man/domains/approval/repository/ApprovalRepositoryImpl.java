package sys.be4man.domains.approval.repository;

import static sys.be4man.domains.approval.model.entity.QApproval.approval;
import static sys.be4man.domains.account.model.entity.QAccount.account;
import static sys.be4man.domains.deployment.model.entity.QDeployment.deployment;
import static sys.be4man.domains.project.model.entity.QProject.project;

import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import sys.be4man.domains.approval.model.type.ApprovalType;
import sys.be4man.domains.deployment.model.entity.Deployment;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ApprovalRepositoryImpl implements ApprovalRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    /**
     * 복구현황 목록 조회 (ROLLBACK 타입 Approval의 Deployment 조회)
     */
    @Override
    public List<Deployment> findRollbackDeployments(int offset, int pageSize) {
        log.debug("복구현황 목록 조회 - offset: {}, pageSize: {}", offset, pageSize);

        return queryFactory
                .selectFrom(deployment)
                .innerJoin(approval).on(approval.deployment.id.eq(deployment.id))
                .innerJoin(deployment.project, project).fetchJoin()
                .innerJoin(deployment.issuer, account).fetchJoin()
                .where(
                        approval.type.eq(ApprovalType.ROLLBACK)
                                .and(approval.isDeleted.eq(false))
                                .and(deployment.isDeleted.eq(false))
                )
                .orderBy(deployment.createdAt.desc())
                .offset(offset)
                .limit(pageSize)
                .distinct()
                .fetch();
    }

    /**
     * 복구현황 목록 총 개수 조회
     */
    @Override
    public long countRollbackDeployments() {
        log.debug("복구현황 목록 총 개수 조회");

        return queryFactory
                .select(deployment.id.countDistinct())
                .from(deployment)
                .innerJoin(approval).on(approval.deployment.id.eq(deployment.id))
                .where(
                        approval.type.eq(ApprovalType.ROLLBACK)
                                .and(approval.isDeleted.eq(false))
                                .and(deployment.isDeleted.eq(false))
                )
                .fetchOne();
    }
}

