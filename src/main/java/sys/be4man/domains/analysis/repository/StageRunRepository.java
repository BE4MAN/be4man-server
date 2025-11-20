package sys.be4man.domains.analysis.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import sys.be4man.domains.analysis.dto.response.StageRunResponseDto;
import sys.be4man.domains.analysis.model.entity.StageRun;

public interface StageRunRepository extends JpaRepository<StageRun, Long>, StageRunRepositoryCustom {

}
