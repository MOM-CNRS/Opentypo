package fr.cnrs.opentypo.infrastructure.persistence;

import fr.cnrs.opentypo.domain.entity.Entity;
import fr.cnrs.opentypo.domain.entity.Parametrage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository pour l'entité Parametrage (paramétrage OpenTheso par collection).
 */
@Repository
public interface ParametrageRepository extends JpaRepository<Parametrage, Long> {

    Optional<Parametrage> findByEntity(Entity entity);

    Optional<Parametrage> findByEntityId(Long entityId);
}
