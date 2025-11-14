package sys.be4man.domains.project.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sys.be4man.domains.project.model.entity.Project;
import sys.be4man.domains.project.model.entity.RelatedProject;

public interface RelatedProjectRepository extends JpaRepository<RelatedProject, Long> {

    List<RelatedProject> findByProject(Project project);

    /**
     * 특정 프로젝트 ID들의 관련 프로젝트 조회 (단방향)
     * - project_id IN :projectIds인 경우의 relatedProject만 반환
     * 
     * @param projectIds 프로젝트 ID 목록
     * @return RelatedProject 목록
     */
    @Query("SELECT rp FROM RelatedProject rp " +
            "JOIN FETCH rp.project p " +
            "JOIN FETCH rp.relatedProject r " +
            "WHERE p.id IN :projectIds " +
            "AND p.isDeleted = false AND r.isDeleted = false")
    List<RelatedProject> findByProjectIdIn(@Param("projectIds") List<Long> projectIds);

    void deleteByProject(Project project);

    boolean existsByProjectAndRelatedProject(Project project, Project relatedProject);
}
