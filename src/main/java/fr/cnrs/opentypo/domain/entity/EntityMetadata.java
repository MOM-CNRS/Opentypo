package fr.cnrs.opentypo.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;

import java.io.Serializable;

/**
 * Métadonnées d'une entité
 * Contient toutes les informations détaillées d'une entité
 * Relation OneToOne avec Entity
 */
@Entity
@Table(name = "entity_metadata")
@Audited
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EntityMetadata implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relation OneToOne avec Entity (propriétaire de la relation)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entity_id", nullable = false, unique = true)
    private fr.cnrs.opentypo.domain.entity.Entity entity;

    // Code de l'entité (identifiant unique)
    @Column(name = "code", nullable = false, length = 100)
    private String code;

    // Commentaire datation
    @Column(name = "commentaire_datation", columnDefinition = "TEXT")
    private String commentaireDatation;

    // Bibliographie
    @Column(name = "bibliographie", columnDefinition = "TEXT")
    private String bibliographie;

    // Appellation usuelle
    @Column(name = "appellation", length = 255)
    private String appellation;

    // Référence bibliographique
    @Column(name = "rereference_bibliographique", columnDefinition = "TEXT")
    private String rereferenceBibliographique;

    // Alignement externe
    @Column(name = "alignement_externe", columnDefinition = "TEXT")
    private String alignementExterne;

    // Référentiel
    @Column(name = "reference", length = 255)
    private String reference;

    // Typologie scientifique
    @Column(name = "typologie_scientifique", length = 255)
    private String typologieScientifique;

    // Identifiant pérenne
    @Column(name = "identifiant_perenne", length = 255)
    private String identifiantPerenne;

    // Ancienne version
    @Column(name = "ancienne_version", length = 255)
    private String ancienneVersion;

    // Datation : Terminus Post Quem (année de début)
    @Column(name = "tpq")
    private Integer tpq;

    // Datation : Terminus Ante Quem (année de fin)
    @Column(name = "taq")
    private Integer taq;

    // Relation externe
    @Column(name = "relation_externe", columnDefinition = "TEXT")
    private String relationExterne;

    // Ateliers (liste séparée par ';')
    @Column(name = "ateliers", columnDefinition = "TEXT")
    private String ateliers;

    // Attestations (liste séparée par ';')
    @Column(name = "attestations", columnDefinition = "TEXT")
    private String attestations;

    // Sites archéologiques (liste séparée par ';')
    @Column(name = "sites_archeologiques", columnDefinition = "TEXT")
    private String sitesArcheologiques;

    // Corpus externe
    @Column(name = "corpus_externe", columnDefinition = "TEXT")
    private String corpusExterne;

    // Commentaire (visible dans formulaires groupe, catégorie, série, type)
    @Column(name = "commentaire", columnDefinition = "TEXT")
    private String commentaire;
}
