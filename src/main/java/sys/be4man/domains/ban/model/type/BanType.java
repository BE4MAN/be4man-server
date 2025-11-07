package sys.be4man.domains.ban.model.type;

import lombok.Getter;

/**
 * 작업 금지 유형
 */
@Getter
public enum BanType {
    DB_MIGRATION("DB 마이그레이션"),
    ACCIDENT("재난 재해"),
    MAINTENANCE("점검"),
    EXTERNAL_SCHEDULE("외부 일정");

    private final String koreanName;

    BanType(String koreanName) {
        this.koreanName = koreanName;
    }
}


