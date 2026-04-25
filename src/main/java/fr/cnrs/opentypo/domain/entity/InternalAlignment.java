package fr.cnrs.opentypo.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;

import java.io.Serializable;

@jakarta.persistence.Entity
@Table(name = "internal_alignment", uniqueConstraints = @UniqueConstraint(columnNames = {"source_type_id", "target_type_id"}))
@Audited
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InternalAlignment implements Serializable {

    @EmbeddedId
    private InternalAlignmentId id = new InternalAlignmentId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("sourceTypeId")
    @JoinColumn(name = "source_type_id", nullable = false)
    private Entity sourceType;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("targetTypeId")
    @JoinColumn(name = "target_type_id", nullable = false)
    private Entity targetType;

    @Column(name = "match_type", nullable = false, length = 20)
    private String matchType;

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InternalAlignmentId implements Serializable {
        @Column(name = "source_type_id")
        private Long sourceTypeId;

        @Column(name = "target_type_id")
        private Long targetTypeId;
    }
}
