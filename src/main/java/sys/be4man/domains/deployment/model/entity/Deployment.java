// 작성자 : 이원석
package sys.be4man.domains.deployment.model.entity;

import jakarta.persistence.*;
import java.time.Duration;
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

    @Column(name = "scheduled_at", nullable = true)
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

    /** 배포 성공 여부 업데이트 */
    public void updateIsDeployed(boolean isDeployed) {
        this.isDeployed = isDeployed;
    }

    /** 배포 작업 상태 업데이트 */
    public void updateStatus(DeploymentStatus status) {
        this.status = status;
    }

    /** 배포 작업 단계 업데이트 */
    public void updateStage(DeploymentStage stage) {
        this.stage = stage;
    }

    /** 제목/본문/버전 단일 업데이트 (선택) */
    public void updateTitle(String title) {
        if (title != null) this.title = title;
    }

    public void updateContent(String content) {
        if (content != null) this.content = content;
    }

    public void updateVersion(String version) {
        this.version = version;
    }

    /**
     * 스케줄 전체 업데이트: 시작/종료 시각을 세팅하고 두 시각 차이를 분(String)으로 저장
     * - end 가 start 이전이면 end = start 로 보정(소요 시간 0분)
     * - start 또는 end 가 null이면 가능한 값만 저장, expectedDuration 은 null 처리
     */
    public void updateSchedule(LocalDateTime start, LocalDateTime end) {
        this.scheduledAt = start;
        this.scheduledTo_EndedAtSafe(end);
        recomputeExpectedDuration();
    }

    /** 시작 시각만 수정 */
    public void updateScheduledAt(LocalDateTime start) {
        this.scheduledAt = start;
        recomputeExpectedDuration();
    }

    /** 종료 시각만 수정 */
    public void updateScheduledToEndedAt(LocalDateTime end) {
        this.scheduledTo_EndedAtSafe(end);
        recomputeExpectedDuration();
    }

    private void scheduledTo_EndedAtSafe(LocalDateTime end) {
        // 보정만 담당
        if (end != null && scheduledAt != null && end.isBefore(scheduledAt)) {
            // end가 start보다 이르면 0분 처리 위해 start와 동일하게 맞춤
            this.scheduledToEndedAt = scheduledAt;
        } else {
            this.scheduledToEndedAt = end;
        }
    }

    /** start/end가 모두 존재할 때만 분 차이를 계산하여 expectedDuration에 문자열로 저장 */
    private void recomputeExpectedDuration() {
        if (this.scheduledAt != null && this.scheduledToEndedAt != null) {
            long minutes = Duration.between(this.scheduledAt, this.scheduledToEndedAt).toMinutes();
            if (minutes < 0) minutes = 0;
            this.expectedDuration = String.valueOf(minutes);
        } else {
            this.expectedDuration = null;
        }
    }
}
