package sys.be4man.domains.ban.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import sys.be4man.domains.ban.model.entity.Ban;
import sys.be4man.domains.ban.repository.BanRepository;
import sys.be4man.domains.schedule.exception.type.ScheduleExceptionType;
import sys.be4man.global.exception.NotFoundException;

/**
 * 작업 금지 기간 존재 여부 검증 유틸리티
 */
@Component
@RequiredArgsConstructor
public class BanChecker {

    private final BanRepository banRepository;

    /**
     * Ban ID로 Ban 존재 여부 확인
     *
     * @param banId Ban ID
     * @return 존재하는 Ban
     * @throws NotFoundException Ban을 찾을 수 없는 경우
     */
    public Ban checkBanExists(Long banId) {
        return banRepository.findByIdAndIsDeletedFalse(banId)
                .orElseThrow(() -> new NotFoundException(ScheduleExceptionType.BAN_NOT_FOUND));
    }
}

