package sys.be4man.domains.approval.model.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import sys.be4man.domains.account.model.entity.Account;
import sys.be4man.domains.approval.model.type.ApprovalLineType;

@Entity
@Table(name = "approval_line")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApprovalLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approval_id", nullable = false)
    private Approval approval;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "comment", nullable = false, columnDefinition = "TEXT")
    private String comment;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private ApprovalLineType type;

    @Builder
    private ApprovalLine(
            Approval approval,
            Account account,
            String comment,
            ApprovalLineType type
    ) {
        this.approval = approval;
        this.account = account;
        this.comment = comment;
        this.type = (type != null) ? type : ApprovalLineType.APPROVE;
    }

    void setApproval(Approval approval) {
        this.approval = approval;
    }

    public void updateComment(String comment) {
        this.comment = comment;
    }
}
