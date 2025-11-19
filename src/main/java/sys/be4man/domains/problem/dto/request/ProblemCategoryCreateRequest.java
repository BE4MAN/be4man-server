package sys.be4man.domains.problem.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ProblemCategoryCreateRequest {

    private Long projectId;
    private Long accountId;
    private String title;
    private String description;
}
