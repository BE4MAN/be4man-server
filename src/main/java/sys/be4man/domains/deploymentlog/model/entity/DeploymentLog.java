package sys.be4man.domains.deploymentlog.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import sys.be4man.domains.deployment.model.entity.Deployment;
import sys.be4man.global.model.entity.BaseEntity;

/**
 * 배포 로그 엔티티
 */
@Entity
@Table(name = "deployment_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeploymentLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deploy_id", nullable = false)
    private Deployment deployment;

    @Column(name = "jenkins_job_name", nullable = false)
    private String jenkinsJobName;

    @Column(name = "log", columnDefinition = "TEXT", nullable = false)
    private String log;

    @Column(name = "duration", nullable = false)
    private Integer duration;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at", nullable = false)
    private LocalDateTime endedAt;

    @Builder
    public DeploymentLog(Deployment deployment, String jenkinsJobName, String log,
            Integer duration, LocalDateTime startedAt, LocalDateTime endedAt) {
        this.deployment = deployment;
        this.jenkinsJobName = jenkinsJobName;
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
     * 종료 시간 업데이트
     */
    public void updateEndedAt(LocalDateTime endedAt) {
        this.endedAt = endedAt;
    }
}
