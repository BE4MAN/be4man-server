// 작성자 : 이원석
package sys.be4man.domains.schedule.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Arrays;
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
import sys.be4man.domains.ban.model.type.BanType;
import sys.be4man.domains.project.model.entity.Project;
import sys.be4man.domains.project.repository.ProjectRepository;
import sys.be4man.domains.schedule.dto.response.ScheduleMetadataResponse;

@ExtendWith(MockitoExtension.class)
@DisplayName("ScheduleService 테스트")
class ScheduleServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private sys.be4man.domains.ban.repository.BanRepository banRepository;

    @Mock
    private sys.be4man.domains.ban.repository.ProjectBanRepository projectBanRepository;

    @Mock
    private sys.be4man.domains.account.service.AccountChecker accountChecker;

    @Mock
    private sys.be4man.domains.deployment.repository.DeploymentRepository deploymentRepository;

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

        project1 = Project.builder()
                .manager(testAccount)
                .name("프로젝트 B")
                .isRunning(true)
                .jenkinsIp("192.168.1.1")
                .build();
        ReflectionTestUtils.setField(project1, "id", 1L);

        project2 = Project.builder()
                .manager(testAccount)
                .name("프로젝트 A")
                .isRunning(true)
                .jenkinsIp("192.168.1.2")
                .build();
        ReflectionTestUtils.setField(project2, "id", 2L);
    }

    @Test
    @DisplayName("스케줄 관리 메타데이터 조회 - 성공")
    void getScheduleMetadata_Success() {
        // given
        List<Project> projects = Arrays.asList(project1, project2);
        when(projectRepository.findAllByIsDeletedFalse()).thenReturn(projects);

        // when
        ScheduleMetadataResponse response = scheduleService.getScheduleMetadata();

        // then
        assertThat(response).isNotNull();
        assertThat(response.projects()).hasSize(2);
        // ID 오름차순 정렬 확인 (ID 1이 먼저)
        assertThat(response.projects().get(0).id()).isEqualTo(1L);
        assertThat(response.projects().get(0).name()).isEqualTo("프로젝트 B");
        assertThat(response.projects().get(1).id()).isEqualTo(2L);
        assertThat(response.projects().get(1).name()).isEqualTo("프로젝트 A");

        // BanType enum 값 확인
        assertThat(response.restrictedPeriodTypes()).hasSize(4);
        assertThat(response.restrictedPeriodTypes()).extracting("value")
                .containsExactlyInAnyOrder(
                        BanType.DB_MIGRATION.name(),
                        BanType.ACCIDENT.name(),
                        BanType.MAINTENANCE.name(),
                        BanType.EXTERNAL_SCHEDULE.name()
                );
        assertThat(response.restrictedPeriodTypes()).extracting("label")
                .containsExactlyInAnyOrder(
                        BanType.DB_MIGRATION.getKoreanName(),
                        BanType.ACCIDENT.getKoreanName(),
                        BanType.MAINTENANCE.getKoreanName(),
                        BanType.EXTERNAL_SCHEDULE.getKoreanName()
                );

        // RecurrenceType 확인
        assertThat(response.recurrenceTypes()).hasSize(3);
        assertThat(response.recurrenceTypes()).extracting("value")
                .containsExactly("DAILY", "WEEKLY", "MONTHLY");

        // RecurrenceWeekday 확인
        assertThat(response.recurrenceWeekdays()).hasSize(7);
        assertThat(response.recurrenceWeekdays()).extracting("value")
                .containsExactly("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN");

        // RecurrenceWeekOfMonth 확인
        assertThat(response.recurrenceWeeksOfMonth()).hasSize(5);
        assertThat(response.recurrenceWeeksOfMonth()).extracting("value")
                .containsExactly("FIRST", "SECOND", "THIRD", "FOURTH", "FIFTH");
    }

    @Test
    @DisplayName("스케줄 관리 메타데이터 조회 - 프로젝트가 없는 경우")
    void getScheduleMetadata_NoProjects() {
        // given
        when(projectRepository.findAllByIsDeletedFalse()).thenReturn(List.of());

        // when
        ScheduleMetadataResponse response = scheduleService.getScheduleMetadata();

        // then
        assertThat(response).isNotNull();
        assertThat(response.projects()).isEmpty();
        assertThat(response.restrictedPeriodTypes()).hasSize(4);
        assertThat(response.recurrenceTypes()).hasSize(3);
        assertThat(response.recurrenceWeekdays()).hasSize(7);
        assertThat(response.recurrenceWeeksOfMonth()).hasSize(5);
    }
}

