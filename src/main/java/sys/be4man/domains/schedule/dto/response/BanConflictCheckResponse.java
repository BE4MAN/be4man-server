package sys.be4man.domains.schedule.dto.response;

import java.util.List;

/**
 * Ban 등록 전 충돌 Deployment 조회 응답 DTO
 */
public record BanConflictCheckResponse(
        List<ConflictingDeploymentResponse> conflictingDeployments,
        int conflictCount
) {
    public BanConflictCheckResponse(List<ConflictingDeploymentResponse> conflictingDeployments) {
        this(conflictingDeployments, conflictingDeployments.size());
    }
}



