// 작성자 : 조윤상
package sys.be4man.domains.statistics.dto.response;

import java.util.List;
import lombok.Builder;

@Builder
public record DeployDurationResponse(
        List<MonthDurationDto> months,
        List<ServiceOptionDto> services
) {

    public static DeployDurationResponse onlyMonths(List<MonthDurationDto> months) {
        return DeployDurationResponse.builder().months(months).services(null).build();
    }
}
