package sys.be4man.domains.ban.model.type;

import lombok.Getter;

/**
 * 작업 금지 유형
 */
@Getter
public enum BanType {
    DB_MIGRATION("DB 마이그레이션"),
    BLACK_OUT("사고"),
    EVENT("이벤트");

    private final String koreanName;

    BanType(String koreanName) {
        this.koreanName = koreanName;
    }
}
