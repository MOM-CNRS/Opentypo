package fr.cnrs.opentypo.presentation.bean;

import fr.cnrs.opentypo.application.dto.EntityRevisionDTO;
import fr.cnrs.opentypo.application.service.AuditService;
import fr.cnrs.opentypo.domain.entity.Entity;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Bean pour gérer l'affichage de l'historique des modifications d'une entité
 */
@Slf4j
@Getter
@Setter
@SessionScoped
@Named("historyBean")
public class HistoryBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private transient ApplicationBean applicationBean;

    @Inject
    private transient fr.cnrs.opentypo.application.service.AuditService auditService;

    // Entité sélectionnée pour laquelle on affiche l'historique
    private Entity selectedEntity;

    // Liste des révisions de l'entité
    private List<EntityRevisionDTO> revisions = new ArrayList<>();

    // Révision actuellement sélectionnée pour afficher les détails
    private EntityRevisionDTO selectedRevision;

    // Champs modifiés dans la révision sélectionnée (pour l'affichage des différences)
    private Map<String, Map<String, Object>> changedFields = new HashMap<>();

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
                log.warn("Aucune entité sélectionnée pour afficher la révision {}", revisionNumber);
                return null;
            }

            // Trouver la révision dans la liste
            selectedRevision = revisions.stream()
                    .filter(r -> r.getRevisionNumber().equals(revisionNumber))
                    .findFirst()
                    .orElse(null);

            if (selectedRevision == null) {
                log.warn("Révision {} non trouvée pour l'entité {}", revisionNumber, selectedEntity.getId());
                return null;
            }

            // Calculer les champs modifiés
            changedFields = calculateChangedFields(selectedRevision);
            
            log.info("Détails de la révision {} chargés pour l'entité {} : {} champs modifiés", 
                    revisionNumber, selectedEntity.getId(), changedFields.size());
            
            return "/history/history-detail.xhtml?faces-redirect=true";
        } catch (Exception e) {
            log.error("Erreur lors du chargement des détails de la révision {} pour l'entité {}", 
                    revisionNumber, selectedEntity != null ? selectedEntity.getId() : "null", e);
            return null;
        }
    }

    /**
     * Retourne à l'entité depuis la liste des révisions
     */
    public String backToEntity() {
        if (selectedEntity != null && applicationBean != null) {
            // Retourner à la page de référence si c'est une référence
            if (applicationBean.getSelectedReference() != null && 
                applicationBean.getSelectedReference().getId().equals(selectedEntity.getId())) {
                return "/details/reference.xhtml?faces-redirect=true";
            }
            // Sinon, retourner à la page de l'entité (à adapter selon votre navigation)
            return "/details/reference.xhtml?faces-redirect=true";
        }
        return "/index.xhtml?faces-redirect=true";
    }

    /**
     * Retourne à la liste des révisions depuis les détails
     */
    public String backToList() {
        return "/history/history-list.xhtml?faces-redirect=true";
    }

    /**
     * Retourne le libellé d'un champ pour l'affichage
     */
    public String getFieldLabel(String fieldName) {
        if (fieldName == null) {
            return "Champ inconnu";
        }

        // Gérer les champs avec des points (ex: "labels.fr", "descriptions.en")
        if (fieldName.contains(".")) {
            String[] parts = fieldName.split("\\.", 2);
            String baseField = parts[0];
            String languageCode = parts.length > 1 ? parts[1].toUpperCase() : "";
            
            String baseLabel = getBaseFieldLabel(baseField);
            if (!languageCode.isEmpty()) {
                return baseLabel + " (" + languageCode + ")";
            }
            return baseLabel;
        }

        return getBaseFieldLabel(fieldName);
    }

    /**
     * Retourne le libellé de base d'un champ
     */
    private String getBaseFieldLabel(String fieldName) {
        switch (fieldName) {
            case "id":
                return "ID";
            case "code":
                return "Code";
            case "nom":
            case "labels":
                return "Nom/Label";
            case "commentaire":
            case "descriptions":
                return "Description";
            case "bibliographie":
                return "Bibliographie";
            case "appellation":
                return "Appellation";
            case "reference":
                return "Référence";
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
                return "Ateliers";
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
            case "decors":
                return "Décors";
            case "marques":
                return "Marques/Estampilles";
            case "fonctionUsage":
                return "Fonction/Usage";
            case "materiaux":
                return "Matériaux";
            case "metrologieRef":
                return "Métrologie";
            case "fabricationFaconnage":
                return "Fabrication/Façonnage";
            case "descriptionPate":
                return "Description pâte";
            case "couleurPate":
                return "Couleur de pâte";
            case "naturePate":
                return "Nature de pâte";
            case "inclusions":
                return "Inclusions";
            case "cuissonPostCuisson":
                return "Cuisson/Post-cuisson";
            case "referencesOpentheso":
                return "Références OpenTheso";
            default:
                // Capitaliser la première lettre et remplacer les majuscules par des espaces
                return fieldName.substring(0, 1).toUpperCase() + 
                       fieldName.substring(1).replaceAll("([A-Z])", " $1").trim();
        }
    }

    /**
     * Formate une valeur pour l'affichage
     */
    public String formatValue(Object value) {
        if (value == null) {
            return "<em>Aucune valeur</em>";
        }

        // Gérer les Map (pour les labels et descriptions multilingues)
        if (value instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, String> map = (Map<String, String>) value;
            if (map.isEmpty()) {
                return "<em>Aucune valeur</em>";
            }
            
            StringBuilder sb = new StringBuilder("<ul>");
            for (Map.Entry<String, String> entry : map.entrySet()) {
                sb.append("<li><strong>").append(entry.getKey().toUpperCase())
                  .append(":</strong> ").append(entry.getValue() != null ? entry.getValue() : "<em>N/A</em>")
                  .append("</li>");
            }
            sb.append("</ul>");
            return sb.toString();
        }

        // Gérer les List
        if (value instanceof List) {
            @SuppressWarnings("unchecked")
            List<?> list = (List<?>) value;
            if (list.isEmpty()) {
                return "<em>Aucune valeur</em>";
            }
            return String.join(", ", list.stream()
                    .map(item -> item != null ? item.toString() : "<em>N/A</em>")
                    .toArray(String[]::new));
        }

        // Gérer les Boolean
        if (value instanceof Boolean) {
            return (Boolean) value ? "Oui" : "Non";
        }

        // Par défaut, convertir en String
        return value.toString();
    }

    /**
     * Calcule les champs modifiés entre la révision actuelle et la précédente
     */
    private Map<String, Map<String, Object>> calculateChangedFields(EntityRevisionDTO revision) {
        Map<String, Map<String, Object>> changes = new HashMap<>();
        
        if (revision == null || revision.getEntityData() == null) {
            return changes;
        }

        Map<String, Object> currentData = revision.getEntityData();
        Map<String, Object> previousData = revision.getPreviousEntityData();

        // Si pas de données précédentes, tous les champs sont nouveaux
        if (previousData == null || previousData.isEmpty()) {
            for (Map.Entry<String, Object> entry : currentData.entrySet()) {
                Map<String, Object> change = new HashMap<>();
                change.put("old", null);
                change.put("new", entry.getValue());
                changes.put(entry.getKey(), change);
            }
            return changes;
        }

        // Comparer tous les champs de currentData
        for (Map.Entry<String, Object> entry : currentData.entrySet()) {
            String fieldName = entry.getKey();
            Object currentValue = entry.getValue();
            Object previousValue = previousData.get(fieldName);

            // Vérifier si la valeur a changé
            if (!areEqual(currentValue, previousValue)) {
                Map<String, Object> change = new HashMap<>();
                change.put("old", previousValue);
                change.put("new", currentValue);
                changes.put(fieldName, change);
            }
        }

        // Vérifier les champs qui existent seulement dans previousData (supprimés)
        for (Map.Entry<String, Object> entry : previousData.entrySet()) {
            String fieldName = entry.getKey();
            if (!currentData.containsKey(fieldName)) {
                Map<String, Object> change = new HashMap<>();
                change.put("old", entry.getValue());
                change.put("new", null);
                changes.put(fieldName, change);
            }
        }

        // Gestion spéciale pour les Map (labels, descriptions) - créer des entrées individuelles par langue
        for (Map.Entry<String, Object> entry : currentData.entrySet()) {
            String fieldName = entry.getKey();
            Object currentValue = entry.getValue();
            
            if (currentValue instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, String> currentMap = (Map<String, String>) currentValue;
                @SuppressWarnings("unchecked")
                Map<String, String> previousMap = previousData.get(fieldName) instanceof Map ? 
                    (Map<String, String>) previousData.get(fieldName) : null;

                // Comparer chaque clé de langue
                for (Map.Entry<String, String> mapEntry : currentMap.entrySet()) {
                    String languageKey = mapEntry.getKey();
                    String currentLangValue = mapEntry.getValue();
                    String previousLangValue = (previousMap != null && previousMap.containsKey(languageKey)) ? 
                        previousMap.get(languageKey) : null;

                    if (!areEqual(currentLangValue, previousLangValue)) {
                        String granularFieldName = fieldName + "." + languageKey;
                        Map<String, Object> change = new HashMap<>();
                        change.put("old", previousLangValue);
                        change.put("new", currentLangValue);
                        changes.put(granularFieldName, change);
                        
                        // Retirer l'entrée globale si elle existe
                        changes.remove(fieldName);
                    }
                }

                // Vérifier les clés qui existent seulement dans previousMap
                if (previousMap != null) {
                    for (Map.Entry<String, String> mapEntry : previousMap.entrySet()) {
                        String languageKey = mapEntry.getKey();
                        if (!currentMap.containsKey(languageKey)) {
                            String granularFieldName = fieldName + "." + languageKey;
                            Map<String, Object> change = new HashMap<>();
                            change.put("old", mapEntry.getValue());
                            change.put("new", null);
                            changes.put(granularFieldName, change);
                            
                            // Retirer l'entrée globale si elle existe
                            changes.remove(fieldName);
                        }
                    }
                }
            }
        }

        return changes;
    }

    /**
     * Compare deux valeurs pour déterminer si elles sont égales
     */
    private boolean areEqual(Object value1, Object value2) {
        if (value1 == null && value2 == null) {
            return true;
        }
        if (value1 == null || value2 == null) {
            return false;
        }
        
        // Comparaison spéciale pour les Map
        if (value1 instanceof Map && value2 instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<?, ?> map1 = (Map<?, ?>) value1;
            @SuppressWarnings("unchecked")
            Map<?, ?> map2 = (Map<?, ?>) value2;
            return map1.equals(map2);
        }
        
        return value1.equals(value2);
    }
}
