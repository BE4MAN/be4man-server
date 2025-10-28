package sys.be4man.domains.pullrequest.model.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import sys.be4man.global.model.entity.BaseEntity;


@Entity
@Table(name = "pull_request") // 소문자 테이블명 사용
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PullRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "repository_default_branch", nullable = false)
    private String repositoryDefaultBranch;

    @Column(name = "repository_name", nullable = false)
    private String repositoryName;

    @Column(name = "repository_url", columnDefinition = "TEXT", nullable = false)
    private String repositoryUrl;

    @Column(name = "branch")
    private String branch;

    @Column(name = "pr_number", nullable = false)
    private Integer prNumber;

    @Column(name = "files_changed", nullable = false)
    private Integer filesChanged;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "lines_added", nullable = false)
    private Integer linesAdded;

    @Column(name = "lines_removed", nullable = false)
    private Integer linesRemoved;

    @Column(name = "commit_count", nullable = false)
    private Integer commitCount;

    @Column(name = "approver")
    private String approver;

    @Column(name = "last_approved_at", columnDefinition = "TEXT")
    private LocalDateTime lastApprovedAt;

    @Column(name = "approval_count")
    private Integer approvalCount;

    @Column(name = "change_request_count")
    private Integer changeRequestCount;

    @Column(name = "comment_count")
    private Integer commentCount;

    @Builder
    public PullRequest(
            String repositoryDefaultBranch,
            String repositoryName,
            String repositoryUrl,
            String branch,
            Integer prNumber,
            Integer filesChanged,
            String title,
            String content,
            Integer linesAdded,
            Integer linesRemoved,
            Integer commitCount,
            String approver,
            LocalDateTime lastApprovedAt,
            Integer approvalCount,
            Integer changeRequestCount,
            Integer commentCount
    ) {
        this.repositoryDefaultBranch = repositoryDefaultBranch;
        this.repositoryName = repositoryName;
        this.repositoryUrl = repositoryUrl;
        this.branch = branch;
        this.prNumber = prNumber;
        this.filesChanged = filesChanged;
        this.title = title;
        this.content = content;
        this.linesAdded = linesAdded;
        this.linesRemoved = linesRemoved;
        this.commitCount = commitCount;
        this.approver = approver;
        this.lastApprovedAt = lastApprovedAt;
        this.approvalCount = approvalCount;
        this.changeRequestCount = changeRequestCount;
        this.commentCount = commentCount;
    }

    public void updateCounts(Integer filesChanged, Integer linesAdded, Integer linesRemoved, Integer commitCount) {
        this.filesChanged = filesChanged;
        this.linesAdded = linesAdded;
        this.linesRemoved = linesRemoved;
        this.commitCount = commitCount;
    }
}
