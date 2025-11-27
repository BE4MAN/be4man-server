// 작성자 : 김민호
package sys.be4man.domains.pullrequest.dto.response;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PullRequestResponse {

    private Long id;
    private Integer prNumber;
    private String repositoryUrl;
    private String branch;
    private String githubEmail;
    private Long githubId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
