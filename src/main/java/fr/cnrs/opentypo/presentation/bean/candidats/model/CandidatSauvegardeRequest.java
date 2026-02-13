package fr.cnrs.opentypo.presentation.bean.candidats.model;

import fr.cnrs.opentypo.domain.entity.Entity;
import fr.cnrs.opentypo.domain.entity.ReferenceOpentheso;
import fr.cnrs.opentypo.domain.entity.Utilisateur;
import lombok.Builder;
import lombok.Data;
import java.util.List;

/** Données requises pour sauvegarder un candidat. */
@Data
@Builder
public class CandidatSauvegardeRequest {
    private Long selectedEntityTypeId;
    private String entityCode;
    private String entityLabel;
    private String selectedLangueCode;
    private Long selectedCollectionId;
    private Entity selectedParentEntity;
    private Entity currentEntity;
    private List<CategoryLabelItem> candidatLabels;
    private List<CategoryDescriptionItem> descriptions;
    private String candidatCommentaire;
    private String candidatCommentaireDatation;
    private String candidatBibliographie;
    private List<String> referencesBibliographiques;
    private String typeDescription;
    private String serieDescription;
    private String groupDescription;
    private String collectionDescription;
    private String imagePrincipaleUrl;
    private String periode;
    private Integer tpq;
    private Integer taq;
    private String droit;
    private String legendeDroit;
    private String coinsMonetairesDroit;
    private String revers;
    private String legendeRevers;
    private String coinsMonetairesRevers;
    private ReferenceOpentheso materiau;
    private ReferenceOpentheso denomination;
    private String metrologieMonnaie;
    private ReferenceOpentheso valeur;
    private ReferenceOpentheso technique;
    private ReferenceOpentheso fabrication;
    private ReferenceOpentheso openThesoCreatedReference;
    private Utilisateur currentUser;
}
