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
            Set<Long> allRevisionNumbers = new TreeSet<>(Collections.reverseOrder());
            
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
                    
                    // Récupérer la date de révision depuis la table REVINFO via le repository
                    LocalDateTime revDate = null;
                    Optional<RevisionInfo> revisionInfo = revisionInfoRepository.findById(revisionNumber);
                    if (revisionInfo.isPresent()) {
                        revDate = revisionInfo.get().getRevisionDate();
                    }
                    
                    // Déterminer le type de révision depuis entity_aud si disponible
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
                            revisionType = String.valueOf(revTypeData.get(0)[0]);
                        } else {
                            // Si pas de type dans entity_aud, déterminer selon le contexte
                            Long maxRevision = allRevisionNumbers.stream().max(Long::compareTo).orElse(null);
                            if (entity != null && revisionNumber.equals(maxRevision)) {
                                // Si c'est la première révision et que l'entité existe, c'est une création
                                revisionType = "0"; // ADD
                            } else if (entity == null) {
                                // Si l'entité n'existe pas à cette révision, c'est probablement une modification dans une table liée
                                revisionType = "1"; // MOD
                            }
                        }
                    } catch (Exception e) {
                        log.debug("Erreur lors de la récupération du type de révision: {}", e.getMessage());
                        // Utiliser le type par défaut
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
        } catch (Exception e) {
            log.warn("Erreur lors de l'extraction des données liées: {}", e.getMessage());
        }
    }

    /**
     * Extrait les données de l'entité en utilisant uniquement des requêtes SQL natives (sans AuditReader)
     */
    private Map<String, Object> extractEntityDataNative(Entity entity, Long entityId, Long revisionNumber) {
        Map<String, Object> data = new HashMap<>();
        
        try {
            if (entity != null && entity.getMetadata() != null) {
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
            
        } catch (Exception e) {
            log.warn("Erreur générale lors de l'extraction native des données pour l'entité {} à la révision {}: {}", 
                    entityId, revisionNumber, e.getMessage(), e);
        }
        
        return data;
    }
}
