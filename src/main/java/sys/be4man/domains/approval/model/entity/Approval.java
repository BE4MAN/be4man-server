// 작성자 : 허겸, 이원석
package sys.be4man.domains.approval.model.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import sys.be4man.domains.account.model.entity.Account;
import sys.be4man.domains.approval.model.type.ApprovalStatus;
import sys.be4man.domains.approval.model.type.ApprovalType;
import sys.be4man.domains.deployment.model.entity.Deployment;
import sys.be4man.global.model.entity.BaseEntity;

@Entity
@Table(name = "approval")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Approval extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 연결된 배포 작업 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deployment_id")
    private Deployment deployment;

    /** 기안자 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "drafter_account_id", nullable = false)
    private Account account;

    /** 다음 결재자 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "next_approver_account_id")
    private Account nextApprover;

    /** 최종 승인 여부 */
    @Column(name = "is_approved", nullable = false)
    private Boolean isApproved = false;

    /** 결재 완료 시간 */
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private ApprovalType type;

    @Column(name = "title", nullable = false, columnDefinition = "TEXT")
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ApprovalStatus status;

    @Column(name = "service", nullable = false, columnDefinition = "TEXT")
    private String service;

    @OneToMany(mappedBy = "approval", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<ApprovalLine> approvalLines = new ArrayList<>();

    @Builder
    private Approval(
            Deployment deployment,
            Account account,
            Account nextApprover,
            Boolean isApproved,
            LocalDateTime approvedAt,
            ApprovalType type,
            String title,
            String content,
            ApprovalStatus status,
            String service
    ) {
        this.deployment = deployment;
        this.account = account;
        this.nextApprover = nextApprover;
        this.isApproved = isApproved;
        this.approvedAt = approvedAt;
        this.type = type;
        this.title = title;
        this.content = content;
        this.status = status;
        this.service = service;
    }

    public void updateStatus(ApprovalStatus status) {
        this.status = status;
    }

    /** 최종 승인 처리 */
    public void approve() {
        this.isApproved = true;
        this.status = ApprovalStatus.APPROVED;

        this.approvedAt = this.approvalLines.stream()
                .filter(l -> Boolean.TRUE.equals(l.getIsApproved()))
                .map(ApprovalLine::getApprovedAt)
                .max(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now());

        this.nextApprover = null;
    }

    /** 최종 승인 처리 (마지막 승인자의 시각 사용) */
    public void approve(LocalDateTime lastApprovedAt) {
        this.isApproved = true;
        this.status = ApprovalStatus.APPROVED;
        this.approvedAt = lastApprovedAt != null ? lastApprovedAt : LocalDateTime.now();
        this.nextApprover = null;
    }

    /** 반려 처리 */
    public void reject() {
        this.isApproved = false;
        this.status = ApprovalStatus.REJECTED;
    }

    public void addApprovalLine(ApprovalLine line) {
        this.approvalLines.add(line);
        line.setApproval(this);
    }

    /** 다음 결재자 세팅 */
    public void updateNextApprover(Account nextApprover) {
        this.nextApprover = nextApprover;
    }

    public void updateApprovedAt(LocalDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }

    public void updateTitle(String title) { this.title = title; }
    public void updateContent(String content) { this.content = content; }
    public void updateService(String service) { this.service = service; }
    public void updateType(ApprovalType type) { this.type = type; }
}
