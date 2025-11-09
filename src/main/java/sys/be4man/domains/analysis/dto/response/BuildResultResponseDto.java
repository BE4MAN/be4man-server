package sys.be4man.domains.analysis.dto.response;

import java.time.LocalDateTime;

public record BuildResultResponseDto(Long deploymentId, Long buildRunId, Boolean isDeployed, Long duration, LocalDateTime startedAt, LocalDateTime endedAt, Integer prNumber, String prUrl) {

}
