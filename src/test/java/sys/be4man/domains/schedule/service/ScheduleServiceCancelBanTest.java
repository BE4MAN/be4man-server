// 작성자 : 이원석
package sys.be4man.domains.schedule.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import sys.be4man.domains.account.model.entity.Account;
import sys.be4man.domains.account.model.type.JobDepartment;
import sys.be4man.domains.account.model.type.JobPosition;
import sys.be4man.domains.account.model.type.Role;
import sys.be4man.domains.account.service.AccountChecker;
import sys.be4man.domains.ban.model.entity.Ban;
import sys.be4man.domains.ban.model.type.BanType;
import sys.be4man.domains.ban.repository.BanRepository;
import sys.be4man.domains.schedule.exception.type.ScheduleExceptionType;
import sys.be4man.global.exception.ForbiddenException;
import sys.be4man.global.exception.NotFoundException;

@ExtendWith(MockitoExtension.class)
@DisplayName("ScheduleService cancelBan 테스트")
class ScheduleServiceCancelBanTest {

    @Mock
    private BanRepository banRepository;

    @Mock
    private AccountChecker accountChecker;

    @InjectMocks
    private ScheduleServiceImpl scheduleService;

    private Account managerAccount;
    private Account developerAccount;
    private Account headAccount;
    private Ban testBan;

    @BeforeEach
    void setUp() {
        managerAccount = Account.builder()
                .githubId(12345L)
                .name("매니저")
                .email("manager@example.com")
                .role(Role.MANAGER)
                .position(JobPosition.STAFF)
                .department(JobDepartment.IT)
                .githubAccessToken("test-token")
                .build();
        ReflectionTestUtils.setField(managerAccount, "id", 1L);

        developerAccount = Account.builder()
                .githubId(12346L)
                .name("개발자")
                .email("developer@example.com")
                .role(Role.DEVELOPER)
                .position(JobPosition.STAFF)
                .department(JobDepartment.IT)
                .githubAccessToken("test-token")
                .build();
        ReflectionTestUtils.setField(developerAccount, "id", 2L);

        headAccount = Account.builder()
                .githubId(12347L)
                .name("헤드")
                .email("head@example.com")
                .role(Role.HEAD)
                .position(JobPosition.STAFF)
                .department(JobDepartment.IT)
                .githubAccessToken("test-token")
                .build();
        ReflectionTestUtils.setField(headAccount, "id", 3L);

        testBan = Ban.builder()
                .account(managerAccount)
                .title("테스트 작업 금지")
                .description("테스트 설명")
                .type(BanType.MAINTENANCE)
                .startDate(LocalDate.of(2025, 1, 15))
                .startTime(LocalTime.of(10, 0))
                .durationMinutes(240)
                .endedAt(LocalDateTime.of(2025, 1, 15, 14, 0))
                .build();
        ReflectionTestUtils.setField(testBan, "id", 1L);
    }

    @Test
    @DisplayName("작업 금지 기간 취소 - MANAGER 권한 성공")
    void cancelBan_Success_WithManagerRole() {
        // given
        Long banId = 1L;
        Long accountId = 1L;

        when(accountChecker.checkAccountExists(accountId)).thenReturn(managerAccount);
        when(banRepository.findByIdAndIsDeletedFalse(banId)).thenReturn(Optional.of(testBan));
        when(banRepository.save(any(Ban.class))).thenReturn(testBan);

        // when
        scheduleService.cancelBan(banId, accountId);

        // then
        verify(banRepository).findByIdAndIsDeletedFalse(banId);
        verify(banRepository).save(testBan);
        assertThat(testBan.getIsDeleted()).isTrue();
    }

    @Test
    @DisplayName("작업 금지 기간 취소 - HEAD 권한 성공")
    void cancelBan_Success_WithHeadRole() {
        // given
        Long banId = 1L;
        Long accountId = 3L;

        when(accountChecker.checkAccountExists(accountId)).thenReturn(headAccount);
        when(banRepository.findByIdAndIsDeletedFalse(banId)).thenReturn(Optional.of(testBan));
        when(banRepository.save(any(Ban.class))).thenReturn(testBan);

        // when
        scheduleService.cancelBan(banId, accountId);

        // then
        verify(banRepository).findByIdAndIsDeletedFalse(banId);
        verify(banRepository).save(testBan);
        assertThat(testBan.getIsDeleted()).isTrue();
    }

    @Test
    @DisplayName("작업 금지 기간 취소 - DEVELOPER 권한 실패")
    void cancelBan_Failure_WithDeveloperRole() {
        // given
        Long banId = 1L;
        Long accountId = 2L;

        when(accountChecker.checkAccountExists(accountId)).thenReturn(developerAccount);

        // when & then
        assertThatThrownBy(() -> scheduleService.cancelBan(banId, accountId))
                .isInstanceOf(ForbiddenException.class)
                .extracting("exceptionType.message")
                .isEqualTo(ScheduleExceptionType.INSUFFICIENT_PERMISSION.getMessage());
    }

    @Test
    @DisplayName("작업 금지 기간 취소 - 존재하지 않는 Ban")
    void cancelBan_Failure_BanNotFound() {
        // given
        Long banId = 999L;
        Long accountId = 1L;

        when(accountChecker.checkAccountExists(accountId)).thenReturn(managerAccount);
        when(banRepository.findByIdAndIsDeletedFalse(banId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> scheduleService.cancelBan(banId, accountId))
                .isInstanceOf(NotFoundException.class)
                .extracting("exceptionType.message")
                .isEqualTo(ScheduleExceptionType.BAN_NOT_FOUND.getMessage());
    }
}




