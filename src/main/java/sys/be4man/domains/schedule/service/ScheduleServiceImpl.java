package sys.be4man.domains.schedule.service;

import java.util.List;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sys.be4man.domains.ban.model.type.BanType;
import sys.be4man.domains.project.repository.ProjectRepository;
import sys.be4man.domains.schedule.dto.response.ScheduleMetadataResponse;

/**
 * 스케줄 관리 비즈니스 로직 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleServiceImpl implements ScheduleService {

    private final ProjectRepository projectRepository;

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
}

