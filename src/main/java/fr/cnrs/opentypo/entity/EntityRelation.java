package fr.cnrs.opentypo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;

import java.io.Serializable;

/**
 * Entité de relation pour les relations parent-enfant entre entités
 */
@jakarta.persistence.Entity
@Audited
@Table(name = "entity_relation", uniqueConstraints = @UniqueConstraint(columnNames = {"parent_id", "child_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EntityRelation implements Serializable {

    @EmbeddedId
    private EntityRelationId id = new EntityRelationId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("parentId")
    @JoinColumn(name = "parent_id", nullable = false)
    private Entity parent;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("childId")
    @JoinColumn(name = "child_id", nullable = false)
    private Entity child;

    /**
     * Clé composite pour EntityRelation
     */
    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EntityRelationId implements Serializable {
        @Column(name = "parent_id")
        private Long parentId;

        @Column(name = "child_id")
        private Long childId;
    }
}

