package sys.be4man.domains.schedule.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sys.be4man.domains.account.model.entity.Account;
import sys.be4man.domains.account.model.type.Role;
import sys.be4man.domains.account.service.AccountChecker;
import sys.be4man.domains.ban.model.entity.Ban;
import sys.be4man.domains.ban.model.entity.ProjectBan;
import sys.be4man.domains.ban.model.type.BanType;
import sys.be4man.domains.ban.model.type.RecurrenceType;
import sys.be4man.domains.ban.model.type.RecurrenceWeekOfMonth;
import sys.be4man.domains.ban.model.type.RecurrenceWeekday;
import sys.be4man.domains.ban.repository.BanRepository;
import sys.be4man.domains.ban.repository.ProjectBanRepository;
import sys.be4man.domains.deployment.model.entity.Deployment;
import sys.be4man.domains.deployment.model.type.DeploymentStatus;
import sys.be4man.domains.deployment.repository.DeploymentRepository;
import sys.be4man.domains.project.model.entity.Project;
import sys.be4man.domains.project.repository.ProjectRepository;
import sys.be4man.domains.project.repository.RelatedProjectRepository;
import sys.be4man.domains.schedule.dto.request.CreateBanRequest;
import sys.be4man.domains.schedule.dto.response.BanConflictCheckResponse;
import sys.be4man.domains.schedule.dto.response.BanResponse;
import sys.be4man.domains.schedule.dto.response.ConflictingDeploymentResponse;
import sys.be4man.domains.schedule.dto.response.DeploymentScheduleResponse;
import sys.be4man.domains.schedule.dto.response.ScheduleMetadataResponse;
import sys.be4man.domains.schedule.exception.type.ScheduleExceptionType;
import sys.be4man.domains.schedule.util.RecurrenceCalculator;
import sys.be4man.domains.schedule.util.RecurrenceCalculator.Period;
import sys.be4man.global.exception.BadRequestException;
import sys.be4man.global.exception.ForbiddenException;
import sys.be4man.global.exception.NotFoundException;

/**
 * 스케줄 관리 비즈니스 로직 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleServiceImpl implements ScheduleService {

    private final ProjectRepository projectRepository;
    private final BanRepository banRepository;
    private final ProjectBanRepository projectBanRepository;
    private final AccountChecker accountChecker;
    private final DeploymentRepository deploymentRepository;
    private final RelatedProjectRepository relatedProjectRepository;

    @Override
    @Transactional(readOnly = true)
    public ScheduleMetadataResponse getScheduleMetadata() {
        log.info("스케줄 관리 메타데이터 조회");

        List<Project> projects = projectRepository.findAllByIsDeletedFalse();
        return ScheduleMetadataResponse.from(projects);
    }

    @Override
    @Transactional
    public BanResponse createBan(CreateBanRequest request, Long accountId) {
        log.info("작업 금지 기간 생성 요청 - accountId: {}, title: {}", accountId, request.title());

        Account account = accountChecker.checkAccountExists(accountId);
        validatePermission(account.getRole());

        List<Project> projects = projectRepository
                .findAllByIdInAndIsDeletedFalse(request.relatedProjectIds());

        if (projects.size() != request.relatedProjectIds().size()) {
            throw new NotFoundException(ScheduleExceptionType.PROJECT_NOT_FOUND);
        }

        Ban newBan = Ban.builder()
                .account(account)
                .startDate(request.startDate())
                .startTime(request.startTime())
                .durationMinutes(request.durationMinutes())
                .endedAt(getEndedAt(request))
                .recurrenceType(request.recurrenceType())
                .recurrenceWeekday(request.recurrenceWeekday())
                .recurrenceWeekOfMonth(request.recurrenceWeekOfMonth())
                .recurrenceEndDate(request.recurrenceEndDate())
                .title(request.title())
                .description(request.description())
                .type(request.type())
                .build();

        try {
            newBan.validateDurationPositive();
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(ScheduleExceptionType.INVALID_DURATION);
        }

        try {
            newBan.validateRecurrenceOptions();
        } catch (IllegalStateException e) {
            throw new BadRequestException(ScheduleExceptionType.INVALID_RECURRENCE_OPTION);
        }

        final Ban savedBan = banRepository.save(newBan);

        List<ProjectBan> projectBans = projects.stream()
                .map(project -> ProjectBan.builder()
                        .project(project)
                        .ban(savedBan)
                        .build())
                .toList();
        projectBanRepository.saveAll(projectBans);

        cancelOverlappingDeploymentsForBan(savedBan, request.relatedProjectIds());

        List<String> relatedProjectNames = projects.stream()
                .map(Project::getName)
                .toList();

        return BanResponse.from(savedBan, relatedProjectNames);
    }

    private static LocalDateTime getEndedAt(CreateBanRequest request) {
        LocalDateTime startedAt = LocalDateTime.of(request.startDate(), request.startTime());

        LocalDateTime endedAt = request.endedAt();
        if (endedAt == null) {
            endedAt = startedAt.plusHours(request.durationMinutes());
        }

        if (endedAt.isBefore(startedAt)) {
            throw new BadRequestException(ScheduleExceptionType.INVALID_TIME_RANGE);
        }
        return endedAt;
    }

    /**
     * Ban과 겹치는 Deployment를 취소 (반복 Ban 포함)
     */
    private void cancelOverlappingDeploymentsForBan(Ban ban, List<Long> projectIds) {
        final int DEFAULT_RECURRENCE_QUERY_YEARS = 1;

        LocalDate queryStartDate = ban.getStartDate();
        LocalDate queryEndDate = ban.getRecurrenceEndDate() != null
                ? ban.getRecurrenceEndDate()
                : queryStartDate.plusYears(DEFAULT_RECURRENCE_QUERY_YEARS);

        List<Deployment> overlappingDeployments = findOverlappingDeploymentsForBan(
                ban, projectIds, queryStartDate, queryEndDate);

        if (overlappingDeployments.isEmpty()) {
            return;
        }

        log.info("Ban 생성으로 인한 Deployment 취소 - banId: {}, 취소할 Deployment 수: {}",
                 ban.getId(), overlappingDeployments.size());

        for (Deployment deployment : overlappingDeployments) {
            deployment.updateStatus(DeploymentStatus.CANCELED);
            log.info("Deployment 취소 - deploymentId: {}, title: {}, scheduledAt: {}",
                     deployment.getId(), deployment.getTitle(), deployment.getScheduledAt());
        }

        deploymentRepository.saveAll(overlappingDeployments);
    }

    /**
     * Ban과 겹치는 Deployment 찾기 (반복 Ban 포함)
     */
    private List<Deployment> findOverlappingDeploymentsForBan(
            Ban ban,
            List<Long> projectIds,
            LocalDate queryStartDate,
            LocalDate queryEndDate
    ) {
        List<Period> periods =
                RecurrenceCalculator.calculateRecurrenceDates(ban, queryStartDate, queryEndDate);

        return periods.stream()
                .flatMap(period ->
                                 deploymentRepository.findOverlappingDeployments(
                                         period.startDateTime(),
                                         period.endDateTime(),
                                         projectIds
                                 ).stream())
                .distinct()
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeploymentScheduleResponse> getDeploymentSchedules(LocalDate startDate,
            LocalDate endDate) {
        log.info("배포 작업 목록 조회 - startDate: {}, endDate: {}", startDate, endDate);

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

        List<Deployment> deployments = deploymentRepository.findScheduledDeployments(
                startDateTime,
                endDateTime
        );

        if (deployments.isEmpty()) {
            return List.of();
        }

        // N+1 쿼리 방지를 위한 배치 조회
        List<Long> deploymentIds = deployments.stream()
                .map(Deployment::getId)
                .toList();

        return deployments.stream()
                .map(DeploymentScheduleResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BanResponse> getBanSchedules(
            String query,
            LocalDate startDate,
            LocalDate endDate,
            BanType type,
            List<Long> projectIds
    ) {
        log.info("작업 금지 기간 목록 조회 - query: {}, startDate: {}, endDate: {}, type: {}, projectIds: {}",
                 query, startDate, endDate, type, projectIds);

        LocalDate effectiveStartDate = startDate != null ? startDate : LocalDate.now();
        LocalDate effectiveEndDate = endDate != null ? endDate : effectiveStartDate.plusMonths(1);

        List<Ban> bans = banRepository
                .findBans(query, effectiveStartDate, effectiveEndDate, type, projectIds);

        if (bans.isEmpty()) {
            return List.of();
        }

        Map<Long, List<String>> banProjectNamesMap = buildBanProjectNamesMap(bans);

        List<BanResponse> responses = new ArrayList<>();
        for (Ban ban : bans) {
            List<String> relatedProjects = banProjectNamesMap.getOrDefault(ban.getId(), List.of());

            if (ban.getRecurrenceType() == null) {
                responses.add(BanResponse.from(ban, relatedProjects));
            } else {
                List<Period> periods = RecurrenceCalculator
                        .calculateRecurrenceDates(ban, effectiveStartDate, effectiveEndDate);

                for (Period period : periods) {
                    responses.add(BanResponse.from(
                            ban,
                            relatedProjects,
                            period.startDateTime().toLocalDate(),
                            period.endDateTime()
                    ));
                }
            }
        }

        return responses;
    }

    /**
     * Ban 목록에 대한 프로젝트 이름 Map 생성
     */
    private Map<Long, List<String>> buildBanProjectNamesMap(List<Ban> bans) {
        List<Long> banIds = bans.stream()
                .map(Ban::getId)
                .toList();

        List<ProjectBan> projectBans = projectBanRepository.findAllByBan_IdInAndIsDeletedFalse(
                banIds);

        return projectBans.stream()
                .collect(Collectors.groupingBy(
                        pb -> pb.getBan().getId(),
                        Collectors.mapping(
                                pb -> pb.getProject().getName(),
                                Collectors.toList()
                        )
                ));
    }

    @Override
    @Transactional
    public void cancelBan(Long banId, Long accountId) {
        log.info("작업 금지 기간 취소 요청 - banId: {}, accountId: {}", banId, accountId);

        Account account = accountChecker.checkAccountExists(accountId);
        validatePermission(account.getRole());

        Ban ban = banRepository.findByIdAndIsDeletedFalse(banId)
                .orElseThrow(() -> new NotFoundException(ScheduleExceptionType.BAN_NOT_FOUND));

        ban.softDelete();
        banRepository.save(ban);

        log.info("작업 금지 기간 취소 완료 - banId: {}, accountId: {}", banId, accountId);
    }

//    @Override
//    @Transactional(readOnly = true)
//    public BanConflictCheckResponse checkBanConflicts(
//            List<Long> projectIds,
//            LocalDate startDate,
//            LocalTime startTime,
//            Integer durationHours,
//            RecurrenceType recurrenceType,
//            RecurrenceWeekday recurrenceWeekday,
//            RecurrenceWeekOfMonth recurrenceWeekOfMonth,
//            LocalDate recurrenceEndDate,
//            LocalDate queryStartDate,
//            LocalDate queryEndDate
//    ) {
//        log.info(
//                "Ban 충돌 체크 - projectIds: {}, startDate: {}, startTime: {}, recurrenceType: {}, queryRange: {} ~ {}",
//                projectIds, startDate, startTime, recurrenceType, queryStartDate, queryEndDate);
//
//        List<Period> periods = RecurrenceCalculator.calculateRecurrenceDates(
//                recurrenceType,
//                startDate,
//                startTime,
//                durationHours,
//                null,
//                recurrenceWeekday,
//                recurrenceWeekOfMonth,
//                recurrenceEndDate,
//                queryStartDate,
//                queryEndDate
//        );
//
//        List<Deployment> conflictingDeployments = periods.stream()
//                .flatMap(period ->
//                                 deploymentRepository.findOverlappingDeployments(
//                                         period.startDateTime(),
//                                         period.endDateTime(),
//                                         projectIds
//                                 ).stream())
//                .distinct()
//                .toList();
//
//        if (conflictingDeployments.isEmpty()) {
//            return new BanConflictCheckResponse(List.of());
//        }
//
//        List<Long> deploymentIds = conflictingDeployments.stream()
//                .map(Deployment::getId)
//                .toList();
//
//        Map<Long, List<String>> deploymentProjectNamesMap = relatedProjectRepository
//                .findByDeploymentIdIn(deploymentIds)
//                .stream()
//                .collect(Collectors.groupingBy(
//                        rp -> rp.getDeployment().getId(),
//                        Collectors.mapping(
//                                rp -> rp.getProject().getName(),
//                                Collectors.collectingAndThen(
//                                        Collectors.toList(),
//                                        list -> list.stream().distinct().sorted().toList()
//                                )
//                        )
//                ));
//
//        List<ConflictingDeploymentResponse> uniqueConflicts = conflictingDeployments.stream()
//                .map(deployment -> new ConflictingDeploymentResponse(
//                        deployment.getId(),
//                        deployment.getTitle(),
//                        deploymentProjectNamesMap.getOrDefault(deployment.getId(), List.of()),
//                        deployment.getScheduledAt(),
//                        deployment.getScheduledToEndedAt()
//                ))
//                .toList();
//
//        return new BanConflictCheckResponse(uniqueConflicts);
//    }

    /**
     * 권한 검증 (MANAGER, HEAD만 허용)
     */
    private void validatePermission(Role role) {
        if (role == Role.DEVELOPER) {
            throw new ForbiddenException(ScheduleExceptionType.INSUFFICIENT_PERMISSION);
        }
    }
}

