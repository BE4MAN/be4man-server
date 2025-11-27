// 작성자 : 김민호
// src/main/java/sys/be4man/domains/problem/dto/response/ProblemCategoryResponse.java
package sys.be4man.domains.problem.dto.response;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProblemCategoryResponse {

    private Long id;
    private Long projectId;
    private Long accountId;

    private String title;
    private String description;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
