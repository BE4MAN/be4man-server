package sys.be4man.domains.problem.model.entity;

import jakarta.persistence.*;
import lombok.*;
import sys.be4man.domains.account.model.entity.Account;
import sys.be4man.domains.problem.model.type.Importance;
import sys.be4man.global.model.entity.BaseEntity;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "problem")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Problem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 문제 유형 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private ProblemCategory category;

    /** 생성자 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    /** 제목 */
    @Column(nullable = false)
    private String title;

    /** 설명(발생상황, 예방법) */
    @Column(nullable = false, columnDefinition = "text")
    private String description;

    /** 중요도 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Importance importance;

    /** 배포 매핑들 */
    @OneToMany(mappedBy = "problem")
    private List<ProblemDeployment> problemDeployments = new ArrayList<>();

    @Builder
    private Problem(ProblemCategory category,
            Account account,
            String title,
            String description,
            Importance importance) {
        this.category = category;
        this.account = account;
        this.title = title;
        this.description = description;
        this.importance = importance;
    }

    public void addProblemDeployment(ProblemDeployment link) {
        this.problemDeployments.add(link);
    }

    public void updateTitle(String title) {
        this.title = title;
    }

    public void updateDescription(String description) {
        this.description = description;
    }

    public void updateImportance(Importance importance) {
        this.importance = importance;
    }
}
