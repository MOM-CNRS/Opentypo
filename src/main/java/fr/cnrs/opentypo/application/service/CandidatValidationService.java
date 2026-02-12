package fr.cnrs.opentypo.application.service;

import fr.cnrs.opentypo.application.dto.ReferenceOpenthesoEnum;
import fr.cnrs.opentypo.common.constant.EntityConstants;
import fr.cnrs.opentypo.domain.entity.DescriptionDetail;
import fr.cnrs.opentypo.domain.entity.Entity;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRepository;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.primefaces.PrimeFaces;
import org.primefaces.model.TreeNode;
import org.springframework.stereotype.Service;

/**
 * Service de validation pour le formulaire de candidat.
 * Centralise la logique de validation des étapes et des champs obligatoires.
 */
@Service
@Slf4j
public class CandidatValidationService {

    @Inject
    private EntityRepository entityRepository;

    /**
     * Valide l'étape 1 du formulaire (Type et identification).
     */
    public boolean validateStep1(Long selectedEntityTypeId, String entityCode, String entityLabel, String selectedLangueCode) {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        boolean isValid = true;

        if (selectedEntityTypeId == null) {
            facesContext.addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "Le type d'entité est requis."));
            isValid = false;
        }
        if (entityCode == null || entityCode.trim().isEmpty()) {
            facesContext.addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "Le code est requis."));
            isValid = false;
        }
        if (entityLabel == null || entityLabel.trim().isEmpty()) {
            facesContext.addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "Le label est requis."));
            isValid = false;
        }
        if (selectedLangueCode == null) {
            facesContext.addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "La langue est requise."));
            isValid = false;
        }

        if (isValid && entityCode != null && !entityCode.trim().isEmpty()) {
            if (entityRepository.existsByCode(entityCode.trim())) {
                facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "Ce code existe déjà. Veuillez en choisir un autre."));
                isValid = false;
            }
        }

        if (isValid && entityLabel != null && !entityLabel.trim().isEmpty() && selectedLangueCode != null) {
            if (entityRepository.existsByLabelNomAndLangueCode(entityLabel.trim(), selectedLangueCode)) {
                facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur",
                        "Un élément avec ce label dans cette langue existe déjà. Veuillez modifier le label ou la langue."));
                isValid = false;
            }
        }

        return isValid;
    }

    /**
     * Valide l'étape 2 du formulaire (Collection et référentiel).
     */
    public boolean validateStep2(Long selectedCollectionId, TreeNode selectedTreeNode) {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        boolean isValid = true;

        if (selectedCollectionId == null) {
            facesContext.addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "La collection est requise."));
            isValid = false;
        }
        if (selectedTreeNode == null) {
            facesContext.addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "Le référentiel est requis."));
            isValid = false;
        }

        return isValid;
    }

    /**
     * Valide les champs obligatoires selon le type d'entité.
     * Règles :
     * - CATEGORIE : au moins une description
     * - GROUPE    : période obligatoire + au moins une description
     * - SERIE     : période obligatoire + au moins une description
     * - TYPE      : période, TPQ, TAQ, au moins une description, production,
     *               au moins une aire de circulation, fonction/usage,
     *               identifiant pérenne et bibliographie
     */
    public boolean validateRequiredFieldsForEntity(Entity entity) {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (entity == null || entity.getEntityType() == null) {
            return true;
        }

        String typeCode = entity.getEntityType().getCode();
        boolean isValid = true;

        boolean hasAtLeastOneDescription =
            entity.getDescriptions() != null && !entity.getDescriptions().isEmpty();

        boolean hasPeriode = entity.getPeriode() != null;
        boolean hasTpq = entity.getTpq() != null;
        boolean hasTaq = entity.getTaq() != null;
        boolean hasProduction = entity.getProduction() != null;

        boolean hasAireCirculation = entity.getAiresCirculation() != null
            && entity.getAiresCirculation().stream()
                .anyMatch(ref -> ReferenceOpenthesoEnum.AIRE_CIRCULATION.name().equals(ref.getCode()));

        DescriptionDetail descDetail = entity.getDescriptionDetail();
        boolean hasFonctionUsage = descDetail != null && descDetail.getFonction() != null;

        String identifiant = entity.getIdentifiantPerenne();
        boolean hasIdentifiantPerenne = identifiant != null && !identifiant.trim().isEmpty();

        String biblio = entity.getBibliographie();
        boolean hasBibliographie = biblio != null && !biblio.trim().isEmpty();

        if (EntityConstants.ENTITY_TYPE_CATEGORY.equals(typeCode)) {
            if (!hasAtLeastOneDescription) {
                facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur",
                        "Pour une entité de type \"Catégorie\", au moins une description est obligatoire."));
                isValid = false;
            }
        } else if (EntityConstants.ENTITY_TYPE_GROUP.equals(typeCode)) {
            if (!hasPeriode) {
                facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur",
                        "Pour une entité de type \"Groupe\", le champ \"Période\" est obligatoire."));
                isValid = false;
            }
            if (!hasAtLeastOneDescription) {
                facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur",
                        "Pour une entité de type \"Groupe\", au moins une description est obligatoire."));
                isValid = false;
            }
        } else if (EntityConstants.ENTITY_TYPE_SERIES.equals(typeCode)) {
            if (!hasPeriode) {
                facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur",
                        "Pour une entité de type \"Série\", le champ \"Période\" est obligatoire."));
                isValid = false;
            }
            if (!hasAtLeastOneDescription) {
                facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur",
                        "Pour une entité de type \"Série\", au moins une description est obligatoire."));
                isValid = false;
            }
        } else if (EntityConstants.ENTITY_TYPE_TYPE.equals(typeCode)) {
            if (!hasPeriode) {
                facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur",
                        "Pour une entité de type \"Type\", le champ \"Période\" est obligatoire."));
                isValid = false;
            }
            if (!hasTpq) {
                facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur",
                        "Pour une entité de type \"Type\", le champ \"TPQ\" est obligatoire."));
                isValid = false;
            }
            if (!hasTaq) {
                facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur",
                        "Pour une entité de type \"Type\", le champ \"TAQ\" est obligatoire."));
                isValid = false;
            }
            if (!hasAtLeastOneDescription) {
                facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur",
                        "Pour une entité de type \"Type\", au moins une description est obligatoire."));
                isValid = false;
            }
            if (!hasProduction) {
                facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur",
                        "Pour une entité de type \"Type\", le champ \"Production\" est obligatoire."));
                isValid = false;
            }
            if (!hasAireCirculation) {
                facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur",
                        "Pour une entité de type \"Type\", au moins une \"Aire de circulation\" est obligatoire."));
                isValid = false;
            }
            if (!hasFonctionUsage) {
                facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur",
                        "Pour une entité de type \"Type\", le champ \"Fonction/usage\" est obligatoire."));
                isValid = false;
            }
            if (!hasIdentifiantPerenne) {
                facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur",
                        "Pour une entité de type \"Type\", le champ \"Identifiant pérenne\" est obligatoire."));
                isValid = false;
            }
            if (!hasBibliographie) {
                facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur",
                        "Pour une entité de type \"Type\", le champ \"Bibliographie\" est obligatoire."));
                isValid = false;
            }
        }

        if (!isValid) {
            PrimeFaces.current().ajax().update(":growl");
        }

        return isValid;
    }
}
