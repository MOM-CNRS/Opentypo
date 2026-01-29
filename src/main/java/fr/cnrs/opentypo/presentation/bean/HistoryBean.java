package fr.cnrs.opentypo.presentation.bean;

import fr.cnrs.opentypo.application.dto.EntityRevisionDTO;
import fr.cnrs.opentypo.application.service.AuditService;
import fr.cnrs.opentypo.domain.entity.Entity;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRepository;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Bean pour gérer l'interface d'historique des modifications
 */
@Slf4j
@Getter
@Setter
@SessionScoped
@Named("historyBean")
public class HistoryBean implements Serializable {

    @Inject
    private AuditService auditService;

    @Inject
    private EntityRepository entityRepository;

    private Entity selectedEntity;
    private List<EntityRevisionDTO> revisions = new ArrayList<>();
    private EntityRevisionDTO selectedRevision;
    private Map<String, Map<String, Object>> changedFields;

    @PostConstruct
    public void init() {
        // Initialisation vide, sera rempli lors de la sélection d'une entité
    }

    /**
     * Charge l'historique d'une entité
     */
    public String loadHistory(Entity entity) {
        if (entity == null || entity.getId() == null) {
            log.warn("Tentative de chargement de l'historique avec une entité null ou sans ID");
            return null;
        }
        
        try {
            this.selectedEntity = entity;
            this.revisions = auditService.getEntityRevisions(entity.getId());
            this.selectedRevision = null;
            this.changedFields = null;
            
            log.info("Historique chargé pour l'entité {} : {} révisions trouvées", 
                    entity.getCode(), revisions.size());
            
            return "/history/history-list.xhtml?faces-redirect=true";
        } catch (Exception e) {
            log.error("Erreur lors du chargement de l'historique pour l'entité {}", entity.getId(), e);
            return null;
        }
    }

    /**
     * Charge les détails d'une révision spécifique
     */
    public String viewRevision(Long revisionNumber) {
        try {
            if (selectedEntity == null) {
                log.warn("Aucune entité sélectionnée pour visualiser la révision");
                return null;
            }
            
            this.selectedRevision = auditService.getEntityRevision(
                selectedEntity.getId(), 
                revisionNumber
            );
            
            if (selectedRevision != null) {
                this.changedFields = auditService.getChangedFields(selectedRevision);
                log.info("Révision {} chargée pour l'entité {}", revisionNumber, selectedEntity.getCode());
                return "/history/history-detail.xhtml?faces-redirect=true";
            } else {
                log.warn("Révision {} non trouvée pour l'entité {}", revisionNumber, selectedEntity.getId());
                return null;
            }
        } catch (Exception e) {
            log.error("Erreur lors du chargement de la révision {} pour l'entité {}", 
                    revisionNumber, selectedEntity.getId(), e);
            return null;
        }
    }

    /**
     * Retourne à la liste des révisions
     */
    public String backToList() {
        this.selectedRevision = null;
        this.changedFields = null;
        return "/history/history-list.xhtml?faces-redirect=true";
    }

    /**
     * Retourne au détail de l'entité
     */
    public String backToEntity() {
        if (selectedEntity == null) {
            return "/details/reference.xhtml?faces-redirect=true";
        }
        
        // Déterminer le type d'entité et rediriger vers la bonne page
        if (selectedEntity.getEntityType() != null) {
            String entityTypeCode = selectedEntity.getEntityType().getCode();
            
            // Pour l'instant, on retourne vers la référence
            // TODO: Adapter selon le type d'entité
            return "/details/reference.xhtml?faces-redirect=true";
        }
        
        return "/details/reference.xhtml?faces-redirect=true";
    }

    /**
     * Formate une valeur pour l'affichage
     */
    public String formatValue(Object value) {
        if (value == null) {
            return "<em class='text-muted'>Aucune valeur</em>";
        }
        if (value instanceof Boolean) {
            return (Boolean) value ? "Oui" : "Non";
        }
        return value.toString();
    }

    /**
     * Retourne le label d'un champ
     */
    public String getFieldLabel(String fieldName) {
        // Mapping des noms de champs vers des labels lisibles
        switch (fieldName) {
            case "code":
                return "Code";
            case "nom":
                return "Nom";
            case "commentaire":
                return "Commentaire";
            case "bibliographie":
                return "Bibliographie";
            case "appellation":
                return "Appellation";
            case "reference":
                return "Référentiel";
            case "typologieScientifique":
                return "Typologie scientifique";
            case "identifiantPerenne":
                return "Identifiant pérenne";
            case "ancienneVersion":
                return "Ancienne version";
            case "statut":
                return "Statut";
            case "tpq":
                return "TPQ";
            case "taq":
                return "TAQ";
            case "publique":
                return "Public";
            case "ateliers":
                return "Atelier(s)";
            case "attestations":
                return "Attestations";
            case "sitesArcheologiques":
                return "Sites archéologiques";
            case "createDate":
                return "Date de création";
            case "createBy":
                return "Créé par";
            case "entityTypeCode":
                return "Type d'entité";
            default:
                return fieldName;
        }
    }
}
