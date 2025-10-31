package sys.be4man.domains.analysis.model.entity;

import jakarta.persistence.*;
import lombok.*;
import sys.be4man.global.model.entity.BaseEntity;

@Entity
@Table(name = "stage_run")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StageRun extends BaseEntity {

    @Id @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "build_run_id", nullable = false)
    private BuildRun buildRun;

    @Column(name = "stage_name", nullable = false)
    private String stageName;

    @Column(name = "is_success", nullable = false)
    private Boolean isSuccess;

    @Column(name = "order_index", nullable = false)
    private Long orderIndex;

    @Column(name = "log", nullable = false, columnDefinition = "TEXT")
    private String log;

    @Column(name = "problem_summary", columnDefinition = "TEXT")
    private String problemSummary;

    @Column(name = "problem_solution", columnDefinition = "TEXT")
    private String problemSolution;

    @Builder
    public StageRun(BuildRun buildRun, String stageName, Boolean isSuccess, Long orderIndex,
            String log, String problemSummary, String problemSolution) {
        this.buildRun = buildRun;
        this.stageName = stageName;
        this.isSuccess = isSuccess;
        this.orderIndex = orderIndex;
        this.log = log;
        this.problemSummary = problemSummary;
        this.problemSolution = problemSolution;
    }

    public void updateAnalysis(String problemSummary, String problemSolution) {
        this.problemSummary = problemSummary;
        this.problemSolution = problemSolution;
    }
}
