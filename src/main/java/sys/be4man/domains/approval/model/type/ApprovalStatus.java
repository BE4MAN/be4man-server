// 작성자 : 김민호
package sys.be4man.domains.approval.model.type;

import lombok.Getter;

/**
 * 결재 상태
 */
@Getter
public enum ApprovalStatus {
    PENDING("승인대기"),
    APPROVED("승인완료"),
    REJECTED("반려"),
    CANCELED("취소"),
    INPROGRESS("진행중"),
    COMPLETED("완료"),
    DRAFT("임시저장");

    private final String koreanName;

    ApprovalStatus(String koreanName) {
        this.koreanName = koreanName;
    }
}
