// 작성자 : 조윤상
package sys.be4man.domains.deployment.model.type;

public enum DeploymentResult {

    SUCCESS("SUCCESS", Boolean.TRUE),
    FAILURE("FAILURE", Boolean.FALSE),
    UNSTABLE("UNSTABLE", Boolean.TRUE), // 불안정 상태도 배포 자체는 성공한 것으로 간주될 수 있음
    ABORTED("ABORTED", Boolean.FALSE),
    NOT_BUILT("NOT_BUILT", Boolean.FALSE),

    UNKNOWN("UNKNOWN", Boolean.FALSE);

    private final String jenkinsStatus;
    private final Boolean isDeployed;

    DeploymentResult(String jenkinsStatus, Boolean isDeployed) {
        this.jenkinsStatus = jenkinsStatus;
        this.isDeployed = isDeployed;
    }

    public static DeploymentResult fromJenkinsStatus(String jenkinsStatus) {
        if (jenkinsStatus == null) {
            return UNKNOWN;
        }
        for (DeploymentResult result : values()) {
            if (result.jenkinsStatus.equalsIgnoreCase(jenkinsStatus)) {
                return result;
            }
        }
        return UNKNOWN;
    }

    public Boolean getIsDeployed() {
        return isDeployed;
    }

    public String getJenkinsStatus() {
        return jenkinsStatus;
    }

}
