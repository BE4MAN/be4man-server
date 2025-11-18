package sys.be4man.domains.taskmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sys.be4man.domains.deployment.model.entity.Deployment;

/**
 * 작업 관리 페이지용 Repository
 * - Deployment 엔티티를 사용하여 작업 관리 내역 조회
 */
@Repository
public interface TaskManagementRepository extends JpaRepository<Deployment, Long>, TaskManagementRepositoryCustom {
}
