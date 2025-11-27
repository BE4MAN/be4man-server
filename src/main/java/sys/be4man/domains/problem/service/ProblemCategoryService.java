// 작성자 : 김민호
package sys.be4man.domains.problem.service;

import java.util.List;
import sys.be4man.domains.problem.dto.request.ProblemCategoryCreateRequest;
import sys.be4man.domains.problem.dto.response.ProblemCategoryResponse;

public interface ProblemCategoryService {

    Long createCategory(ProblemCategoryCreateRequest request);

    ProblemCategoryResponse getCategory(Long id);

    List<ProblemCategoryResponse> getCategoriesByProject(Long projectId);

    List<ProblemCategoryResponse> getAllCategories();
}
