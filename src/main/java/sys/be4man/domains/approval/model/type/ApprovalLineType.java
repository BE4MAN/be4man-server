package sys.be4man.domains.approval.model.type;

import lombok.Getter;

/**
 * 결재 라인 타입
 * - 기안 / 결재 / 합의 / 참조
 */
@Getter
public enum ApprovalLineType {
    DRAFT("기안"),
    APPROVE("결재"),
    CONSENT("합의"),
    CC("참조");

    private final String koreanName;

    ApprovalLineType(String koreanName) {
        this.koreanName = koreanName;
    }
}
