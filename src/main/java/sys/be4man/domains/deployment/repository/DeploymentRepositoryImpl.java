package sys.be4man.domains.deployment.repository;

import static sys.be4man.domains.deployment.model.entity.QDeployment.deployment;
import static sys.be4man.domains.project.model.entity.QProject.project;
import static sys.be4man.domains.pullrequest.model.entity.QPullRequest.pullRequest;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import sys.be4man.domains.deployment.model.entity.Deployment;

@Repository
@RequiredArgsConstructor
public class DeploymentRepositoryImpl implements DeploymentRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Deployment> findScheduledDeployments(
            LocalDateTime startDateTime,
            LocalDateTime endDateTime
    ) {
        BooleanBuilder builder = new BooleanBuilder()
                .and(deployment.isDeleted.eq(false))
                .and(deployment.scheduledAt.goe(startDateTime))
                .and(deployment.scheduledAt.loe(endDateTime));

        return queryFactory
                .selectFrom(deployment)
                .innerJoin(deployment.project, project).fetchJoin()
                .innerJoin(deployment.pullRequest, pullRequest).fetchJoin()
                .where(builder)
                .orderBy(deployment.scheduledAt.asc())
                .fetch();
    }
}
