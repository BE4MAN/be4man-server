package sys.be4man.domains.analysis.dto.response;

public record StageRunResponseDto(Long deploymentId, Long buildRunId, Long stageRunId,
                                  String stageName, Boolean isSuccess, Long orderIndex,
                                  String log, String problemSummary, String problemSolution) {

}
