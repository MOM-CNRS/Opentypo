package fr.cnrs.opentypo.application.service;

import fr.cnrs.opentypo.application.dto.EntityRevisionDTO;
import fr.cnrs.opentypo.domain.entity.*;
import fr.cnrs.opentypo.infrastructure.persistence.EntityTypeRepository;
import fr.cnrs.opentypo.infrastructure.persistence.LangueRepository;
import fr.cnrs.opentypo.infrastructure.persistence.ReferenceOpenthesoRepository;
import fr.cnrs.opentypo.infrastructure.persistence.RevisionInfoRepository;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Service pour accéder aux données d'audit Hibernate Envers
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class AuditService {

    @PersistenceContext
    private EntityManager entityManager;

    @Inject
    private RevisionInfoRepository revisionInfoRepository;

    @Inject
    private EntityTypeRepository entityTypeRepository;

    @Inject
    private LangueRepository langueRepository;

    @Inject
    private ReferenceOpenthesoRepository referenceOpenthesoRepository;

    /**
     * Récupère toutes les révisions d'une entité depuis les tables d'audit (entity_aud et tables en relation)
     * Inclut les modifications dans toutes les tables liées (description_aud, label_aud, etc.)
     * Utilise NOT_SUPPORTED pour suspendre la transaction existante et éviter les rollbacks
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public List<EntityRevisionDTO> getEntityRevisions(Long entityId) {
        List<EntityRevisionDTO> revisions = new ArrayList<>();
        AuditReader auditReader = null;
        try {
            log.debug("Début de la récupération des révisions pour l'entité {}", entityId);
            // Créer une transaction manuelle pour les opérations de lecture
            try {
                auditReader = AuditReaderFactory.get(entityManager);
            } catch (Exception e) {
                log.warn("Erreur lors de la création de l'AuditReader: {}", e.getMessage());
                // Continuer sans AuditReader, utiliser uniquement les requêtes natives
            }
            
            // Récupérer toutes les révisions depuis toutes les tables d'audit liées
            // Ordre ASCENDANT (plus ancienne d'abord) pour que previousEntityData = révision chronologiquement avant
            Set<Long> allRevisionNumbers = new TreeSet<>();
            
            // 1. Révisions depuis entity_aud
            collectRevisionsFromTable("entity_aud", "id", entityId, allRevisionNumbers);
            
            // 2. Révisions depuis description_aud
            collectRevisionsFromTable("description_aud", "entity_id", entityId, allRevisionNumbers);
            
            // 3. Révisions depuis label_aud
            collectRevisionsFromTable("label_aud", "entity_id", entityId, allRevisionNumbers);
            
            // 4. Révisions depuis description_detail_aud
            collectRevisionsFromTable("description_detail_aud", "entity_id", entityId, allRevisionNumbers);
            
            // 5. Révisions depuis caracteristique_physique_aud
            collectRevisionsFromTable("caracteristique_physique_aud", "entity_id", entityId, allRevisionNumbers);
            
            // 6. Révisions depuis description_pate_aud
            collectRevisionsFromTable("description_pate_aud", "entity_id", entityId, allRevisionNumbers);
            
            // 7. Révisions depuis reference_opentheso_aud
            collectRevisionsFromTable("reference_opentheso_aud", "entity_id", entityId, allRevisionNumbers);
            
            // 8. Révisions depuis entity_metadata_aud
            collectRevisionsFromTable("entity_metadata_aud", "entity_id", entityId, allRevisionNumbers);
            
            // 9. Révisions depuis image_aud
            collectRevisionsFromTable("image_aud", "entity_id", entityId, allRevisionNumbers);
            
            // 10. Révisions depuis entity_relation_aud (parent OU child)
            collectRevisionsFromEntityRelation(entityId, allRevisionNumbers);
            
            // 11. Révisions depuis description_monnaie_aud
            collectRevisionsFromTable("description_monnaie_aud", "entity_id", entityId, allRevisionNumbers);
            
            // 12. Révisions depuis caracteristique_physique_monnaie_aud
            collectRevisionsFromTable("caracteristique_physique_monnaie_aud", "entity_id", entityId, allRevisionNumbers);
            
            log.info("Total de révisions uniques trouvées pour l'entité {}: {}", entityId, allRevisionNumbers.size());
            
            // Si aucune révision trouvée, retourner une liste vide
            if (allRevisionNumbers.isEmpty()) {
                log.warn("Aucune révision trouvée pour l'entité {} dans aucune table d'audit", entityId);
                return new ArrayList<>();
            }
            
            EntityRevisionDTO previousRevision = null;
            
            // Pour chaque révision unique, récupérer l'entité complète
            for (Long revisionNumber : allRevisionNumbers) {
                try {
                    // Récupérer l'entité à cette révision (peut être null si la révision vient d'une table liée)
                    Entity entity = null;
                    if (auditReader != null) {
                        try {
                            entity = auditReader.find(Entity.class, entityId, revisionNumber);
                        } catch (org.hibernate.envers.exception.RevisionDoesNotExistException e) {
                            // C'est normal si l'entité n'existe pas à cette révision (modification dans une table liée)
                            log.debug("Entité {} non trouvée à la révision {} (probablement une modification dans une table liée)", 
                                     entityId, revisionNumber);
                        } catch (Exception e) {
                            // Autre exception - logger mais continuer
                            log.debug("Exception lors de la recherche de l'entité {} à la révision {}: {}", 
                                     entityId, revisionNumber, e.getMessage());
                        }
                    }
                    
                    // Récupérer la date et l'utilisateur depuis la table REVINFO via le repository
                    LocalDateTime revDate = null;
                    String modifiedBy = null;
                    Optional<RevisionInfo> revisionInfo = revisionInfoRepository.findById(revisionNumber);
                    if (revisionInfo.isPresent()) {
                        revDate = revisionInfo.get().getRevisionDate();
                        modifiedBy = revisionInfo.get().getModifiedBy();
                    }
                    
                    // Déterminer le type de révision : ADD(0)=Création, MOD(1)=Modification, DEL(2)=Suppression
                    String revisionType = "1"; // MOD par défaut
                    try {
                        @SuppressWarnings("unchecked")
                        List<Object[]> revTypeData = entityManager.createNativeQuery(
                            "SELECT revtype FROM entity_aud WHERE id = :entityId AND rev = :revisionNumber LIMIT 1"
                        )
                        .setParameter("entityId", entityId)
                        .setParameter("revisionNumber", revisionNumber)
                        .getResultList();
                        
                        if (!revTypeData.isEmpty() && revTypeData.get(0)[0] != null) {
                            revisionType = normalizeRevisionType(revTypeData.get(0)[0]);
                        } else {
                            // Pas de ligne dans entity_aud : la révision vient d'une table liée uniquement
                            // Si c'est la révision la plus ancienne (min) et que l'entité existe, c'était la création
                            Long minRevision = allRevisionNumbers.stream().min(Long::compareTo).orElse(null);
                            if (entity != null && minRevision != null && revisionNumber.equals(minRevision)) {
                                revisionType = "0"; // ADD - première apparition de l'entité
                            } else {
                                revisionType = "1"; // MOD - modification dans une table liée
                            }
                        }
                    } catch (Exception e) {
                        log.debug("Erreur lors de la récupération du type de révision: {}", e.getMessage());
                    }
                    
                    // Extraire les données de l'entité ET de ses relations auditées à cette révision
                    Map<String, Object> entityData = new HashMap<>();
                    try {
                        if (auditReader != null) {
                            entityData = extractEntityData(auditReader, entity, entityId, revisionNumber);
                        } else {
                            // Si pas d'AuditReader, utiliser uniquement les requêtes natives
                            entityData = extractEntityDataNative(entity, entityId, revisionNumber);
                        }
                    } catch (Exception e) {
                        log.warn("Erreur lors de l'extraction des données pour l'entité {} à la révision {}: {}", 
                                entityId, revisionNumber, e.getMessage(), e);
                        // Garder la map vide
                    }
                    
                    EntityRevisionDTO revisionDTO = EntityRevisionDTO.builder()
                        .entityId(entityId)
                        .revisionNumber(revisionNumber)
                        .revisionType(revisionType)
                        .revisionDate(revDate)
                        .modifiedBy(modifiedBy)
                        .entityData(entityData != null ? entityData : new HashMap<>())
                        .previousEntityData(previousRevision != null ? previousRevision.getEntityData() : null)
                        .build();
                    
                    revisions.add(revisionDTO);
                    previousRevision = revisionDTO;
                } catch (Throwable t) {
                    // Catch toutes les exceptions y compris les Error
                    log.warn("Erreur lors de la récupération de l'entité {} à la révision {}: {}", 
                             entityId, revisionNumber, t.getMessage(), t);
                    // Continuer avec la révision suivante
                }
            }
            
            log.info("Fin de la récupération des révisions pour l'entité {}: {} révisions retournées", entityId, revisions.size());
            // Inverser pour afficher les plus récentes en premier (previousEntityData est maintenant correct)
            Collections.reverse(revisions);
            return revisions;
        } catch (org.hibernate.envers.exception.AuditException e) {
            log.warn("Erreur AuditException lors de la récupération des révisions pour l'entité {}: {}", entityId, e.getMessage());
            // Retourner les révisions déjà collectées ou une liste vide
            return revisions;
        } catch (RuntimeException e) {
            log.error("Erreur RuntimeException lors de la récupération des révisions pour l'entité {}: {}", entityId, e.getMessage(), e);
            // Log la cause racine si elle existe
            if (e.getCause() != null) {
                log.error("Cause racine: {}", e.getCause().getMessage(), e.getCause());
            }
            return revisions;
        } catch (Exception e) {
            log.error("Erreur Exception lors de la récupération des révisions pour l'entité {}: {}", entityId, e.getMessage(), e);
            // Log la cause racine si elle existe
            if (e.getCause() != null) {
                log.error("Cause racine: {}", e.getCause().getMessage(), e.getCause());
            }
            return revisions;
        } catch (Throwable t) {
            log.error("Erreur Throwable lors de la récupération des révisions pour l'entité {}: {}", entityId, t.getMessage(), t);
            return revisions;
        }
    }

    /**
     * Normalise la valeur revtype (ADD=0, MOD=1, DEL=2) depuis un résultat SQL.
     * Gère Short, Integer, Long, BigDecimal, etc.
     */
    private String normalizeRevisionType(Object value) {
        if (value == null) {
            return "1";
        }
        if (value instanceof Number) {
            int n = ((Number) value).intValue();
            return n == 0 ? "0" : (n == 2 ? "2" : "1");
        }
        String s = value.toString().trim();
        if ("0".equals(s) || "ADD".equalsIgnoreCase(s)) return "0";
        if ("2".equals(s) || "DEL".equalsIgnoreCase(s)) return "2";
        return "1";
    }

    /**
     * Collecte les numéros de révision depuis entity_relation_aud (parent_id ou child_id = entityId)
     */
    private void collectRevisionsFromEntityRelation(Long entityId, Set<Long> allRevisionNumbers) {
        try {
            @SuppressWarnings("unchecked")
            List<Object> results = entityManager.createNativeQuery(
                "SELECT DISTINCT rev FROM entity_relation_aud WHERE parent_id = :entityId OR child_id = :entityId"
            )
            .setParameter("entityId", entityId)
            .getResultList();
            
            int addedCount = 0;
            for (Object result : results) {
                try {
                    if (result instanceof Number) {
                        allRevisionNumbers.add(((Number) result).longValue());
                        addedCount++;
                    } else if (result instanceof Object[]) {
                        Object[] row = (Object[]) result;
                        if (row.length > 0 && row[0] instanceof Number) {
                            allRevisionNumbers.add(((Number) row[0]).longValue());
                            addedCount++;
                        }
                    }
                } catch (Exception e) {
                    log.debug("Erreur lors du traitement d'un résultat de entity_relation_aud: {}", e.getMessage());
                }
            }
            if (addedCount > 0) {
                log.debug("Révisions trouvées dans entity_relation_aud pour l'entité {}: {} ({} ajoutées)", entityId, results.size(), addedCount);
            }
        } catch (Exception e) {
            log.debug("Aucune révision ou erreur dans entity_relation_aud pour l'entité {}: {}", entityId, e.getMessage());
        }
    }

    /**
     * Collecte les numéros de révision depuis une table d'audit spécifique
     */
    private void collectRevisionsFromTable(String tableName, String idColumn, Long entityId, Set<Long> allRevisionNumbers) {
        try {
            // Vérifier d'abord si la table existe
            @SuppressWarnings("unchecked")
            List<Object> results = entityManager.createNativeQuery(
                String.format("SELECT DISTINCT rev FROM %s WHERE %s = :entityId", tableName, idColumn)
            )
            .setParameter("entityId", entityId)
            .getResultList();
            
            int addedCount = 0;
            for (Object result : results) {
                try {
                    if (result instanceof Number) {
                        allRevisionNumbers.add(((Number) result).longValue());
                        addedCount++;
                    } else if (result instanceof Object[]) {
                        Object[] row = (Object[]) result;
                        if (row.length > 0 && row[0] instanceof Number) {
                            allRevisionNumbers.add(((Number) row[0]).longValue());
                            addedCount++;
                        }
                    }
                } catch (Exception e) {
                    log.debug("Erreur lors du traitement d'un résultat de {}: {}", tableName, e.getMessage());
                }
            }
            if (addedCount > 0) {
                log.debug("Révisions trouvées dans {} pour l'entité {}: {} ({} ajoutées)", tableName, entityId, results.size(), addedCount);
            }
        } catch (Exception e) {
            // Ne pas logger comme erreur car certaines tables peuvent ne pas exister ou être vides
            log.debug("Aucune révision ou erreur dans {} pour l'entité {}: {}", tableName, entityId, e.getMessage());
        }
    }

    /**
     * Extrait les données principales d'une entité ET de ses relations auditées depuis les tables d'audit
     */
    private Map<String, Object> extractEntityData(AuditReader auditReader, Entity entity, Long entityId, Long revisionNumber) {
        Map<String, Object> data = new HashMap<>();
        
        try {
            // Si entity est null, essayer de récupérer les données depuis les tables liées uniquement
            if (entity == null) {
                if (revisionNumber == null || auditReader == null || entityId == null) {
                    return data;
                }
                extractRelatedDataOnly(auditReader, data, entityId, revisionNumber);
                return data;
            }
            
            Long actualEntityId = entity.getId() != null ? entity.getId() : entityId;
            
            // Données principales de l'entité
            data.put("id", entity.getId());
            if (entity.getMetadata() != null) {
                try {
                    data.put("code", entity.getMetadata().getCode());
                } catch (Exception e) {
                    log.debug("Erreur lors de la récupération du code: {}", e.getMessage());
                }
            }
            
            // Récupérer les labels depuis label_aud avec requête native
            try {
                @SuppressWarnings("unchecked")
                List<Object[]> labelRows = entityManager.createNativeQuery(
                    "SELECT l.nom, l.code_langue FROM label_aud l WHERE l.entity_id = :entityId AND l.rev = :revisionNumber"
                )
                .setParameter("entityId", actualEntityId)
                .setParameter("revisionNumber", revisionNumber)
                .getResultList();
                
                if (labelRows != null && !labelRows.isEmpty()) {
                    Map<String, String> labelsMap = new HashMap<>();
                    for (Object[] row : labelRows) {
                        try {
                            String nom = row[0] != null ? row[0].toString() : "";
                            String langueCode = row.length > 1 && row[1] != null ? row[1].toString() : "unknown";
                            labelsMap.put(langueCode, nom);
                        } catch (Exception e) {
                            log.debug("Erreur lors du traitement du label: {}", e.getMessage());
                        }
                    }
                    if (!labelsMap.isEmpty()) {
                        data.put("labels", labelsMap);
                    }
                }
            } catch (Exception e) {
                log.debug("Erreur lors de la récupération des labels: {}", e.getMessage());
            }
            
            // Récupérer les descriptions depuis description_aud avec requête native
            try {
                @SuppressWarnings("unchecked")
                List<Object[]> descriptionRows = entityManager.createNativeQuery(
                    "SELECT d.valeur, d.code_langue FROM description_aud d WHERE d.entity_id = :entityId AND d.rev = :revisionNumber"
                )
                .setParameter("entityId", actualEntityId)
                .setParameter("revisionNumber", revisionNumber)
                .getResultList();
                
                if (descriptionRows != null && !descriptionRows.isEmpty()) {
                    Map<String, String> descriptionsMap = new HashMap<>();
                    for (Object[] row : descriptionRows) {
                        try {
                            String valeur = row[0] != null ? row[0].toString() : "";
                            String langueCode = row.length > 1 && row[1] != null ? row[1].toString() : "unknown";
                            descriptionsMap.put(langueCode, valeur);
                        } catch (Exception e) {
                            log.debug("Erreur lors du traitement de la description: {}", e.getMessage());
                        }
                    }
                    if (!descriptionsMap.isEmpty()) {
                        data.put("descriptions", descriptionsMap);
                    }
                }
            } catch (Exception e) {
                log.debug("Erreur lors de la récupération des descriptions: {}", e.getMessage());
            }
            
            // Données complémentaires : entity_aud, entity_metadata_aud, entités liées, images
            extractEnrichedData(data, actualEntityId, revisionNumber);
            
        } catch (Exception e) {
            log.warn("Erreur générale lors de l'extraction des données pour l'entité {} à la révision {}: {}", 
                    entityId, revisionNumber, e.getMessage(), e);
        }
        
        return data;
    }

    /**
     * Extrait uniquement les données des tables liées (sans les données de entity_aud)
     */
    private void extractRelatedDataOnly(AuditReader auditReader, Map<String, Object> data, Long entityId, Long revisionNumber) {
        if (auditReader == null || entityId == null || revisionNumber == null) {
            return;
        }
        
        try {
            // Labels avec requête native
            try {
                @SuppressWarnings("unchecked")
                List<Object[]> labelRows = entityManager.createNativeQuery(
                    "SELECT l.nom, l.code_langue FROM label_aud l WHERE l.entity_id = :entityId AND l.rev = :revisionNumber"
                )
                .setParameter("entityId", entityId)
                .setParameter("revisionNumber", revisionNumber)
                .getResultList();
                
                if (labelRows != null && !labelRows.isEmpty()) {
                    Map<String, String> labelsMap = new HashMap<>();
                    for (Object[] row : labelRows) {
                        try {
                            String nom = row[0] != null ? row[0].toString() : "";
                            String langueCode = row.length > 1 && row[1] != null ? row[1].toString() : "unknown";
                            labelsMap.put(langueCode, nom);
                        } catch (Exception e) {
                            log.debug("Erreur lors du traitement du label: {}", e.getMessage());
                        }
                    }
                    if (!labelsMap.isEmpty()) {
                        data.put("labels", labelsMap);
                    }
                }
            } catch (Exception e) {
                log.debug("Erreur lors de la récupération des labels: {}", e.getMessage());
            }
            
            // Descriptions avec requête native
            try {
                @SuppressWarnings("unchecked")
                List<Object[]> descriptionRows = entityManager.createNativeQuery(
                    "SELECT d.valeur, d.code_langue FROM description_aud d WHERE d.entity_id = :entityId AND d.rev = :revisionNumber"
                )
                .setParameter("entityId", entityId)
                .setParameter("revisionNumber", revisionNumber)
                .getResultList();
                
                if (descriptionRows != null && !descriptionRows.isEmpty()) {
                    Map<String, String> descriptionsMap = new HashMap<>();
                    for (Object[] row : descriptionRows) {
                        try {
                            String valeur = row[0] != null ? row[0].toString() : "";
                            String langueCode = row.length > 1 && row[1] != null ? row[1].toString() : "unknown";
                            descriptionsMap.put(langueCode, valeur);
                        } catch (Exception e) {
                            log.debug("Erreur lors du traitement de la description: {}", e.getMessage());
                        }
                    }
                    if (!descriptionsMap.isEmpty()) {
                        data.put("descriptions", descriptionsMap);
                    }
                }
            } catch (Exception e) {
                log.debug("Erreur lors de la récupération des descriptions: {}", e.getMessage());
            }
            
            extractEnrichedData(data, entityId, revisionNumber);
        } catch (Exception e) {
            log.warn("Erreur lors de l'extraction des données liées: {}", e.getMessage());
        }
    }

    /**
     * Extrait les données enrichies (entity_aud, entity_metadata_aud, entités liées, images)
     */
    private void extractEnrichedData(Map<String, Object> data, Long entityId, Long revisionNumber) {
        if (entityId == null || revisionNumber == null) {
            return;
        }
        try {
            // entity_aud : statut, id_ark, display_order
            try {
                @SuppressWarnings("unchecked")
                List<Object[]> entityRows = entityManager.createNativeQuery(
                    "SELECT statut, id_ark, display_order FROM entity_aud WHERE id = :entityId AND rev = :revisionNumber"
                )
                .setParameter("entityId", entityId)
                .setParameter("revisionNumber", revisionNumber)
                .getResultList();
                if (!entityRows.isEmpty() && entityRows.get(0) != null) {
                    Object[] row = entityRows.get(0);
                    if (row.length > 0 && row[0] != null) data.put("statut", row[0].toString());
                    if (row.length > 1 && row[1] != null) data.put("idArk", row[1].toString());
                    if (row.length > 2 && row[2] != null) data.put("displayOrder", row[2]);
                }
            } catch (Exception e) {
                log.debug("Erreur entity_aud: {}", e.getMessage());
            }
            
            // entity_metadata_aud : code, commentaire, bibliographie, appellation, tpq, taq, etc.
            try {
                @SuppressWarnings("unchecked")
                List<Object[]> metaRows = entityManager.createNativeQuery(
                    "SELECT code, commentaire, bibliographie, appellation, typologie_scientifique, identifiant_perenne, " +
                    "ancienne_version, tpq, taq, ateliers, attestations, sites_archeologiques, reference, appartient, associe " +
                    "FROM entity_metadata_aud WHERE entity_id = :entityId AND rev = :revisionNumber"
                )
                .setParameter("entityId", entityId)
                .setParameter("revisionNumber", revisionNumber)
                .getResultList();
                if (!metaRows.isEmpty() && metaRows.get(0) != null) {
                    Object[] row = metaRows.get(0);
                    if (row.length > 0 && row[0] != null) data.put("code", row[0].toString());
                    if (row.length > 1 && row[1] != null) data.put("commentaireMetadata", row[1].toString());
                    if (row.length > 2 && row[2] != null) data.put("bibliographie", row[2].toString());
                    if (row.length > 3 && row[3] != null) data.put("appellation", row[3].toString());
                    if (row.length > 4 && row[4] != null) data.put("typologieScientifique", row[4].toString());
                    if (row.length > 5 && row[5] != null) data.put("identifiantPerenne", row[5].toString());
                    if (row.length > 6 && row[6] != null) data.put("ancienneVersion", row[6].toString());
                    if (row.length > 7 && row[7] != null) data.put("tpq", row[7]);
                    if (row.length > 8 && row[8] != null) data.put("taq", row[8]);
                    if (row.length > 9 && row[9] != null) data.put("ateliers", row[9].toString());
                    if (row.length > 10 && row[10] != null) data.put("attestations", row[10].toString());
                    if (row.length > 11 && row[11] != null) data.put("sitesArcheologiques", row[11].toString());
                    if (row.length > 12 && row[12] != null) data.put("reference", row[12].toString());
                    if (row.length > 13 && row[13] != null) data.put("appartient", row[13].toString());
                    if (row.length > 14 && row[14] != null) data.put("associe", row[14].toString());
                }
            } catch (Exception e) {
                log.debug("Erreur entity_metadata_aud: {}", e.getMessage());
            }
            
            // description_detail_aud : decors, marques, metrologie
            try {
                @SuppressWarnings("unchecked")
                List<Object[]> ddRows = entityManager.createNativeQuery(
                    "SELECT decors, marques, metrologie FROM description_detail_aud WHERE entity_id = :entityId AND rev = :revisionNumber"
                )
                .setParameter("entityId", entityId)
                .setParameter("revisionNumber", revisionNumber)
                .getResultList();
                if (!ddRows.isEmpty() && ddRows.get(0) != null) {
                    Object[] row = ddRows.get(0);
                    if (row.length > 0 && row[0] != null) data.put("decors", row[0].toString());
                    if (row.length > 1 && row[1] != null) data.put("marques", row[1].toString());
                    if (row.length > 2 && row[2] != null) data.put("metrologieDetail", row[2].toString());
                }
            } catch (Exception e) {
                log.debug("Erreur description_detail_aud: {}", e.getMessage());
            }
            
            // description_pate_aud : description
            try {
                @SuppressWarnings("unchecked")
                List<Object[]> dpRows = entityManager.createNativeQuery(
                    "SELECT description FROM description_pate_aud WHERE entity_id = :entityId AND rev = :revisionNumber"
                )
                .setParameter("entityId", entityId)
                .setParameter("revisionNumber", revisionNumber)
                .getResultList();
                if (!dpRows.isEmpty() && dpRows.get(0) != null && dpRows.get(0).length > 0 && dpRows.get(0)[0] != null) {
                    data.put("descriptionPate", dpRows.get(0)[0].toString());
                }
            } catch (Exception e) {
                log.debug("Erreur description_pate_aud: {}", e.getMessage());
            }
            
            // image_aud : url, legende (liste d'images)
            try {
                @SuppressWarnings("unchecked")
                List<Object[]> imgRows = entityManager.createNativeQuery(
                    "SELECT url, legende FROM image_aud WHERE entity_id = :entityId AND rev = :revisionNumber"
                )
                .setParameter("entityId", entityId)
                .setParameter("revisionNumber", revisionNumber)
                .getResultList();
                if (!imgRows.isEmpty()) {
                    List<Map<String, String>> imagesList = new ArrayList<>();
                    for (Object[] row : imgRows) {
                        Map<String, String> imgMap = new HashMap<>();
                        if (row != null && row.length > 0 && row[0] != null) imgMap.put("url", row[0].toString());
                        if (row != null && row.length > 1 && row[1] != null) imgMap.put("legende", row[1].toString());
                        if (!imgMap.isEmpty()) imagesList.add(imgMap);
                    }
                    if (!imagesList.isEmpty()) data.put("images", imagesList);
                }
            } catch (Exception e) {
                log.debug("Erreur image_aud: {}", e.getMessage());
            }
        } catch (Exception e) {
            log.debug("Erreur extractEnrichedData: {}", e.getMessage());
        }
    }

    /**
     * Extrait les données de l'entité en utilisant uniquement des requêtes SQL natives (sans AuditReader)
     */
    private Map<String, Object> extractEntityDataNative(Entity entity, Long entityId, Long revisionNumber) {
        Map<String, Object> data = new HashMap<>();
        
        try {
            if (entity != null) {
                data.put("id", entity.getId());
                if (entity.getMetadata() != null) {
                    try {
                        data.put("code", entity.getMetadata().getCode());
                    } catch (Exception e) {
                        log.debug("Erreur lors de la récupération du code: {}", e.getMessage());
                    }
                }
            }
            
            // Récupérer les labels depuis label_aud avec requête native
            try {
                @SuppressWarnings("unchecked")
                List<Object[]> labelRows = entityManager.createNativeQuery(
                    "SELECT l.nom, l.code_langue FROM label_aud l WHERE l.entity_id = :entityId AND l.rev = :revisionNumber"
                )
                .setParameter("entityId", entityId)
                .setParameter("revisionNumber", revisionNumber)
                .getResultList();
                
                if (labelRows != null && !labelRows.isEmpty()) {
                    Map<String, String> labelsMap = new HashMap<>();
                    for (Object[] row : labelRows) {
                        try {
                            String nom = row[0] != null ? row[0].toString() : "";
                            String langueCode = row.length > 1 && row[1] != null ? row[1].toString() : "unknown";
                            labelsMap.put(langueCode, nom);
                        } catch (Exception e) {
                            log.debug("Erreur lors du traitement du label: {}", e.getMessage());
                        }
                    }
                    if (!labelsMap.isEmpty()) {
                        data.put("labels", labelsMap);
                    }
                }
            } catch (Exception e) {
                log.debug("Erreur lors de la récupération des labels: {}", e.getMessage());
            }
            
            // Récupérer les descriptions depuis description_aud avec requête native
            try {
                @SuppressWarnings("unchecked")
                List<Object[]> descriptionRows = entityManager.createNativeQuery(
                    "SELECT d.valeur, d.code_langue FROM description_aud d WHERE d.entity_id = :entityId AND d.rev = :revisionNumber"
                )
                .setParameter("entityId", entityId)
                .setParameter("revisionNumber", revisionNumber)
                .getResultList();
                
                if (descriptionRows != null && !descriptionRows.isEmpty()) {
                    Map<String, String> descriptionsMap = new HashMap<>();
                    for (Object[] row : descriptionRows) {
                        try {
                            String valeur = row[0] != null ? row[0].toString() : "";
                            String langueCode = row.length > 1 && row[1] != null ? row[1].toString() : "unknown";
                            descriptionsMap.put(langueCode, valeur);
                        } catch (Exception e) {
                            log.debug("Erreur lors du traitement de la description: {}", e.getMessage());
                        }
                    }
                    if (!descriptionsMap.isEmpty()) {
                        data.put("descriptions", descriptionsMap);
                    }
                }
            } catch (Exception e) {
                log.debug("Erreur lors de la récupération des descriptions: {}", e.getMessage());
            }
            
            extractEnrichedData(data, entityId, revisionNumber);
        } catch (Exception e) {
            log.warn("Erreur générale lors de l'extraction native des données pour l'entité {} à la révision {}: {}", 
                    entityId, revisionNumber, e.getMessage(), e);
        }
        
        return data;
    }
}
