package fr.cnrs.opentypo.infrastructure.persistence;

import fr.cnrs.opentypo.domain.entity.CaracteristiquePhysiqueMonnaie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository Spring Data JPA pour l'entité CaracteristiquePhysiqueMonnaie
 */
@Repository
public interface CaracteristiquePhysiqueMonnaieRepository extends JpaRepository<CaracteristiquePhysiqueMonnaie, Long> {

    @Modifying
    @Query("DELETE FROM CaracteristiquePhysiqueMonnaie cpm WHERE cpm.entity.id = :entityId")
    void deleteByEntityId(@Param("entityId") Long entityId);
}

