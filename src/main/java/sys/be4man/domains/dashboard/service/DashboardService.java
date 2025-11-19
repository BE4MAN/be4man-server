package sys.be4man.domains.dashboard.service;

import java.util.List;
import sys.be4man.domains.dashboard.dto.response.InProgressTaskResponse;
import sys.be4man.domains.dashboard.dto.response.NotificationResponse;
import sys.be4man.domains.dashboard.dto.response.PaginationResponse;
import sys.be4man.domains.dashboard.dto.response.PendingApprovalResponse;
import sys.be4man.domains.dashboard.dto.response.RecoveryResponse;

/**
 * 홈(Dashboard) 페이지 비즈니스 로직 인터페이스
 */
public interface DashboardService {

    /**
     * 승인 대기 목록 조회
     *
     * @param accountId 현재 사용자 ID
     * @return 승인 대기 목록
     */
    List<PendingApprovalResponse> getPendingApprovals(Long accountId);

    /**
     * 진행중인 업무 목록 조회
     *
     * @param accountId 현재 사용자 ID
     * @return 진행중인 업무 목록
     */
    List<InProgressTaskResponse> getInProgressTasks(Long accountId);

    /**
     * 알림 목록 조회
     *
     * @param accountId 현재 사용자 ID
     * @return 알림 목록 (취소 + 반려)
     */
    List<NotificationResponse> getNotifications(Long accountId);

    /**
     * 복구현황 목록 조회
     *
     * @param page     페이지 번호 (1부터 시작)
     * @param pageSize 페이지당 항목 수
     * @return 복구현황 목록과 페이지네이션 정보
     */
    PaginationResponse<RecoveryResponse> getRecovery(int page, int pageSize);
}

