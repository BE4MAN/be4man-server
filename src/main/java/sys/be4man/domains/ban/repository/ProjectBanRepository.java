// 작성자 : 이원석
package sys.be4man.domains.ban.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sys.be4man.domains.ban.model.entity.ProjectBan;

@Repository
public interface ProjectBanRepository extends JpaRepository<ProjectBan, Long> {

    /**
     * Ban ID로 연관된 모든 ProjectBan 조회 (삭제되지 않은 것만)
     */
    List<ProjectBan> findAllByBan_IdAndIsDeletedFalse(Long banId);

    /**
     * Ban ID 목록으로 연관된 모든 ProjectBan 조회 (삭제되지 않은 것만)
     */
    List<ProjectBan> findAllByBan_IdInAndIsDeletedFalse(List<Long> banIds);
}

