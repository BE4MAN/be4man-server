package sys.be4man.domains.deployment.model.type;

/**
 * 배포 작업 상태 매퍼 DeploymentStage와 DeploymentStatus 조합으로 상태 문자열을 생성합니다.
 */
public enum DeploymentStatusMapper {

    PLAN_PENDING("PLAN_PENDING", "작업계획서 승인 대기"),
    PLAN_REJECTED("PLAN_REJECTED", "작업계획서 반려"),
    PLAN_APPROVED("PLAN_APPROVED", "작업계획서 승인"),
    DEPLOYMENT_PENDING("DEPLOYMENT_PENDING", "배포 대기"),
    DEPLOYMENT_CANCELED("DEPLOYMENT_CANCELED", "배포 취소"),
    DEPLOYMENT_IN_PROGRESS("DEPLOYMENT_IN_PROGRESS", "배포 진행중"),
    DEPLOYMENT_SUCCESS("DEPLOYMENT_SUCCESS", "배포 성공"),
    DEPLOYMENT_FAILURE("DEPLOYMENT_FAILURE", "배포 실패"),
    REPORT_PENDING("REPORT_PENDING", "결과보고서 승인 대기"),
    REPORT_APPROVED("REPORT_APPROVED", "결과보고서 승인");

    private final String value;
    private final String description;

    DeploymentStatusMapper(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public String getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }

    /**
     * DeploymentStage와 DeploymentStatus를 조합하여 상태 문자열을 반환합니다.
     *
     * @param stage      배포 단계
     * @param status     배포 상태
     * @param isDeployed 배포 성공 여부 (DEPLOYMENT 단계에서 COMPLETED 상태일 때만 사용)
     * @return 매핑된 상태 문자열
     */
    public static String map(DeploymentStage stage, DeploymentStatus status, Boolean isDeployed) {
        return switch (stage) {
            case PLAN -> switch (status) {
                case PENDING -> PLAN_PENDING.getValue();
                case REJECTED -> PLAN_REJECTED.getValue();
                case APPROVED -> PLAN_APPROVED.getValue();
                default -> null;
            };
            case DEPLOYMENT -> switch (status) {
                case PENDING -> DEPLOYMENT_PENDING.getValue();
                case CANCELED -> DEPLOYMENT_CANCELED.getValue();
                case IN_PROGRESS -> DEPLOYMENT_IN_PROGRESS.getValue();
                case COMPLETED -> {
                    if (isDeployed == null) {
                        yield null;
                    }
                    yield isDeployed ? DEPLOYMENT_SUCCESS.getValue()
                            : DEPLOYMENT_FAILURE.getValue();
                }
                default -> null;
            };
            case REPORT -> switch (status) {
                case PENDING -> REPORT_PENDING.getValue();
                case APPROVED -> REPORT_APPROVED.getValue();
                default -> null;
            };
        };
    }
}

