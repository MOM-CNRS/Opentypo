package fr.cnrs.opentypo.entity;

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
 * Entité représentant les détails de description d'une entité (décors, marques)
 */
@jakarta.persistence.Entity
@Audited
@Table(name = "description_detail")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DescriptionDetail implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "decors", columnDefinition = "TEXT")
    private String decors;

    @Column(name = "marques", columnDefinition = "TEXT")
    private String marques;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fonction_id")
    private ReferentielOpentheso fonction;

    @Column(name = "metrologie", columnDefinition = "TEXT")
    private String metrologie;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entity_id", nullable = false, unique = true)
    private Entity entity;
}

