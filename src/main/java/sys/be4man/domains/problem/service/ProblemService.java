// 작성자 : 김민호
package sys.be4man.domains.problem.service;

import java.util.List;
import sys.be4man.domains.problem.dto.request.ProblemCreateRequest;
import sys.be4man.domains.problem.dto.request.ProblemUpdateRequest;
import sys.be4man.domains.problem.dto.response.ProblemResponse;

public interface ProblemService {

    Long createProblem(ProblemCreateRequest request);

    ProblemResponse getProblem(Long id);

    List<ProblemResponse> getProblemsByCategory(Long categoryId);
    List<ProblemResponse> getAllProblems();
    void updateProblem(Long id, ProblemUpdateRequest request);
    void deleteProblem(Long id);
}
