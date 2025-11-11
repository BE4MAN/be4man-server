package sys.be4man.domains.approval.model.type;

import lombok.Getter;

/**
 * 결재 유형
 */
@Getter
public enum ApprovalType {
    PLAN("작업계획서"),
    DEPLOYMENT("배포"),
    REPORT("결과보고서"),
    RETRY("재배포"),
    ROLLBACK("복구"),
    DRAFT("임시저장");

    private final String koreanName;

    ApprovalType(String koreanName) {
        this.koreanName = koreanName;
    }
}


