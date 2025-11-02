package sys.be4man.domains.analysis.repository;

import java.util.List;
import sys.be4man.domains.analysis.dto.response.StageRunResponseDto;
import sys.be4man.domains.analysis.model.entity.BuildRun;

public interface StageRunRepositoryCustom {

    Long deleteAllByBuildRunId(BuildRun buildRun);

    List<StageRunResponseDto> findAllStageRunsByDeploymentId(Long deploymentId);
}
