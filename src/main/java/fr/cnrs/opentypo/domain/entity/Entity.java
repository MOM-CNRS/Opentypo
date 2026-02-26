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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
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
public class Entity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "statut", length = 50)
    private String statut;

    @Column(name = "publique", nullable = false)
    private Boolean publique = true; // Par défaut, l'entité est publique

    @Column(name = "image_principale_url", length = 500)
    private String imagePrincipaleUrl;

    @Column(name = "id_ark", length = 255)
    private String idArk;

    // Relation OneToOne avec EntityMetadata (mappedBy côté EntityMetadata)
    // Exclure les getters/setters automatiques car on utilise des méthodes personnalisées
    @OneToOne(mappedBy = "entity", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private EntityMetadata metadata;

    // Relations avec EntityType
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entity_type_id", nullable = false)
    private EntityType entityType;

    // Relations avec Image (OneToMany : plusieurs images par entité)
    @OneToMany(mappedBy = "entity", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Image> images = new ArrayList<>();

    /**
     * Retourne la première image (compatibilité avec l'affichage d'une image principale).
     */
    public Image getImage() {
        return (images != null && !images.isEmpty()) ? images.get(0) : null;
    }

    // Relations auto-référencées
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "periode_id")
    private ReferenceOpentheso periode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "production_id")
    private ReferenceOpentheso production;

    @SQLRestriction("code = 'AIRE_CIRCULATION'")
    @OneToMany(mappedBy = "entity", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReferenceOpentheso> airesCirculation = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categorie_fonctionnelle")
    private ReferenceOpentheso categorieFonctionnelle;

    // Relations avec DescriptionDetail
    @OneToOne(mappedBy = "entity", cascade = CascadeType.ALL, orphanRemoval = true)
    private DescriptionDetail descriptionDetail;

    // Relations avec CaracteristiquePhysique
    @OneToOne(mappedBy = "entity", cascade = CascadeType.ALL, orphanRemoval = true)
    private CaracteristiquePhysique caracteristiquePhysique;

    // Relations avec DescriptionPate
    @OneToOne(mappedBy = "entity", cascade = CascadeType.ALL, orphanRemoval = true)
    private DescriptionPate descriptionPate;

    // Relations avec DescriptionMonnaie (collection MONNAIE)
    @OneToOne(mappedBy = "entity", cascade = CascadeType.ALL, orphanRemoval = true)
    private DescriptionMonnaie descriptionMonnaie;

    // Relations avec CaracteristiquePhysiqueMonnaie (collection MONNAIE)
    @OneToOne(mappedBy = "entity", cascade = CascadeType.ALL, orphanRemoval = true)
    private CaracteristiquePhysiqueMonnaie caracteristiquePhysiqueMonnaie;

    // Relations avec EntityRelation (parent)
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EntityRelation> parentRelations = new ArrayList<>();

    // Relations avec EntityRelation (child)
    @OneToMany(mappedBy = "child", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EntityRelation> childRelations = new ArrayList<>();

    // Relations avec Description
    @OneToMany(mappedBy = "entity", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Description> descriptions = new ArrayList<>();

    // Relations avec Label (BatchSize évite N+1 lors du chargement en masse pour l'arbre)
    @BatchSize(size = 50)
    @OneToMany(mappedBy = "entity", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Label> labels = new ArrayList<>();

    // Relations avec UserPermission
    @OneToMany(mappedBy = "entity", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserPermission> userPermissions = new ArrayList<>();

    // Relations avec Commentaire (un ou plusieurs commentaires par entité)
    @NotAudited
    @OneToMany(mappedBy = "entity", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Commentaire> commentaires = new ArrayList<>();

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

    // ============================================
    // Méthodes personnalisées pour les métadonnées
    // ============================================

    /**
     * Obtient les métadonnées de l'entité
     */
    public EntityMetadata getMetadata() {
        return metadata;
    }

    /**
     * Définit les métadonnées de l'entité
     */
    public void setMetadata(EntityMetadata metadata) {
        this.metadata = metadata;
        if (metadata != null && metadata.getEntity() != this) {
            metadata.setEntity(this);
        }
    }

    /**
     * Obtient le nom de l'entité depuis les labels selon la langue
     * @param langueCode Code de la langue (ex: "fr", "en"). Si null, retourne le premier label disponible
     * @return Le nom de l'entité dans la langue demandée, ou le premier label disponible, ou null
     */
    public String getNom(String langueCode) {
        if (labels == null || labels.isEmpty()) {
            return null;
        }
        
        if (langueCode != null) {
            return labels.stream()
                .filter(label -> label.getLangue() != null && langueCode.equals(label.getLangue().getCode()))
                .map(Label::getNom)
                .findFirst()
                .orElse(null);
        }
        
        // Si aucune langue spécifiée, retourner le premier label disponible
        return labels.stream()
            .map(Label::getNom)
            .findFirst()
            .orElse(null);
    }

    /**
     * Obtient le nom de l'entité (premier label disponible)
     * Méthode de compatibilité pour le code existant
     * @return Le nom de l'entité ou null
     */
    public String getNom() {
        return getNom(null);
    }

    /**
     * Obtient le code de l'entité depuis les métadonnées
     * @return Le code de l'entité ou null
     */
    public String getCode() {
        return metadata != null ? metadata.getCode() : null;
    }

    /**
     * Définit le code de l'entité dans les métadonnées
     * Crée les métadonnées si elles n'existent pas
     */
    public void setCode(String code) {
        ensureMetadata();
        metadata.setCode(code);
    }

    /**
     * Obtient le commentaire depuis les métadonnées (champ entity_metadata.commentaire).
     * Visible dans les formulaires groupe, catégorie, série, type.
     */
    public String getMetadataCommentaire() {
        return metadata != null ? metadata.getCommentaire() : null;
    }

    /**
     * Définit le commentaire dans les métadonnées.
     */
    public void setMetadataCommentaire(String commentaire) {
        ensureMetadata();
        metadata.setCommentaire(commentaire);
    }

    /**
     * Obtient le contenu du premier commentaire (compatibilité avec l'ancien champ unique).
     * Pour accéder à tous les commentaires, utiliser {@link #getCommentaires()}.
     */
    public String getCommentaire() {
        if (commentaires != null && !commentaires.isEmpty()) {
            return commentaires.get(0).getContenu();
        }
        return null;
    }

    /**
     * Définit le commentaire (compatibilité). Remplace tous les commentaires par un seul.
     * Pour ajouter un commentaire avec auteur, utiliser {@link #addCommentaire(String, Utilisateur)}.
     */
    public void setCommentaire(String commentaire) {
        if (commentaires == null) {
            commentaires = new ArrayList<>();
        }
        commentaires.clear();
        if (commentaire != null && !commentaire.isBlank()) {
            Commentaire c = new Commentaire();
            c.setEntity(this);
            c.setContenu(commentaire.trim());
            c.setDateCreation(LocalDateTime.now());
            commentaires.add(c);
        }
    }

    /**
     * Ajoute un commentaire créé par un utilisateur.
     */
    public void addCommentaire(String contenu, Utilisateur utilisateur) {
        if (contenu == null || contenu.isBlank()) {
            return;
        }
        if (commentaires == null) {
            commentaires = new ArrayList<>();
        }
        Commentaire c = new Commentaire();
        c.setEntity(this);
        c.setContenu(contenu.trim());
        c.setUtilisateur(utilisateur);
        c.setDateCreation(LocalDateTime.now());
        commentaires.add(c);
    }

    /**
     * Définit le commentaire dans les métadonnées
     */
    public void setCommentaireDatation(String commentaireDatation) {
        ensureMetadata();
        metadata.setCommentaireDatation(commentaireDatation);
    }

    /**
     * Définit le commentaire dans les métadonnées
     */
    public String getCommentaireDatation() {
        return metadata != null ? metadata.getCommentaireDatation() : null;
    }

    /**
     * Obtient la bibliographie depuis les métadonnées
     */
    public String getBibliographie() {
        return metadata != null ? metadata.getBibliographie() : null;
    }

    /**
     * Définit la bibliographie dans les métadonnées
     */
    public void setBibliographie(String bibliographie) {
        ensureMetadata();
        metadata.setBibliographie(bibliographie);
    }

    /**
     * Obtient l'appellation depuis les métadonnées
     */
    public String getAppellation() {
        return metadata != null ? metadata.getAppellation() : null;
    }

    /**
     * Définit l'appellation dans les métadonnées
     */
    public void setAppellation(String appellation) {
        ensureMetadata();
        metadata.setAppellation(appellation);
    }

    /**
     * Obtient la référence bibliographique depuis les métadonnées
     */
    public String getRereferenceBibliographique() {
        return metadata != null ? metadata.getRereferenceBibliographique() : null;
    }

    /**
     * Définit la référence bibliographique dans les métadonnées
     */
    public void setRereferenceBibliographique(String rereferenceBibliographique) {
        ensureMetadata();
        metadata.setRereferenceBibliographique(rereferenceBibliographique);
    }

    /**
     * Obtient l'alignement externe depuis les métadonnées
     */
    public String getAlignementExterne() {
        return metadata != null ? metadata.getAlignementExterne() : null;
    }

    /**
     * Définit l'alignement externe dans les métadonnées
     */
    public void setAlignementExterne(String alignementExterne) {
        ensureMetadata();
        metadata.setAlignementExterne(alignementExterne);
    }

    /**
     * Obtient la référence depuis les métadonnées
     */
    public String getReference() {
        return metadata != null ? metadata.getReference() : null;
    }

    /**
     * Définit la référence dans les métadonnées
     */
    public void setReference(String reference) {
        ensureMetadata();
        metadata.setReference(reference);
    }

    /**
     * Obtient la typologie scientifique depuis les métadonnées
     */
    public String getTypologieScientifique() {
        return metadata != null ? metadata.getTypologieScientifique() : null;
    }

    /**
     * Définit la typologie scientifique dans les métadonnées
     */
    public void setTypologieScientifique(String typologieScientifique) {
        ensureMetadata();
        metadata.setTypologieScientifique(typologieScientifique);
    }

    /**
     * Obtient l'identifiant pérenne depuis les métadonnées
     */
    public String getIdentifiantPerenne() {
        return metadata != null ? metadata.getIdentifiantPerenne() : null;
    }

    /**
     * Définit l'identifiant pérenne dans les métadonnées
     */
    public void setIdentifiantPerenne(String identifiantPerenne) {
        ensureMetadata();
        metadata.setIdentifiantPerenne(identifiantPerenne);
    }

    /**
     * Obtient l'ancienne version depuis les métadonnées
     */
    public String getAncienneVersion() {
        return metadata != null ? metadata.getAncienneVersion() : null;
    }

    /**
     * Définit l'ancienne version dans les métadonnées
     */
    public void setAncienneVersion(String ancienneVersion) {
        ensureMetadata();
        metadata.setAncienneVersion(ancienneVersion);
    }

    /**
     * Obtient TPQ depuis les métadonnées
     */
    public Integer getTpq() {
        return metadata != null ? metadata.getTpq() : null;
    }

    /**
     * Définit TPQ dans les métadonnées
     */
    public void setTpq(Integer tpq) {
        ensureMetadata();
        metadata.setTpq(tpq);
    }

    /**
     * Obtient TAQ depuis les métadonnées
     */
    public Integer getTaq() {
        return metadata != null ? metadata.getTaq() : null;
    }

    /**
     * Définit TAQ dans les métadonnées
     */
    public void setTaq(Integer taq) {
        ensureMetadata();
        metadata.setTaq(taq);
    }

    /**
     * Obtient la relation externe depuis les métadonnées
     */
    public String getRelationExterne() {
        return metadata != null ? metadata.getRelationExterne() : null;
    }

    /**
     * Définit la relation externe dans les métadonnées
     */
    public void setRelationExterne(String relationExterne) {
        ensureMetadata();
        metadata.setRelationExterne(relationExterne);
    }

    /**
     * Obtient les ateliers depuis les métadonnées
     */
    public String getAteliers() {
        return metadata != null ? metadata.getAteliers() : null;
    }

    /**
     * Définit les ateliers dans les métadonnées
     */
    public void setAteliers(String ateliers) {
        ensureMetadata();
        metadata.setAteliers(ateliers);
    }

    /**
     * Obtient les attestations depuis les métadonnées
     */
    public String getAttestations() {
        return metadata != null ? metadata.getAttestations() : null;
    }

    /**
     * Définit les attestations dans les métadonnées
     */
    public void setAttestations(String attestations) {
        ensureMetadata();
        metadata.setAttestations(attestations);
    }

    /**
     * Obtient les attestations depuis les métadonnées
     */
    public String getReferences() {
        return metadata != null ? metadata.getReference() : null;
    }

    /**
     * Définit les attestations dans les métadonnées
     */
    public void setReferences(String references) {
        ensureMetadata();
        metadata.setReference(references);
    }

    /**
     * Obtient les sites archéologiques depuis les métadonnées
     */
    public String getSitesArcheologiques() {
        return metadata != null ? metadata.getSitesArcheologiques() : null;
    }

    /**
     * Définit les sites archéologiques dans les métadonnées
     */
    public void setSitesArcheologiques(String sitesArcheologiques) {
        ensureMetadata();
        metadata.setSitesArcheologiques(sitesArcheologiques);
    }

    /**
     * S'assure que les métadonnées existent, les crée si nécessaire
     */
    private void ensureMetadata() {
        if (metadata == null) {
            metadata = new EntityMetadata();
            metadata.setEntity(this);
        }
    }
}
