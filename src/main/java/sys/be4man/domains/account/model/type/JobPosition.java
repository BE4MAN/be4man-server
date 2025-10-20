package sys.be4man.domains.account.model.type;

import lombok.Getter;

/**
 * 계정 직급을 나타내는 enum
 */
@Getter
public enum JobPosition {
    STAFF("사원"),
    ASSISTANT_MANAGER("대리"),
    SENIOR_MANAGER("과장"),
    DEPUTY_GENERAL_MANAGER("차장"),
    GENERAL_MANAGER("부장"),
    EXECUTIVE("임원");

    private final String koreanName;

    JobPosition(String koreanName) {
        this.koreanName = koreanName;
    }

}



