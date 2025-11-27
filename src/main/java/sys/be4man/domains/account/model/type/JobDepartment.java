// 작성자 : 이원석
package sys.be4man.domains.account.model.type;

import lombok.Getter;

/**
 * 계정 부서를 나타내는 enum
 */
@Getter
public enum JobDepartment {
    PLANNING("기획"),
    LEGAL("법무"),
    SALES("영업"),
    HR("인사"),
    FINANCE("재무"),
    IT("IT");

    private final String koreanName;

    JobDepartment(String koreanName) {
        this.koreanName = koreanName;
    }
}

