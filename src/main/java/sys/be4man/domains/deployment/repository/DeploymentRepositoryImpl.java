package sys.be4man.domains.deployment.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import sys.be4man.domains.account.repository.AccountRepositoryCustom;

@Repository
@RequiredArgsConstructor
public class DeploymentRepositoryImpl implements DeploymentRepositoryCustom {

    private final JPAQueryFactory queryFactory;
}
