package sys.be4man.domains.pullrequest.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import sys.be4man.domains.pullrequest.model.entity.PullRequest;

public interface PullRequestRepository extends JpaRepository<PullRequest, Long> {
    Optional<PullRequest> findByPrNumber(Integer prNumber);
    boolean existsByPrNumber(Integer prNumber);
    List<PullRequest> findByGithubId(Long githubId);
    Optional<PullRequest> findByPrNumberAndRepositoryUrl(Integer prNumber, String repositoryUrl);
}
