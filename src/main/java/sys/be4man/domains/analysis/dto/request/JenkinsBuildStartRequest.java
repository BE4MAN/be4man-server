// 작성자 : 조윤상
package sys.be4man.domains.analysis.dto.request;

public record JenkinsBuildStartRequest(
        Long deploymentId,
        String jobName,
        Integer buildNumber
) {}