package sys.be4man.domains.schedule.service;

import java.time.LocalDate;
import java.util.List;
import sys.be4man.domains.schedule.dto.request.CreateBanRequest;
import sys.be4man.domains.schedule.dto.response.BanResponse;
import sys.be4man.domains.schedule.dto.response.DeploymentScheduleResponse;
import sys.be4man.domains.schedule.dto.response.ScheduleMetadataResponse;

/**
 * 스케줄 관리 비즈니스 로직 인터페이스
 */
public interface ScheduleService {

    /**
     * 스케줄 관리 메타데이터 조회
     * - 프로젝트 목록
     * - 작업 금지 유형 목록
     */
    ScheduleMetadataResponse getScheduleMetadata();

    /**
     * 작업 금지 기간 생성
     */
    BanResponse createBan(CreateBanRequest request, Long accountId);

    /**
     * 배포 작업 목록 조회
     */
    List<DeploymentScheduleResponse> getDeploymentSchedules(LocalDate startDate, LocalDate endDate);
}

