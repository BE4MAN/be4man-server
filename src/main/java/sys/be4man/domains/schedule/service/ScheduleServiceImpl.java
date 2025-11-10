package sys.be4man.domains.schedule.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
import sys.be4man.domains.schedule.dto.request.CreateBanRequest;
import sys.be4man.domains.schedule.dto.response.BanResponse;
import sys.be4man.domains.schedule.dto.response.DeploymentScheduleResponse;
import sys.be4man.domains.schedule.dto.response.ScheduleMetadataResponse;
import sys.be4man.domains.schedule.exception.type.ScheduleExceptionType;
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

    @Override
    @Transactional(readOnly = true)
    public ScheduleMetadataResponse getScheduleMetadata() {
        log.info("스케줄 관리 메타데이터 조회");

        List<ScheduleMetadataResponse.ProjectMetadata> projects = projectRepository
                .findAllByIsDeletedFalse()
                .stream()
                .map(ScheduleMetadataResponse.ProjectMetadata::from)
                .toList();

        List<ScheduleMetadataResponse.BanMetadata> restrictedPeriodTypes = Stream.of(BanType.values())
                .map(ScheduleMetadataResponse.BanMetadata::from)
                .toList();

        List<ScheduleMetadataResponse.EnumMetadata> recurrenceTypes = buildRecurrenceTypesMetadata();
        List<ScheduleMetadataResponse.EnumMetadata> recurrenceWeekdays = Stream.of(RecurrenceWeekday.values())
                .map(weekday -> ScheduleMetadataResponse.EnumMetadata.builder()
                        .value(weekday.name())
                        .label(weekday.getKoreanName())
                        .build())
                .toList();
        List<ScheduleMetadataResponse.EnumMetadata> recurrenceWeeksOfMonth = Stream.of(RecurrenceWeekOfMonth.values())
                .map(weekOfMonth -> ScheduleMetadataResponse.EnumMetadata.builder()
                        .value(weekOfMonth.name())
                        .label(weekOfMonth.getKoreanName())
                        .build())
                .toList();

        return new ScheduleMetadataResponse(
                projects,
                restrictedPeriodTypes,
                recurrenceTypes,
                recurrenceWeekdays,
                recurrenceWeeksOfMonth
        );
    }

    /**
     * 반복 유형 메타데이터 생성 (NONE 포함)
     */
    private List<ScheduleMetadataResponse.EnumMetadata> buildRecurrenceTypesMetadata() {
        List<ScheduleMetadataResponse.EnumMetadata> types = Stream.of(RecurrenceType.values())
                .map(type -> ScheduleMetadataResponse.EnumMetadata.builder()
                        .value(type.name())
                        .label(type.getKoreanName())
                        .build())
                .collect(Collectors.toList());

        types.add(0, ScheduleMetadataResponse.EnumMetadata.builder()
                .value("NONE")
                .label("없음")
                .build());

        return types;
    }

    @Override
    @Transactional
    public BanResponse createBan(CreateBanRequest request, Long accountId) {
        log.info("작업 금지 기간 생성 요청 - accountId: {}, title: {}", accountId, request.title());

        Account account = accountChecker.checkAccountExists(accountId);

        List<Project> projects = projectRepository.findAllByIdInAndIsDeletedFalse(
                request.relatedProjectIds());

        if (projects.size() != request.relatedProjectIds().size()) {
            throw new NotFoundException(ScheduleExceptionType.PROJECT_NOT_FOUND);
        }

        LocalDateTime startedAt = null;
        if (request.startDate() != null) {
            startedAt = LocalDateTime.of(request.startDate(), request.startTime());
        }
        if (request.recurrenceType() == null && startedAt == null) {
            throw new BadRequestException(ScheduleExceptionType.START_DATE_REQUIRED);
        }

        LocalDateTime endedAt = request.endedAt();
        if (endedAt == null && startedAt != null) {
            endedAt = startedAt.plusHours(request.durationHours());
        }

        if (startedAt != null && endedAt != null && endedAt.isBefore(startedAt)) {
            throw new BadRequestException(ScheduleExceptionType.INVALID_TIME_RANGE);
        }

        Ban newBan = Ban.builder()
                .account(account)
                .startDate(request.startDate())
                .startTime(request.startTime())
                .durationHours(request.durationHours())
                .endedAt(endedAt)
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

        if (startedAt != null && endedAt != null) {
            cancelOverlappingDeployments(startedAt, endedAt, request.relatedProjectIds());
        }

        List<String> relatedProjectNames = projects.stream()
                .map(Project::getName)
                .toList();

        return BanResponse.from(savedBan, relatedProjectNames);
    }

    /**
     * Ban과 겹치는 Deployment를 취소
     */
    private void cancelOverlappingDeployments(
            LocalDateTime banStartDateTime,
            LocalDateTime banEndDateTime,
            List<Long> projectIds
    ) {
        List<Deployment> overlappingDeployments = deploymentRepository.findOverlappingDeployments(
                banStartDateTime,
                banEndDateTime,
                projectIds
        );

        if (overlappingDeployments.isEmpty()) {
            return;
        }

        log.info("Ban 생성으로 인한 Deployment 취소 - banStart: {}, banEnd: {}, 취소할 Deployment 수: {}",
                banStartDateTime, banEndDateTime, overlappingDeployments.size());

        for (Deployment deployment : overlappingDeployments) {
            deployment.updateStatus(DeploymentStatus.CANCELED);
            log.info("Deployment 취소 - deploymentId: {}, title: {}, scheduledAt: {}",
                    deployment.getId(), deployment.getTitle(), deployment.getScheduledAt());
        }

        deploymentRepository.saveAll(overlappingDeployments);
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

        List<Ban> bans = banRepository.findBans(query, startDate, endDate, type, projectIds);

        if (bans.isEmpty()) {
            return List.of();
        }

        List<Long> banIds = bans.stream()
                .map(Ban::getId)
                .toList();

        List<ProjectBan> projectBans = projectBanRepository.findAllByBan_IdInAndIsDeletedFalse(
                banIds);

        Map<Long, List<String>> banProjectNamesMap = projectBans.stream()
                .collect(Collectors.groupingBy(
                        pb -> pb.getBan().getId(),
                        Collectors.mapping(
                                pb -> pb.getProject().getName(),
                                Collectors.toList()
                        )
                ));

        return bans.stream()
                .map(ban -> {
                    List<String> relatedProjects = banProjectNamesMap.getOrDefault(ban.getId(),
                                                                                   List.of());
                    return BanResponse.from(ban, relatedProjects);
                })
                .toList();
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

    /**
     * 권한 검증 (MANAGER, HEAD만 허용)
     */
    private void validatePermission(Role role) {
        if (role == Role.DEVELOPER) {
            throw new ForbiddenException(ScheduleExceptionType.INSUFFICIENT_PERMISSION);
        }
    }
}

