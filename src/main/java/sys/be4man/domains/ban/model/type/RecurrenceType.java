package sys.be4man.domains.ban.model.type;

import lombok.Getter;

@Getter
public enum RecurrenceType {
    DAILY("매일"),
    WEEKLY("매주"),
    MONTHLY("매월");

    private final String koreanName;

    RecurrenceType(String koreanName) {
        this.koreanName = koreanName;
    }
}
