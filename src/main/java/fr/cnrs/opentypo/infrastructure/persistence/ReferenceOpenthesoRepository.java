package fr.cnrs.opentypo.infrastructure.persistence;

import fr.cnrs.opentypo.domain.entity.ReferenceOpentheso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository Spring Data JPA pour l'entité referenceOpentheso
 */
@Repository
public interface ReferenceOpenthesoRepository extends JpaRepository<ReferenceOpentheso, Long> {

    /**
     * Trouve un référentiel par son code
     */
    Optional<ReferenceOpentheso> findByCode(String code);

    /**
     * Trouve tous les référentiels par code (insensible à la casse)
     */
    @Query("SELECT r FROM ReferenceOpentheso r WHERE LOWER(r.code) LIKE LOWER(CONCAT('%', :code, '%'))")
    List<ReferenceOpentheso> findByCodeContainingIgnoreCase(@Param("code") String code);

    /**
     * Trouve tous les référentiels par valeur (insensible à la casse)
     */
    @Query("SELECT r FROM ReferenceOpentheso r WHERE LOWER(r.valeur) LIKE LOWER(CONCAT('%', :valeur, '%'))")
    List<ReferenceOpentheso> findByValeurContainingIgnoreCase(@Param("valeur") String valeur);

    /**
     * Trouve toutes les références pour une entité et un code donnés
     */
    @Query("SELECT r FROM ReferenceOpentheso r WHERE r.entity.id = :entityId AND r.code = :code")
    List<ReferenceOpentheso> findByEntityIdAndCode(@Param("entityId") Long entityId, @Param("code") String code);

    /**
     * Trouve toutes les références pour une entité donnée
     */
    @Query("SELECT r FROM ReferenceOpentheso r WHERE r.entity.id = :entityId")
    List<ReferenceOpentheso> findByEntityId(@Param("entityId") Long entityId);
}

