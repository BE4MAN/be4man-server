package sys.be4man.domains.deployment.model.type;

import lombok.Getter;

/**
 * 배포 작업 종류
 */
@Getter
public enum DeploymentType {
    DEPLOY("배포"),
    EMERGENCY("긴급 배포"),
    BUILD("빌드"),
    TEST("테스트"),
    DB_MIGRATION("DB 마이그레이션"),
    ROLLBACK("롤백");

    private final String koreanName;

    DeploymentType(String koreanName) {
        this.koreanName = koreanName;
    }
}
