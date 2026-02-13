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
 * Caractéristiques physiques spécifiques pour les entités de la collection MONNAIE.
 */
@jakarta.persistence.Entity
@Audited
@Table(name = "caracteristique_physique_monnaie")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CaracteristiquePhysiqueMonnaie implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "materiau_id")
    private ReferenceOpentheso materiau;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "denomination_id")
    private ReferenceOpentheso denomination;

    @Column(name = "metrologie", columnDefinition = "TEXT")
    private String metrologie;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "valeur_id")
    private ReferenceOpentheso valeur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "technique_id")
    private ReferenceOpentheso technique;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fabrication_id")
    private ReferenceOpentheso fabrication;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entity_id", nullable = false, unique = true)
    private Entity entity;
}
