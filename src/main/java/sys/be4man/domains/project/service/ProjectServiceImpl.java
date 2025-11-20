package sys.be4man.domains.project.service;

import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sys.be4man.domains.account.repository.AccountRepository;
import sys.be4man.domains.project.dto.response.AccountProjectResponse;
import sys.be4man.domains.project.dto.response.ProjectResponse;
import sys.be4man.domains.project.model.entity.AccountProject;
import sys.be4man.domains.project.model.entity.Project;
import sys.be4man.domains.project.repository.AccountProjectRepository;
import sys.be4man.domains.project.repository.ProjectRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final AccountRepository accountRepository;
    private final AccountProjectRepository accountProjectRepository;

    @Override
    public ProjectResponse getById(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Project not found. id=" + id));
        return toResponse(project);
    }

    @Override
    public List<ProjectResponse> getAll() {
        return projectRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<AccountProjectResponse> getAccountProjectsByAccountId(Long accountId) {
        accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found. id=" + accountId));

        List<AccountProject> rows = accountProjectRepository.findAllByAccount_Id(accountId);

        return rows.stream()
                .map(ap -> AccountProjectResponse.builder()
                        .accountProjectId(ap.getId())
                        .accountId(ap.getAccount().getId())
                        .projectId(ap.getProject().getId())
                        .projectName(ap.getProject().getName())
                        .build())
                .collect(Collectors.toList());
    }

    private ProjectResponse toResponse(Project project) {
        return ProjectResponse.builder()
                .id(project.getId())
                .managerAccountId(project.getManager().getId())
                .name(project.getName())
                .discordWebhookUrl(project.getDiscordWebhookUrl())
                .isRunning(project.getIsRunning())
                .jenkinsIp(project.getJenkinsIp())
                .build();
    }
}
