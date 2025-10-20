package sys.be4man.domains.deployment.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import sys.be4man.domains.account.model.entity.Account;
import sys.be4man.domains.deployment.model.type.DeployStatus;
import sys.be4man.domains.deployment.model.type.RiskLevel;
import sys.be4man.domains.project.model.entity.Project;
import sys.be4man.global.model.entity.BaseEntity;

/**
 * 배포 작업 엔티티
 */
@Entity
@Table(name = "deployment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Deployment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issuer_id", nullable = false)
    private Account issuer;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "body", columnDefinition = "TEXT", nullable = false)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private DeployStatus status;

    @Column(name = "pr_number", nullable = false)
    private Integer prNumber;

    @Column(name = "pr_description", columnDefinition = "TEXT", nullable = false)
    private String prDescription;

    @Column(name = "branch", nullable = false)
    private String branch;

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    @Column(name = "scheduled_to_ended_at")
    private LocalDateTime scheduledToEndedAt;

    @Column(name = "git_repository_name", nullable = false)
    private String gitRepositoryName;

    @Column(name = "git_repository_default_branch", nullable = false)
    private String gitRepositoryDefaultBranch;

    @Column(name = "git_repository_url", columnDefinition = "TEXT", nullable = false)
    private String gitRepositoryUrl;

    @Column(name = "related_project", columnDefinition = "TEXT")
    private String relatedProject;

    @Column(name = "risk_score", nullable = false)
    private Double riskScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false)
    private RiskLevel riskLevel;

    @Column(name = "build_number")
    private Long buildNumber;

    @Builder
    public Deployment(Project project, Account issuer, String title, String body,
            DeployStatus status, Integer prNumber, String prDescription,
            String branch, LocalDateTime scheduledAt, LocalDateTime scheduledToEndedAt,
            String gitRepositoryName, String gitRepositoryDefaultBranch,
            String gitRepositoryUrl, String relatedProject, Double riskScore,
            RiskLevel riskLevel, Long buildNumber) {
        this.project = project;
        this.issuer = issuer;
        this.title = title;
        this.body = body;
        this.status = status;
        this.prNumber = prNumber;
        this.prDescription = prDescription;
        this.branch = branch;
        this.scheduledAt = scheduledAt;
        this.scheduledToEndedAt = scheduledToEndedAt;
        this.gitRepositoryName = gitRepositoryName;
        this.gitRepositoryDefaultBranch = gitRepositoryDefaultBranch;
        this.gitRepositoryUrl = gitRepositoryUrl;
        this.relatedProject = relatedProject;
        this.riskScore = riskScore;
        this.riskLevel = riskLevel;
        this.buildNumber = buildNumber;
    }

    /**
     * 배포 상태 업데이트
     */
    public void updateStatus(DeployStatus status) {
        this.status = status;
    }

    /**
     * 빌드 번호 업데이트
     */
    public void updateBuildNumber(Long buildNumber) {
        this.buildNumber = buildNumber;
    }
}
