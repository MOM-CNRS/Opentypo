package fr.cnrs.opentypo.infrastructure.persistence;

import fr.cnrs.opentypo.domain.entity.CaracteristiquePhysique;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Repository Spring Data JPA pour l'entité CaracteristiquePhysique
 */
@Repository
public interface CaracteristiquePhysiqueRepository extends JpaRepository<CaracteristiquePhysique, Long> {

    @Modifying
    @Transactional
    @Query("DELETE FROM CaracteristiquePhysique cp WHERE cp.entity.id = :entityId")
    void deleteByEntityId(@Param("entityId") Long entityId);
}

