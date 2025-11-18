package sys.be4man.domains.statistics.repository.projection;

import java.time.LocalDateTime;

public record IntraBuildRow(
        Long deploymentId,
        Long projectId,
        String projectName,
        LocalDateTime startedAt,
        Boolean isBuild
) {}