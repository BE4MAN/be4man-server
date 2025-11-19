package sys.be4man.domains.dashboard.service;

import java.util.List;
import sys.be4man.domains.dashboard.dto.response.PendingApprovalResponse;

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

    // TODO: Step 3에서 진행중인 업무 목록 조회 메서드 구현 예정
    // List<InProgressTaskResponse> getInProgressTasks(Long accountId);

    // TODO: Step 4에서 알림 목록 조회 메서드 구현 예정
    // List<NotificationResponse> getNotifications(Long accountId);

    // TODO: Step 5에서 복구현황 목록 조회 메서드 구현 예정
    // PageResponse<RecoveryResponse> getRecovery(int page, int pageSize);
}

