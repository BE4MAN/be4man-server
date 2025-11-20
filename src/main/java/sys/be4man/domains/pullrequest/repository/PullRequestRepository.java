package sys.be4man.domains.pullrequest.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sys.be4man.domains.pullrequest.model.entity.PullRequest;

public interface PullRequestRepository extends JpaRepository<PullRequest, Long> {
    Optional<PullRequest> findByPrNumber(Integer prNumber);
    boolean existsByPrNumber(Integer prNumber);
    List<PullRequest> findByGithubId(Long githubId);
    Optional<PullRequest> findByPrNumberAndRepositoryUrl(Integer prNumber, String repositoryUrl);

    @Query("""
        select p from PullRequest p
        where p.githubId = :githubId
          and p.createdAt = (
               select max(p2.createdAt) from PullRequest p2
               where p2.githubId = :githubId
                 and p2.repositoryUrl = p.repositoryUrl
                 and p2.branch = p.branch
          )
        order by p.repositoryUrl asc, p.branch asc, p.createdAt desc
    """)
    List<PullRequest> findLatestPerRepoBranchByGithubId(@Param("githubId") Long githubId);
}
