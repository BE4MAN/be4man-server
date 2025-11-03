package sys.be4man.domains.common.model.type;

import lombok.Getter;

/**
 * 처리 상태 (승인대기 / 반려 / 진행중 / 취소 / 완료 / 승인)
 */
@Getter
public enum ProcessingStatus {
    PENDING("승인대기"),
    REJECTED("반려"),
    IN_PROGRESS("진행중"),
    CANCELED("취소"),
    COMPLETED("완료"),
    APPROVED("승인");

    private final String koreanName;

    ProcessingStatus(String koreanName) {
        this.koreanName = koreanName;
    }
}
