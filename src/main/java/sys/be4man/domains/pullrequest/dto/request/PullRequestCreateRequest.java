// 작성자 : 김민호
package sys.be4man.domains.pullrequest.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PullRequestCreateRequest {

    private Integer prNumber;
    private String repositoryUrl;
    private String branch;
    private String githubEmail;
    private Long githubId;
}
