// 작성자 : 김민호
package sys.be4man.domains.deployment.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
}
