package sys.be4man.domains.deployment.model.type;

import lombok.Getter;

/**
 * 배포 위험 레벨
 */
@Getter
public enum RiskLevel {
    HIGH("높음"),
    MEDIUM("중간"),
    LOW("낮음");

    private final String koreanName;

    RiskLevel(String koreanName) {
        this.koreanName = koreanName;
    }
}
