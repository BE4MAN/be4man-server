package sys.be4man.domains.schedule.service;

import java.time.LocalDateTime;
import java.util.List;
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
import sys.be4man.domains.project.model.entity.Project;
import sys.be4man.domains.project.repository.ProjectRepository;
import sys.be4man.domains.schedule.dto.request.CreateBanRequest;
import sys.be4man.domains.schedule.dto.response.BanResponse;
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

    @Override
    @Transactional(readOnly = true)
    public ScheduleMetadataResponse getScheduleMetadata() {
        log.info("스케줄 관리 메타데이터 조회");

        // 프로젝트 목록 조회 (삭제되지 않은 것만, ID 오름차순)
        List<ScheduleMetadataResponse.ProjectMetadata> projects = projectRepository
                .findAllByIsDeletedFalse()
                .stream()
                .map(ScheduleMetadataResponse.ProjectMetadata::from)
                .toList();

        // BanType enum 값 목록 변환
        List<ScheduleMetadataResponse.BanMetadata> banTypes = Stream.of(BanType.values())
                .map(ScheduleMetadataResponse.BanMetadata::from)
                .toList();

        return new ScheduleMetadataResponse(projects, banTypes);
    }

    @Override
    @Transactional
    public BanResponse createBan(CreateBanRequest request, Long accountId) {
        log.info("작업 금지 기간 생성 요청 - accountId: {}, title: {}", accountId, request.title());

        // 날짜/시간 유효성 검증
        validateDateAndTime(request.startDate(), request.startTime(), request.endDate(),
                            request.endTime());

        // Account 조회
        Account account = accountChecker.checkAccountExists(accountId);

        // Project 조회
        List<Project> projects = projectRepository.findAllByIdInAndIsDeletedFalse(
                request.relatedProjectIds());

        // 요청한 프로젝트 수와 조회된 프로젝트 수가 일치하는지 확인
        if (projects.size() != request.relatedProjectIds().size()) {
            throw new NotFoundException(ScheduleExceptionType.PROJECT_NOT_FOUND);
        }

        // LocalDateTime 계산
        LocalDateTime startedAt = LocalDateTime.of(request.startDate(), request.startTime());
        LocalDateTime endedAt = LocalDateTime.of(request.endDate(), request.endTime());

        // Ban 엔티티 생성 및 저장
        final Ban savedBan = banRepository.save(Ban.builder()
                                                        .account(account)
                                                        .title(request.title())
                                                        .description(request.description())
                                                        .type(request.type())
                                                        .startedAt(startedAt)
                                                        .endedAt(endedAt)
                                                        .build());

        // ProjectBan 엔티티들 생성 및 저장
        List<ProjectBan> projectBans = projects.stream()
                .map(project -> ProjectBan.builder()
                        .project(project)
                        .ban(savedBan)
                        .build())
                .toList();
        projectBanRepository.saveAll(projectBans);

        // 연관 프로젝트 이름 목록 추출
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
}

