package sys.be4man.domains.schedule.dto.response;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 충돌하는 Deployment 응답 DTO
 */
public record ConflictingDeploymentResponse(
        Long id,
        String title,
        List<String> relatedProjects,
        LocalDateTime scheduledAt,
        LocalDateTime scheduledToEndedAt
) {
}


