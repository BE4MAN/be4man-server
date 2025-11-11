package sys.be4man.domains.approval.model.type;

import lombok.Getter;

/**
 * 결재 상태
 */
@Getter
public enum ApprovalStatus {
    REQUESTED("승인요청"),
    PENDING("승인대기"),
    APPROVED("승인완료"),
    REJECTED("반려"),
    CANCELED("취소"),
    DRAFT("임시저장");

    private final String koreanName;

    ApprovalStatus(String koreanName) {
        this.koreanName = koreanName;
    }
}
