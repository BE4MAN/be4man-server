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
 * 계정-프로젝트 연관관계 엔티티 (Primary Key만 사용)
 */
@Entity
@Table(name = "account_project")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AccountProject extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private Project project;

    @Builder
    public AccountProject(Account account, Project project) {
        this.account = account;
        this.project = project;
    }
}
