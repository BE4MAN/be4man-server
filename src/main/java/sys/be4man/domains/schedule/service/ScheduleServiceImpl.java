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
import sys.be4man.domains.account.service.AccountChecker;
import sys.be4man.domains.ban.model.entity.Ban;
import sys.be4man.domains.ban.model.entity.ProjectBan;
import sys.be4man.domains.ban.model.type.BanType;
import sys.be4man.domains.ban.repository.BanRepository;
import sys.be4man.domains.ban.repository.ProjectBanRepository;
import sys.be4man.domains.deployment.model.entity.Deployment;
import sys.be4man.domains.deployment.repository.DeploymentRepository;
import sys.be4man.domains.project.model.entity.Project;
import sys.be4man.domains.project.repository.ProjectRepository;
import sys.be4man.domains.schedule.dto.request.CreateBanRequest;
import sys.be4man.domains.schedule.dto.response.BanResponse;
import sys.be4man.domains.schedule.dto.response.DeploymentScheduleResponse;
import sys.be4man.domains.schedule.dto.response.ScheduleMetadataResponse;
import sys.be4man.domains.schedule.exception.type.ScheduleExceptionType;
import sys.be4man.global.exception.BadRequestException;
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

        List<ScheduleMetadataResponse.BanMetadata> banTypes = Stream.of(BanType.values())
                .map(ScheduleMetadataResponse.BanMetadata::from)
                .toList();

        return new ScheduleMetadataResponse(projects, banTypes);
    }

    @Override
    @Transactional
    public BanResponse createBan(CreateBanRequest request, Long accountId) {
        log.info("작업 금지 기간 생성 요청 - accountId: {}, title: {}", accountId, request.title());

        validateDateAndTime(request.startDate(), request.startTime(), request.endDate(),
                            request.endTime());

        Account account = accountChecker.checkAccountExists(accountId);

        List<Project> projects = projectRepository.findAllByIdInAndIsDeletedFalse(
                request.relatedProjectIds());

        if (projects.size() != request.relatedProjectIds().size()) {
            throw new NotFoundException(ScheduleExceptionType.PROJECT_NOT_FOUND);
        }

        LocalDateTime startedAt = LocalDateTime.of(request.startDate(), request.startTime());
        LocalDateTime endedAt = LocalDateTime.of(request.endDate(), request.endTime());

        final Ban savedBan = banRepository.save(Ban.builder()
                                                        .account(account)
                                                        .title(request.title())
                                                        .description(request.description())
                                                        .type(request.type())
                                                        .startedAt(startedAt)
                                                        .endedAt(endedAt)
                                                        .build());

        List<ProjectBan> projectBans = projects.stream()
                .map(project -> ProjectBan.builder()
                        .project(project)
                        .ban(savedBan)
                        .build())
                .toList();
        projectBanRepository.saveAll(projectBans);

        List<String> relatedProjectNames = projects.stream()
                .map(Project::getName)
                .toList();

        return BanResponse.from(savedBan, relatedProjectNames);
    }

    /**
     * 날짜 및 시간 유효성 검증
     */
    private void validateDateAndTime(
            java.time.LocalDate startDate,
            java.time.LocalTime startTime,
            java.time.LocalDate endDate,
            java.time.LocalTime endTime
    ) {
        // 종료일이 시작일보다 이전인 경우
        if (endDate.isBefore(startDate)) {
            throw new BadRequestException(ScheduleExceptionType.INVALID_DATE_RANGE);
        }

        // 같은 날인 경우 종료시간이 시작시간보다 이전인지 확인
        if (endDate.equals(startDate) && endTime.isBefore(startTime)) {
            throw new BadRequestException(ScheduleExceptionType.INVALID_TIME_RANGE);
        }
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
}

