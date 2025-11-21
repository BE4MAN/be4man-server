package sys.be4man.domains.problem.model.entity;

import jakarta.persistence.*;
import lombok.*;
import sys.be4man.domains.deployment.model.entity.Deployment;
import sys.be4man.global.model.entity.BaseEntity;

@Entity
@Table(name = "problem_deployment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProblemDeployment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 배포 작업 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deployment_id", nullable = false)
    private Deployment deployment;

    /** 문제 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "problem_id", nullable = false)
    private Problem problem;

    @Builder
    private ProblemDeployment(Deployment deployment, Problem problem) {
        this.deployment = deployment;
        this.problem = problem;
    }
}
