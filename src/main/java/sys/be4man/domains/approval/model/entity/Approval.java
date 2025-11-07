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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import sys.be4man.domains.account.model.entity.Account;
import sys.be4man.domains.approval.model.type.ApprovalLineType;
import sys.be4man.domains.approval.model.type.ApprovalStatus;
import sys.be4man.domains.approval.model.type.ApprovalType;
import sys.be4man.domains.deployment.model.entity.Deployment;
import sys.be4man.domains.deployment.model.type.DeploymentStage;
import sys.be4man.global.model.entity.BaseEntity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 배포 승인 엔티티
 *
 * 배포 계획서의 승인 문서 정보를 관리
 * 실제 승인자별 처리 내역은 ApprovalLine에서 관리
 */
@Entity
@Table(name = "approval")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Approval extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /* ========================================
       === 관계 정보 (Foreign Keys) ===
       ======================================== */

    /**
     * 배포 작업 참조
     * - 이 승인이 어느 배포 작업에 대한 것인지 나타냄
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deployment_id", nullable = false)
    private Deployment deployment;

    /**
     * 기안자 (승인 문서를 작성한 사람)
     * - 배포를 신청하고 계획서를 작성한 담당자
     * - DB: account_id
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    /**
     * 현재 승인 차례인 사람
     * - 순차적 승인 프로세스에서 지금 승인을 해야 하는 사람
     * - 승인 완료 시 다음 승인자로 자동 업데이트
     * - 모든 승인 완료 시 null로 설정
     * - DB: current_approver_account_id
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_approver_account_id")
    private Account currentApprover;

    /**
     * 승인자 라인 (1:N 관계)
     * - 이 승인 문서에 대한 모든 승인자 정보
     * - 각 승인자별 승인/반려/참조 상태를 개별적으로 추적
     */
    @OneToMany(mappedBy = "approval", fetch = FetchType.LAZY,
            cascade = jakarta.persistence.CascadeType.ALL)
    private List<ApprovalLine> approvalLines = new ArrayList<>();

    /* ========================================
       === 승인 문서 내용 ===
       ======================================== */

    /**
     * 승인 제목
     * - 예: "API Gateway v2.5 배포", "결제 시스템 롤백"
     * - DB: title
     */
    @Column(name = "title", nullable = false, columnDefinition = "TEXT")
    private String title;

    /**
     * 승인 상세 내용
     * - 배포 목표, 변경 사항, 영향도 등 상세 정보
     * - DB: content
     */
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * 서비스명
     * - 배포 대상 서비스 (예: "Payment Gateway", "API Server")
     * - DB: service
     */
    @Column(name = "service", columnDefinition = "TEXT")
    private String service;

    /**
     * 승인 타입
     * - APPROVAL: 결재 (승인이 필수)
     * - AGREEMENT: 합의 (협의 필요)
     * - REFERENCE: 참조 (알림용)
     * - DB: type (STAGE 타입으로 저장)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 20)
    private ApprovalType type;

    /**
     * 기안자의 의견/설명
     * - 기안자가 승인자들에게 전달하는 추가 설명
     * - DB: comment (없음 - 이 필드는 실제로 불필요할 수 있음)
     */
    @Column(name = "comment", length = 255)
    private String comment;

    /* ========================================
       === 승인 상태 ===
       ======================================== */

    /**
     * 승인 여부 (Boolean)
     * - true: 모든 승인자가 승인 완료
     * - false: 반려되었거나 승인 대기 중
     * - DB: is_approved
     */
    @Column(name = "is_approved", nullable = false)
    private Boolean isApproved = false;

    /**
     * 승인 상태 (상세)
     * - DRAFT: 임시저장
     * - REQUESTED: ALTER TABLE approval
     * ADD CONSTRAINT approval_status_check
     * CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELED', 'IN_PROGRESS', 'COMPLETED'));
     * ALTER TABLE approval
     * ADD CONSTRAINT approval_status_check
     * CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELED', 'IN_PROGRESS', 'COMPLETED'));
     * ALTER TABLE approval
     * ADD CONSTRAINT approval_status_check
     * CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELED', 'IN_PROGRESS', 'COMPLETED'));
     * - PENDING: 승인 대기 중
     * - APPROVED: 승인 완료
     * - REJECTED: 반려됨
     * - CANCELED: 취소됨
     * - DB: status
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ApprovalStatus status;

    /**
     * 승인 단계
     * - PLAN: 계획서 승인 단계
     * - DEPLOYMENT: 배포 진행 중
     * - REPORT: 결과 보고 승인 단계
     * - RETRY: 재배포 승인
     * - ROLLBACK: 복구 승인
     * - DRAFT: 임시 저장 상태
     * - DB: approval_stage (STAGE 타입으로 저장)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "approval_stage", nullable = false, length = 20)
    private DeploymentStage approvalStage;

    /* ========================================
       === 시간 추적 ===
       ======================================== */

    /**
     * 기안 시간
     * - 승인 문서를 작성한 시간
     * - DB: drafted_at (없음 - 필드 추가 필요할 수도)
     */
    @Column(name = "drafted_at", nullable = false)
    private LocalDateTime draftedAt;

    /**
     * 승인 완료 시간
     * - 모든 승인자가 승인을 완료한 시간
     * - 배포 실행 시간 결정에 사용
     * - DB: approved_at
     */
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    /**
     * 처리 시간
     * - 마지막 승인자가 처리(승인/반려)한 시간
     * - 감사 로그 용도
     * - DB: processed_at
     */
    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    /* ========================================
       === 참고: BaseEntity에서 상속되는 필드 ===
       ======================================== */
    // created_at: 승인 문서 생성 시간
    // updated_at: 승인 문서 수정 시간
    // is_deleted: 논리적 삭제 여부 (Soft Delete)

    @Builder
    public Approval(Deployment deployment, Account account, Account currentApprover,
            String title, String content, String service, ApprovalType type,
            String comment, Boolean isApproved, ApprovalStatus status,
            DeploymentStage approvalStage, LocalDateTime draftedAt,
            LocalDateTime approvedAt, LocalDateTime processedAt) {
        this.deployment = deployment;
        this.account = account;  // 기안자
        this.currentApprover = currentApprover;
        this.title = title;
        this.content = content;
        this.service = service;
        this.type = type;
        this.comment = comment;
        this.isApproved = isApproved != null ? isApproved : false;
        this.status = status;
        this.approvalStage = approvalStage;
        this.draftedAt = draftedAt;
        this.approvedAt = approvedAt;
        this.processedAt = processedAt;
    }

    /* ========================================
       === 비즈니스 로직 메서드 ===
       ======================================== */

    /**
     * 현재 승인자 업데이트
     */
    public void updateCurrentApprover(Account nextApprover) {
        this.currentApprover = nextApprover;
    }

    /**
     * 다음 승인자로 자동 이동
     */
    public void moveToNextApprover() {
        ApprovalLine nextLine = getNextApproverLine();
        if (nextLine != null) {
            this.currentApprover = nextLine.getAccount();
        } else {
            this.currentApprover = null;
        }
    }

    private ApprovalLine getNextApproverLine() {
        int currentIndex = -1;
        List<ApprovalLine> sortedLines = approvalLines.stream()
                .sorted((a, b) -> a.getId().compareTo(b.getId()))
                .toList();

        for (int i = 0; i < sortedLines.size(); i++) {
            if (sortedLines.get(i).getAccount().getId().equals(
                    this.currentApprover != null ? this.currentApprover.getId() : null)) {
                currentIndex = i;
                break;
            }
        }

        if (currentIndex >= 0) {
            for (int i = currentIndex + 1; i < sortedLines.size(); i++) {
                ApprovalLine line = sortedLines.get(i);
                if (line.getType() != ApprovalLineType.CC) {
                    return line;
                }
            }
        }

        return null;
    }

    /**
     * 모든 승인자 승인 여부 확인
     * - CC(참조)는 제외
     */
    public boolean isAllApproved() {
        if (approvalLines.isEmpty()) return false;
        return approvalLines.stream()
                .filter(line -> line.getType() != ApprovalLineType.CC)
                .allMatch(line -> line.getIsApproved() != null && line.getIsApproved());
    }

    /**
     * 반려 여부 확인
     * - 한 명이라도 반려하면 true
     */
    public boolean isRejected() {
        return approvalLines.stream()
                .anyMatch(line -> line.getIsApproved() != null && !line.getIsApproved());
    }

    /**
     * 승인 상태 업데이트
     */
    public void updateStatus(ApprovalStatus status) {
        this.status = status;
    }

    /**
     * 승인 완료 처리
     */
    public void approve(LocalDateTime approvedAt) {
        this.isApproved = true;
        this.status = ApprovalStatus.APPROVED;
        this.approvedAt = approvedAt;
    }

    /**
     * 반려 처리
     */
    public void reject(String comment) {
        this.isApproved = false;
        this.status = ApprovalStatus.REJECTED;
        this.comment = comment;
        this.processedAt = LocalDateTime.now();
    }

    /**
     * 취소 처리
     */
    public void cancel() {
        this.status = ApprovalStatus.CANCELED;
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
