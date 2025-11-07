package sys.be4man.domains.approval.model.type;

import lombok.Getter;

/**
 * 승인 문서 상태
 */
@Getter
public enum ApprovalStatus {
    DRAFT("임시저장"),
    REQUESTED("요청됨"),
    PENDING("승인대기"),
    APPROVED("승인완료"),
    REJECTED("반려"),
    CANCELED("취소");

    private final String koreanName;

    ApprovalStatus(String koreanName) {
        this.koreanName = koreanName;
    }
}