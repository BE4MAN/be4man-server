package sys.be4man.domains.project.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sys.be4man.domains.deployment.model.entity.Deployment;
import sys.be4man.domains.project.model.entity.RelatedProject;

public interface RelatedProjectRepository extends JpaRepository<RelatedProject, Long> {

    List<RelatedProject> findByDeployment(Deployment deployment);

    /**
     * Deployment ID 목록으로 RelatedProject 배치 조회
     */
    @Query("SELECT rp FROM RelatedProject rp " +
           "JOIN FETCH rp.project p " +
           "JOIN FETCH rp.deployment d " +
           "WHERE d.id IN :deploymentIds")
    List<RelatedProject> findByDeploymentIdIn(@Param("deploymentIds") List<Long> deploymentIds);
}
