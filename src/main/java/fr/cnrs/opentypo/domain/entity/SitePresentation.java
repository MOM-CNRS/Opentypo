package fr.cnrs.opentypo.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;

import java.io.Serializable;

/**
 * Entité représentant le contenu de la présentation du site par langue.
 * Les langues disponibles sont celles présentes dans la table langue.
 */
@Entity
@Table(name = "site_presentation")
@Audited
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SitePresentation implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "langue_code", nullable = false, unique = true)
    private Langue langue;

    @Column(name = "titre", length = 255)
    private String titre;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
}
