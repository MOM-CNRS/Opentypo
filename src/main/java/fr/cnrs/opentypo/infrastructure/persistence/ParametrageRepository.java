package fr.cnrs.opentypo.infrastructure.persistence;

import fr.cnrs.opentypo.domain.entity.Entity;
import fr.cnrs.opentypo.domain.entity.Parametrage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Repository pour l'entité Parametrage (paramétrage OpenTheso par collection).
 */
@Repository
public interface ParametrageRepository extends JpaRepository<Parametrage, Long> {

    Optional<Parametrage> findByEntity(Entity entity);

    Optional<Parametrage> findByEntityId(Long entityId);

    @Modifying
    @Transactional
    @Query("DELETE FROM Parametrage par WHERE par.entity.id = :idEntity")
    void deleteByEntityId(@Param("idEntity") Long idEntity);

}
