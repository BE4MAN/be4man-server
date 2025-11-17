package sys.be4man.domains.pullrequest.model.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

import sys.be4man.global.model.entity.BaseEntity;

@Entity
@Table(name = "pull_request")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PullRequest extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer prNumber;

    @Column(nullable = false, length = 300)
    private String repositoryUrl;

    @Column(length = 120)
    private String branch;

    @Column
    private Long githubId;

    @Column(length = 200)
    private String githubEmail;

    @Builder
    public PullRequest(Integer prNumber, String repositoryUrl, String branch, Long githubId, String githubEmail) {
        this.prNumber = prNumber;
        this.repositoryUrl = repositoryUrl;
        this.branch = branch;
        this.githubId = githubId;
        this.githubEmail = githubEmail;
    }

    public void updateBranch(String branch) { this.branch = branch; }
    public void updateGithubEmail(String email) { this.githubEmail = email; }
    public void updateGithubId(Long githubId) { this.githubId = githubId; }
    public void touchUpdatedAt(LocalDateTime now) { }
}
