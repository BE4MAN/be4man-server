// 작성자 : 김민호
package sys.be4man.domains.pullrequest.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PullRequestUpdateRequest {

    private String repositoryUrl;
    private String branch;
    private Long githubId;
}
