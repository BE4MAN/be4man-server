package sys.be4man.domains.deployment.dto.response;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DeploymentResponse {

    private Long id;
    private Long projectId;
    private Long issuerId;
    private Long pullRequestId;
    private String title;
    private String stage;
    private String status;
    private Boolean isDeployed;
    private LocalDateTime scheduledAt;
    private LocalDateTime scheduledToEndedAt;
    private String expectedDuration;
    private String version;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
