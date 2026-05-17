package fr.cnrs.opentypo.infrastructure.persistence;

import fr.cnrs.opentypo.domain.entity.CaracteristiquePhysiqueMonnaie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Repository Spring Data JPA pour l'entité CaracteristiquePhysiqueMonnaie
 */
@Repository
public interface CaracteristiquePhysiqueMonnaieRepository extends JpaRepository<CaracteristiquePhysiqueMonnaie, Long> {

    @Query("SELECT cpm FROM CaracteristiquePhysiqueMonnaie cpm "
            + "LEFT JOIN FETCH cpm.materiaux "
            + "LEFT JOIN FETCH cpm.denomination "
            + "LEFT JOIN FETCH cpm.valeur "
            + "LEFT JOIN FETCH cpm.technique "
            + "WHERE cpm.entity.id = :entityId")
    Optional<CaracteristiquePhysiqueMonnaie> findByEntityIdForApi(@Param("entityId") Long entityId);

    @Modifying
    @Transactional
    @Query("DELETE FROM CaracteristiquePhysiqueMonnaie cpm WHERE cpm.entity.id = :entityId")
    void deleteByEntityId(@Param("entityId") Long entityId);
}

