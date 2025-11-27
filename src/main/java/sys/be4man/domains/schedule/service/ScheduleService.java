// 작성자 : 이원석
package sys.be4man.domains.schedule.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import sys.be4man.domains.schedule.dto.request.CreateBanRequest;
import sys.be4man.domains.schedule.dto.response.BanConflictCheckResponse;
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

    /**
     * 작업 금지 기간 목록 조회
     */
    List<BanResponse> getBanSchedules(
            String query,
            LocalDate startDate,
            LocalDate endDate,
            sys.be4man.domains.ban.model.type.BanType type,
            List<Long> projectIds
    );

    /**
     * 작업 금지 기간 취소
     * - MANAGER, HEAD 권한만 가능
     */
    void cancelBan(Long banId, Long accountId);

    /**
     * Ban 등록 전 충돌 Deployment 조회
     * - 입력 폼이 모두 채워진 상태에서 충돌하는 Deployment 목록 조회
     */
    BanConflictCheckResponse checkBanConflicts(
            List<Long> projectIds,
            LocalDate startDate,
            LocalTime startTime,
            Integer durationMinutes,
            sys.be4man.domains.ban.model.type.RecurrenceType recurrenceType,
            sys.be4man.domains.ban.model.type.RecurrenceWeekday recurrenceWeekday,
            sys.be4man.domains.ban.model.type.RecurrenceWeekOfMonth recurrenceWeekOfMonth,
            LocalDate recurrenceEndDate,
            LocalDate queryStartDate,
            LocalDate queryEndDate
    );
}

