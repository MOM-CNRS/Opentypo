package fr.cnrs.opentypo.domain.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entité centrale représentant tous les types d'entités (Référentiel, Catégorie, Groupe, Série, Type)
 */
@jakarta.persistence.Entity
@Table(name = "entity")
@Audited
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Entity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nom", nullable = false, length = 255)
    private String nom;

    @Column(name = "code", nullable = false, length = 100)
    private String code;

    @Column(name = "commentaire", columnDefinition = "TEXT")
    private String commentaire;

    @Column(name = "bibliographie", columnDefinition = "TEXT")
    private String bibliographie;

    @Column(name = "appellation", length = 255)
    private String appellation;

    @Column(name = "rereference_bibliographique", columnDefinition = "TEXT")
    private String rereferenceBibliographique;

    @Column(name = "alignement_externe", columnDefinition = "TEXT")
    private String alignementExterne;

    @Column(name = "referentiel", length = 255)
    private String referentiel;

    @Column(name = "typologie_scientifique", length = 255)
    private String typologieScientifique;

    @Column(name = "identifiant_perenne", length = 255)
    private String identifiantPerenne;

    @Column(name = "ancienne_version", length = 255)
    private String ancienneVersion;

    @Column(name = "statut", length = 50)
    private String statut;

    @Column(name = "tpq")
    private Integer tpq;

    @Column(name = "taq")
    private Integer taq;

    @Column(name = "relation_externe", columnDefinition = "TEXT")
    private String relationExterne;

    @Column(name = "publique", nullable = false)
    private Boolean publique = true; // Par défaut, l'entité est publique

    // Relations avec EntityType
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entity_type_id", nullable = false)
    private EntityType entityType;

    // Relations avec Image
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "image_id")
    private Image image;

    // Relations auto-référencées
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "periode_id")
    private ReferentielOpentheso periode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "production_id")
    private ReferentielOpentheso production;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aire_circulation")
    private ReferentielOpentheso aireCirculation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categorie_fonctionnelle")
    private ReferentielOpentheso categorieFonctionnelle;

    // Relations avec DescriptionDetail
    @OneToOne(mappedBy = "entity", cascade = CascadeType.ALL, orphanRemoval = true)
    private DescriptionDetail descriptionDetail;

    // Relations avec CaracteristiquePhysique
    @OneToOne(mappedBy = "entity", cascade = CascadeType.ALL, orphanRemoval = true)
    private CaracteristiquePhysique caracteristiquePhysique;

    // Relations avec EntityRelation (parent)
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EntityRelation> parentRelations = new ArrayList<>();

    // Relations avec EntityRelation (child)
    @OneToMany(mappedBy = "child", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EntityRelation> childRelations = new ArrayList<>();

    // Relations avec Description
    @OneToMany(mappedBy = "entity", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Description> descriptions = new ArrayList<>();

    // Relations avec Label
    @OneToMany(mappedBy = "entity", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Label> labels = new ArrayList<>();

    // Relations avec UserPermission
    @OneToMany(mappedBy = "entity", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserPermission> userPermissions = new ArrayList<>();

    // Relations avec Auteur (Many-to-Many avec Utilisateur)
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "auteur",
        joinColumns = @JoinColumn(name = "entity_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    private List<Utilisateur> auteurs = new ArrayList<>();

    // Audit fields
    @CreatedDate
    @Column(name = "create_date", nullable = false, updatable = false)
    private LocalDateTime createDate;

    @CreatedBy
    @Column(name = "create_by", length = 100, updatable = false)
    private String createBy;
}

