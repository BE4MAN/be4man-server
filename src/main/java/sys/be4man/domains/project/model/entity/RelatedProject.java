package sys.be4man.domains.project.model.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 배포 작업-프로젝트 연관관계 엔티티
 */
@Entity
@Table(name = "related_project")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RelatedProject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_id", nullable = false)
    private Project relatedProject;


    @Builder
    public RelatedProject(Project project, Project relatedProject) {
        this.project = project;
        this.relatedProject = relatedProject;
    }
}
