package sys.be4man.domains.project.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProjectResponse {

    private Long id;
    private Long managerAccountId;
    private String name;
    private String discordWebhookUrl;
    private boolean isRunning;
    private String jenkinsIp;
}
