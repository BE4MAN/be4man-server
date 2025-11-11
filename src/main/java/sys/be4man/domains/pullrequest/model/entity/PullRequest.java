package sys.be4man.domains.pullrequest.model.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import sys.be4man.global.model.entity.BaseEntity;

@Entity
@Table(name = "pull_request")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PullRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pr_number", nullable = false)
    private Integer prNumber;

    @Column(name = "repository_url", nullable = false, columnDefinition = "TEXT")
    private String repositoryUrl;

    @Column(name = "branch", nullable = false, length = 255)
    private String branch;

    @Column(name = "github_email", length = 255)
    private String githubEmail;

    @Builder
    private PullRequest(Integer prNumber, String repositoryUrl, String branch, String githubEmail) {
        this.prNumber = prNumber;
        this.repositoryUrl = repositoryUrl;
        this.branch = branch;
        this.githubEmail = githubEmail;
    }

    /** 수정 메서드 */
    public void update(String repositoryUrl, String branch) {
        this.repositoryUrl = repositoryUrl;
        this.branch = branch;
    }

    public void updateGithubEmail(String githubEmail) {
        this.githubEmail = githubEmail;
    }
}
