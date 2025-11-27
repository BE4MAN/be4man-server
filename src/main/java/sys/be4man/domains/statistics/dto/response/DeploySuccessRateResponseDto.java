// 작성자 : 조윤상
package sys.be4man.domains.statistics.dto.response;

import java.util.List;

public record DeploySuccessRateResponseDto(
        List<ServiceRate> services,
        Count all
) {
    public record ServiceRate(Long id, String name, long success, long failed) {}
    public record Count(long success, long failed) {}
}