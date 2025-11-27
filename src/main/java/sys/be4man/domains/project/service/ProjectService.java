// 작성자 : 김민호
package sys.be4man.domains.project.service;

import java.util.List;
import sys.be4man.domains.project.dto.response.AccountProjectResponse;
import sys.be4man.domains.project.dto.response.ProjectResponse;

public interface ProjectService {
    ProjectResponse getById(Long id);
    List<ProjectResponse> getAll();
    List<AccountProjectResponse> getAccountProjectsByAccountId(Long accountId);
}
