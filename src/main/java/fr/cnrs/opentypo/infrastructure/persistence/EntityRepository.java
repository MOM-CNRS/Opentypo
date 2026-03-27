package fr.cnrs.opentypo.infrastructure.persistence;

import fr.cnrs.opentypo.domain.entity.Entity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

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
     * Recherche par code métier (metadata), égalité insensible à la casse.
     */
    @Query("SELECT e.id FROM Entity e JOIN e.metadata m "
            + "WHERE LOWER(CAST(m.code AS string)) = LOWER(CAST(:code AS string))")
    Optional<Long> findIdByMetadataCodeExactIgnoreCase(@Param("code") String code);

    /**
     * Recherche par code métier (metadata), sous-chaîne (contient), insensible à la casse.
     * <p>
     * {@code GROUP BY} au lieu de {@code DISTINCT … ORDER BY createDate} : PostgreSQL exige que les expressions
     * du {@code ORDER BY} figurent dans le {@code SELECT} lorsque {@code DISTINCT} est utilisé sur l’id seul.
     */
    @Query("SELECT e.id FROM Entity e JOIN e.metadata m "
            + "WHERE LOWER(CAST(m.code AS string)) LIKE LOWER(CAST(CONCAT('%', CAST(:q AS string), '%') AS string)) "
            + "GROUP BY e.id, e.createDate ORDER BY e.createDate DESC")
    List<Long> findIdsByMetadataCodeContaining(@Param("q") String q);

    /**
     * Recherche par libellé dans une langue donnée, égalité insensible à la casse.
     */
    @Query("SELECT e.id FROM Entity e JOIN e.labels l JOIN l.langue lang "
            + "WHERE lang.code = :langCode "
            + "AND LOWER(CAST(l.nom AS string)) = LOWER(CAST(:nom AS string)) "
            + "GROUP BY e.id, e.createDate ORDER BY e.createDate DESC")
    List<Long> findIdsByLabelExactInLang(@Param("nom") String nom, @Param("langCode") String langCode);

    /**
     * Recherche par libellé dans une langue donnée, sous-chaîne (contient), insensible à la casse.
     */
    @Query("SELECT e.id FROM Entity e JOIN e.labels l JOIN l.langue lang "
            + "WHERE lang.code = :langCode "
            + "AND LOWER(CAST(l.nom AS string)) LIKE LOWER(CAST(CONCAT('%', CAST(:q AS string), '%') AS string)) "
            + "GROUP BY e.id, e.createDate ORDER BY e.createDate DESC")
    List<Long> findIdsByLabelContainingInLang(@Param("q") String q, @Param("langCode") String langCode);

    /**
     * Loads an entity with type, metadata and labels (REST API / detail views).
     */
    @Query("SELECT DISTINCT e FROM Entity e "
            + "LEFT JOIN FETCH e.entityType "
            + "LEFT JOIN FETCH e.metadata "
            + "LEFT JOIN FETCH e.labels l LEFT JOIN FETCH l.langue "
            + "WHERE e.id = :id")
    Optional<Entity> findByIdForApi(@Param("id") Long id);

    /**
     * Même graphe que {@link #findByIdForApi(Long)} pour plusieurs identifiants (une requête, évite le N+1).
     * Ne pas appeler avec une collection vide.
     */
    @Query("SELECT DISTINCT e FROM Entity e "
            + "LEFT JOIN FETCH e.entityType "
            + "LEFT JOIN FETCH e.metadata "
            + "LEFT JOIN FETCH e.labels l LEFT JOIN FETCH l.langue "
            + "WHERE e.id IN :ids")
    List<Entity> findByIdsForApi(@Param("ids") Collection<Long> ids);

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
     * Vérifie si une entité du type donné existe avec un label ayant le même nom et la même langue
     * (comparaison insensible à la casse pour le nom).
     */
    @Query("SELECT COUNT(e) > 0 FROM Entity e JOIN e.labels l JOIN l.langue lang JOIN e.entityType et " +
           "WHERE LOWER(CAST(l.nom AS string)) = LOWER(CAST(:nom AS string)) AND lang.code = :langueCode AND et.code = :entityTypeCode")
    boolean existsByLabelNomAndLangueCodeAndEntityTypeCode(@Param("nom") String nom, @Param("langueCode") String langueCode,
                                                          @Param("entityTypeCode") String entityTypeCode);

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
           "  LOWER(CAST(m.code AS string)) LIKE LOWER(CAST(CONCAT('%', CAST(:searchTerm AS string), '%') AS string)) " +
           "  OR EXISTS (" +
           "    SELECT 1 FROM Label lbl " +
           "    WHERE lbl.entity.id = e.id " +
           "    AND lbl.langue.code = :langCode " +
           "    AND LOWER(CAST(lbl.nom AS string)) LIKE LOWER(CAST(CONCAT('%', CAST(:searchTerm AS string), '%') AS string))" +
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
           "  LOWER(CAST(m.code AS string)) LIKE LOWER(CAST(CONCAT(CAST(:searchTerm AS string), '%') AS string)) " +
           "  OR EXISTS (" +
           "    SELECT 1 FROM Label lbl " +
           "    WHERE lbl.entity.id = e.id " +
           "    AND lbl.langue.code = :langCode " +
           "    AND LOWER(CAST(lbl.nom AS string)) LIKE LOWER(CAST(CONCAT(CAST(:searchTerm AS string), '%') AS string))" +
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
           "  LOWER(CAST(m.code AS string)) = LOWER(CAST(:searchTerm AS string)) " +
           "  OR EXISTS (" +
           "    SELECT 1 FROM Label lbl " +
           "    WHERE lbl.entity.id = e.id " +
           "    AND lbl.langue.code = :langCode " +
           "    AND LOWER(CAST(lbl.nom AS string)) = LOWER(CAST(:searchTerm AS string))" +
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
     * Charge une entité par ID avec ses images (pour la galerie).
     */
    @Query("SELECT DISTINCT e FROM Entity e " +
           "LEFT JOIN FETCH e.images " +
           "WHERE e.id = :id")
    Optional<Entity> findByIdWithImages(@Param("id") Long id);

    /**
     * Supprime une entité par ID sans la charger (évite la cascade sur metadata
     * qui provoquerait une double suppression).
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM Entity e WHERE e.id = :id")
    void deleteByIdDirect(@Param("id") Long id);
}

