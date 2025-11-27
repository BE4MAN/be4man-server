// 작성자 : 이원석
package sys.be4man.domains.schedule.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
import sys.be4man.domains.deployment.model.entity.Deployment;
import sys.be4man.domains.deployment.model.type.DeploymentStage;
import sys.be4man.domains.deployment.model.type.DeploymentStatus;
import sys.be4man.domains.deployment.repository.DeploymentRepository;
import sys.be4man.domains.pullrequest.model.entity.PullRequest;
import sys.be4man.domains.project.model.entity.Project;
import sys.be4man.domains.project.repository.ProjectRepository;
import sys.be4man.domains.schedule.dto.request.CreateBanRequest;
import sys.be4man.domains.schedule.dto.response.BanResponse;
import sys.be4man.domains.schedule.exception.type.ScheduleExceptionType;
import sys.be4man.global.exception.BadRequestException;
import sys.be4man.global.exception.ForbiddenException;
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

    @Mock
    private DeploymentRepository deploymentRepository;

    @InjectMocks
    private ScheduleServiceImpl scheduleService;

    private Account managerAccount;
    private Account developerAccount;
    private Account headAccount;
    private Project project1;
    private Project project2;

    @BeforeEach
    void setUp() {
        managerAccount = Account.builder()
                .githubId(12345L)
                .name("매니저 계정")
                .email("manager@example.com")
                .role(Role.MANAGER)
                .position(JobPosition.STAFF)
                .department(JobDepartment.IT)
                .githubAccessToken("test-token")
                .build();
        ReflectionTestUtils.setField(managerAccount, "id", 1L);

        developerAccount = Account.builder()
                .githubId(12346L)
                .name("개발자 계정")
                .email("developer@example.com")
                .role(Role.DEVELOPER)
                .position(JobPosition.STAFF)
                .department(JobDepartment.IT)
                .githubAccessToken("test-token")
                .build();
        ReflectionTestUtils.setField(developerAccount, "id", 2L);

        headAccount = Account.builder()
                .githubId(12347L)
                .name("헤드 계정")
                .email("head@example.com")
                .role(Role.HEAD)
                .position(JobPosition.STAFF)
                .department(JobDepartment.IT)
                .githubAccessToken("test-token")
                .build();
        ReflectionTestUtils.setField(headAccount, "id", 3L);

        project1 = Project.builder()
                .manager(managerAccount)
                .name("프로젝트 1")
                .isRunning(true)
                .jenkinsIp("192.168.1.1")
                .build();
        ReflectionTestUtils.setField(project1, "id", 1L);

        project2 = Project.builder()
                .manager(managerAccount)
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
        480,
        null,
                BanType.MAINTENANCE,
        Arrays.asList(1L, 2L),
        null,
        null,
        null,
        null
        );

        when(accountChecker.checkAccountExists(1L)).thenReturn(managerAccount);
        when(projectRepository.findAllByIdInAndIsDeletedFalse(Arrays.asList(1L, 2L)))
                .thenReturn(Arrays.asList(project1, project2));
        when(banRepository.save(any(Ban.class))).thenAnswer(invocation -> {
            Ban ban = invocation.getArgument(0);
            ReflectionTestUtils.setField(ban, "id", 1L);
            return ban;
        });
        when(projectBanRepository.saveAll(anyList())).thenAnswer(
                invocation -> invocation.getArgument(0));
        when(deploymentRepository.findOverlappingDeployments(any(), any(), any()))
                .thenReturn(Collections.emptyList());

        // when
        BanResponse response = scheduleService.createBan(request, 1L);

        // then
        assertThat(response).isNotNull();
        assertThat(response.title()).isEqualTo("테스트 작업 금지");
        assertThat(response.description()).isEqualTo("테스트 설명");
        assertThat(response.type()).isEqualTo(BanType.MAINTENANCE.name());
        assertThat(response.services()).containsExactlyInAnyOrder("프로젝트 1", "프로젝트 2");
        assertThat(response.startDate()).isEqualTo("2025-01-01");
        assertThat(response.startTime()).isEqualTo("10:00");
        assertThat(response.durationMinutes()).isEqualTo(480);
        assertThat(response.endedAt()).isEqualTo("2025-01-01T18:00");
        assertThat(response.registrant()).isEqualTo("매니저 계정");
        assertThat(response.registrantDepartment()).isEqualTo(JobDepartment.IT.name());

        verify(banRepository).save(any(Ban.class));
        verify(projectBanRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("작업 금지 기간 생성 - 종료일이 시작일보다 이전인 경우 실패")
    void createBan_InvalidDateRange() {
        // given
        CreateBanRequest request = new CreateBanRequest(
                "테스트 작업 금지",
                "설명",
                LocalDate.of(2025, 1, 1),
                LocalTime.of(10, 0),
                240,
                LocalDateTime.of(2024, 12, 31, 23, 0),
                BanType.MAINTENANCE,
                List.of(1L),
                null,
                null,
                null,
                null
        );

        when(accountChecker.checkAccountExists(1L)).thenReturn(managerAccount);
        when(projectRepository.findAllByIdInAndIsDeletedFalse(List.of(1L)))
                .thenReturn(List.of(project1));

        // when & then
        assertThatThrownBy(() -> scheduleService.createBan(request, 1L))
                .isInstanceOf(BadRequestException.class)
                .extracting("exceptionType.message")
                .isEqualTo(ScheduleExceptionType.INVALID_TIME_RANGE.getMessage());
    }

    @Test
    @DisplayName("작업 금지 기간 생성 - 같은 날 종료시간이 시작시간보다 이전인 경우 실패")
    void createBan_InvalidTimeRange() {
        // given
        CreateBanRequest request = new CreateBanRequest(
                "테스트 작업 금지",
                "설명",
                LocalDate.of(2025, 1, 1),
                LocalTime.of(18, 0),
                -120,
                null,
                BanType.MAINTENANCE,
                List.of(1L),
                null,
                null,
                null,
                null
        );

        when(accountChecker.checkAccountExists(1L)).thenReturn(managerAccount);
        when(projectRepository.findAllByIdInAndIsDeletedFalse(List.of(1L)))
                .thenReturn(List.of(project1));

        // when & then
        assertThatThrownBy(() -> scheduleService.createBan(request, 1L))
                .isInstanceOf(BadRequestException.class)
                .extracting("exceptionType.message")
                .isEqualTo(ScheduleExceptionType.INVALID_DURATION.getMessage());
    }

    @Test
    @DisplayName("작업 금지 기간 생성 - 존재하지 않는 프로젝트 ID인 경우 실패")
    void createBan_ProjectNotFound() {
        // given
        CreateBanRequest request = new CreateBanRequest(
                "테스트 작업 금지",
                "설명",
                LocalDate.of(2025, 1, 1),
                LocalTime.of(10, 0),
                240,
                null,
                BanType.MAINTENANCE,
                Arrays.asList(1L, 999L),
                null,
                null,
                null,
                null
        );

        when(accountChecker.checkAccountExists(1L)).thenReturn(managerAccount);
        when(projectRepository.findAllByIdInAndIsDeletedFalse(Arrays.asList(1L, 999L)))
                .thenReturn(Collections.singletonList(project1)); // 999L 프로젝트는 존재하지 않음

        // when & then
        assertThatThrownBy(() -> scheduleService.createBan(request, 1L))
                .isInstanceOf(NotFoundException.class)
                .extracting("exceptionType.message")
                .isEqualTo(ScheduleExceptionType.PROJECT_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("작업 금지 기간 생성 - 겹치는 Deployment 취소")
    void createBan_CancelOverlappingDeployments() {
        // given
        CreateBanRequest request = new CreateBanRequest(
                "테스트 작업 금지",
                "설명",
                LocalDate.of(2025, 1, 15),
                LocalTime.of(16, 0),
                240,
                null,
                BanType.MAINTENANCE,
                Arrays.asList(1L),
                null,
                null,
                null,
                null
        );

        when(accountChecker.checkAccountExists(1L)).thenReturn(managerAccount);
        when(projectRepository.findAllByIdInAndIsDeletedFalse(Arrays.asList(1L)))
                .thenReturn(Arrays.asList(project1));

        PullRequest testPullRequest = PullRequest.builder()
                .prNumber(123)
                .repositoryUrl("https://github.com/test/test-repo")
                .branch("feature-branch")
                .build();
        ReflectionTestUtils.setField(testPullRequest, "id", 1L);

        Deployment overlappingDeployment = Deployment.builder()
                .project(project1)
                .issuer(managerAccount)
                .pullRequest(testPullRequest)
                .title("겹치는 배포")
                .content("내용")
                .stage(DeploymentStage.DEPLOYMENT)
                .status(DeploymentStatus.PENDING)
                .scheduledAt(LocalDateTime.of(2025, 1, 15, 18, 0))
                .build();
        ReflectionTestUtils.setField(overlappingDeployment, "id", 1L);

        when(deploymentRepository.findOverlappingDeployments(
                LocalDateTime.of(2025, 1, 15, 16, 0),
                LocalDateTime.of(2025, 1, 15, 20, 0),
                Arrays.asList(1L)
        )).thenReturn(Arrays.asList(overlappingDeployment));

        when(banRepository.save(any(Ban.class))).thenAnswer(invocation -> {
            Ban ban = invocation.getArgument(0);
            ReflectionTestUtils.setField(ban, "id", 1L);
            return ban;
        });
        when(projectBanRepository.saveAll(any())).thenAnswer(
                invocation -> invocation.getArgument(0));
        when(deploymentRepository.saveAll(any())).thenAnswer(
                invocation -> invocation.getArgument(0));

        // when
        BanResponse response = scheduleService.createBan(request, 1L);

        // then
        assertThat(response).isNotNull();
        assertThat(overlappingDeployment.getStatus()).isEqualTo(DeploymentStatus.CANCELED);
        verify(deploymentRepository).saveAll(any(List.class));
    }

    @Test
    @DisplayName("작업 금지 기간 생성 - MANAGER 권한 성공")
    void createBan_Success_WithManagerRole() {
        // given
        CreateBanRequest request = new CreateBanRequest(
                "테스트 작업 금지",
                "테스트 설명",
                LocalDate.of(2025, 1, 1),
                LocalTime.of(10, 0),
                480,
                null,
                BanType.MAINTENANCE,
                Arrays.asList(1L),
                null,
                null,
                null,
                null
        );

        when(accountChecker.checkAccountExists(1L)).thenReturn(managerAccount);
        when(projectRepository.findAllByIdInAndIsDeletedFalse(Arrays.asList(1L)))
                .thenReturn(Arrays.asList(project1));
        when(banRepository.save(any(Ban.class))).thenAnswer(invocation -> {
            Ban ban = invocation.getArgument(0);
            ReflectionTestUtils.setField(ban, "id", 1L);
            return ban;
        });
        when(projectBanRepository.saveAll(anyList())).thenAnswer(
                invocation -> invocation.getArgument(0));
        when(deploymentRepository.findOverlappingDeployments(any(), any(), any()))
                .thenReturn(Collections.emptyList());

        // when
        BanResponse response = scheduleService.createBan(request, 1L);

        // then
        assertThat(response).isNotNull();
        assertThat(response.title()).isEqualTo("테스트 작업 금지");
        verify(banRepository).save(any(Ban.class));
    }

    @Test
    @DisplayName("작업 금지 기간 생성 - HEAD 권한 성공")
    void createBan_Success_WithHeadRole() {
        // given
        CreateBanRequest request = new CreateBanRequest(
                "테스트 작업 금지",
                "테스트 설명",
                LocalDate.of(2025, 1, 1),
                LocalTime.of(10, 0),
                480,
                null,
                BanType.MAINTENANCE,
                Arrays.asList(1L),
                null,
                null,
                null,
                null
        );

        when(accountChecker.checkAccountExists(3L)).thenReturn(headAccount);
        when(projectRepository.findAllByIdInAndIsDeletedFalse(Arrays.asList(1L)))
                .thenReturn(Arrays.asList(project1));
        when(banRepository.save(any(Ban.class))).thenAnswer(invocation -> {
            Ban ban = invocation.getArgument(0);
            ReflectionTestUtils.setField(ban, "id", 1L);
            return ban;
        });
        when(projectBanRepository.saveAll(anyList())).thenAnswer(
                invocation -> invocation.getArgument(0));
        when(deploymentRepository.findOverlappingDeployments(any(), any(), any()))
                .thenReturn(Collections.emptyList());

        // when
        BanResponse response = scheduleService.createBan(request, 3L);

        // then
        assertThat(response).isNotNull();
        assertThat(response.title()).isEqualTo("테스트 작업 금지");
        verify(banRepository).save(any(Ban.class));
    }

    @Test
    @DisplayName("작업 금지 기간 생성 - DEVELOPER 권한 실패")
    void createBan_Failure_WithDeveloperRole() {
        // given
        CreateBanRequest request = new CreateBanRequest(
                "테스트 작업 금지",
                "테스트 설명",
                LocalDate.of(2025, 1, 1),
                LocalTime.of(10, 0),
                480,
                null,
                BanType.MAINTENANCE,
                Arrays.asList(1L),
                null,
                null,
                null,
                null
        );

        when(accountChecker.checkAccountExists(2L)).thenReturn(developerAccount);

        // when & then
        assertThatThrownBy(() -> scheduleService.createBan(request, 2L))
                .isInstanceOf(ForbiddenException.class)
                .extracting("exceptionType.message")
                .isEqualTo(ScheduleExceptionType.INSUFFICIENT_PERMISSION.getMessage());
    }
}

