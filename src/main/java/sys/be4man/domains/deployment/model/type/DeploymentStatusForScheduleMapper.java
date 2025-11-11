package sys.be4man.domains.deployment.model.type;

/**
 * 배포 작업 상태 매퍼 DeploymentStage와 DeploymentStatus 조합으로 상태 문자열을 생성합니다.
 */
public enum DeploymentStatusForScheduleMapper {

    PLAN_PENDING("PLAN_PENDING", "작업계획서 승인 대기"),
    DEPLOYMENT_PENDING("DEPLOYMENT_PENDING", "배포 대기"),
    DEPLOYMENT_IN_PROGRESS("DEPLOYMENT_IN_PROGRESS", "배포 진행중"),
    DEPLOYMENT_SUCCESS("DEPLOYMENT_SUCCESS", "배포 성공"),
    DEPLOYMENT_FAILURE("DEPLOYMENT_FAILURE", "배포 실패");

    private final String value;
    private final String description;

    DeploymentStatusForScheduleMapper(String value, String description) {
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
        final String s = isDeployed == null ? null :
                isDeployed
                        ? DEPLOYMENT_SUCCESS.getValue()
                        : DEPLOYMENT_FAILURE.getValue();

        return switch (stage) {
            case PLAN -> switch (status) {
                case PENDING -> PLAN_PENDING.getValue();
                default -> null;
            };
            case DEPLOYMENT -> switch (status) {
                case PENDING -> DEPLOYMENT_PENDING.getValue();
                case IN_PROGRESS -> DEPLOYMENT_IN_PROGRESS.getValue();
                case COMPLETED -> s;
                default -> null;
            };
            case REPORT -> s;
            // 밑에 3개 스테이지 추가 된거라서 null로 일단 추가한거 나중에 확인해주세요
            case RETRY -> null;
            case ROLLBACK -> null;
            case DRAFT -> null;
        };
    }
}

