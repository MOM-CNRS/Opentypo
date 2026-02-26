package fr.cnrs.opentypo.presentation.bean.candidats.model;

import fr.cnrs.opentypo.domain.entity.ReferenceOpentheso;
import fr.cnrs.opentypo.domain.entity.Utilisateur;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO contenant les données du formulaire étape 3 chargées depuis une Entity.
 */
@Data
@Builder
public class Step3FormData {
    @Builder.Default
    private List<CategoryLabelItem> candidatLabels = new ArrayList<>();
    @Builder.Default
    private List<CategoryDescriptionItem> descriptions = new ArrayList<>();
    private String candidatCommentaireDatation;
    private String candidatMetadataCommentaire;
    private String candidatBibliographie;
    @Builder.Default
    private List<String> referencesBibliographiques = new ArrayList<>();
    @Builder.Default
    private List<String> ateliers = new ArrayList<>();
    @Builder.Default
    private List<String> attestations = new ArrayList<>();
    @Builder.Default
    private List<String> sitesArcheologiques = new ArrayList<>();
    @Builder.Default
    private List<String> references = new ArrayList<>();
    @Builder.Default
    private List<ReferenceOpentheso> airesCirculation = new ArrayList<>();
    @Builder.Default
    private List<ReferenceOpentheso> referentiels = new ArrayList<>();
    private String decors;
    @Builder.Default
    private List<String> marquesEstampilles = new ArrayList<>();
    private ReferenceOpentheso fonctionUsage;
    private ReferenceOpentheso metrologie;
    private ReferenceOpentheso fabricationFaconnage;
    private String descriptionPate;
    private ReferenceOpentheso couleurPate;
    private ReferenceOpentheso naturePate;
    private ReferenceOpentheso inclusions;
    private ReferenceOpentheso cuissonPostCuisson;
    private String typologieScientifique;
    private String identifiantPerenne;
    private String ancienneVersion;
    private String candidatProduction;
    private Boolean collectionPublique;
    private String typeDescription;
    private Integer tpq;
    private Integer taq;
    private String periode;
    private String corpusExterne;
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
    private String reference;
    @Builder.Default
    private List<Utilisateur> selectedAuteurs = new ArrayList<>();
}
