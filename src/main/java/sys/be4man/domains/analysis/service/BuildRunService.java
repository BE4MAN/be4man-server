// 작성자 : 조윤상
package sys.be4man.domains.analysis.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import sys.be4man.domains.analysis.dto.response.BuildResultResponseDto;
import sys.be4man.domains.analysis.dto.response.BuildRunConsoleLogResponseDto;
import sys.be4man.domains.analysis.exception.type.BuildRunExceptionType;
import sys.be4man.domains.analysis.model.entity.BuildRun;
import sys.be4man.domains.analysis.repository.BuildRunRepository;
import sys.be4man.global.exception.NotFoundException;

@RequiredArgsConstructor
@Service
public class BuildRunService {

    private final BuildRunRepository buildRunRepository;

    public BuildRunConsoleLogResponseDto getConsoleLogByDeploymentIdAndBuildRunId(Long deploymentId, Long buildRunId) {
        BuildRun buildRun = buildRunRepository.findByDeploymentIdAndIdAndIsDeletedFalse(deploymentId, buildRunId)
                .orElseThrow(
                        () -> new NotFoundException(BuildRunExceptionType.BUILD_RUN_NOT_FOUND)
                );

        return BuildRunConsoleLogResponseDto.toDto(deploymentId, buildRun);
    }

    public List<BuildResultResponseDto> getAllBuildResultsByDeploymentId(Long deploymentId) {
        return buildRunRepository.findAllBuildResultsByDeploymentId(deploymentId);
    }

    public BuildResultResponseDto getBuildResultByDeploymentIdAndBuildRunId(Long deploymentId,
            Long buildRunId) {
        return buildRunRepository.findBuildResultByDeploymentId(deploymentId, buildRunId).orElseThrow();
    }
}
