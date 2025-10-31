package sys.be4man.domains.analysis.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import sys.be4man.domains.deployment.model.entity.Deployment;
import sys.be4man.global.model.entity.BaseEntity;

/**
 * 빌드 실행 엔티티
 */
@Entity
@Table(name = "build_run",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_build_run_build_number_jenkins_job_name",
                columnNames = {"build_number", "jenkins_job_name"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeploymentLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deployment_id", nullable = false)
    private Deployment deployment;

    @Column(name = "jenkins_job_name", nullable = false)
    private String jenkinsJobName;

    @Column(name = "build_number", nullable = false)
    private long buildNumber;

    @Column(name = "log", columnDefinition = "TEXT", nullable = false)
    private String log;

    @Column(name = "duration", nullable = false)
    private Long duration;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at", nullable = false)
    private LocalDateTime endedAt;

    @Builder
    public DeploymentLog(Deployment deployment, String jenkinsJobName, Long buildNumber,
            String log, Long duration, LocalDateTime startedAt, LocalDateTime endedAt) {
        this.deployment = deployment;
        this.jenkinsJobName = jenkinsJobName;
        this.buildNumber = buildNumber;
        this.log = log;
        this.duration = duration;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
    }

    /**
     * 로그 내용 업데이트
     */
    public void updateLog(String log) {
        this.log = log;
    }

    /**
     * 빌드 소요 시간 업데이트
     */
    public void updateDuration(Long duration){
        this.duration = duration;
    }

    /**
     * 빌드 시작 시간 업데이트
     */
    public void updateStartedAt(LocalDateTime startedAt){
        this.startedAt = startedAt;
    }

    /**
     * 빌드 종료 시간 업데이트
     */
    public void updateEndedAt(LocalDateTime endedAt){
        this.endedAt = endedAt;
    }

}
