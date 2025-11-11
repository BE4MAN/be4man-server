package sys.be4man.domains.project.service;

import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sys.be4man.domains.project.dto.response.ProjectResponse;
import sys.be4man.domains.project.model.entity.Project;
import sys.be4man.domains.project.repository.ProjectRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;

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
