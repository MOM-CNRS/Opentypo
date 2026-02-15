package fr.cnrs.opentypo.infrastructure.persistence;

import fr.cnrs.opentypo.domain.entity.Entity;
import fr.cnrs.opentypo.domain.entity.EntityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Repository Spring Data JPA pour l'entité Entity
 */
@Repository
public interface EntityRepository extends JpaRepository<Entity, Long> {

    /**
     * Trouve une entité par son code (via metadata)
     */
    @Query("SELECT e FROM Entity e JOIN e.metadata m WHERE m.code = :code")
    Optional<Entity> findByCode(@Param("code") String code);

    /**
     * Vérifie si une entité existe avec le code donné (via metadata)
     */
    @Query("SELECT COUNT(e) > 0 FROM Entity e JOIN e.metadata m WHERE m.code = :code")
    boolean existsByCode(@Param("code") String code);

    /**
     * Vérifie si une entité existe avec le code donné, en excluant une entité (pour mode édition).
     */
    @Query("SELECT COUNT(e) > 0 FROM Entity e JOIN e.metadata m WHERE m.code = :code AND e.id != :excludeEntityId")
    boolean existsByCodeExcludingEntityId(@Param("code") String code, @Param("excludeEntityId") Long excludeEntityId);

    /**
     * Vérifie si une entité existe avec un label ayant le même nom et la même langue (unicité label + langue).
     */
    @Query("SELECT COUNT(e) > 0 FROM Entity e JOIN e.labels l JOIN l.langue lang WHERE l.nom = :nom AND lang.code = :langueCode")
    boolean existsByLabelNomAndLangueCode(@Param("nom") String nom, @Param("langueCode") String langueCode);

    /**
     * Trouve toutes les entités d'un type donné
     */
    List<Entity> findByEntityType(EntityType entityType);

    /**
     * Trouve toutes les entités d'un type par le code du type
     */
    @Query("SELECT e FROM Entity e WHERE e.entityType.code = :typeCode")
    List<Entity> findByEntityTypeCode(@Param("typeCode") String typeCode);

    /**
     * Trouve toutes les entités d'un type par le code du type avec leurs labels
     */
    @Query("SELECT DISTINCT e FROM Entity e " +
           "LEFT JOIN FETCH e.labels " +
           "WHERE e.entityType.code = :typeCode")
    List<Entity> findByEntityTypeCodeWithLabels(@Param("typeCode") String typeCode);

    /**
     * Trouve toutes les entités par nom (insensible à la casse)
     * Recherche dans les labels de toutes les langues
     */
    @Query("SELECT DISTINCT e FROM Entity e " +
           "JOIN e.labels l " +
           "WHERE LOWER(l.nom) LIKE LOWER(CONCAT('%', :nom, '%'))")
    List<Entity> findByNomContainingIgnoreCase(@Param("nom") String nom);

    /**
     * Trouve toutes les entités par nom contenant le terme de recherche (insensible à la casse)
     * Recherche dans les labels de toutes les langues
     */
    @Query("SELECT DISTINCT e FROM Entity e " +
           "JOIN e.labels l " +
           "WHERE LOWER(l.nom) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Entity> findByNomContainingIgnoreCaseQuery(@Param("searchTerm") String searchTerm);

    /**
     * Trouve toutes les entités par statut
     */
    @Query("SELECT DISTINCT e FROM Entity e " +
           "LEFT JOIN FETCH e.labels " +
           "LEFT JOIN FETCH e.entityType " +
           "WHERE e.statut = :statut " +
           "ORDER BY e.createDate DESC")
    List<Entity> findByStatut(@Param("statut") String statut);

    /**
     * Recherche des entités par code ou label selon la langue (contient)
     * Recherche sur le code de l'entité (via metadata) OU sur les labels dans la langue spécifiée
     */
    @Query("SELECT DISTINCT e FROM Entity e " +
           "LEFT JOIN FETCH e.labels l " +
           "LEFT JOIN FETCH e.entityType " +
           "LEFT JOIN FETCH e.metadata m " +
           "WHERE (" +
           "  LOWER(m.code) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "  OR EXISTS (" +
           "    SELECT 1 FROM Label lbl " +
           "    WHERE lbl.entity.id = e.id " +
           "    AND lbl.langue.code = :langCode " +
           "    AND LOWER(lbl.nom) LIKE LOWER(CONCAT('%', :searchTerm, '%'))" +
           "  )" +
           ")")
    List<Entity> searchByCodeOrLabelContains(@Param("searchTerm") String searchTerm, 
                                             @Param("langCode") String langCode);

    /**
     * Recherche des entités par code ou label selon la langue (commence par)
     */
    @Query("SELECT DISTINCT e FROM Entity e " +
           "LEFT JOIN FETCH e.labels l " +
           "LEFT JOIN FETCH e.entityType " +
           "LEFT JOIN FETCH e.metadata m " +
           "WHERE (" +
           "  LOWER(m.code) LIKE LOWER(CONCAT(:searchTerm, '%')) " +
           "  OR EXISTS (" +
           "    SELECT 1 FROM Label lbl " +
           "    WHERE lbl.entity.id = e.id " +
           "    AND lbl.langue.code = :langCode " +
           "    AND LOWER(lbl.nom) LIKE LOWER(CONCAT(:searchTerm, '%'))" +
           "  )" +
           ")")
    List<Entity> searchByCodeOrLabelStartsWith(@Param("searchTerm") String searchTerm, 
                                                @Param("langCode") String langCode);

    /**
     * Recherche des entités par code ou label selon la langue (chaîne exacte)
     */
    @Query("SELECT DISTINCT e FROM Entity e " +
           "LEFT JOIN FETCH e.labels l " +
           "LEFT JOIN FETCH e.entityType " +
           "LEFT JOIN FETCH e.metadata m " +
           "WHERE (" +
           "  LOWER(m.code) = LOWER(:searchTerm) " +
           "  OR EXISTS (" +
           "    SELECT 1 FROM Label lbl " +
           "    WHERE lbl.entity.id = e.id " +
           "    AND lbl.langue.code = :langCode " +
           "    AND LOWER(lbl.nom) = LOWER(:searchTerm)" +
           "  )" +
           ")")
    List<Entity> searchByCodeOrLabelExact(@Param("searchTerm") String searchTerm, 
                                          @Param("langCode") String langCode);

    /**
     * Charge toutes les entités dont l'id est dans la liste, avec entityType chargé (évite N+1).
     * Ne pas appeler avec une liste vide.
     */
    @Query("SELECT DISTINCT e FROM Entity e JOIN FETCH e.entityType WHERE e.id IN :ids")
    List<Entity> findByIdInWithEntityType(@Param("ids") Collection<Long> ids);

    /**
     * Charge une entité par ID avec labels et descriptions (pour édition).
     * Metadata est chargé à la demande (lazy) ; labels et descriptions sont fetchees.
     */
    @Query("SELECT DISTINCT e FROM Entity e " +
           "LEFT JOIN FETCH e.labels l LEFT JOIN FETCH l.langue " +
           "LEFT JOIN FETCH e.descriptions d LEFT JOIN FETCH d.langue " +
           "WHERE e.id = :id")
    Optional<Entity> findByIdWithLabelsAndDescriptions(@Param("id") Long id);

    /**
     * Charge une entité par ID avec ses images (pour la galerie).
     */
    @Query("SELECT DISTINCT e FROM Entity e " +
           "LEFT JOIN FETCH e.images " +
           "WHERE e.id = :id")
    Optional<Entity> findByIdWithImages(@Param("id") Long id);
}

