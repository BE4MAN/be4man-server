package sys.be4man.domains.problem.model.entity;

import jakarta.persistence.*;
import lombok.*;
import sys.be4man.domains.account.model.entity.Account;
import sys.be4man.domains.project.model.entity.Project;
import sys.be4man.global.model.entity.BaseEntity;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "problem_category")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProblemCategory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 프로젝트 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    /** 생성자 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    /** 제목 */
    @Column(nullable = false)
    private String title;

    /** 설명 */
    @Column(nullable = false, columnDefinition = "text")
    private String description;

    @OneToMany(mappedBy = "category")
    private List<Problem> problems = new ArrayList<>();

    @Builder
    private ProblemCategory(Project project,
            Account account,
            String title,
            String description) {
        this.project = project;
        this.account = account;
        this.title = title;
        this.description = description;
    }

    public void updateTitle(String title) {
        this.title = title;
    }

    public void updateDescription(String description) {
        this.description = description;
    }
}
