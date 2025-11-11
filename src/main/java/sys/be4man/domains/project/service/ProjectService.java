package sys.be4man.domains.project.service;

import java.util.List;
import sys.be4man.domains.project.dto.response.ProjectResponse;

public interface ProjectService {
    ProjectResponse getById(Long id);
    List<ProjectResponse> getAll();
}
