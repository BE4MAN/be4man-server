package sys.be4man.domains.project.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sys.be4man.domains.project.model.entity.Project;
import sys.be4man.domains.project.model.entity.RelatedProject;

public interface RelatedProjectRepository extends JpaRepository<RelatedProject, Long> {

    List<RelatedProject> findByProject(Project project);

    @Query("SELECT rp FROM RelatedProject rp " +
            "JOIN FETCH rp.project p " +
            "JOIN FETCH rp.relatedProject r " +
            "WHERE p.id IN :projectIds")
    List<RelatedProject> findByProjectIdIn(@Param("projectIds") List<Long> projectIds);

    void deleteByProject(Project project);

    boolean existsByProjectAndRelatedProject(Project project, Project relatedProject);
}
