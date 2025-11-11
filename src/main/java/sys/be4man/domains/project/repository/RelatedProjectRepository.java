package sys.be4man.domains.project.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import sys.be4man.domains.deployment.model.entity.Deployment;
import sys.be4man.domains.project.model.entity.RelatedProject;

public interface RelatedProjectRepository extends JpaRepository<RelatedProject, Long> {

    List<RelatedProject> findByDeployment(Deployment deployment);
}
