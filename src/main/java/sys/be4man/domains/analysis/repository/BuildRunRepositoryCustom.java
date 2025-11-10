package sys.be4man.domains.analysis.repository;

import java.util.List;
import java.util.Optional;
import sys.be4man.domains.analysis.dto.response.BuildResultResponseDto;

public interface BuildRunRepositoryCustom {
    List<BuildResultResponseDto> findAllBuildResultsByDeploymentId(Long deploymentId);

    Optional<BuildResultResponseDto> findBuildResultByDeploymentId(Long deploymentId, Long buildRunId);

}
