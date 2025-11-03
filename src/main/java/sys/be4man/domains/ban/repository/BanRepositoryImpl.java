package sys.be4man.domains.ban.repository;

import static sys.be4man.domains.ban.model.entity.QBan.ban;
import static sys.be4man.domains.ban.model.entity.QProjectBan.projectBan;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import sys.be4man.domains.ban.model.entity.Ban;
import sys.be4man.domains.ban.model.type.BanType;

@Repository
@RequiredArgsConstructor
public class BanRepositoryImpl implements BanRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Ban> findBans(
            String query,
            LocalDate startDate,
            LocalDate endDate,
            BanType type,
            List<Long> projectIds
    ) {
        BooleanBuilder builder = new BooleanBuilder()
                .and(ban.isDeleted.eq(false));

        if (query != null && !query.trim().isEmpty()) {
            String searchQuery = "%" + query.trim().toLowerCase() + "%";
            builder.and(
                    ban.title.lower().like(searchQuery)
                            .or(ban.description.lower().like(searchQuery))
            );
        }

        if (startDate != null || endDate != null) {
            if (startDate != null && endDate != null) {
                LocalDateTime startDateTime = startDate.atStartOfDay();
                LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
                builder.and(
                        ban.startedAt.loe(endDateTime)
                                .and(ban.endedAt.goe(startDateTime))
                );
            } else if (startDate != null) {
                LocalDateTime startDateTime = startDate.atStartOfDay();
                builder.and(ban.endedAt.goe(startDateTime));
            } else if (endDate != null) {
                LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
                builder.and(ban.startedAt.loe(endDateTime));
            }
        }

        if (type != null) {
            builder.and(ban.type.eq(type));
        }

        if (projectIds != null && !projectIds.isEmpty()) {
            builder.and(
                    ban.id.in(
                            queryFactory
                                    .select(projectBan.ban.id)
                                    .from(projectBan)
                                    .where(projectBan.project.id.in(projectIds)
                                                   .and(projectBan.isDeleted.eq(false)))
                    )
            );
        }

        return queryFactory
                .selectFrom(ban)
                .where(builder)
                .orderBy(ban.startedAt.asc())
                .fetch();
    }
}

