package sys.be4man.domains.project.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sys.be4man.domains.project.model.entity.Project;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    /**
     * 프로젝트 ID로 프로젝트 조회 (삭제되지 않은 것만)
     */
    Optional<Project> findByIdAndIsDeletedFalse(Long id);

    /**
     * 모든 프로젝트 조회 (삭제되지 않은 것만)
     */
    List<Project> findAllByIsDeletedFalse();
}

