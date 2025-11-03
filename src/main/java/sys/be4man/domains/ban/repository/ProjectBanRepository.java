package sys.be4man.domains.ban.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sys.be4man.domains.ban.model.entity.ProjectBan;

@Repository
public interface ProjectBanRepository extends JpaRepository<ProjectBan, Long> {

    /**
     * Ban ID로 연관된 모든 ProjectBan 조회
     */
    List<ProjectBan> findAllByBan_Id(Long banId);
}

