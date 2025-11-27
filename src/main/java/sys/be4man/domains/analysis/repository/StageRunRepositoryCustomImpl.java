// 작성자 : 조윤상
package sys.be4man.domains.analysis.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import sys.be4man.domains.analysis.dto.response.StageRunResponseDto;
import sys.be4man.domains.analysis.model.entity.QBuildRun;
import sys.be4man.domains.analysis.model.entity.QStageRun;
import sys.be4man.domains.deployment.model.entity.QDeployment;

@RequiredArgsConstructor
public class StageRunRepositoryCustomImpl implements StageRunRepositoryCustom {

    private final JPAQueryFactory jpaQueryFactory;
    private final QStageRun stageRun = QStageRun.stageRun;
    private final QBuildRun buildRun = QBuildRun.buildRun;
    private final QDeployment deployment = QDeployment.deployment;

    @Override
    public List<StageRunResponseDto> findAllStageRunsByBuildRunId(Long buildRunId) {
        return jpaQueryFactory.select(
                        Projections.constructor(StageRunResponseDto.class,
                                buildRun.deployment.id.as("deploymentId"),
                                buildRun.id.as("buildRunId"),
                                stageRun.id.as("stageRunId"),
                                stageRun.stageName.as("stageName"),
                                stageRun.isSuccess.as("isSuccess"),
                                stageRun.orderIndex.as("orderIndex"),
                                stageRun.log.as("log"),
                                stageRun.problemSummary.as("problemSummary"),
                                stageRun.problemSolution.as("problemSolution")
                        )
                ).from(stageRun)
                .join(stageRun.buildRun, buildRun)
                .join(buildRun.deployment, deployment)
                .where(buildRun.id.eq(buildRunId))
                .fetch()
                ;
    }
}
