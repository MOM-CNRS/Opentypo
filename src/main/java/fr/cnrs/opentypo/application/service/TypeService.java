package fr.cnrs.opentypo.application.service;

import fr.cnrs.opentypo.common.constant.EntityConstants;
import fr.cnrs.opentypo.domain.entity.CaracteristiquePhysique;
import fr.cnrs.opentypo.domain.entity.CaracteristiquePhysiqueMonnaie;
import fr.cnrs.opentypo.domain.entity.Commentaire;
import fr.cnrs.opentypo.domain.entity.Description;
import fr.cnrs.opentypo.domain.entity.DescriptionDetail;
import fr.cnrs.opentypo.domain.entity.DescriptionMonnaie;
import fr.cnrs.opentypo.domain.entity.DescriptionPate;
import fr.cnrs.opentypo.domain.entity.Entity;
import fr.cnrs.opentypo.domain.entity.EntityMetadata;
import fr.cnrs.opentypo.domain.entity.EntityRelation;
import fr.cnrs.opentypo.domain.entity.EntityType;
import fr.cnrs.opentypo.domain.entity.Label;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRelationRepository;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRepository;
import fr.cnrs.opentypo.infrastructure.persistence.EntityTypeRepository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


@Slf4j
@Service
@Transactional
public class TypeService implements Serializable {

    @Autowired
    private EntityRelationRepository entityRelationRepository;

    @Autowired
    private EntityRepository entityRepository;

    @Autowired
    private EntityTypeRepository entityTypeRepository;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private GroupService groupService;

    @Autowired
    private SerieService serieService;


    /**
     * Charge les types rattachés au groupe sélectionné, ordonnés par display_order puis par code.
     * Ordre par défaut : alphabétique croissant quand display_order est null.
     */
    public List<Entity> loadGroupTypes(Entity selectedGroup) {
        return loadTypesOrdered(selectedGroup, EntityConstants.ENTITY_TYPE_TYPE);
    }

    /**
     * Charge les types rattachés à la série sélectionnée, ordonnés par display_order puis par code.
     * Ordre par défaut : alphabétique croissant quand display_order est null.
     */
    public List<Entity> loadSerieTypes(Entity selectedSerie) {
        return loadTypesOrdered(selectedSerie, EntityConstants.ENTITY_TYPE_TYPE);
    }

    private List<Entity> loadTypesOrdered(Entity parent, String typeCode) {
        List<EntityRelation> relations = entityRelationRepository.findRelationsByParentAndTypeOrdered(parent, typeCode);
        return relations.stream()
                .map(EntityRelation::getChild)
                .collect(Collectors.toList());
    }

    /**
     * Vérifie si le parent (groupe ou série) a des types avec un ordre personnalisé.
     */
    public boolean hasCustomTypesOrder(Entity parent) {
        if (parent == null || parent.getId() == null) return false;
        return entityRelationRepository.hasCustomOrderForChildren(parent.getId(), EntityConstants.ENTITY_TYPE_TYPE);
    }

    /**
     * Met à jour l'ordre d'affichage des types pour un parent donné.
     * @param parentId ID du parent (série ou groupe)
     * @param orderedChildIds Liste des IDs des types dans l'ordre souhaité
     */
    public void updateTypesDisplayOrder(Long parentId, List<Long> orderedChildIds) {
        if (parentId == null || orderedChildIds == null || orderedChildIds.isEmpty()) return;
        for (int i = 0; i < orderedChildIds.size(); i++) {
            final int order = i;
            Long childId = orderedChildIds.get(i);
            entityRelationRepository.findByParentIdAndChildId(parentId, childId)
                    .ifPresent(rel -> rel.setDisplayOrder(order));
        }
    }

    /**
     * Retourne la liste des groupes et séries pouvant être parents d'un type (même référentiel).
     */
    public List<Entity> getPossibleParentsForType(Entity type) {
        if (type == null) return new ArrayList<>();
        Entity reference = findReferenceAncestor(type);
        return getPossibleParentsForReference(reference);
    }

    /**
     * Retourne la liste des groupes et séries pouvant être parents d'un type pour un référentiel donné.
     */
    public List<Entity> getPossibleParentsForReference(Entity reference) {
        if (reference == null) return new ArrayList<>();
        List<Entity> result = new ArrayList<>();
        List<Entity> categories = categoryService.loadCategoriesByReference(reference);
        for (Entity category : categories) {
            List<Entity> groups = groupService.loadCategoryGroups(category);
            for (Entity group : groups) {
                result.add(group);
                List<Entity> series = serieService.loadGroupSeries(group);
                result.addAll(series);
            }
        }
        return result;
    }

    /** Retourne le référentiel ancêtre d'une entité (pour vérification des permissions). */
    public Entity findReferenceAncestor(Entity entity) {
        if (entity == null) return null;
        Entity current = entity;
        while (current != null) {
            if (current.getEntityType() != null
                    && EntityConstants.ENTITY_TYPE_REFERENCE.equals(current.getEntityType().getCode())) {
                return current;
            }
            List<Entity> parents = entityRelationRepository.findParentsByChild(current);
            if (parents == null || parents.isEmpty()) break;
            current = parents.get(0);
        }
        return null;
    }

    /**
     * Duplique un type : nouveau code, parent choisi, toutes les données copiées sauf les images.
     *
     * @param source Type source à dupliquer
     * @param newCode Nouveau code pour le type dupliqué
     * @param parent Parent (groupe ou série) auquel rattacher le nouveau type
     * @return Le type dupliqué créé
     */
    public Entity duplicateType(Entity source, String newCode, Entity parent) {
        if (source == null || newCode == null || newCode.isBlank() || parent == null) {
            throw new IllegalArgumentException("Paramètres invalides pour la duplication");
        }
        if (source.getEntityType() == null || !EntityConstants.ENTITY_TYPE_TYPE.equals(source.getEntityType().getCode())) {
            throw new IllegalArgumentException("Seuls les types peuvent être dupliqués");
        }

        EntityType typeEntityType = entityTypeRepository.findByCode(EntityConstants.ENTITY_TYPE_TYPE)
                .orElseThrow(() -> new IllegalStateException("Le type d'entité 'TYPE' n'existe pas."));

        Entity duplicate = new Entity();
        duplicate.setCode(newCode.trim());
        duplicate.setEntityType(typeEntityType);
        duplicate.setStatut(source.getStatut());
        duplicate.setCreateDate(LocalDateTime.now());
        duplicate.setCreateBy(source.getCreateBy());
        duplicate.setPeriode(source.getPeriode());
        duplicate.setProduction(source.getProduction());
        duplicate.setCategorieFonctionnelle(source.getCategorieFonctionnelle());
        duplicate.setAuteurs(source.getAuteurs() != null ? new ArrayList<>(source.getAuteurs()) : new ArrayList<>());
        duplicate.setImages(new ArrayList<>()); // Pas d'images

        // Métadonnées (sauf code déjà défini)
        EntityMetadata srcMeta = source.getMetadata();
        if (srcMeta != null) {
            EntityMetadata newMeta = new EntityMetadata();
            newMeta.setEntity(duplicate);
            newMeta.setCode(newCode.trim());
            newMeta.setCommentaireDatation(srcMeta.getCommentaireDatation());
            newMeta.setBibliographie(srcMeta.getBibliographie());
            newMeta.setAppellation(srcMeta.getAppellation());
            newMeta.setRereferenceBibliographique(srcMeta.getRereferenceBibliographique());
            newMeta.setAlignementExterne(srcMeta.getAlignementExterne());
            newMeta.setReference(srcMeta.getReference());
            newMeta.setTypologieScientifique(srcMeta.getTypologieScientifique());
            newMeta.setIdentifiantPerenne(srcMeta.getIdentifiantPerenne());
            newMeta.setAncienneVersion(srcMeta.getAncienneVersion());
            newMeta.setTpq(srcMeta.getTpq());
            newMeta.setTaq(srcMeta.getTaq());
            newMeta.setRelationExterne(srcMeta.getRelationExterne());
            newMeta.setRelationImitation(srcMeta.getRelationImitation());
            newMeta.setDenominationInstrumentum(srcMeta.getDenominationInstrumentum());
            newMeta.setAteliers(srcMeta.getAteliers());
            newMeta.setAttestations(srcMeta.getAttestations());
            newMeta.setCorpusLies(srcMeta.getCorpusLies());
            newMeta.setSitesArcheologiques(srcMeta.getSitesArcheologiques());
            newMeta.setCorpusExterne(srcMeta.getCorpusExterne());
            newMeta.setCommentaire(srcMeta.getCommentaire());
            newMeta.setInterne(srcMeta.getInterne());
            duplicate.setMetadata(newMeta);
        }

        // Labels
        if (source.getLabels() != null && !source.getLabels().isEmpty()) {
            List<Label> newLabels = new ArrayList<>();
            for (Label srcLabel : source.getLabels()) {
                Label nl = new Label();
                nl.setNom(srcLabel.getNom());
                nl.setEntity(duplicate);
                nl.setLangue(srcLabel.getLangue());
                newLabels.add(nl);
            }
            duplicate.setLabels(newLabels);
        }

        // Descriptions
        if (source.getDescriptions() != null && !source.getDescriptions().isEmpty()) {
            List<Description> newDescs = new ArrayList<>();
            for (Description srcDesc : source.getDescriptions()) {
                Description nd = new Description();
                nd.setValeur(srcDesc.getValeur());
                nd.setEntity(duplicate);
                nd.setLangue(srcDesc.getLangue());
                newDescs.add(nd);
            }
            duplicate.setDescriptions(newDescs);
        }

        // Commentaires
        if (source.getCommentaires() != null && !source.getCommentaires().isEmpty()) {
            List<Commentaire> newComms = new ArrayList<>();
            for (Commentaire srcComm : source.getCommentaires()) {
                Commentaire nc = new Commentaire();
                nc.setEntity(duplicate);
                nc.setContenu(srcComm.getContenu());
                nc.setUtilisateur(srcComm.getUtilisateur());
                nc.setDateCreation(LocalDateTime.now());
                newComms.add(nc);
            }
            duplicate.setCommentaires(newComms);
        }

        // DescriptionDetail
        DescriptionDetail srcDD = source.getDescriptionDetail();
        if (srcDD != null) {
            DescriptionDetail newDD = new DescriptionDetail();
            newDD.setEntity(duplicate);
            newDD.setDecors(srcDD.getDecors());
            newDD.setMarques(srcDD.getMarques());
            newDD.setFonction(srcDD.getFonction());
            newDD.setMetrologie(srcDD.getMetrologie());
            duplicate.setDescriptionDetail(newDD);
        }

        // CaracteristiquePhysique
        CaracteristiquePhysique srcCP = source.getCaracteristiquePhysique();
        if (srcCP != null) {
            CaracteristiquePhysique newCP = new CaracteristiquePhysique();
            newCP.setEntity(duplicate);
            newCP.setMetrologie(srcCP.getMetrologie());
            newCP.setMateriaux(srcCP.getMateriaux());
            newCP.setForme(srcCP.getForme());
            newCP.setDimensions(srcCP.getDimensions());
            newCP.setTechnique(srcCP.getTechnique());
            newCP.setFabrication(srcCP.getFabrication());
            duplicate.setCaracteristiquePhysique(newCP);
        }

        // DescriptionPate
        DescriptionPate srcDP = source.getDescriptionPate();
        if (srcDP != null) {
            DescriptionPate newDP = new DescriptionPate();
            newDP.setEntity(duplicate);
            newDP.setDescription(srcDP.getDescription());
            newDP.setCouleur(srcDP.getCouleur());
            newDP.setNature(srcDP.getNature());
            newDP.setInclusion(srcDP.getInclusion());
            newDP.setCuisson(srcDP.getCuisson());
            duplicate.setDescriptionPate(newDP);
        }

        // DescriptionMonnaie
        DescriptionMonnaie srcDM = source.getDescriptionMonnaie();
        if (srcDM != null) {
            DescriptionMonnaie newDM = new DescriptionMonnaie();
            newDM.setEntity(duplicate);
            newDM.setDroit(srcDM.getDroit());
            newDM.setLegendeDroit(srcDM.getLegendeDroit());
            newDM.setRevers(srcDM.getRevers());
            newDM.setLegendeRevers(srcDM.getLegendeRevers());
            duplicate.setDescriptionMonnaie(newDM);
        }

        // CaracteristiquePhysiqueMonnaie
        CaracteristiquePhysiqueMonnaie srcCPM = source.getCaracteristiquePhysiqueMonnaie();
        if (srcCPM != null) {
            CaracteristiquePhysiqueMonnaie newCPM = new CaracteristiquePhysiqueMonnaie();
            newCPM.setEntity(duplicate);
            newCPM.setMateriaux(srcCPM.getMateriaux());
            newCPM.setDenomination(srcCPM.getDenomination());
            newCPM.setMetrologie(srcCPM.getMetrologie());
            newCPM.setValeur(srcCPM.getValeur());
            duplicate.setCaracteristiquePhysiqueMonnaie(newCPM);
        }

        Entity saved = entityRepository.save(duplicate);

        if (!entityRelationRepository.existsByParentAndChild(parent.getId(), saved.getId())) {
            EntityRelation relation = new EntityRelation();
            relation.setParent(parent);
            relation.setChild(saved);
            entityRelationRepository.save(relation);
        }

        return saved;
    }

    /**
     * Change le parent d'un type : supprime l'ancienne relation et crée la nouvelle.
     */
    public void changeTypeParent(Entity type, Entity newParent) {
        if (type == null || newParent == null) return;
        List<Entity> oldParents = entityRelationRepository.findParentsByChild(type);
        for (Entity oldParent : oldParents) {
            entityRelationRepository.findByParentIdAndChildId(oldParent.getId(), type.getId())
                    .ifPresent(entityRelationRepository::delete);
        }
        if (!entityRelationRepository.existsByParentAndChild(newParent.getId(), type.getId())) {
            EntityRelation relation = new EntityRelation();
            relation.setParent(newParent);
            relation.setChild(type);
            entityRelationRepository.save(relation);
        }
    }
}
