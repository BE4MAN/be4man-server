// 작성자 : 이원석
package sys.be4man.domains.account.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sys.be4man.domains.account.model.entity.Account;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long>, AccountRepositoryCustom {

    /**
     * 이메일로 계정 조회
     */
    Optional<Account> findByEmail(String email);

    /**
     * GitHub ID로 계정 조회
     */
    Optional<Account> findByGithubId(Long githubId);
}
