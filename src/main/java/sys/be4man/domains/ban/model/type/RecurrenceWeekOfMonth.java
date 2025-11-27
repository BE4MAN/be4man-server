// 작성자 : 이원석
package sys.be4man.domains.ban.model.type;

import lombok.Getter;

@Getter
public enum RecurrenceWeekOfMonth {
    FIRST("첫째 주"),
    SECOND("둘째 주"),
    THIRD("셋째 주"),
    FOURTH("넷째 주"),
    FIFTH("다섯째 주");

    private final String koreanName;

    RecurrenceWeekOfMonth(String koreanName) {
        this.koreanName = koreanName;
    }
}
