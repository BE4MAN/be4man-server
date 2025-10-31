package sys.be4man.domains.analysis.dto.response;

import sys.be4man.domains.analysis.model.entity.BuildRun;

public record BuildRunConsoleLogResponseDto(Long deploymentId, Long buildRunId, String log) {

    public static BuildRunConsoleLogResponseDto toDto(Long deploymentId, BuildRun buildRun) {
        return new BuildRunConsoleLogResponseDto(deploymentId, buildRun.getId(), buildRun.getLog());
    }

}
