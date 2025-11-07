package sys.be4man.domains.schedule.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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
import sys.be4man.domains.ban.repository.ProjectBanRepository;
import sys.be4man.domains.project.model.entity.Project;
import sys.be4man.domains.project.repository.ProjectRepository;
import sys.be4man.domains.schedule.dto.request.CreateBanRequest;
import sys.be4man.domains.schedule.dto.response.BanResponse;
import sys.be4man.domains.schedule.exception.type.ScheduleExceptionType;
import sys.be4man.global.exception.BadRequestException;
import sys.be4man.global.exception.NotFoundException;

@ExtendWith(MockitoExtension.class)
@DisplayName("ScheduleService createBan 테스트")
class ScheduleServiceCreateBanTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private BanRepository banRepository;

    @Mock
    private ProjectBanRepository projectBanRepository;

    @Mock
    private AccountChecker accountChecker;

    @InjectMocks
    private ScheduleServiceImpl scheduleService;

    private Account testAccount;
    private Project project1;
    private Project project2;

    @BeforeEach
    void setUp() {
        testAccount = Account.builder()
                .githubId(12345L)
                .name("테스트 계정")
                .email("test@example.com")
                .role(Role.DEVELOPER)
                .position(JobPosition.STAFF)
                .department(JobDepartment.IT)
                .githubAccessToken("test-token")
                .build();
        ReflectionTestUtils.setField(testAccount, "id", 1L);

        project1 = Project.builder()
                .manager(testAccount)
                .name("프로젝트 1")
                .isRunning(true)
                .jenkinsIp("192.168.1.1")
                .build();
        ReflectionTestUtils.setField(project1, "id", 1L);

        project2 = Project.builder()
                .manager(testAccount)
                .name("프로젝트 2")
                .isRunning(true)
                .jenkinsIp("192.168.1.2")
                .build();
        ReflectionTestUtils.setField(project2, "id", 2L);
    }

    @Test
    @DisplayName("작업 금지 기간 생성 - 성공")
    void createBan_Success() {
        // given
        CreateBanRequest request = new CreateBanRequest(
                "테스트 작업 금지",
                "테스트 설명",
                LocalDate.of(2025, 1, 1),
                LocalTime.of(10, 0),
                LocalDate.of(2025, 1, 2),
                LocalTime.of(18, 0),
                BanType.MAINTENANCE,
                Arrays.asList(1L, 2L)
        );

        when(accountChecker.checkAccountExists(1L)).thenReturn(testAccount);
        when(projectRepository.findAllByIdInAndIsDeletedFalse(Arrays.asList(1L, 2L)))
                .thenReturn(Arrays.asList(project1, project2));
        when(banRepository.save(any(Ban.class))).thenAnswer(invocation -> {
            Ban ban = invocation.getArgument(0);
            ReflectionTestUtils.setField(ban, "id", 1L);
            return ban;
        });
        when(projectBanRepository.saveAll(any())).thenAnswer(
                invocation -> invocation.getArgument(0));

        // when
        BanResponse response = scheduleService.createBan(request, 1L);

        // then
        assertThat(response).isNotNull();
        assertThat(response.title()).isEqualTo("테스트 작업 금지");
        assertThat(response.description()).isEqualTo("테스트 설명");
        assertThat(response.type()).isEqualTo(BanType.MAINTENANCE.name());
        assertThat(response.relatedProjects()).containsExactlyInAnyOrder("프로젝트 1", "프로젝트 2");
        assertThat(response.startDate()).isEqualTo("2025-01-01");
        assertThat(response.endDate()).isEqualTo("2025-01-02");

        verify(banRepository).save(any(Ban.class));
        verify(projectBanRepository).saveAll(any(List.class));
    }

    @Test
    @DisplayName("작업 금지 기간 생성 - 종료일이 시작일보다 이전인 경우 실패")
    void createBan_InvalidDateRange() {
        // given
        CreateBanRequest request = new CreateBanRequest(
                "테스트 작업 금지",
                null,
                LocalDate.of(2025, 1, 2),
                LocalTime.of(10, 0),
                LocalDate.of(2025, 1, 1),
                LocalTime.of(18, 0),
                BanType.MAINTENANCE,
                List.of(1L)
        );

        // when & then
        assertThatThrownBy(() -> scheduleService.createBan(request, 1L))
                .isInstanceOf(BadRequestException.class)
                .extracting("exceptionType.message")
                .isEqualTo(ScheduleExceptionType.INVALID_DATE_RANGE.getMessage());
    }

    @Test
    @DisplayName("작업 금지 기간 생성 - 같은 날 종료시간이 시작시간보다 이전인 경우 실패")
    void createBan_InvalidTimeRange() {
        // given
        CreateBanRequest request = new CreateBanRequest(
                "테스트 작업 금지",
                null,
                LocalDate.of(2025, 1, 1),
                LocalTime.of(18, 0),
                LocalDate.of(2025, 1, 1),
                LocalTime.of(10, 0),
                BanType.MAINTENANCE,
                List.of(1L)
        );

        // when & then
        assertThatThrownBy(() -> scheduleService.createBan(request, 1L))
                .isInstanceOf(BadRequestException.class)
                .extracting("exceptionType.message")
                .isEqualTo(ScheduleExceptionType.INVALID_TIME_RANGE.getMessage());
    }

    @Test
    @DisplayName("작업 금지 기간 생성 - 존재하지 않는 프로젝트 ID인 경우 실패")
    void createBan_ProjectNotFound() {
        // given
        CreateBanRequest request = new CreateBanRequest(
                "테스트 작업 금지",
                null,
                LocalDate.of(2025, 1, 1),
                LocalTime.of(10, 0),
                LocalDate.of(2025, 1, 2),
                LocalTime.of(18, 0),
                BanType.MAINTENANCE,
                Arrays.asList(1L, 999L)
        );

        when(accountChecker.checkAccountExists(1L)).thenReturn(testAccount);
        when(projectRepository.findAllByIdInAndIsDeletedFalse(Arrays.asList(1L, 999L)))
                .thenReturn(Collections.singletonList(project1)); // 999L 프로젝트는 존재하지 않음

        // when & then
        assertThatThrownBy(() -> scheduleService.createBan(request, 1L))
                .isInstanceOf(NotFoundException.class)
                .extracting("exceptionType.message")
                .isEqualTo(ScheduleExceptionType.PROJECT_NOT_FOUND.getMessage());
    }
}

