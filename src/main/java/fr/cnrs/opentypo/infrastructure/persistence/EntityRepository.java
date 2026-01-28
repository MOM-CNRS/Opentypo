package fr.cnrs.opentypo.infrastructure.persistence;

import fr.cnrs.opentypo.domain.entity.Entity;
import fr.cnrs.opentypo.domain.entity.EntityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository Spring Data JPA pour l'entité Entity
 */
@Repository
public interface EntityRepository extends JpaRepository<Entity, Long> {

    /**
     * Trouve une entité par son code
     */
    Optional<Entity> findByCode(String code);

    /**
     * Vérifie si une entité existe avec le code donné
     */
    boolean existsByCode(String code);

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
     */
    List<Entity> findByNomContainingIgnoreCase(String nom);

    /**
     * Trouve toutes les entités par nom contenant le terme de recherche (insensible à la casse)
     */
    @Query("SELECT e FROM Entity e WHERE LOWER(e.nom) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
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
     * Recherche sur le code de l'entité OU sur les labels dans la langue spécifiée
     */
    @Query("SELECT DISTINCT e FROM Entity e " +
           "LEFT JOIN FETCH e.labels l " +
           "LEFT JOIN FETCH e.entityType " +
           "WHERE (" +
           "  LOWER(e.code) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
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
           "WHERE (" +
           "  LOWER(e.code) LIKE LOWER(CONCAT(:searchTerm, '%')) " +
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
           "WHERE (" +
           "  LOWER(e.code) = LOWER(:searchTerm) " +
           "  OR EXISTS (" +
           "    SELECT 1 FROM Label lbl " +
           "    WHERE lbl.entity.id = e.id " +
           "    AND lbl.langue.code = :langCode " +
           "    AND LOWER(lbl.nom) = LOWER(:searchTerm)" +
           "  )" +
           ")")
    List<Entity> searchByCodeOrLabelExact(@Param("searchTerm") String searchTerm, 
                                          @Param("langCode") String langCode);
}

