package sys.be4man.domains.approval.model.entity;

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
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import sys.be4man.domains.account.model.entity.Account;
import sys.be4man.domains.approval.model.type.ApprovalType;
import sys.be4man.domains.deployment.model.entity.Deployment;
import sys.be4man.global.model.entity.BaseEntity;

/**
 * 배포 승인 엔티티
 */
@Entity
@Table(name = "approval")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Approval extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id", nullable = false)
    private Account reviewer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deployment_id", nullable = false)
    private Deployment deployment;

    @Column(name = "is_approved", nullable = false)
    private Boolean isApproved;

    @Column(name = "comment", length = 255)
    private String comment;

    @Enumerated(EnumType.STRING)
    @Column(name="type")
    private ApprovalType type;

    @Builder
    public Approval(Account reviewer, Deployment deployment, Boolean isApproved,
            String comment, ApprovalType type) {
        this.reviewer = reviewer;
        this.deployment = deployment;
        this.isApproved = isApproved;
        this.comment = comment;
        this.type = type;
    }

    /**
     * 승인 상태 업데이트
     */
    public void updateApprovalStatus(Boolean isApproved) {
        this.isApproved = isApproved;
    }

    /**
     * 코멘트 업데이트
     */
    public void updateComment(String comment) {
        this.comment = comment;
    }
}


