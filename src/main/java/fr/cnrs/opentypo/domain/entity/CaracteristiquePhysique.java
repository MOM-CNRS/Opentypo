package fr.cnrs.opentypo.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;

import java.io.Serializable;

/**
 * Entité représentant les caractéristiques physiques d'une entité
 */
@jakarta.persistence.Entity
@Audited
@Table(name = "caracteristique_physique")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CaracteristiquePhysique implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entity_id", nullable = false, unique = true)
    private Entity entity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "metrologie_id")
    private ReferenceOpentheso metrologie;

    @Column(name = "materiaux", columnDefinition = "TEXT")
    private String materiaux;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "forme_id")
    private ReferenceOpentheso forme;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dimensions_id")
    private ReferenceOpentheso dimensions;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "technique_id")
    private ReferenceOpentheso technique;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fabrication_id")
    private ReferenceOpentheso fabrication;
}

