package sys.be4man.domains.schedule.service;

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
import sys.be4man.domains.ban.model.entity.Ban;
import sys.be4man.domains.ban.model.entity.ProjectBan;
import sys.be4man.domains.ban.model.type.BanType;
import sys.be4man.domains.ban.repository.BanRepository;
import sys.be4man.domains.ban.repository.ProjectBanRepository;
import sys.be4man.domains.project.model.entity.Project;
import sys.be4man.domains.schedule.dto.response.BanResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("작업 금지 기간 목록 조회 서비스 테스트")
class ScheduleServiceGetBanSchedulesTest {

    @Mock
    private BanRepository banRepository;

    @Mock
    private ProjectBanRepository projectBanRepository;

    @InjectMocks
    private ScheduleServiceImpl scheduleService;

    private Account account;
    private Project project1;
    private Project project2;
    private Ban ban1;
    private Ban ban2;
    private ProjectBan projectBan1;
    private ProjectBan projectBan2;

    @BeforeEach
    void setUp() {
        // Account 모킹
        account = Account.builder()
                .githubId(1L)
                .name("테스트 계정")
                .email("test@example.com")
                .role(Role.DEVELOPER)
                .position(JobPosition.STAFF)
                .department(JobDepartment.IT)
                .githubAccessToken("test-token")
                .build();
        ReflectionTestUtils.setField(account, "id", 1L);

        // Project 모킹
        project1 = Project.builder()
                .manager(account)
                .name("프로젝트 1")
                .isRunning(true)
                .jenkinsIp("192.168.1.1")
                .build();
        ReflectionTestUtils.setField(project1, "id", 1L);

        project2 = Project.builder()
                .manager(account)
                .name("프로젝트 2")
                .isRunning(true)
                .jenkinsIp("192.168.1.2")
                .build();
        ReflectionTestUtils.setField(project2, "id", 2L);

        // Ban 모킹
        ban1 = Ban.builder()
                .account(account)
                .title("작업 금지 기간 1")
                .description("설명 1")
                .type(BanType.MAINTENANCE)
                .startedAt(LocalDateTime.of(2025, 1, 15, 9, 0))
                .endedAt(LocalDateTime.of(2025, 1, 15, 18, 0))
                .build();
        ReflectionTestUtils.setField(ban1, "id", 1L);

        ban2 = Ban.builder()
                .account(account)
                .title("작업 금지 기간 2")
                .description("설명 2")
                .type(BanType.DB_MIGRATION)
                .startedAt(LocalDateTime.of(2025, 1, 16, 10, 0))
                .endedAt(LocalDateTime.of(2025, 1, 16, 12, 0))
                .build();
        ReflectionTestUtils.setField(ban2, "id", 2L);

        // ProjectBan 모킹
        projectBan1 = ProjectBan.builder()
                .ban(ban1)
                .project(project1)
                .build();

        projectBan2 = ProjectBan.builder()
                .ban(ban1)
                .project(project2)
                .build();
    }

    @Test
    @DisplayName("작업 금지 기간 목록 조회 - 성공")
    void getBanSchedules_Success() {
        // given
        String query = null;
        LocalDate startDate = LocalDate.of(2025, 1, 15);
        LocalDate endDate = LocalDate.of(2025, 1, 17);
        BanType type = null;
        List<Long> projectIds = null;

        List<Ban> bans = Arrays.asList(ban1, ban2);
        List<ProjectBan> projectBans = Arrays.asList(projectBan1, projectBan2);

        when(banRepository.findBans(query, startDate, endDate, type, projectIds))
                .thenReturn(bans);
        when(projectBanRepository.findAllByBan_IdInAndIsDeletedFalse(anyList()))
                .thenReturn(projectBans);

        // when
        List<BanResponse> response = scheduleService.getBanSchedules(
                query, startDate, endDate, type, projectIds);

        // then
        assertThat(response).isNotNull();
        assertThat(response).hasSize(2);

        // 첫 번째 작업 금지 기간 검증
        BanResponse first = response.get(0);
        assertThat(first.id()).isEqualTo(ban1.getId().toString());
        assertThat(first.title()).isEqualTo("작업 금지 기간 1");
        assertThat(first.description()).isEqualTo("설명 1");
        assertThat(first.type()).isEqualTo(BanType.MAINTENANCE.name());
        assertThat(first.startDate()).isEqualTo("2025-01-15");
        assertThat(first.startTime()).isEqualTo("09:00");
        assertThat(first.endDate()).isEqualTo("2025-01-15");
        assertThat(first.endTime()).isEqualTo("18:00");
        assertThat(first.relatedProjects()).containsExactly("프로젝트 1", "프로젝트 2");

        // 두 번째 작업 금지 기간 검증
        BanResponse second = response.get(1);
        assertThat(second.id()).isEqualTo(ban2.getId().toString());
        assertThat(second.title()).isEqualTo("작업 금지 기간 2");
        assertThat(second.description()).isEqualTo("설명 2");
        assertThat(second.type()).isEqualTo(BanType.DB_MIGRATION.name());
        assertThat(second.startDate()).isEqualTo("2025-01-16");
        assertThat(second.startTime()).isEqualTo("10:00");
        assertThat(second.endDate()).isEqualTo("2025-01-16");
        assertThat(second.endTime()).isEqualTo("12:00");
        assertThat(second.relatedProjects()).isEmpty();
    }

    @Test
    @DisplayName("작업 금지 기간 목록 조회 - 빈 목록")
    void getBanSchedules_Empty() {
        // given
        String query = null;
        LocalDate startDate = LocalDate.of(2025, 1, 15);
        LocalDate endDate = LocalDate.of(2025, 1, 17);
        BanType type = null;
        List<Long> projectIds = null;

        when(banRepository.findBans(query, startDate, endDate, type, projectIds))
                .thenReturn(Collections.emptyList());

        // when
        List<BanResponse> response = scheduleService.getBanSchedules(
                query, startDate, endDate, type, projectIds);

        // then
        assertThat(response).isNotNull();
        assertThat(response).isEmpty();
    }

    @Test
    @DisplayName("작업 금지 기간 목록 조회 - 검색어 필터링")
    void getBanSchedules_WithQuery() {
        // given
        String query = "금지";
        LocalDate startDate = null;
        LocalDate endDate = null;
        BanType type = null;
        List<Long> projectIds = null;

        List<Ban> bans = Arrays.asList(ban1, ban2);
        List<ProjectBan> projectBans = Arrays.asList(projectBan1, projectBan2);

        when(banRepository.findBans(query, startDate, endDate, type, projectIds))
                .thenReturn(bans);
        when(projectBanRepository.findAllByBan_IdInAndIsDeletedFalse(anyList()))
                .thenReturn(projectBans);

        // when
        List<BanResponse> response = scheduleService.getBanSchedules(
                query, startDate, endDate, type, projectIds);

        // then
        assertThat(response).isNotNull();
        assertThat(response).hasSize(2);
    }

    @Test
    @DisplayName("작업 금지 기간 목록 조회 - 타입 필터링")
    void getBanSchedules_WithType() {
        // given
        String query = null;
        LocalDate startDate = null;
        LocalDate endDate = null;
        BanType type = BanType.MAINTENANCE;
        List<Long> projectIds = null;

        List<Ban> bans = Arrays.asList(ban1);
        List<ProjectBan> projectBans = Arrays.asList(projectBan1, projectBan2);

        when(banRepository.findBans(query, startDate, endDate, type, projectIds))
                .thenReturn(bans);
        when(projectBanRepository.findAllByBan_IdInAndIsDeletedFalse(anyList()))
                .thenReturn(projectBans);

        // when
        List<BanResponse> response = scheduleService.getBanSchedules(
                query, startDate, endDate, type, projectIds);

        // then
        assertThat(response).isNotNull();
        assertThat(response).hasSize(1);
        assertThat(response.get(0).type()).isEqualTo(BanType.MAINTENANCE.name());
    }
}

