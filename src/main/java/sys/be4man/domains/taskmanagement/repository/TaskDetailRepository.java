package sys.be4man.domains.taskmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sys.be4man.domains.deployment.model.entity.Deployment;

/**
 * 작업 상세 페이지용 Repository
 * - TaskManagementRepository와 분리하여 상세 조회 전용으로 사용
 */
@Repository
public interface TaskDetailRepository extends JpaRepository<Deployment, Long> {
}