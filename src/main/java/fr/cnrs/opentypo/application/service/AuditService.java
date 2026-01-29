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
import org.hibernate.envers.query.AuditEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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
     */
    public List<EntityRevisionDTO> getEntityRevisions(Long entityId) {
        try {
            AuditReader auditReader = AuditReaderFactory.get(entityManager);
            
            // Utiliser une requête native pour récupérer les révisions depuis entity_aud
            // Cela évite les problèmes d'alias avec forRevisionsOfEntity
            // Le suffixe _aud est configuré dans application.yaml (audit_table_suffix: _aud)
            @SuppressWarnings("unchecked")
            List<Object[]> revisionData = entityManager.createNativeQuery(
                "SELECT rev, revtype FROM entity_aud WHERE id = :entityId ORDER BY rev DESC"
            )
            .setParameter("entityId", entityId)
            .getResultList();
            
            // Si aucune révision trouvée, retourner une liste vide
            if (revisionData == null || revisionData.isEmpty()) {
                log.debug("Aucune révision trouvée pour l'entité {}", entityId);
                return new ArrayList<>();
            }
            
            List<EntityRevisionDTO> revisions = new ArrayList<>();
            EntityRevisionDTO previousRevision = null;
            
            // Pour chaque révision, récupérer l'entité complète
            for (Object[] row : revisionData) {
                Number revisionNumber = (Number) row[0];
                String revisionType = String.valueOf(row[1]);
                
                try {
                    // Récupérer l'entité à cette révision
                    // Note: Les relations ManyToOne ne seront pas chargées, on les récupérera via des requêtes natives
                    Entity entity = auditReader.find(Entity.class, entityId, revisionNumber.longValue());
                    
                    if (entity == null) {
                        log.debug("Entité {} non trouvée à la révision {}", entityId, revisionNumber);
                        continue;
                    }
                    
                    // Récupérer la date de révision depuis la table REVINFO via le repository
                    LocalDateTime revDate = null;
                    Optional<RevisionInfo> revisionInfo = revisionInfoRepository.findById(revisionNumber.longValue());
                    if (revisionInfo.isPresent()) {
                        revDate = revisionInfo.get().getRevisionDate();
                    }
                    
                    // Extraire les données de l'entité ET de ses relations auditées à cette révision
                    // Cette méthode gère les relations ManyToOne via des requêtes natives et repositories
                    Map<String, Object> entityData = extractEntityDataWithRelations(auditReader, entity, revisionNumber.longValue());
                    
                    EntityRevisionDTO revisionDTO = EntityRevisionDTO.builder()
                        .entityId(entityId)
                        .revisionNumber(revisionNumber.longValue())
                        .revisionType(revisionType)
                        .revisionDate(revDate)
                        .entityData(entityData)
                        .previousEntityData(previousRevision != null ? previousRevision.getEntityData() : null)
                        .build();
                    
                    revisions.add(revisionDTO);
                    previousRevision = revisionDTO;
                    
                } catch (Exception e) {
                    log.debug("Erreur lors de la récupération de l'entité {} à la révision {}: {}", 
                             entityId, revisionNumber, e.getMessage());
                    // Continuer avec la révision suivante
                }
            }
            
            return revisions;
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des révisions pour l'entité {}", entityId, e);
            return new ArrayList<>();
        }
    }

    /**
     * Récupère une révision spécifique d'une entité
     */
    public EntityRevisionDTO getEntityRevision(Long entityId, Long revisionNumber) {
        try {
            AuditReader auditReader = AuditReaderFactory.get(entityManager);
            
            // Récupérer l'entité à cette révision
            Entity entity = auditReader.find(Entity.class, entityId, revisionNumber);
            
            if (entity == null) {
                return null;
            }
            
            // Récupérer la révision précédente pour comparaison
            Entity previousEntity = null;
            if (revisionNumber > 1) {
                try {
                    previousEntity = auditReader.find(Entity.class, entityId, revisionNumber - 1);
                } catch (Exception e) {
                    log.debug("Pas de révision précédente pour l'entité {} à la révision {}", entityId, revisionNumber);
                }
            }
            
            // Extraire les données avec les relations auditées
            Map<String, Object> entityData = extractEntityDataWithRelations(auditReader, entity, revisionNumber);
            Map<String, Object> previousEntityData = null;
            if (previousEntity != null && revisionNumber > 1) {
                previousEntityData = extractEntityDataWithRelations(auditReader, previousEntity, revisionNumber - 1);
            }
            
            // Déterminer le type de révision
            String revisionType = "1"; // MOD par défaut
            if (previousEntity == null) {
                revisionType = "0"; // ADD
            }
            
            // Récupérer la date de révision depuis la table REVINFO
            LocalDateTime revDate = null;
            Optional<RevisionInfo> revisionInfo = revisionInfoRepository.findById(revisionNumber);
            if (revisionInfo.isPresent()) {
                revDate = revisionInfo.get().getRevisionDate();
            }
            
            return EntityRevisionDTO.builder()
                .entityId(entityId)
                .revisionNumber(revisionNumber)
                .revisionType(revisionType)
                .revisionDate(revDate)
                .entityData(entityData)
                .previousEntityData(previousEntityData)
                .build();
        } catch (Exception e) {
            log.error("Erreur lors de la récupération de la révision {} pour l'entité {}", revisionNumber, entityId, e);
            return null;
        }
    }

    /**
     * Extrait les données principales d'une entité dans une Map (méthode de compatibilité)
     */
    private Map<String, Object> extractEntityData(Entity entity) {
        return extractEntityDataWithRelations(null, entity, null);
    }

    /**
     * Extrait les données principales d'une entité ET de ses relations auditées depuis les tables d'audit
     * Utilise les tables entity_aud, label_aud, description_detail_aud, etc.
     * Utilise les repositories pour récupérer les données des relations non-auditées ou actuelles
     */
    private Map<String, Object> extractEntityDataWithRelations(AuditReader auditReader, Entity entity, Long revisionNumber) {
        Map<String, Object> data = new HashMap<>();
        
        if (entity == null) {
            return data;
        }
        
        // Données principales de l'entité (depuis entity_aud)
        data.put("id", entity.getId());
        data.put("code", entity.getCode());
        data.put("nom", entity.getNom());
        data.put("commentaire", entity.getCommentaire());
        data.put("bibliographie", entity.getBibliographie());
        data.put("appellation", entity.getAppellation());
        data.put("reference", entity.getReference());
        data.put("typologieScientifique", entity.getTypologieScientifique());
        data.put("identifiantPerenne", entity.getIdentifiantPerenne());
        data.put("ancienneVersion", entity.getAncienneVersion());
        data.put("statut", entity.getStatut());
        data.put("tpq", entity.getTpq());
        data.put("taq", entity.getTaq());
        data.put("publique", entity.getPublique());
        data.put("ateliers", entity.getAteliers());
        data.put("attestations", entity.getAttestations());
        data.put("sitesArcheologiques", entity.getSitesArcheologiques());
        data.put("createDate", entity.getCreateDate());
        data.put("createBy", entity.getCreateBy());
        
        // Type d'entité - utiliser une requête native pour récupérer l'ID depuis entity_aud
        try {
            Long entityTypeId = getEntityTypeIdFromAudit(entity.getId(), revisionNumber);
            if (entityTypeId != null) {
                Optional<EntityType> entityType = entityTypeRepository.findById(entityTypeId);
                if (entityType.isPresent()) {
                    data.put("entityTypeCode", entityType.get().getCode());
                }
            }
        } catch (Exception e) {
            log.debug("Erreur lors de la récupération du type d'entité pour l'entité {} à la révision {}: {}", 
                     entity.getId(), revisionNumber, e.getMessage());
        }
        
        // Récupérer les données des relations auditées depuis les tables d'audit
        if (auditReader != null && revisionNumber != null) {
            try {
                // Labels (depuis label_aud) - utiliser les repositories pour simplifier
                List<Label> labels = getLabelsAtRevision(auditReader, entity.getId(), revisionNumber);
                if (labels != null && !labels.isEmpty()) {
                    Map<String, String> labelsMap = new HashMap<>();
                    for (Label label : labels) {
                        try {
                            String langueCode = "unknown";
                            // Récupérer le code de langue depuis label_aud avec une requête native
                            String codeLangue = getLangueCodeFromLabelAudit(label.getId(), revisionNumber);
                            if (codeLangue != null) {
                                langueCode = codeLangue;
                            } else {
                                // Fallback : essayer d'accéder à la relation (peut échouer)
                                try {
                                    if (label.getLangue() != null) {
                                        langueCode = label.getLangue().getCode();
                                    }
                                } catch (Exception e) {
                                    // Relation non chargée, utiliser le code récupéré ou "unknown"
                                }
                            }
                            labelsMap.put(langueCode, label.getNom());
                        } catch (Exception e) {
                            log.debug("Erreur lors du traitement du label {}: {}", label.getId(), e.getMessage());
                        }
                    }
                    data.put("labels", labelsMap);
                }
                
                // DescriptionDetail (depuis description_detail_aud)
                DescriptionDetail descriptionDetail = getDescriptionDetailAtRevision(auditReader, entity.getId(), revisionNumber);
                if (descriptionDetail != null) {
                    data.put("decors", descriptionDetail.getDecors());
                    data.put("marques", descriptionDetail.getMarques());
                    data.put("metrologie", descriptionDetail.getMetrologie());
                    // Fonction/Usage - gérer la relation ManyToOne avec try-catch
                    try {
                        if (descriptionDetail.getFonction() != null) {
                            data.put("fonctionUsage", descriptionDetail.getFonction().getValeur());
                        }
                    } catch (Exception e) {
                        log.debug("Erreur lors de la récupération de la fonction pour DescriptionDetail: {}", e.getMessage());
                        // Récupérer depuis description_detail_aud avec une requête native puis utiliser le repository
                        Long fonctionId = getFonctionIdFromDescriptionDetailAudit(descriptionDetail.getId(), revisionNumber);
                        if (fonctionId != null) {
                            Optional<ReferenceOpentheso> fonction = referenceOpenthesoRepository.findById(fonctionId);
                            if (fonction.isPresent()) {
                                data.put("fonctionUsage", fonction.get().getValeur());
                            }
                        }
                    }
                }
                
                // CaracteristiquePhysique (depuis caracteristique_physique_aud)
                CaracteristiquePhysique caracteristiquePhysique = getCaracteristiquePhysiqueAtRevision(auditReader, entity.getId(), revisionNumber);
                if (caracteristiquePhysique != null) {
                    data.put("materiaux", caracteristiquePhysique.getMateriaux());
                    // Métrologie - récupérer l'ID depuis l'audit puis utiliser le repository
                    try {
                        if (caracteristiquePhysique.getMetrologie() != null) {
                            data.put("metrologieRef", caracteristiquePhysique.getMetrologie().getValeur());
                        }
                    } catch (Exception e) {
                        Long metrologieId = getMetrologieIdFromCaracteristiquePhysiqueAudit(caracteristiquePhysique.getId(), revisionNumber);
                        if (metrologieId != null) {
                            Optional<ReferenceOpentheso> metrologie = referenceOpenthesoRepository.findById(metrologieId);
                            if (metrologie.isPresent()) {
                                data.put("metrologieRef", metrologie.get().getValeur());
                            }
                        }
                    }
                    // Fabrication - récupérer l'ID depuis l'audit puis utiliser le repository
                    try {
                        if (caracteristiquePhysique.getFabrication() != null) {
                            data.put("fabricationFaconnage", caracteristiquePhysique.getFabrication().getValeur());
                        }
                    } catch (Exception e) {
                        Long fabricationId = getFabricationIdFromCaracteristiquePhysiqueAudit(caracteristiquePhysique.getId(), revisionNumber);
                        if (fabricationId != null) {
                            Optional<ReferenceOpentheso> fabrication = referenceOpenthesoRepository.findById(fabricationId);
                            if (fabrication.isPresent()) {
                                data.put("fabricationFaconnage", fabrication.get().getValeur());
                            }
                        }
                    }
                }
                
                // DescriptionPate (depuis description_pate_aud)
                DescriptionPate descriptionPate = getDescriptionPateAtRevision(auditReader, entity.getId(), revisionNumber);
                if (descriptionPate != null) {
                    data.put("descriptionPate", descriptionPate.getDescription());
                    // Récupérer les IDs depuis l'audit puis utiliser le repository pour chaque relation ManyToOne
                    Long couleurId = getCouleurIdFromDescriptionPateAudit(descriptionPate.getId(), revisionNumber);
                    if (couleurId != null) {
                        Optional<ReferenceOpentheso> couleur = referenceOpenthesoRepository.findById(couleurId);
                        if (couleur.isPresent()) {
                            data.put("couleurPate", couleur.get().getValeur());
                        }
                    }
                    
                    Long natureId = getNatureIdFromDescriptionPateAudit(descriptionPate.getId(), revisionNumber);
                    if (natureId != null) {
                        Optional<ReferenceOpentheso> nature = referenceOpenthesoRepository.findById(natureId);
                        if (nature.isPresent()) {
                            data.put("naturePate", nature.get().getValeur());
                        }
                    }
                    
                    Long inclusionId = getInclusionIdFromDescriptionPateAudit(descriptionPate.getId(), revisionNumber);
                    if (inclusionId != null) {
                        Optional<ReferenceOpentheso> inclusion = referenceOpenthesoRepository.findById(inclusionId);
                        if (inclusion.isPresent()) {
                            data.put("inclusions", inclusion.get().getValeur());
                        }
                    }
                    
                    Long cuissonId = getCuissonIdFromDescriptionPateAudit(descriptionPate.getId(), revisionNumber);
                    if (cuissonId != null) {
                        Optional<ReferenceOpentheso> cuisson = referenceOpenthesoRepository.findById(cuissonId);
                        if (cuisson.isPresent()) {
                            data.put("cuissonPostCuisson", cuisson.get().getValeur());
                        }
                    }
                }
                
                // ReferenceOpentheso liées (depuis reference-opentheso_aud)
                List<ReferenceOpentheso> referencesOpentheso = getReferenceOpenthesoAtRevision(auditReader, entity.getId(), revisionNumber);
                if (referencesOpentheso != null && !referencesOpentheso.isEmpty()) {
                    Map<String, List<String>> refsByCode = new HashMap<>();
                    for (ReferenceOpentheso ref : referencesOpentheso) {
                        if (ref.getCode() != null) {
                            refsByCode.computeIfAbsent(ref.getCode(), k -> new ArrayList<>()).add(ref.getValeur());
                        }
                    }
                    data.put("referencesOpentheso", refsByCode);
                }
                
            } catch (Exception e) {
                log.debug("Erreur lors de la récupération des relations auditées pour l'entité {} à la révision {}: {}", 
                         entity.getId(), revisionNumber, e.getMessage());
            }
        }
        
        return data;
    }

    /**
     * Récupère les labels d'une entité à une révision donnée depuis label_aud
     */
    private List<Label> getLabelsAtRevision(AuditReader auditReader, Long entityId, Long revisionNumber) {
        try {
            // Utiliser une requête native pour récupérer les labels depuis label_aud
            // car Envers ne supporte pas bien les relations to-many inverses
            @SuppressWarnings("unchecked")
            List<Label> allLabels = auditReader.createQuery()
                    .forEntitiesAtRevision(Label.class, revisionNumber)
                    .getResultList();
            
            // Filtrer manuellement les labels de cette entité
            List<Label> labels = allLabels.stream()
                    .filter(l -> l.getEntity() != null && entityId.equals(l.getEntity().getId()))
                    .collect(Collectors.toList());
            
            return labels;
        } catch (Exception e) {
            log.debug("Erreur lors de la récupération des labels pour l'entité {} à la révision {}: {}", 
                     entityId, revisionNumber, e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Récupère DescriptionDetail d'une entité à une révision donnée depuis description_detail_aud
     */
    private DescriptionDetail getDescriptionDetailAtRevision(AuditReader auditReader, Long entityId, Long revisionNumber) {
        try {
            // Pour OneToOne, on peut utiliser relatedId
            @SuppressWarnings("unchecked")
            List<DescriptionDetail> results = auditReader.createQuery()
                    .forEntitiesAtRevision(DescriptionDetail.class, revisionNumber)
                    .add(AuditEntity.relatedId("entity").eq(entityId))
                    .getResultList();
            
            return results.isEmpty() ? null : results.get(0);
        } catch (Exception e) {
            // Si relatedId ne fonctionne pas, essayer avec property
            try {
                @SuppressWarnings("unchecked")
                List<DescriptionDetail> allResults = auditReader.createQuery()
                        .forEntitiesAtRevision(DescriptionDetail.class, revisionNumber)
                        .getResultList();
                
                return allResults.stream()
                        .filter(dd -> dd.getEntity() != null && entityId.equals(dd.getEntity().getId()))
                        .findFirst()
                        .orElse(null);
            } catch (Exception e2) {
                log.debug("DescriptionDetail non trouvée pour l'entité {} à la révision {}: {}", 
                         entityId, revisionNumber, e2.getMessage());
                return null;
            }
        }
    }

    /**
     * Récupère CaracteristiquePhysique d'une entité à une révision donnée depuis caracteristique_physique_aud
     */
    private CaracteristiquePhysique getCaracteristiquePhysiqueAtRevision(AuditReader auditReader, Long entityId, Long revisionNumber) {
        try {
            @SuppressWarnings("unchecked")
            List<CaracteristiquePhysique> results = auditReader.createQuery()
                    .forEntitiesAtRevision(CaracteristiquePhysique.class, revisionNumber)
                    .add(AuditEntity.relatedId("entity").eq(entityId))
                    .getResultList();
            
            return results.isEmpty() ? null : results.get(0);
        } catch (Exception e) {
            // Fallback : récupérer toutes et filtrer
            try {
                @SuppressWarnings("unchecked")
                List<CaracteristiquePhysique> allResults = auditReader.createQuery()
                        .forEntitiesAtRevision(CaracteristiquePhysique.class, revisionNumber)
                        .getResultList();
                
                return allResults.stream()
                        .filter(cp -> cp.getEntity() != null && entityId.equals(cp.getEntity().getId()))
                        .findFirst()
                        .orElse(null);
            } catch (Exception e2) {
                log.debug("CaracteristiquePhysique non trouvée pour l'entité {} à la révision {}: {}", 
                         entityId, revisionNumber, e2.getMessage());
                return null;
            }
        }
    }

    /**
     * Récupère DescriptionPate d'une entité à une révision donnée depuis description_pate_aud
     */
    private DescriptionPate getDescriptionPateAtRevision(AuditReader auditReader, Long entityId, Long revisionNumber) {
        try {
            @SuppressWarnings("unchecked")
            List<DescriptionPate> results = auditReader.createQuery()
                    .forEntitiesAtRevision(DescriptionPate.class, revisionNumber)
                    .add(AuditEntity.relatedId("entity").eq(entityId))
                    .getResultList();
            
            return results.isEmpty() ? null : results.get(0);
        } catch (Exception e) {
            // Fallback : récupérer toutes et filtrer
            try {
                @SuppressWarnings("unchecked")
                List<DescriptionPate> allResults = auditReader.createQuery()
                        .forEntitiesAtRevision(DescriptionPate.class, revisionNumber)
                        .getResultList();
                
                return allResults.stream()
                        .filter(dp -> dp.getEntity() != null && entityId.equals(dp.getEntity().getId()))
                        .findFirst()
                        .orElse(null);
            } catch (Exception e2) {
                log.debug("DescriptionPate non trouvée pour l'entité {} à la révision {}: {}", 
                         entityId, revisionNumber, e2.getMessage());
                return null;
            }
        }
    }

    /**
     * Récupère les ReferenceOpentheso d'une entité à une révision donnée depuis reference-opentheso_aud
     */
    private List<ReferenceOpentheso> getReferenceOpenthesoAtRevision(AuditReader auditReader, Long entityId, Long revisionNumber) {
        try {
            @SuppressWarnings("unchecked")
            List<ReferenceOpentheso> results = auditReader.createQuery()
                    .forEntitiesAtRevision(ReferenceOpentheso.class, revisionNumber)
                    .add(AuditEntity.relatedId("entity").eq(entityId))
                    .getResultList();
            
            return results != null ? results : new ArrayList<>();
        } catch (Exception e) {
            // Fallback : récupérer toutes et filtrer
            try {
                @SuppressWarnings("unchecked")
                List<ReferenceOpentheso> allResults = auditReader.createQuery()
                        .forEntitiesAtRevision(ReferenceOpentheso.class, revisionNumber)
                        .getResultList();
                
                return allResults.stream()
                        .filter(ref -> ref.getEntity() != null && entityId.equals(ref.getEntity().getId()))
                        .collect(Collectors.toList());
            } catch (Exception e2) {
                log.debug("ReferenceOpentheso non trouvées pour l'entité {} à la révision {}: {}", 
                         entityId, revisionNumber, e2.getMessage());
                return new ArrayList<>();
            }
        }
    }

    /**
     * Compare deux révisions et retourne les champs modifiés
     */
    public Map<String, Map<String, Object>> getChangedFields(EntityRevisionDTO revision) {
        Map<String, Map<String, Object>> changes = new HashMap<>();
        
        if (revision == null || revision.getPreviousEntityData() == null) {
            return changes;
        }
        
        Map<String, Object> current = revision.getEntityData();
        Map<String, Object> previous = revision.getPreviousEntityData();
        
        for (String key : current.keySet()) {
            Object currentValue = current.get(key);
            Object previousValue = previous.get(key);
            
            if (!areEqual(currentValue, previousValue)) {
                Map<String, Object> change = new HashMap<>();
                change.put("old", previousValue);
                change.put("new", currentValue);
                changes.put(key, change);
            }
        }
        
        return changes;
    }

    /**
     * Compare deux valeurs en gérant les nulls
     */
    private boolean areEqual(Object obj1, Object obj2) {
        if (obj1 == null && obj2 == null) {
            return true;
        }
        if (obj1 == null || obj2 == null) {
            return false;
        }
        return obj1.equals(obj2);
    }

    /**
     * Récupère l'ID du type d'entité depuis entity_aud à une révision donnée
     * Utilise une requête native pour éviter les problèmes de lazy loading
     */
    private Long getEntityTypeIdFromAudit(Long entityId, Long revisionNumber) {
        try {
            Object result = entityManager.createNativeQuery(
                "SELECT entity_type_id FROM entity_aud WHERE id = :entityId AND rev = :revisionNumber"
            )
            .setParameter("entityId", entityId)
            .setParameter("revisionNumber", revisionNumber)
            .getSingleResult();
            
            if (result instanceof Number) {
                return ((Number) result).longValue();
            }
            return null;
        } catch (Exception e) {
            log.debug("Erreur lors de la récupération de entity_type_id depuis entity_aud: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Récupère l'ID de la langue depuis label_aud à une révision donnée
     */
    private String getLangueCodeFromLabelAudit(Long labelId, Long revisionNumber) {
        try {
            Object result = entityManager.createNativeQuery(
                "SELECT code_langue FROM label_aud WHERE id = :labelId AND rev = :revisionNumber"
            )
            .setParameter("labelId", labelId)
            .setParameter("revisionNumber", revisionNumber)
            .getSingleResult();
            
            return result != null ? result.toString() : null;
        } catch (Exception e) {
            log.debug("Erreur lors de la récupération de code_langue depuis label_aud: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Récupère l'ID de la fonction depuis description_detail_aud à une révision donnée
     */
    private Long getFonctionIdFromDescriptionDetailAudit(Long descriptionDetailId, Long revisionNumber) {
        try {
            Object result = entityManager.createNativeQuery(
                "SELECT fonction_id FROM description_detail_aud WHERE id = :descriptionDetailId AND rev = :revisionNumber"
            )
            .setParameter("descriptionDetailId", descriptionDetailId)
            .setParameter("revisionNumber", revisionNumber)
            .getSingleResult();
            
            if (result instanceof Number) {
                return ((Number) result).longValue();
            }
            return null;
        } catch (Exception e) {
            log.debug("Erreur lors de la récupération de fonction_id depuis description_detail_aud: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Récupère l'ID de la métrologie depuis caracteristique_physique_aud
     */
    private Long getMetrologieIdFromCaracteristiquePhysiqueAudit(Long caracteristiquePhysiqueId, Long revisionNumber) {
        try {
            Object result = entityManager.createNativeQuery(
                "SELECT metrologie_id FROM caracteristique_physique_aud WHERE id = :id AND rev = :revisionNumber"
            )
            .setParameter("id", caracteristiquePhysiqueId)
            .setParameter("revisionNumber", revisionNumber)
            .getSingleResult();
            
            return result instanceof Number ? ((Number) result).longValue() : null;
        } catch (Exception e) {
            log.debug("Erreur lors de la récupération de metrologie_id: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Récupère l'ID de la fabrication depuis caracteristique_physique_aud
     */
    private Long getFabricationIdFromCaracteristiquePhysiqueAudit(Long caracteristiquePhysiqueId, Long revisionNumber) {
        try {
            Object result = entityManager.createNativeQuery(
                "SELECT fabrication_id FROM caracteristique_physique_aud WHERE id = :id AND rev = :revisionNumber"
            )
            .setParameter("id", caracteristiquePhysiqueId)
            .setParameter("revisionNumber", revisionNumber)
            .getSingleResult();
            
            return result instanceof Number ? ((Number) result).longValue() : null;
        } catch (Exception e) {
            log.debug("Erreur lors de la récupération de fabrication_id: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Récupère l'ID de la couleur depuis description_pate_aud
     */
    private Long getCouleurIdFromDescriptionPateAudit(Long descriptionPateId, Long revisionNumber) {
        try {
            Object result = entityManager.createNativeQuery(
                "SELECT couleur_id FROM description_pate_aud WHERE id = :id AND rev = :revisionNumber"
            )
            .setParameter("id", descriptionPateId)
            .setParameter("revisionNumber", revisionNumber)
            .getSingleResult();
            
            return result instanceof Number ? ((Number) result).longValue() : null;
        } catch (Exception e) {
            log.debug("Erreur lors de la récupération de couleur_id: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Récupère l'ID de la nature depuis description_pate_aud
     */
    private Long getNatureIdFromDescriptionPateAudit(Long descriptionPateId, Long revisionNumber) {
        try {
            Object result = entityManager.createNativeQuery(
                "SELECT nature_id FROM description_pate_aud WHERE id = :id AND rev = :revisionNumber"
            )
            .setParameter("id", descriptionPateId)
            .setParameter("revisionNumber", revisionNumber)
            .getSingleResult();
            
            return result instanceof Number ? ((Number) result).longValue() : null;
        } catch (Exception e) {
            log.debug("Erreur lors de la récupération de nature_id: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Récupère l'ID de l'inclusion depuis description_pate_aud
     */
    private Long getInclusionIdFromDescriptionPateAudit(Long descriptionPateId, Long revisionNumber) {
        try {
            Object result = entityManager.createNativeQuery(
                "SELECT inclusion_id FROM description_pate_aud WHERE id = :id AND rev = :revisionNumber"
            )
            .setParameter("id", descriptionPateId)
            .setParameter("revisionNumber", revisionNumber)
            .getSingleResult();
            
            return result instanceof Number ? ((Number) result).longValue() : null;
        } catch (Exception e) {
            log.debug("Erreur lors de la récupération de inclusion_id: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Récupère l'ID de la cuisson depuis description_pate_aud
     */
    private Long getCuissonIdFromDescriptionPateAudit(Long descriptionPateId, Long revisionNumber) {
        try {
            Object result = entityManager.createNativeQuery(
                "SELECT cuisson_id FROM description_pate_aud WHERE id = :id AND rev = :revisionNumber"
            )
            .setParameter("id", descriptionPateId)
            .setParameter("revisionNumber", revisionNumber)
            .getSingleResult();
            
            return result instanceof Number ? ((Number) result).longValue() : null;
        } catch (Exception e) {
            log.debug("Erreur lors de la récupération de cuisson_id: {}", e.getMessage());
            return null;
        }
    }
}
