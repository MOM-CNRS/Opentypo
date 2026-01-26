package fr.cnrs.opentypo.domain.entity;

import jakarta.persistence.*;
import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;

import java.io.Serializable;

/**
 * Entité représentant la description de pâte multilingue
 */
@Entity
@Table(name = "description_pate")
@Audited
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DescriptionPate implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "description", length = 255)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "couleur_id")
    private ReferenceOpentheso couleur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nature_id")
    private ReferenceOpentheso nature;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inclusion_id")
    private ReferenceOpentheso inclusion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuisson_id")
    private ReferenceOpentheso cuisson;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entity_id", nullable = false, unique = true)
    private fr.cnrs.opentypo.domain.entity.Entity entity;

}

