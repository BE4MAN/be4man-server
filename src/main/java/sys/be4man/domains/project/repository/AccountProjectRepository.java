package sys.be4man.domains.project.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import sys.be4man.domains.project.model.entity.AccountProject;

public interface AccountProjectRepository extends JpaRepository<AccountProject, Long> {
    // account_id와 project_id로 행이 존재하는지 확인하는 메서드
    boolean existsByAccount_IdAndProject_Id(Long accountId, Long projectId);
    // account_id로 계정이 참여하는 모든 프로젝트 조회하는 메서드
    List<AccountProject> findAllByAccount_Id(Long accountId);
    // project_id로 프로젝트에 참여중인 모든 계정 조회하는 메서드
    List<AccountProject> findAllByProject_Id(Long projectId);
    // 프로젝트에 참여중인 팀원을 삭제하는 메서드
    void deleteByAccount_IdAndProject_Id(Long accountId, Long projectId);
}