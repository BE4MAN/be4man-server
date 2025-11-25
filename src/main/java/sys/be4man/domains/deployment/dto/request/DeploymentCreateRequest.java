package sys.be4man.domains.deployment.dto.request;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Deployment 생성 시 필요한 요청 DTO
 *
 * 프론트에서 일정(scheduledAt, scheduledToEndedAt)을 함께 넘겨주면
 * DeploymentServiceImpl에서 그대로 반영합니다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeploymentCreateRequest {

    private Long projectId;
    private Long issuerId;
    private Long pullRequestId;
    private String title;
    private String stage;
    private String status;
    private String expectedDuration;
    private String version;
    private String content;

    private LocalDateTime scheduledAt;
    private LocalDateTime scheduledToEndedAt;
}
