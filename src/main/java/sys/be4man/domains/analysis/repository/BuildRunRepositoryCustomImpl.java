package sys.be4man.domains.analysis.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import sys.be4man.domains.analysis.dto.response.BuildResultResponseDto;
import sys.be4man.domains.analysis.model.entity.QBuildRun;
import sys.be4man.domains.deployment.model.entity.QDeployment;
import sys.be4man.domains.pullrequest.model.entity.QPullRequest;

@RequiredArgsConstructor
public class BuildRunRepositoryCustomImpl implements BuildRunRepositoryCustom {

    private final JPAQueryFactory jpaQueryFactory;
    QBuildRun buildRun = QBuildRun.buildRun;
    QDeployment deployment = QDeployment.deployment;
    QPullRequest pullRequest = QPullRequest.pullRequest;

    @Override
    public Optional<BuildResultResponseDto> findBuildResultByDeploymentId(Long deploymentId) {
        var prUrlExpr = Expressions.stringTemplate(
                "concat(concat({0}, {1}), {2})",
                pullRequest.repositoryUrl,
                Expressions.constant("/pull/"),
                pullRequest.prNumber
        );

        return Optional.ofNullable(jpaQueryFactory.select(Projections.constructor(
                                BuildResultResponseDto.class,
                                deployment.id.as("deploymentId"),
                                deployment.isDeployed.as("isDeployed"),
                                buildRun.duration.as("duration"),
                                buildRun.startedAt.as("startedAt"),
                                buildRun.endedAt.as("endedAt"),
                                pullRequest.prNumber.as("prNumber"),
                                prUrlExpr.as("prUrl")
                        )
                )
                .from(buildRun)
                .join(buildRun.deployment, deployment)
                .join(deployment.pullRequest, pullRequest)
                .where(
                        deployment.id.eq(deploymentId),
                        pullRequest.isDeleted.isFalse(),
                        deployment.isDeleted.isFalse(),
                        buildRun.isDeleted.isFalse()
                ).fetchOne());

    }
}
