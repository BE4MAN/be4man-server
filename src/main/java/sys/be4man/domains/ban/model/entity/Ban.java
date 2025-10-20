package sys.be4man.domains.ban.model.entity;

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
import sys.be4man.domains.ban.model.type.BanType;
import sys.be4man.global.model.entity.BaseEntity;

/**
 * 작업 금지 엔티티
 */
@Entity
@Table(name = "ban")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Ban extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at", nullable = false)
    private LocalDateTime endedAt;

    @Column(name = "title", length = 25, nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT", nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private BanType type;

    @Builder
    public Ban(Account account, LocalDateTime startedAt, LocalDateTime endedAt,
            String title, String description, BanType type) {
        this.account = account;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.title = title;
        this.description = description;
        this.type = type;
    }

    /**
     * 금지 기간 업데이트
     */
    public void updatePeriod(LocalDateTime startedAt, LocalDateTime endedAt) {
        this.startedAt = startedAt;
        this.endedAt = endedAt;
    }

    /**
     * 설명 업데이트
     */
    public void updateDescription(String description) {
        this.description = description;
    }
}
