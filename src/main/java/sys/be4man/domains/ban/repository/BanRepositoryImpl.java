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
            BooleanBuilder overlapCondition = buildDateRangeCondition(startDate, endDate);
            builder.and(overlapCondition);
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
                .orderBy(
                        ban.startDate.asc().nullsLast(),
                        ban.startTime.asc()
                )
                .fetch();
    }

    private BooleanBuilder buildDateRangeCondition(LocalDate startDate, LocalDate endDate) {
        BooleanBuilder singleEventCondition = new BooleanBuilder()
                .and(ban.startDate.isNotNull());

        if (startDate != null) {
            LocalDateTime startDateTime = startDate.atStartOfDay();
            singleEventCondition.and(
                    ban.endedAt.isNull().or(ban.endedAt.goe(startDateTime))
            );
        }

        if (endDate != null) {
            singleEventCondition.and(ban.startDate.loe(endDate));
        }

        BooleanBuilder recurrenceCondition = new BooleanBuilder()
                .and(ban.recurrenceType.isNotNull());

        if (startDate != null) {
            recurrenceCondition.and(
                    ban.recurrenceEndDate.isNull().or(ban.recurrenceEndDate.goe(startDate))
            );
        }

        return new BooleanBuilder()
                .or(singleEventCondition)
                .or(recurrenceCondition);
    }
}

