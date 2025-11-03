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

        // query: 제목 또는 설명에 포함 (대소문자 구분 없음)
        if (query != null && !query.trim().isEmpty()) {
            String searchQuery = "%" + query.trim().toLowerCase() + "%";
            builder.and(
                    ban.title.lower().like(searchQuery)
                            .or(ban.description.lower().like(searchQuery))
            );
        }

        // startDate/endDate: Ban 기간이 요청 범위와 겹치면 포함 (부분 겹침 포함)
        if (startDate != null || endDate != null) {
            if (startDate != null && endDate != null) {
                // startDate와 endDate 둘 다 있는 경우: Ban 기간이 요청 범위와 겹치면 포함
                LocalDateTime startDateTime = startDate.atStartOfDay();
                LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
                builder.and(
                        ban.startedAt.loe(endDateTime)
                                .and(ban.endedAt.goe(startDateTime))
                );
            } else if (startDate != null) {
                // startDate만 있는 경우: Ban의 endedAt이 startDate 이후이면 포함
                LocalDateTime startDateTime = startDate.atStartOfDay();
                builder.and(ban.endedAt.goe(startDateTime));
            } else if (endDate != null) {
                // endDate만 있는 경우: Ban의 startedAt이 endDate 이전이면 포함
                LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
                builder.and(ban.startedAt.loe(endDateTime));
            }
        }

        // type: BanType 필터링
        if (type != null) {
            builder.and(ban.type.eq(type));
        }

        // projectIds: 연관된 프로젝트 중 하나라도 포함되면 포함
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

