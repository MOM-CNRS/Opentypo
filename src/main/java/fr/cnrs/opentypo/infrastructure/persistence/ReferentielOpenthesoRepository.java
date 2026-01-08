package fr.cnrs.opentypo.infrastructure.persistence;

import fr.cnrs.opentypo.domain.entity.ReferentielOpentheso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository Spring Data JPA pour l'entité ReferentielOpentheso
 */
@Repository
public interface ReferentielOpenthesoRepository extends JpaRepository<ReferentielOpentheso, Long> {

    /**
     * Trouve un référentiel par son code
     */
    Optional<ReferentielOpentheso> findByCode(String code);

    /**
     * Trouve tous les référentiels par code (insensible à la casse)
     */
    @Query("SELECT r FROM ReferentielOpentheso r WHERE LOWER(r.code) LIKE LOWER(CONCAT('%', :code, '%'))")
    List<ReferentielOpentheso> findByCodeContainingIgnoreCase(@Param("code") String code);

    /**
     * Trouve tous les référentiels par valeur (insensible à la casse)
     */
    @Query("SELECT r FROM ReferentielOpentheso r WHERE LOWER(r.valeur) LIKE LOWER(CONCAT('%', :valeur, '%'))")
    List<ReferentielOpentheso> findByValeurContainingIgnoreCase(@Param("valeur") String valeur);
}

