// 작성자 : 조윤상
package sys.be4man.domains.analysis.repository;

import java.util.List;
import sys.be4man.domains.analysis.dto.response.StageRunResponseDto;

public interface StageRunRepositoryCustom {

    List<StageRunResponseDto> findAllStageRunsByBuildRunId(Long buildRunId);
}
