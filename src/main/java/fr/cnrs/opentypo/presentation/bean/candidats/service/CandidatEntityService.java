package fr.cnrs.opentypo.presentation.bean.candidats.service;

import fr.cnrs.opentypo.application.dto.EntityStatusEnum;
import fr.cnrs.opentypo.domain.entity.*;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRelationRepository;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRepository;
import fr.cnrs.opentypo.infrastructure.persistence.EntityTypeRepository;
import fr.cnrs.opentypo.infrastructure.persistence.LangueRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.inject.Inject;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service pour les opérations sur les entités dans le contexte des candidats
 * Centralise la logique métier pour la création et la mise à jour des entités
 */
@Service
@Slf4j
public class CandidatEntityService {

    @Inject
    private EntityRepository entityRepository;

    @Inject
    private EntityTypeRepository entityTypeRepository;

    @Inject
    private EntityRelationRepository entityRelationRepository;

    @Inject
    private LangueRepository langueRepository;

    /**
     * Crée une nouvelle entité avec les données de base (étape 1 du wizard)
     * @param entityTypeId ID du type d'entité
     * @param code Code de l'entité
     * @param label Label de l'entité
     * @param langueCode Code de la langue
     * @param currentUser Utilisateur actuel (peut être null)
     */
    @Transactional
    public Entity createEntityFromStep1(Long entityTypeId, String code, String label, String langueCode, Utilisateur currentUser) {
        // Récupérer le type d'entité
        EntityType entityType = entityTypeRepository.findById(entityTypeId)
            .orElseThrow(() -> new IllegalStateException("Le type d'entité sélectionné n'existe pas."));
        
        // Créer la nouvelle entité avec seulement les données de l'étape 1
        Entity newEntity = new Entity();
        newEntity.setCode(code.trim());
        newEntity.setNom(label.trim());
        newEntity.setEntityType(entityType);
        newEntity.setStatut(EntityStatusEnum.PROPOSITION.name());
        newEntity.setPublique(true);
        newEntity.setCreateDate(LocalDateTime.now());
        
        // Définir l'utilisateur actuel si fourni
        if (currentUser != null) {
            newEntity.setCreateBy(currentUser.getEmail());
            List<Utilisateur> auteurs = new ArrayList<>();
            auteurs.add(currentUser);
            newEntity.setAuteurs(auteurs);
        }
        
        // Ajouter le label principal (de l'étape 1)
        Langue languePrincipale = langueRepository.findByCode(langueCode);
        if (languePrincipale != null) {
            Label labelPrincipal = new Label();
            labelPrincipal.setNom(label.trim());
            labelPrincipal.setLangue(languePrincipale);
            labelPrincipal.setEntity(newEntity);
            List<Label> labels = new ArrayList<>();
            labels.add(labelPrincipal);
            newEntity.setLabels(labels);
        }
        
        // Sauvegarder l'entité
        Entity savedEntity = entityRepository.save(newEntity);
        
        log.info("Entité créée à l'étape 1: ID={}, Code={}, Nom={}", 
            savedEntity.getId(), savedEntity.getCode(), savedEntity.getNom());
        
        return savedEntity;
    }

    /**
     * Crée une relation parent-enfant entre deux entités
     */
    @Transactional
    public void createParentChildRelation(Entity parent, Entity child) {
        if (parent == null || child == null) {
            throw new IllegalArgumentException("Parent et child ne peuvent pas être null");
        }

        // Vérifier si la relation existe déjà
        boolean relationExists = entityRelationRepository.existsByParentAndChild(
            parent.getId(), child.getId());
        
        if (!relationExists) {
            // Créer la relation parent-enfant
            EntityRelation relation = new EntityRelation();
            relation.setParent(parent);
            relation.setChild(child);
            entityRelationRepository.save(relation);
            
            log.info("Relation créée entre parent (ID={}) et enfant (ID={})", 
                parent.getId(), child.getId());
        } else {
            log.info("Relation existe déjà entre parent (ID={}) et enfant (ID={})", 
                parent.getId(), child.getId());
        }
    }

    /**
     * Supprime une entité et toutes ses relations
     */
    @Transactional
    public void deleteEntityWithRelations(Entity entity) {
        if (entity == null || entity.getId() == null) {
            return;
        }

        // Recharger l'entité depuis la base
        Entity refreshedEntity = entityRepository.findById(entity.getId()).orElse(null);
        if (refreshedEntity == null) {
            return;
        }

        // Supprimer toutes les relations où cette entité est parent
        List<EntityRelation> parentRelations = entityRelationRepository.findByParent(refreshedEntity);
        if (parentRelations != null) {
            entityRelationRepository.deleteAll(parentRelations);
        }

        // Supprimer toutes les relations où cette entité est enfant
        List<EntityRelation> childRelations = entityRelationRepository.findByChild(refreshedEntity);
        if (childRelations != null) {
            entityRelationRepository.deleteAll(childRelations);
        }

        // Supprimer l'entité elle-même
        entityRepository.delete(refreshedEntity);
        
        log.info("Entité ID={} et toutes ses relations ont été supprimées", refreshedEntity.getId());
    }

    /**
     * Met à jour les champs de base d'une entité
     */
    @Transactional
    public void updateEntityBasicFields(Entity entity, String commentaire, String bibliographie, 
                                       String referencesBibliographiques, String ateliers, Integer tpq, Integer taq) {
        if (entity == null || entity.getId() == null) {
            return;
        }

        // Recharger l'entité depuis la base
        Entity refreshedEntity = entityRepository.findById(entity.getId()).orElse(null);
        if (refreshedEntity == null) {
            return;
        }

        // Mettre à jour les champs
        if (commentaire != null) {
            refreshedEntity.setCommentaire(commentaire.trim());
        }
        if (bibliographie != null) {
            refreshedEntity.setBibliographie(bibliographie.trim());
        }
        if (referencesBibliographiques != null && !referencesBibliographiques.isEmpty()) {
            refreshedEntity.setRereferenceBibliographique(referencesBibliographiques);
        } else {
            refreshedEntity.setRereferenceBibliographique(null);
        }
        if (ateliers != null && !ateliers.isEmpty()) {
            refreshedEntity.setAteliers(ateliers);
        } else {
            refreshedEntity.setAteliers(null);
        }
        if (tpq != null) {
            refreshedEntity.setTpq(tpq);
        }
        if (taq != null) {
            refreshedEntity.setTaq(taq);
        }

        // Sauvegarder
        entityRepository.save(refreshedEntity);
        log.debug("Champs de base mis à jour pour l'entité ID={}", refreshedEntity.getId());
    }
}
