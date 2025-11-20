package sys.be4man.domains.approval.model.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import sys.be4man.domains.account.model.entity.Account;
import sys.be4man.domains.approval.model.type.ApprovalLineType;
import sys.be4man.global.model.entity.BaseEntity;

@Entity
@Table(name = "approval_line")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApprovalLine extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 결재 ID (Approval) */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "approval_id", nullable = false)
    private Approval approval;

    /** 결재자 (Account) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    /** 코멘트 */
    @Column(name = "comment", nullable = false, columnDefinition = "TEXT")
    private String comment;

    /** 결재 종류 (기안, 결재, 합의, 참조 등) */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private ApprovalLineType type;

    /** 승인 일자 */
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    /** 승인 여부 */
    @Column(name = "is_approved")
    private Boolean isApproved;

    @Builder
    private ApprovalLine(
            Approval approval,
            Account account,
            String comment,
            ApprovalLineType type,
            LocalDateTime approvedAt,
            Boolean isApproved
    ) {
        this.approval = approval;
        this.account = account;
        this.comment = (comment != null) ? comment : "";
        this.type = (type != null) ? type : ApprovalLineType.APPROVE;
        this.approvedAt = approvedAt;
        this.isApproved = isApproved;
    }

    /** 결재 참조 연결 */
    void setApproval(Approval approval) {
        this.approval = approval;
    }

    /** 코멘트 업데이트 */
    public void updateComment(String comment) {
        this.comment = comment;
    }

    /** 승인 처리 */
    public void approve() {
        this.isApproved = true;
        this.approvedAt = LocalDateTime.now();
    }

    /** 반려 처리 */
    public void reject() {
        this.isApproved = false;
        this.approvedAt = LocalDateTime.now();
    }

    public void updateApproved(boolean approved) {
        this.isApproved = approved;
    }

    public void updateApprovedAt(LocalDateTime dateTime) {
        this.approvedAt = dateTime;
    }

}
