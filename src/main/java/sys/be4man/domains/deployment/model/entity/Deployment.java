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
import sys.be4man.domains.deployment.model.type.DeploymentStage;
import sys.be4man.domains.deployment.model.type.DeploymentStatus;
import sys.be4man.domains.project.model.entity.Project;
import sys.be4man.domains.pullrequest.model.entity.PullRequest;
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

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issuer_id", nullable = false)
    private Account issuer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pull_request_id", nullable = false)
    private PullRequest pullRequest;

    @Enumerated(EnumType.STRING)
    @Column(name = "stage", nullable = false)
    private DeploymentStage stage;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private DeploymentStatus status;

    @Column(name = "is_deployed")
    private Boolean isDeployed;

    @Column(name = "scheduled_at", nullable = false)
    private LocalDateTime scheduledAt;

    @Column(name = "scheduled_to_ended_at")
    private LocalDateTime scheduledToEndedAt;

    @Column(name = "expected_duration")
    private String expectedDuration;

    @Column(name = "version", columnDefinition = "TEXT")
    private String version;

    @Builder
    public Deployment(
            Project project, Account issuer, PullRequest pullRequest,
            String title, String content,
            DeploymentStatus status, String expectedDuration,
            Boolean isDeployed, LocalDateTime scheduledAt, LocalDateTime scheduledToEndedAt,
            String version, DeploymentStage stage
    ) {
        this.project = project;
        this.issuer = issuer;
        this.pullRequest = pullRequest;
        this.title = title;
        this.content = content;
        this.status = status;
        this.expectedDuration = expectedDuration;
        this.isDeployed = isDeployed;
        this.scheduledAt = scheduledAt;
        this.scheduledToEndedAt = scheduledToEndedAt;
        this.version = version;
        this.stage = stage;
    }

    /**
     * 배포 성공 여부 업데이트
     */
    public void updateIsDeployed(boolean isDeployed) {
        this.isDeployed = isDeployed;
    }

    /**
     * 배포 작업 상태 업데이트
     */
    public void updateStatus(DeploymentStatus status) {
        this.status = status;
    }

    /**
     * 배포 작업 단계 업데이트
     */
    public void updateStage(DeploymentStage stage) {
        this.stage = stage;
    }

}


