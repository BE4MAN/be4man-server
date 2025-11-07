package sys.be4man.domains.approval.model.type;

import lombok.Getter;

/**
 * 결재선 유형
 */
@Getter
public enum ApprovalLineType {
    DRAFT("기안"),
    APPROVE("승인"),
    CONSENT("합의"),
    CC("참조");

    private final String koreanName;

    ApprovalLineType(String koreanName) {
        this.koreanName = koreanName;
    }
}