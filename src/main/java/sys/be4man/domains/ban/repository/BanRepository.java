package sys.be4man.domains.ban.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sys.be4man.domains.ban.model.entity.Ban;

@Repository
public interface BanRepository extends JpaRepository<Ban, Long>, BanRepositoryCustom {

    /**
     * Ban ID로 Ban 조회 (삭제되지 않은 것만)
     */
    Optional<Ban> findByIdAndIsDeletedFalse(Long id);
}

