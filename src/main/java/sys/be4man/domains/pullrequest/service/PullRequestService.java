// 작성자 : 김민호
package sys.be4man.domains.pullrequest.service;

import java.util.List;
import sys.be4man.domains.pullrequest.dto.request.PullRequestCreateRequest;
import sys.be4man.domains.pullrequest.dto.response.PullRequestResponse;
import sys.be4man.domains.pullrequest.dto.request.PullRequestUpdateRequest;

public interface PullRequestService {

    List<PullRequestResponse> getAllByGithubId(Long githubId);

    PullRequestResponse getById(Long id);

    PullRequestResponse create(PullRequestCreateRequest request);

    PullRequestResponse update(Long id, PullRequestUpdateRequest request);

    void delete(Long id);
}
