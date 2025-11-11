package sys.be4man.domains.deployment.repository;

import static sys.be4man.domains.deployment.model.entity.QDeployment.deployment;
import static sys.be4man.domains.project.model.entity.QProject.project;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import sys.be4man.domains.deployment.model.entity.Deployment;
import sys.be4man.domains.deployment.model.type.DeploymentStage;
import sys.be4man.domains.deployment.model.type.DeploymentStatus;

@Repository
@RequiredArgsConstructor
public class DeploymentRepositoryImpl implements DeploymentRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Deployment> findScheduledDeployments(
            LocalDateTime startDateTime,
            LocalDateTime endDateTime
    ) {
        BooleanBuilder builder = buildBaseCondition()
                .and(deployment.scheduledAt.goe(startDateTime))
                .and(deployment.scheduledAt.loe(endDateTime))
                .and(buildScheduledDeploymentsFilter());

        return queryFactory
                .selectFrom(deployment)
                .innerJoin(deployment.project, project).fetchJoin()
                .where(builder)
                .orderBy(deployment.scheduledAt.asc())
                .fetch();
    }

    @Override
    public List<Deployment> findOverlappingDeployments(
            LocalDateTime banStartDateTime,
            LocalDateTime banEndDateTime,
            List<Long> projectIds
    ) {
        BooleanBuilder builder = buildBaseCondition()
                .and(buildActiveDeploymentFilter())
                .and(deployment.project.id.in(projectIds))
                .and(buildOverlapCondition(banStartDateTime, banEndDateTime));

        return queryFactory
                .selectFrom(deployment)
                .innerJoin(deployment.project, project).fetchJoin()
                .where(builder)
                .fetch();
    }

    private BooleanBuilder buildBaseCondition() {
        return new BooleanBuilder()
                .and(deployment.isDeleted.eq(false));
    }

    private BooleanBuilder buildScheduledDeploymentsFilter() {
        return new BooleanBuilder()
                .and(
                        deployment.stage.ne(DeploymentStage.PLAN)
                                .or(deployment.status.ne(DeploymentStatus.REJECTED))
                )
                .and(
                        deployment.stage.ne(DeploymentStage.DEPLOYMENT)
                                .or(deployment.status.ne(DeploymentStatus.CANCELED))
                );
    }

    private BooleanBuilder buildActiveDeploymentFilter() {
        return new BooleanBuilder()
                .and(deployment.status.ne(DeploymentStatus.CANCELED))
                .and(deployment.status.ne(DeploymentStatus.COMPLETED))
                .and(deployment.stage.ne(DeploymentStage.REPORT));
    }

    private BooleanBuilder buildOverlapCondition(
            LocalDateTime banStartDateTime,
            LocalDateTime banEndDateTime
    ) {
        return new BooleanBuilder()
                .and(deployment.scheduledAt.goe(banStartDateTime))
                .and(deployment.scheduledAt.lt(banEndDateTime));
    }
}
