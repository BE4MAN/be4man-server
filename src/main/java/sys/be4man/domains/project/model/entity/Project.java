package sys.be4man.domains.project.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import sys.be4man.domains.account.model.entity.Account;
import sys.be4man.global.model.entity.BaseEntity;

/**
 * 프로젝트 엔티티
 */
@Entity
@Table(name = "project")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Project extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id", nullable = false)
    private Account manager;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "discord_webhook_url", columnDefinition = "TEXT")
    private String discordWebhookUrl;

    @Column(name = "is_running", nullable = false)
    private Boolean isRunning;

    @Column(name = "jenkins_ip", columnDefinition = "TEXT", nullable = false)
    private String jenkinsIp;

    @Builder
    public Project(Account manager, String name, String discordWebhookUrl,
                   Boolean isRunning, String jenkinsIp) {
        this.manager = manager;
        this.name = name;
        this.discordWebhookUrl = discordWebhookUrl;
        this.isRunning = isRunning;
        this.jenkinsIp = jenkinsIp;
    }

    /**
     * 프로젝트 이름 업데이트
     */
    public void updateName(String name) {
        this.name = name;
    }

    /**
     * Discord 웹훅 URL 업데이트
     */
    public void updateDiscordWebhookUrl(String discordWebhookUrl) {
        this.discordWebhookUrl = discordWebhookUrl;
    }

    /**
     * 실행 상태 업데이트
     */
    public void updateRunningStatus(Boolean isRunning) {
        this.isRunning = isRunning;
    }

    /**
     * Jenkins IP 업데이트
     */
    public void updateJenkinsIp(String jenkinsIp) {
        this.jenkinsIp = jenkinsIp;
    }
}
