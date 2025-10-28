package sys.be4man.domains.approval.model.type;

import lombok.Getter;

/**
 * 결재 유형
 */
@Getter
public enum ApprovalType {
    APPROVAL("결재"),
    AGREEMENT("합의"),
    REFERENCE("참조");

    private final String koreanName;

    ApprovalType(String koreanName) {
        this.koreanName = koreanName;
    }
}


