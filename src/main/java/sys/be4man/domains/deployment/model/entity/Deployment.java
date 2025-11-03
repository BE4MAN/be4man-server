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
import sys.be4man.domains.deployment.model.type.DeploymentStatus;
import sys.be4man.domains.deployment.model.type.DeploymentType;
import sys.be4man.domains.deployment.model.type.RiskLevel;
import sys.be4man.domains.common.model.type.ReportStatus;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issuer_id", nullable = false)
    private Account issuer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pull_request_id", nullable = false)
    private PullRequest pullRequest;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private DeploymentType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private DeploymentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_status")
    private ReportStatus reportStatus;

    @Column(name = "is_deployed")
    private Boolean isDeployed;

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    @Column(name = "scheduled_to_ended_at")
    private LocalDateTime scheduledToEndedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false)
    private RiskLevel riskLevel;

    @Column(name = "risk_description", columnDefinition = "TEXT")
    private String riskDescription;

    @Column(name = "expected_duration")
    private String expectedDuration;

    @Column(name = "version", columnDefinition = "TEXT")
    private String version;

    @Column(name = "strategy")
    private String strategy;

    @Builder
    public Deployment(
            Project project, Account issuer, PullRequest pullRequest,
            String title, String content, DeploymentType type,
            DeploymentStatus status, ReportStatus reportStatus, RiskLevel riskLevel, String expectedDuration,
            Boolean isDeployed, LocalDateTime scheduledAt, LocalDateTime scheduledToEndedAt,
            String riskDescription, String version, String strategy
    ) {
        this.project = project;
        this.issuer = issuer;
        this.pullRequest = pullRequest;
        this.title = title;
        this.content = content;
        this.type = type;
        this.status = status;
        this.reportStatus = reportStatus;
        this.riskLevel = riskLevel;
        this.expectedDuration = expectedDuration;
        this.isDeployed = isDeployed;
        this.scheduledAt = scheduledAt;
        this.scheduledToEndedAt = scheduledToEndedAt;
        this.riskDescription = riskDescription;
        this.version = version;
        this.strategy = strategy;
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
     * 보고서 상태 업데이트
     */
    public void updateReportStatus(ReportStatus reportStatus) {
        this.reportStatus = reportStatus;
    }

    /**
     * 배포 상태 조회 (DTO 호환용)
     */
    public DeploymentStatus getDeploymentStatus() {
        return this.status;
    }

    /**
     * 기안자 이름 조회 (DTO 편의 메서드)
     */
    public String getDrafter() {
        return this.issuer != null ? this.issuer.getName() : null;
    }

    /**
     * 부서명 조회 (DTO 편의 메서드)
     */
    public String getDepartment() {
        if (this.issuer != null && this.issuer.getDepartment() != null) {
            return this.issuer.getDepartment().getKoreanName();
        }
        return null;
    }

    /**
     * 서비스명 조회 (DTO 편의 메서드)
     */
    public String getServiceName() {
        return this.project != null ? this.project.getName() : null;
    }

    /**
     * 작업 제목 조회 (DTO 편의 메서드)
     */
    public String getWorkTitle() {
        return this.title;
    }

}
