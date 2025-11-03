package sys.be4man.domains.schedule.service;

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
}

