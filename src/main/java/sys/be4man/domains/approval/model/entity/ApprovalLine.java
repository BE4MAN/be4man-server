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
import sys.be4man.domains.approval.model.type.ApprovalLineType;

/**
 * 결재선 엔티티
 */
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
    public ApprovalLine(Approval approval, Account account, String comment, ApprovalLineType type) {
        this.approval = approval;
        this.account = account;
        this.comment = comment;
        this.type = type;
    }

    /**
     * 승인 여부 조회
     * - ApprovalLineType에 따라 승인 여부 반환
     * - DRAFT: 기안자이므로 항상 true
     * - CC: 참조이므로 항상 null (승인 불필요)
     * - APPROVE, CONSENT: 실제 승인 필요 (comment로 판단)
     */
    public Boolean getIsApproved() {
        if (type == ApprovalLineType.DRAFT) {
            return true;  // 기안자는 항상 승인
        }
        if (type == ApprovalLineType.CC) {
            return null;  // 참조는 승인 불필요
        }
        // 실제 승인자: comment가 있으면 처리됨
        // TODO: 더 정확한 로직 필요 (별도 필드 추가 권장)
        return comment != null && !comment.isEmpty();
    }

    /**
     * 코멘트 업데이트
     */
    public void updateComment(String comment) {
        this.comment = comment;
    }

    /**
     * 결재선 타입 업데이트
     */
    public void updateType(ApprovalLineType type) {
        this.type = type;
    }
}