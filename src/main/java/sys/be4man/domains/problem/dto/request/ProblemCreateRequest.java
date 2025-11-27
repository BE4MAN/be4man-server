// 작성자 : 김민호
package sys.be4man.domains.problem.dto.request;

import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import sys.be4man.domains.problem.model.type.Importance;

@Getter
@NoArgsConstructor
public class ProblemCreateRequest {

    private Long categoryId;
    private Long accountId;

    private String title;
    private String description;

    private Importance importance;

    private List<Long> deploymentIds;
    private Boolean isSolved;
}
