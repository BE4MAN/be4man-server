package sys.be4man.domains.account.model.type;

import lombok.Getter;

/**
 * 계정 직급을 나타내는 enum ERD의 ACCOUNT_POSITION 타입과 일치합니다.
 */
@Getter
public enum AccountPosition {
    STAFF("사원"),
    ASSISTANT_MANAGER("대리"),
    SENIOR_MANAGER("과장"),
    DEPUTY_GENERAL_MANAGER("차장"),
    GENERAL_MANAGER("본부장");

    /**
     * -- GETTER -- 한국어 직급명을 반환합니다.
     *
     * @return 한국어 직급명
     */
    private final String koreanName;

    AccountPosition(String koreanName) {
        this.koreanName = koreanName;
    }

}



